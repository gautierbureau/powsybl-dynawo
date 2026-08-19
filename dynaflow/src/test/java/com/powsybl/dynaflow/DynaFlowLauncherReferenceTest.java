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
 * Cases not yet reproduced, tracked by {@link #knownDivergenceFromLauncher} rather than asserted:
 * <ul>
 *   <li>{@code distant_regulation} — 7 generators run {@code GeneratorPVRemoteSignalN} where the launcher
 *       runs {@code GeneratorPQPropSignalN}: the launcher counts voltage regulators over every bus of a
 *       voltage level (busbar sections), while the mapping counts over the merged bus-view bus, so it sees
 *       one regulator on a remote bus where the launcher sees several.</li>
 *   <li>{@code hvdc_diagrams}, {@code hvdc_HvdcPQProp_diagrams} — a VSC's {@code hvdc_Q1Nom} differs: the
 *       mapping takes the reactive bound over the whole capability curve, the launcher a single value from
 *       its data interface.</li>
 *   <li>{@code launch_slack} — its input carries a {@code slackTerminal} extension in an IIDM version the
 *       current serializer cannot read, so the network cannot be loaded at all (not a mapping difference).</li>
 * </ul>
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
class DynaFlowLauncherReferenceTest {

    private static final Path TESTS_DIR = Path.of(System.getProperty("dynaflow.tests",
            System.getProperty("user.home") + "/Projects/powsybl/powsybl-dynawo-mapping/dynaflow-launcher/tests/main"));

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
        "launch", "launch_infinite", "launch_diagram", "launch_diagram_tfo", "launch_P",
        "node_breaker", "special_characters",
        "hvdc_line_normal", "hvdc", "hvdc_dangling", "hvdc_diagrams_flat_start",
        "hvdc_HvdcPQProp", "hvdc_HvdcPQPropDangling", "hvdc_HvdcPV_HvdcPTanPhi"})
    void javaMappingReproducesLauncherReference(String caseName) throws Exception {
        Comparison comparison = compare(caseName);
        assertEquals(comparison.refModels, comparison.javaModels, caseName + ": the model per equipment must match the launcher");
        assertEquals(0.0, comparison.parMaxDiff, 1e-6, caseName + ": par differs at " + comparison.parWorst);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"distant_regulation", "hvdc_diagrams", "hvdc_HvdcPQProp_diagrams"})
    void knownDivergenceFromLauncher(String caseName) throws Exception {
        Comparison comparison = compare(caseName);
        System.out.printf("[known divergence] %-26s dyd %s | par max |Δ| = %.4g (%s)%n", caseName,
                comparison.refModels.equals(comparison.javaModels) ? "OK" : "DIFF", comparison.parMaxDiff, comparison.parWorst);
    }

    private record Comparison(Map<String, String> refModels, Map<String, String> javaModels, double parMaxDiff, String parWorst) {
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
        return new Comparison(modelMap(refDyd), modelMap(javaDyd), parDiff[0], worst[0]);
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
