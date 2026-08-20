/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.it;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.contingency.Contingency;
import com.powsybl.dynaflow.DynaFlowConfig;
import com.powsybl.dynaflow.DynaFlowSecurityAnalysisJavaProvider;
import com.powsybl.dynaflow.DynaFlowSecurityAnalysisProvider;
import com.powsybl.dynamicsimulation.DynamicModelsSupplier;
import com.powsybl.dynamicsimulation.DynamicSimulationParameters;
import com.powsybl.dynawo.algorithms.DynawoAlgorithmsConfig;
import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManagerConstants;
import com.powsybl.security.SecurityAnalysisParameters;
import com.powsybl.security.SecurityAnalysisResult;
import com.powsybl.security.SecurityAnalysisRunParameters;
import com.powsybl.security.dynamic.DynamicSecurityAnalysisParameters;
import com.powsybl.security.dynamic.DynamicSecurityAnalysisRunParameters;
import com.powsybl.security.results.PostContingencyResult;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Pins the Java DynaFlow security analysis ({@code DynaFlowSecurityAnalysisJavaProvider}) against a local
 * DynaFlow Launcher install, at the result level:
 * <ul>
 *   <li>it builds the {@code "DynaFlow"} mapping's models, raises one disconnection per contingency, and runs
 *       the multi-scenario simulation through the real {@code dynawo-algorithms} binary, yielding a
 *       post-contingency result per contingency;</li>
 *   <li>it runs the same network and contingencies through the C++ launcher's own security analysis
 *       ({@code DynaFlowSecurityAnalysisProvider}, which shells out to {@code dynaflow-launcher}) and checks
 *       the two agree on the per-contingency computation status and the limit violations found.</li>
 * </ul>
 * On IEEE14 with line contingencies both paths converge with no violation; the violation comparison bites
 * once a case produces one (a criteria/limit-driven case is a further step). Where {@code
 * DynaFlowSaLauncherReferenceTest} pins the generated event models against the launcher's files, this pins
 * that they execute and reach the same security outcome.
 * <p>
 * The test needs the real binaries, so it is skipped unless the install is present at {@link #INSTALL}
 * (override with {@code -Ddynaflow.install=/path/to/dynaflow-launcher}). It uses a local computation manager,
 * not the Docker one the other integration tests use.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
class DynaFlowSaJavaVsCppTest {

    private static final Path INSTALL = Path.of(System.getProperty("dynaflow.install",
            System.getProperty("user.home") + "/Projects/powsybl/powsybl-dynawo-mapping/DynaFlowLauncher_Linux_v1.7.0/dynaflow-launcher"));

    @Test
    void theJavaSecurityAnalysisRunsEveryContingency() throws Exception {
        assumeTrue(Files.exists(INSTALL.resolve("dynawo-algorithms.sh")) && Files.exists(INSTALL.resolve("dynawo.sh")),
                "local DynaFlow Launcher install required at " + INSTALL);

        try (LocalComputationManager computationManager = new LocalComputationManager()) {
            Network network = IeeeCdfNetworkFactory.create14Solved();
            List<Contingency> contingencies = network.getLineStream().limit(2)
                    .map(line -> Contingency.line(line.getId()))
                    .toList();

            DynamicSecurityAnalysisParameters parameters = new DynamicSecurityAnalysisParameters()
                    .setDynamicSimulationParameters(new DynamicSimulationParameters(0, 100));
            DynamicSecurityAnalysisRunParameters runParameters = new DynamicSecurityAnalysisRunParameters()
                    .setComputationManager(computationManager)
                    .setDynamicSecurityAnalysisParameters(parameters)
                    .setReportNode(ReportNode.NO_OP);

            // the DynaFlow SA ignores the supplied models — they come from the "DynaFlow" mapping
            DynamicModelsSupplier noExtraModels = (n, r) -> List.of();
            SecurityAnalysisResult result = new DynaFlowSecurityAnalysisJavaProvider(new DynawoAlgorithmsConfig(INSTALL, false))
                    .run(network, VariantManagerConstants.INITIAL_VARIANT_ID, noExtraModels, n -> contingencies, runParameters)
                    .join()
                    .getResult();

            assertNotNull(result.getPreContingencyResult(), "the base case must be simulated");
            assertEquals(contingencies.size(), result.getPostContingencyResults().size(),
                    "every contingency must produce a post-contingency result");
            for (PostContingencyResult postContingencyResult : result.getPostContingencyResults()) {
                assertNotNull(postContingencyResult.getStatus(),
                        postContingencyResult.getContingency().getId() + ": the scenario must have run to a status");
            }
        }
    }

    @Test
    void theJavaSecurityAnalysisAgreesWithTheCppLauncher() throws Exception {
        assumeTrue(Files.exists(INSTALL.resolve("dynawo-algorithms.sh")) && Files.exists(INSTALL.resolve("dynaflow-launcher.sh")),
                "local DynaFlow Launcher install required at " + INSTALL);

        try (LocalComputationManager computationManager = new LocalComputationManager()) {
            Network cppNetwork = IeeeCdfNetworkFactory.create14Solved();
            List<Contingency> contingencies = cppNetwork.getLineStream().limit(4)
                    .map(line -> Contingency.line(line.getId()))
                    .toList();
            SecurityAnalysisResult cppResult = runCpp(cppNetwork, contingencies, computationManager);

            Network javaNetwork = IeeeCdfNetworkFactory.create14Solved();
            SecurityAnalysisResult javaResult = runJava(javaNetwork, contingencies, computationManager);

            Map<String, String> cppStatuses = statusByContingency(cppResult);
            Map<String, String> javaStatuses = statusByContingency(javaResult);
            System.out.println("DynaFlow SA C++  statuses: " + cppStatuses);
            System.out.println("DynaFlow SA Java statuses: " + javaStatuses);
            System.out.println("DynaFlow SA C++  violations: " + violationsByContingency(cppResult));
            System.out.println("DynaFlow SA Java violations: " + violationsByContingency(javaResult));

            assertEquals(cppStatuses.keySet(), javaStatuses.keySet(),
                    "both paths must produce a result for the same contingencies");
            assertEquals(cppStatuses, javaStatuses,
                    "the per-contingency computation status must agree with the launcher");
            assertEquals(violationsByContingency(cppResult), violationsByContingency(javaResult),
                    "the limit violations found per contingency must agree with the launcher");
        }
    }

    private static SecurityAnalysisResult runJava(Network network, List<Contingency> contingencies, LocalComputationManager computationManager) {
        DynamicSecurityAnalysisParameters parameters = new DynamicSecurityAnalysisParameters()
                .setDynamicSimulationParameters(new DynamicSimulationParameters(0, 100));
        DynamicSecurityAnalysisRunParameters runParameters = new DynamicSecurityAnalysisRunParameters()
                .setComputationManager(computationManager)
                .setDynamicSecurityAnalysisParameters(parameters)
                .setReportNode(ReportNode.NO_OP);
        DynamicModelsSupplier noExtraModels = (n, r) -> List.of();
        return new DynaFlowSecurityAnalysisJavaProvider(new DynawoAlgorithmsConfig(INSTALL, false))
                .run(network, VariantManagerConstants.INITIAL_VARIANT_ID, noExtraModels, n -> contingencies, runParameters)
                .join()
                .getResult();
    }

    private static SecurityAnalysisResult runCpp(Network network, List<Contingency> contingencies, LocalComputationManager computationManager) {
        SecurityAnalysisRunParameters runParameters = new SecurityAnalysisRunParameters()
                .setComputationManager(computationManager)
                .setSecurityAnalysisParameters(new SecurityAnalysisParameters())
                .setReportNode(ReportNode.NO_OP);
        return new DynaFlowSecurityAnalysisProvider(() -> new DynaFlowConfig(INSTALL, false))
                .run(network, VariantManagerConstants.INITIAL_VARIANT_ID, n -> contingencies, runParameters)
                .join()
                .getResult();
    }

    private static Map<String, String> statusByContingency(SecurityAnalysisResult result) {
        return result.getPostContingencyResults().stream()
                .collect(Collectors.toMap(r -> r.getContingency().getId(), r -> r.getStatus().name(), (a, b) -> a, TreeMap::new));
    }

    /** contingency id -> the sorted set of violated equipment and limit types, for a topology-level comparison. */
    private static Map<String, TreeSet<String>> violationsByContingency(SecurityAnalysisResult result) {
        return result.getPostContingencyResults().stream()
                .collect(Collectors.toMap(r -> r.getContingency().getId(),
                        r -> r.getLimitViolationsResult().getLimitViolations().stream()
                                .map(v -> v.getSubjectId() + "/" + v.getLimitType())
                                .collect(Collectors.toCollection(TreeSet::new)),
                        (a, b) -> a, TreeMap::new));
    }
}
