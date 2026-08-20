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
import com.powsybl.contingency.BoundaryLineContingency;
import com.powsybl.contingency.BranchContingency;
import com.powsybl.contingency.BusbarSectionContingency;
import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyElement;
import com.powsybl.contingency.GeneratorContingency;
import com.powsybl.contingency.HvdcLineContingency;
import com.powsybl.contingency.LineContingency;
import com.powsybl.contingency.ShuntCompensatorContingency;
import com.powsybl.contingency.StaticVarCompensatorContingency;
import com.powsybl.contingency.ThreeWindingsTransformerContingency;
import com.powsybl.contingency.TwoWindingsTransformerContingency;
import com.powsybl.dynawo.algorithms.ContingencyEventModels;
import com.powsybl.dynawo.algorithms.xml.ContingenciesDydXml;
import com.powsybl.dynawo.algorithms.xml.ContingenciesParXml;
import com.powsybl.dynawo.commons.DynawoConstants;
import com.powsybl.dynawo.security.SecurityAnalysisContext;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.serde.NetworkSerDe;
import com.powsybl.security.dynamic.DynamicSecurityAnalysisParameters;
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
 * Checks the Java DynaFlow security analysis reproduces the DynaFlow Launcher's per-contingency disconnection
 * decision: for each contingency in the launcher's {@code main_sa} case it builds the {@code "DynaFlow"} SA
 * context the way {@link DynaFlowSecurityAnalysisJavaProvider} does, writes the contingency dyd/par with the
 * same Dynawo-algorithms writers the run uses (no Dynawo run), and compares to the launcher's committed
 * {@code TestIIDM_launch-<id>.dyd/.par}.
 * <p>
 * The comparison is on the launcher-logic-meaningful signature, not byte-identity: the disconnection model
 * chosen per equipment ({@code EventQuadripoleDisconnection} for a branch, {@code EventSetPointBoolean} for
 * an injection that has a dynamic model, {@code EventConnectedStatus} for a network-only one — the launcher's
 * dynamic-vs-network routing), which equipment it disconnects (the event id {@code Disconnect_<equipmentId>}),
 * and the instant it fires ({@code event_tEvent}). The exact wiring XML is deliberately not asserted: the Java
 * path reuses powsybl's own maintained event models (same Dynawo lib, but e.g. {@code event_state1_value} and
 * a {@code MC_EventBranchDisconnection} macro connector where the launcher writes {@code event_state1} and
 * {@code MC_EventQuadripoleDisconnection}). That the wiring drives a real Dynawo run correctly is covered by
 * the against-the-launcher integration test, not here.
 * <p>
 * Only the element types the shared disconnection engine already covers are listed here. The launcher's
 * network-level disconnections for a three-winding transformer, a dangling line and a busbar section are a
 * known gap (Phase C in DYNAFLOW_SA_PLAN.md). Skipped unless the launcher SA sources are present.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
class DynaFlowSaLauncherReferenceTest {

    private static final Path TESTS_DIR = Path.of(System.getProperty("dynaflow.sa.tests",
            System.getProperty("user.home") + "/Projects/powsybl/powsybl-dynawo-mapping/dynaflow-launcher/tests/main_sa"));

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
        "line_contingency", "two_windings_transformer_contingency", "branch_contingency",
        "generator_contingency", "static_var_compensator_contingency", "hvdcline_contingency",
        "shunt_compensator_contingency", "static_var_compensator_network_contingency",
        "busbarsection_contingency", "dangling_line_contingency"})
    void javaSaReproducesLauncherEvent(String contingencyId) throws Exception {
        Path res = TESTS_DIR.resolve("res");
        assumeTrue(Files.exists(res.resolve("TestIIDM_launch.iidm")), "launcher SA sources required at " + TESTS_DIR);

        Network network = NetworkSerDe.read(res.resolve("TestIIDM_launch.iidm"));
        Contingency contingency = loadContingency(res.resolve("contingencies_launch.json"), contingencyId);

        Path outDir = Files.createTempDirectory("java_sa_" + contingencyId);
        writeContingencyFiles(network, contingency, outDir);

        Path refDir = TESTS_DIR.resolve("reference").resolve("launch");
        assertEquals(eventLibs(refDir.resolve("TestIIDM_launch-" + contingencyId + ".dyd")),
                eventLibs(outDir.resolve(contingencyId + ".dyd")),
                contingencyId + ": the disconnection model per equipment must match the launcher");
        assertEquals(eventTimes(refDir.resolve("TestIIDM_launch-" + contingencyId + ".par")),
                eventTimes(outDir.resolve(contingencyId + ".par")),
                contingencyId + ": the event must fire at the launcher's time");
    }

    /** Writes the contingency's event dyd/par exactly as {@link DynaFlowSecurityAnalysisJavaProvider} does, minus the Dynawo run. */
    private static void writeContingencyFiles(Network network, Contingency contingency, Path outDir) throws Exception {
        DynaFlowJavaProvider.MappedInputs inputs = DynaFlowJavaProvider.buildMappedInputs(network, ReportNode.NO_OP, new DynaFlowParameters());
        SecurityAnalysisContext context = new SecurityAnalysisContext.Builder(network, inputs.blackBoxModels(), List.of(contingency))
                .eventModels(List.of())
                .dynamicSecurityAnalysisParameters(DynaFlowSecurityAnalysisJavaProvider.withDynaFlowDefaults(new DynamicSecurityAnalysisParameters()))
                .dynawoParameters(inputs.dynawoParameters())
                .currentVersion(DynawoConstants.VERSION_MIN)
                .reportNode(ReportNode.NO_OP)
                .build();
        List<ContingencyEventModels> events = context.getContingencyEventModels();
        ContingenciesDydXml.write(outDir, events);
        ContingenciesParXml.write(outDir, events);
    }

    /**
     * Reads the launcher's contingency JSON (which omits the {@code type} field powsybl's own loader wants)
     * and rebuilds the requested contingency, mapping each element's launcher type to a contingency element.
     */
    private static Contingency loadContingency(Path contingenciesFile, String contingencyId) throws Exception {
        JsonNode list = new ObjectMapper().readTree(contingenciesFile.toFile()).get("contingencies");
        for (JsonNode entry : list) {
            if (!contingencyId.equals(entry.get("id").asText())) {
                continue;
            }
            List<ContingencyElement> elements = new java.util.ArrayList<>();
            for (JsonNode element : entry.get("elements")) {
                elements.add(contingencyElement(element.get("type").asText(), element.get("id").asText()));
            }
            return new Contingency(contingencyId, elements);
        }
        throw new AssertionError("contingency " + contingencyId + " not found in " + contingenciesFile);
    }

    private static ContingencyElement contingencyElement(String type, String id) {
        return switch (type) {
            case "LINE" -> new LineContingency(id);
            case "BRANCH" -> new BranchContingency(id);
            case "TWO_WINDINGS_TRANSFORMER" -> new TwoWindingsTransformerContingency(id);
            case "THREE_WINDINGS_TRANSFORMER" -> new ThreeWindingsTransformerContingency(id);
            case "GENERATOR" -> new GeneratorContingency(id);
            case "STATIC_VAR_COMPENSATOR" -> new StaticVarCompensatorContingency(id);
            case "SHUNT_COMPENSATOR" -> new ShuntCompensatorContingency(id);
            case "HVDC_LINE" -> new HvdcLineContingency(id);
            case "BUSBAR_SECTION" -> new BusbarSectionContingency(id);
            // the tcb powsybl-core models a dangling line as a boundary line
            case "DANGLING_LINE" -> new BoundaryLineContingency(id);
            default -> throw new AssertionError("unhandled contingency element type " + type);
        };
    }

    // --- reference comparison: the disconnection model per equipment, and when the event fires ---

    // the launcher writes unprefixed tags ({@code <set>}/{@code <par>}); the powsybl writer prefixes them
    // with {@code dyn:} — both are matched, and values are compared as numbers (10 vs 10.0)
    private static final Pattern BLACK_BOX = Pattern.compile("<(?:dyn:)?blackBoxModel\\s+id=\"([^\"]*)\"\\s+lib=\"([^\"]*)\"");
    private static final Pattern PAR_SET = Pattern.compile("<(?:dyn:)?set\\s+id=\"([^\"]*)\"");
    private static final Pattern EVENT_TIME_PAR = Pattern.compile("<(?:dyn:)?par\\b[^>]*name=\"event_tEvent\"[^>]*/>");
    private static final Pattern PAR_VALUE = Pattern.compile("value=\"([^\"]*)\"");

    /** Maps each event model's id ({@code Disconnect_<equipmentId>}) to its Dynawo lib. */
    private static Map<String, String> eventLibs(Path dyd) throws Exception {
        Matcher bbm = BLACK_BOX.matcher(Files.readString(dyd));
        Map<String, String> libs = new TreeMap<>();
        while (bbm.find()) {
            libs.put(bbm.group(1), bbm.group(2));
        }
        return libs;
    }

    /** Maps each event parameter set id to its {@code event_tEvent}, as a number. */
    private static Map<String, Double> eventTimes(Path par) throws Exception {
        String xml = Files.readString(par);
        Map<String, Double> times = new TreeMap<>();
        for (String block : xml.split("<(?:dyn:)?set ")) {
            Matcher setMatcher = PAR_SET.matcher("<set " + block);
            Matcher timeMatcher = EVENT_TIME_PAR.matcher(block);
            if (setMatcher.find() && timeMatcher.find()) {
                Matcher valueMatcher = PAR_VALUE.matcher(timeMatcher.group());
                if (valueMatcher.find()) {
                    times.put(setMatcher.group(1), Double.parseDouble(valueMatcher.group(1)));
                }
            }
        }
        return times;
    }
}
