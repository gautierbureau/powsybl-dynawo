/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynaflow;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.LineContingency;
import com.powsybl.dynawo.algorithms.ContingencyEventModels;
import com.powsybl.dynawo.commons.DynawoConstants;
import com.powsybl.dynawo.models.BlackBoxModel;
import com.powsybl.dynawo.security.SecurityAnalysisContext;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.serde.NetworkSerDe;
import com.powsybl.security.dynamic.DynamicSecurityAnalysisParameters;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Phase A of the DynaFlow security analysis (see DYNAFLOW_SA_PLAN.md): checks that
 * {@link DynaFlowSecurityAnalysisJavaProvider} assembles its {@link SecurityAnalysisContext} the DynaFlow
 * way — the base models from the {@code "DynaFlow"} mapping (identical to the load flow), plus one
 * disconnection event model per contingency raised by the shared Dynawo security-analysis engine.
 * <p>
 * The context is built exactly as the provider's {@code run(...)} does, but without a Dynawo run (no binary
 * needed): a LINE contingency must yield a {@code EventQuadripoleDisconnection} event model
 * {@code Disconnect_<lineId>}, matching the launcher's {@code TestIIDM_launch-line_contingency.dyd}. The
 * context case reads the launcher's {@code main_sa} network and is skipped when those sources are absent.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
class DynaFlowSecurityAnalysisJavaProviderTest {

    private static final Path TESTS_DIR = Path.of(System.getProperty("dynaflow.sa.tests",
            System.getProperty("user.home") + "/Projects/powsybl/powsybl-dynawo-mapping/dynaflow-launcher/tests/main_sa"));

    @Test
    void theProviderIsNamedForRunningBesideTheCppLauncher() {
        DynaFlowSecurityAnalysisJavaProvider provider = new DynaFlowSecurityAnalysisJavaProvider(new com.powsybl.dynawo.algorithms.DynawoAlgorithmsConfig(Path.of("/dynawo"), false));
        assertEquals("DynaFlowJava", provider.getName());
        assertNotNull(provider.getVersion());
    }

    @Test
    void aLineContingencyRaisesAQuadripoleDisconnectionEvent() throws Exception {
        Path network = TESTS_DIR.resolve("res").resolve("TestIIDM_launch.iidm");
        assumeTrue(Files.exists(network), "launcher SA sources required at " + TESTS_DIR);

        Network net = NetworkSerDe.read(network);
        // the launcher's line_contingency (main_sa/res/contingencies_launch.json)
        String lineId = "_044cd006-c766-11e1-8775-005056c00008";
        Contingency contingency = new Contingency("line_contingency", new LineContingency(lineId));

        SecurityAnalysisContext context = buildContext(net, List.of(contingency));

        List<ContingencyEventModels> events = context.getContingencyEventModels();
        assertEquals(1, events.size(), "one contingency, one event-model group");
        List<BlackBoxModel> eventModels = events.get(0).eventModels();
        assertEquals(1, eventModels.size(), "one element, one disconnection event model");
        BlackBoxModel event = eventModels.get(0);
        assertEquals("EventQuadripoleDisconnection", event.getLib(),
                "a line contingency disconnects a branch, as the launcher does");
        assertEquals("Disconnect_" + lineId, event.getDynamicModelId(),
                "the event id matches the launcher's Disconnect_<lineId>");
    }

    /** Builds the SA context exactly as {@link DynaFlowSecurityAnalysisJavaProvider#run} does, minus the Dynawo run. */
    private static SecurityAnalysisContext buildContext(Network network, List<Contingency> contingencies) {
        DynaFlowJavaProvider.MappedInputs inputs = DynaFlowJavaProvider.buildMappedInputs(network, ReportNode.NO_OP, new DynaFlowParameters());
        return new SecurityAnalysisContext.Builder(network, inputs.blackBoxModels(), contingencies)
                .eventModels(List.of())
                .dynamicSecurityAnalysisParameters(new DynamicSecurityAnalysisParameters())
                .dynawoParameters(inputs.dynawoParameters())
                .currentVersion(DynawoConstants.VERSION_MIN)
                .reportNode(ReportNode.NO_OP)
                .build();
    }
}
