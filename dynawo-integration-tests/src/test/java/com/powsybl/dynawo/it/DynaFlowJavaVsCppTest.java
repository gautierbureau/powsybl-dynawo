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
import com.powsybl.dynaflow.DynaFlowConfig;
import com.powsybl.dynaflow.DynaFlowJavaProvider;
import com.powsybl.dynaflow.DynaFlowParameters;
import com.powsybl.dynaflow.DynaFlowProvider;
import com.powsybl.dynawo.DynawoSimulationConfig;
import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManagerConstants;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.loadflow.LoadFlowRunParameters;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Runs the same network through the two DynaFlow load flows against a local DynaFlow Launcher install: the
 * C++ path ({@code DynaFlowProvider}, which shells out to {@code dynaflow-launcher}) and the Java path
 * ({@code DynaFlowJavaProvider}, which runs the {@code "DynaFlow"} mapping through Dynawo). Both should
 * converge to the same steady state; this pins the Java replacement against the launcher it replaces.
 * <p>
 * The test needs the real binaries, so it is skipped unless the install is present at {@link #INSTALL}
 * (override with {@code -Ddynaflow.install=/path/to/dynaflow-launcher}). It uses a local computation
 * manager, not the Docker one the other integration tests use.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
class DynaFlowJavaVsCppTest {

    private static final Path INSTALL = Path.of(System.getProperty("dynaflow.install",
            System.getProperty("user.home") + "/Projects/powsybl/powsybl-dynawo-mapping/DynaFlowLauncher_Linux_v1.7.0/dynaflow-launcher"));

    // voltages match the C++ launcher exactly on IEEE14; angles differ only by the small phase-reference
    // datum (the SignalN thetaRef / slack anchor), so these guard against a real divergence of the port
    private static final double V_TOLERANCE = 0.05;
    private static final double ANGLE_TOLERANCE = 0.5;

    @Test
    void theJavaPathConvergesLikeTheCppPath() throws Exception {
        assumeTrue(Files.exists(INSTALL.resolve("dynaflow-launcher.sh")) && Files.exists(INSTALL.resolve("dynawo.sh")),
                "local DynaFlow Launcher install required at " + INSTALL);

        try (LocalComputationManager computationManager = new LocalComputationManager()) {
            Network cppNetwork = IeeeCdfNetworkFactory.create14Solved();
            LoadFlowResult cppResult = runCpp(cppNetwork, computationManager);

            Network javaNetwork = IeeeCdfNetworkFactory.create14Solved();
            LoadFlowResult javaResult = runJava(javaNetwork, computationManager);

            assertEquals(LoadFlowResult.ComponentResult.Status.CONVERGED,
                    cppResult.getComponentResults().getFirst().getStatus(), "C++ DynaFlow should converge");
            assertEquals(LoadFlowResult.ComponentResult.Status.CONVERGED,
                    javaResult.getComponentResults().getFirst().getStatus(), "Java DynaFlow should converge");

            compareBusStates(cppNetwork, javaNetwork);
        }
    }

    private static LoadFlowResult runCpp(Network network, LocalComputationManager computationManager) {
        DynaFlowConfig config = new DynaFlowConfig(INSTALL, false);
        DynaFlowProvider provider = new DynaFlowProvider(() -> config);
        LoadFlowParameters parameters = new LoadFlowParameters()
                .setVoltageInitMode(LoadFlowParameters.VoltageInitMode.PREVIOUS_VALUES)
                .setReadSlackBus(false);
        parameters.addExtension(DynaFlowParameters.class, new DynaFlowParameters());
        return provider.run(network, VariantManagerConstants.INITIAL_VARIANT_ID, runParameters(computationManager, parameters)).join();
    }

    private static LoadFlowResult runJava(Network network, LocalComputationManager computationManager) {
        DynawoSimulationConfig config = new DynawoSimulationConfig(INSTALL, false);
        DynaFlowJavaProvider provider = new DynaFlowJavaProvider(() -> config);
        LoadFlowParameters parameters = new LoadFlowParameters()
                .setVoltageInitMode(LoadFlowParameters.VoltageInitMode.PREVIOUS_VALUES)
                .setReadSlackBus(false);
        return provider.run(network, VariantManagerConstants.INITIAL_VARIANT_ID, runParameters(computationManager, parameters)).join();
    }

    private static LoadFlowRunParameters runParameters(LocalComputationManager computationManager, LoadFlowParameters parameters) {
        return new LoadFlowRunParameters()
                .setComputationManager(computationManager)
                .setParameters(parameters)
                .setReportNode(ReportNode.NO_OP);
    }

    /** Every bus in the main component must reach the same voltage on both paths, within tolerance. */
    private static void compareBusStates(Network cppNetwork, Network javaNetwork) {
        double maxV = 0;
        double maxAngle = 0;
        String worstBus = "";
        for (Bus cppBus : cppNetwork.getBusView().getBuses()) {
            if (!cppBus.isInMainConnectedComponent()) {
                continue;
            }
            Bus javaBus = javaNetwork.getBusView().getBus(cppBus.getId());
            double vDiff = Math.abs(cppBus.getV() - javaBus.getV());
            double angleDiff = Math.abs(cppBus.getAngle() - javaBus.getAngle());
            if (vDiff > maxV) {
                maxV = vDiff;
                worstBus = cppBus.getId();
            }
            maxAngle = Math.max(maxAngle, angleDiff);
        }
        System.out.printf("DynaFlow C++ vs Java: max |ΔV| = %.4f kV (bus %s), max |Δangle| = %.4f deg%n", maxV, worstBus, maxAngle);
        assertTrue(maxV < V_TOLERANCE, "max bus voltage difference " + maxV + " kV exceeds " + V_TOLERANCE);
        assertTrue(maxAngle < ANGLE_TOLERANCE, "max bus angle difference " + maxAngle + " deg exceeds " + ANGLE_TOLERANCE);
    }
}
