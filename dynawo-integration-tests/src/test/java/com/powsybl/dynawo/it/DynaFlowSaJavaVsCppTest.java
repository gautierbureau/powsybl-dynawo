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
import com.powsybl.dynaflow.DynaFlowSecurityAnalysisJavaProvider;
import com.powsybl.dynamicsimulation.DynamicModelsSupplier;
import com.powsybl.dynamicsimulation.DynamicSimulationParameters;
import com.powsybl.dynawo.algorithms.DynawoAlgorithmsConfig;
import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManagerConstants;
import com.powsybl.security.SecurityAnalysisResult;
import com.powsybl.security.dynamic.DynamicSecurityAnalysisParameters;
import com.powsybl.security.dynamic.DynamicSecurityAnalysisRunParameters;
import com.powsybl.security.results.PostContingencyResult;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Runs the Java DynaFlow security analysis ({@link DynaFlowSecurityAnalysisJavaProvider}) end to end against a
 * local DynaFlow Launcher install: it builds the {@code "DynaFlow"} mapping's models, raises one disconnection
 * per contingency, and runs the multi-scenario simulation through the real {@code dynawo-algorithms} binary.
 * Where {@link DynaFlowSaLauncherReferenceTest} pins the generated event models against the launcher's files,
 * this pins that they actually execute and yield a post-contingency result per contingency.
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
}
