/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynaflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.dynamicsimulation.DynamicSimulationParameters;
import com.powsybl.dynawo.DynawoFilesUtils;
import com.powsybl.dynawo.DynawoSimulationContext;
import com.powsybl.dynawo.DynawoSimulationParameters;
import com.powsybl.dynawo.commons.DynawoConstants;
import com.powsybl.dynawo.mappings.DynamicModelsMapping;
import com.powsybl.dynawo.mappings.DynamicModelsMappings;
import com.powsybl.dynawo.mappings.MappedModelsSupplier;
import com.powsybl.dynawo.mappings.MappedModelsSupplier.MappedModel;
import com.powsybl.dynawo.mappings.MappingParameters;
import com.powsybl.dynawo.mappings.dynaflow.DynaFlowGlobalParameters;
import com.powsybl.dynawo.mappings.dynaflow.DynaFlowMapping;
import com.powsybl.dynawo.models.BlackBoxModel;
import com.powsybl.dynawo.models.utils.BlackBoxSupplierUtils;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.serde.NetworkSerDe;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Checks the Java DynaFlow mapping reproduces the DynaFlow Launcher's own reference outputs: for each of the
 * launcher's test cases it reads the case network and configuration, generates the dyd and par the way the
 * "DynaFlow" mapping would (no Dynawo run), and compares them to the launcher's committed reference files —
 * the model chosen per equipment (dyd) and the numeric parameters each reads (par).
 * <p>
 * Only the cases the generic mapping covers are listed; the launcher's SVC / assembling-database cases (a
 * {@code SettingPath} / {@code AssemblingPath} in their config) drive RPCL models from the RTE dynamic
 * database, which this mapping does not read. Skipped unless the launcher sources are present at {@link
 * #TESTS_DIR}.
 * <p>
 * {@code launch_slack} and {@code launch_kGover} are left out: their inputs carry a {@code slackTerminal} /
 * {@code activePowerControl} extension in an IIDM version the current serializer cannot read, so the network
 * cannot be loaded at all (not a mapping difference).
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
class DynaFlowLauncherReferenceTest {

    private static final Path TESTS_DIR = Path.of(System.getProperty("dynaflow.tests",
            System.getProperty("user.home") + "/Projects/powsybl/powsybl-dynawo-mapping/dynaflow-launcher/tests/main"));

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
        "launch", "launch_infinite", "launch_diagram", "launch_diagram_tfo", "launch_P",
        "node_breaker", "special_characters", "distant_regulation", "no_SVarC_regulation",
        "hvdc_line_normal", "hvdc", "hvdc_dangling", "hvdc_diagrams", "hvdc_diagrams_flat_start",
        "hvdc_HvdcPQProp", "hvdc_HvdcPQProp_diagrams", "hvdc_HvdcPQPropDangling", "hvdc_HvdcPQPropDangling_diagrams",
        "hvdc_HvdcPQProp_multiple_bus", "hvdc_HvdcPQPropSwitch", "hvdc_HvdcPV_HvdcPTanPhi",
        "hvdc_HvdcPV_HvdcPTanPhi_diagrams", "hvdc_HvdcPVDangling_HvdcPTanPhiDangling",
        "hvdc_HvdcPVDangling_HvdcPTanPhiDangling_diagrams"})
    void javaMappingReproducesLauncherReference(String caseName) throws Exception {
        Comparison comparison = compare(caseName);
        assertEquals(comparison.refModels, comparison.javaModels, caseName + ": the model per equipment must match the launcher");
        assertEquals(comparison.refAutomatons, comparison.javaAutomatons, caseName + ": the automaton models must match the launcher");
        assertEquals(0.0, comparison.parMaxDiff, 1e-6, caseName + ": par differs at " + comparison.parWorst);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"launch_svc", "launch_svc_infinite", "launch_svc_tfo", "launch_svc_tfo_infinite",
        "launch_svc_network"})
    void svcCaseReproducesViaExtensions(String caseName) throws Exception {
        Path res = TESTS_DIR.resolve("res");
        assumeTrue(Files.exists(res.resolve("TestIIDM_" + caseName + ".iidm")), "launcher sources required at " + TESTS_DIR);
        Network network = NetworkSerDe.read(res.resolve("TestIIDM_" + caseName + ".iidm"));
        // drive the SVC through the extensions instead of the RTE assembling/setting database
        applyAssembling(network, res.resolve("assembling_svc.xml"));

        MappingParameters mappingParameters = configToMappingParameters(res.resolve("config_" + caseName + ".json"));
        Path outDir = Files.createTempDirectory("java_svc_" + caseName);
        generateDydPar(network, mappingParameters, outDir);

        // the extensions must reproduce the launcher's SVC model selection: the reactive-power-control-loop
        // generator models, the Rpcl2 machine and the secondary voltage control model itself
        Path refDyd = TESTS_DIR.resolve("reference").resolve(caseName).resolve("TestIIDM_" + caseName + ".dyd");
        Path javaDyd = outDir.resolve("powsybl_dynawo.dyd");
        assertEquals(modelMap(refDyd), modelMap(javaDyd), caseName + ": the SVC generator model selection must match the launcher");
        assertEquals(automatonLibs(refDyd), automatonLibs(javaDyd), caseName + ": the SVC model itself must match the launcher");
    }

    /** Translates the launcher's SVC assembling database into the {@code SecondaryVoltageControl} and
     *  {@code SynchronizedGeneratorProperties} extensions the mapping reads. */
    private static void applyAssembling(Network network, Path assemblingFile) throws Exception {
        String xml = Files.readString(assemblingFile);
        Map<String, String> pilotVl = new java.util.HashMap<>();
        Map<String, String> generatorOfAssociation = new java.util.HashMap<>();
        Matcher association = Pattern.compile("<singleAssociation id=\"([^\"]*)\">(.*?)</singleAssociation>", Pattern.DOTALL).matcher(xml);
        while (association.find()) {
            String vl = attribute(association.group(2), "voltageLevel");
            String generator = attribute(association.group(2), "name");
            if (vl != null) {
                pilotVl.put(association.group(1), vl);
            }
            if (generator != null) {
                generatorOfAssociation.put(association.group(1), generator);
            }
        }
        String pilotBus = network.getVoltageLevel(pilotVl.values().iterator().next()).getBusBreakerView()
                .getBusStream().findFirst().orElseThrow().getId();

        com.powsybl.iidm.network.extensions.SecondaryVoltageControlAdder svc =
                network.newExtension(com.powsybl.iidm.network.extensions.SecondaryVoltageControlAdder.class);
        var zone = svc.newControlZone().withName("SVC");
        zone.newPilotPoint().withBusbarSectionIds(List.of()).withTargetV(1.0)
                .withBuses(List.of(new com.powsybl.iidm.network.extensions.PilotPoint.BusRef(pilotVl.values().iterator().next(), pilotBus))).add();
        for (String generatorId : generatorOfAssociation.values()) {
            zone.newControlUnit().withId(generatorId).withParticipate(true).add();
        }
        zone.add();
        svc.add();

        // the ReactivePowerControlLoop2 property marks the Rpcl2 machines
        Matcher rpcl2 = Pattern.compile("<property id=\"ReactivePowerControlLoop2\">(.*?)</property>", Pattern.DOTALL).matcher(xml);
        if (rpcl2.find()) {
            Matcher device = Pattern.compile("<device id=\"([^\"]*)\"").matcher(rpcl2.group(1));
            while (device.find()) {
                String generatorId = generatorOfAssociation.get(device.group(1));
                if (generatorId != null) {
                    network.getGenerator(generatorId)
                            .newExtension(com.powsybl.dynawo.extensions.api.generator.SynchronizedGeneratorPropertiesAdder.class)
                            .withType("PV").withRpcl2(true).add();
                }
            }
        }
    }

    @org.junit.jupiter.api.Test
    void phaseShifterDeductionReproducesTheLauncherWhenEnabled() throws Exception {
        Path res = TESTS_DIR.resolve("res");
        assumeTrue(Files.exists(res.resolve("TestIIDM_phase_shifter.iidm")), "launcher sources required at " + TESTS_DIR);
        Network network = NetworkSerDe.read(res.resolve("TestIIDM_phase_shifter.iidm"));
        // deducing phase shifters is off by default (it over-produces against the launcher); turn it on here
        MappingParameters mappingParameters = MappingParameters.of(Map.of("dynaflow_phase_shifter_regulation_on", "true"));
        Path outDir = Files.createTempDirectory("java_ps");
        generateDydPar(network, mappingParameters, outDir);

        Path refDyd = TESTS_DIR.resolve("reference").resolve("phase_shifter").resolve("TestIIDM_phase_shifter.dyd");
        Path javaDyd = outDir.resolve("powsybl_dynawo.dyd");
        assertEquals(modelMap(refDyd), modelMap(javaDyd), "phase_shifter: the equipment models must match the launcher");
        assertEquals(automatonLibs(refDyd), automatonLibs(javaDyd), "phase_shifter: the deduced phase shifters must match the launcher");
    }

    private record Comparison(Map<String, String> refModels, Map<String, String> javaModels,
                              Map<String, Long> refAutomatons, Map<String, Long> javaAutomatons,
                              double parMaxDiff, String parWorst) {
    }

    private static Comparison compare(String caseName) throws Exception {
        Path res = TESTS_DIR.resolve("res");
        Path reference = TESTS_DIR.resolve("reference").resolve(caseName);
        assumeTrue(Files.exists(res.resolve("TestIIDM_" + caseName + ".iidm")), "launcher sources required at " + TESTS_DIR);

        Network network = NetworkSerDe.read(res.resolve("TestIIDM_" + caseName + ".iidm"));
        MappingParameters mappingParameters = configToMappingParameters(res.resolve("config_" + caseName + ".json"));
        Path outDir = Files.createTempDirectory("java_ref_" + caseName);
        generateDydPar(network, mappingParameters, outDir);

        Path refDyd = reference.resolve("TestIIDM_" + caseName + ".dyd");
        Path javaDyd = outDir.resolve("powsybl_dynawo.dyd");
        double[] parDiff = new double[] {0};
        String[] worst = new String[] {""};
        comparePar(refDyd, reference.resolve("TestIIDM_" + caseName + ".par"), javaDyd, outDir.resolve("models.par"), parDiff, worst);
        return new Comparison(modelMap(refDyd), modelMap(javaDyd), automatonLibs(refDyd), automatonLibs(javaDyd), parDiff[0], worst[0]);
    }

    private static void generateDydPar(Network network, MappingParameters mappingParameters, Path outDir) throws Exception {
        DynamicModelsMapping mapping = DynamicModelsMappings.getInstance().create(DynaFlowMapping.NAME, mappingParameters);
        mapping.createExtensions(network);
        List<MappedModel> models = mapping.createModelConfigs(network, ReportNode.NO_OP);
        List<BlackBoxModel> blackBoxModels = BlackBoxSupplierUtils.getBlackBoxModelList(
                new MappedModelsSupplier(models), network, ReportNode.NO_OP);
        DynawoSimulationParameters dynawoParameters = new DynawoSimulationParameters()
                .setNetworkParameters(DynaFlowGlobalParameters.networkParameters(mappingParameters))
                .setSolverParameters(DynaFlowGlobalParameters.solverParameters(mappingParameters))
                .setSolverType(mapping.getSolverType())
                .setModelsParameters(mapping.createParameters(network, null));
        DynawoSimulationContext context = new DynawoSimulationContext.Builder(network, blackBoxModels)
                .dynamicSimulationParameters(new DynamicSimulationParameters(0, 100))
                .dynawoParameters(dynawoParameters)
                .currentVersion(DynawoConstants.VERSION_MIN)
                .reportNode(ReportNode.NO_OP)
                .build();
        DynawoFilesUtils.writeInputFiles(outDir, context);
    }

    private static MappingParameters configToMappingParameters(Path configFile) throws Exception {
        JsonNode config = new ObjectMapper().readTree(configFile.toFile()).get("dfl-config");
        Map<String, String> values = new TreeMap<>();
        put(config, "InfiniteReactiveLimits", "dynaflow_infinite_reactive_limits", values);
        put(config, "DsoVoltageLevel", "dynaflow_dso_voltage_level", values);
        put(config, "TfoVoltageLevel", "dynaflow_tfo_voltage_level", values);
        put(config, "ActivePowerCompensation", "dynaflow_active_power_compensation", values);
        put(config, "StartingPointMode", "dynaflow_starting_point_mode", values);
        put(config, "SVCRegulationOn", "dynaflow_svc_regulation_on", values);
        return MappingParameters.of(values);
    }

    private static void put(JsonNode config, String field, String key, Map<String, String> values) {
        if (config != null && config.has(field)) {
            values.put(key, config.get(field).asText());
        }
    }

    // --- dyd / par comparison (a model per equipment, and the numeric parameters it reads) ---

    private static Map<String, String> modelMap(Path dyd) throws Exception {
        Map<String, String> models = new TreeMap<>();
        Matcher tag = Pattern.compile("<dyn:blackBoxModel\\b([^>]*)>").matcher(Files.readString(dyd));
        while (tag.find()) {
            String staticId = attribute(tag.group(1), "staticId");
            String lib = attribute(tag.group(1), "lib");
            if (staticId != null && lib != null) {
                models.put(staticId, lib);
            }
        }
        return models;
    }

    /**
     * The pure-dynamic models keyed by their library, counted — the automatons, the remote-voltage-control
     * coordinators and the frequency signal, which carry no static id. Compared as a multiset because their
     * ids differ between the two tools (the launcher names them from its database, the mapping deduces them).
     */
    private static Map<String, Long> automatonLibs(Path dyd) throws Exception {
        Map<String, Long> libs = new TreeMap<>();
        Matcher tag = Pattern.compile("<dyn:blackBoxModel\\b([^>]*)>").matcher(Files.readString(dyd));
        while (tag.find()) {
            String lib = attribute(tag.group(1), "lib");
            if (attribute(tag.group(1), "staticId") == null && lib != null) {
                libs.merge(lib, 1L, Long::sum);
            }
        }
        return libs;
    }

    private static void comparePar(Path refDyd, Path refPar, Path javaDyd, Path javaPar, double[] maxDiff, String[] worst) throws Exception {
        Map<String, String> refParId = parIdByStaticId(refDyd);
        Map<String, String> javaParId = parIdByStaticId(javaDyd);
        Map<String, Map<String, Double>> refSets = numericParSets(refPar);
        Map<String, Map<String, Double>> javaSets = numericParSets(javaPar);
        for (String staticId : modelMap(refDyd).keySet()) {
            Map<String, Double> refValues = set(refSets, refParId.get(staticId));
            Map<String, Double> javaValues = set(javaSets, javaParId.get(staticId));
            for (String name : refValues.keySet()) {
                if (javaValues.containsKey(name)) {
                    double diff = Math.abs(refValues.get(name) - javaValues.get(name));
                    if (diff > maxDiff[0]) {
                        maxDiff[0] = diff;
                        worst[0] = staticId + "." + name;
                    }
                }
            }
        }
    }

    /** A model's numeric parameter set, or empty when it (or its parId) is absent — a diverging model. */
    private static Map<String, Double> set(Map<String, Map<String, Double>> sets, String parId) {
        return parId == null ? Map.of() : sets.getOrDefault(parId, Map.of());
    }

    private static Map<String, String> parIdByStaticId(Path dyd) throws Exception {
        Map<String, String> parIds = new TreeMap<>();
        Matcher tag = Pattern.compile("<dyn:blackBoxModel\\b([^>]*)>").matcher(Files.readString(dyd));
        while (tag.find()) {
            String staticId = attribute(tag.group(1), "staticId");
            String parId = attribute(tag.group(1), "parId");
            if (staticId != null && parId != null) {
                parIds.put(staticId, parId);
            }
        }
        return parIds;
    }

    private static Map<String, Map<String, Double>> numericParSets(Path par) throws Exception {
        String content = Files.readString(par);
        Map<String, Map<String, Double>> macros = new TreeMap<>();
        Matcher macroMatcher = Pattern.compile("<macroParameterSet\\s+id=\"([^\"]*)\">(.*?)</macroParameterSet>", Pattern.DOTALL).matcher(content);
        while (macroMatcher.find()) {
            macros.put(macroMatcher.group(1), numericPars(macroMatcher.group(2)));
        }
        Map<String, Map<String, Double>> sets = new TreeMap<>();
        Matcher setMatcher = Pattern.compile("<set\\s+id=\"([^\"]*)\">(.*?)</set>", Pattern.DOTALL).matcher(content);
        while (setMatcher.find()) {
            Map<String, Double> values = new TreeMap<>(numericPars(setMatcher.group(2)));
            Matcher macroRef = Pattern.compile("<macroParSet\\s+id=\"([^\"]*)\"").matcher(setMatcher.group(2));
            while (macroRef.find()) {
                values.putAll(macros.getOrDefault(macroRef.group(1), Map.of()));
            }
            sets.put(setMatcher.group(1), values);
        }
        return sets;
    }

    private static Map<String, Double> numericPars(String body) {
        Map<String, Double> values = new TreeMap<>();
        Matcher parMatcher = Pattern.compile("<par\\b([^>]*)/>").matcher(body);
        while (parMatcher.find()) {
            String name = attribute(parMatcher.group(1), "name");
            String value = attribute(parMatcher.group(1), "value");
            if (name != null && value != null) {
                try {
                    values.put(name, Double.parseDouble(value));
                } catch (NumberFormatException e) {
                    // string parameter (e.g. a diagram table name) — skip
                }
            }
        }
        return values;
    }

    private static String attribute(String attrs, String name) {
        Matcher m = Pattern.compile("\\b" + name + "=\"([^\"]*)\"").matcher(attrs);
        return m.find() ? m.group(1) : null;
    }
}
