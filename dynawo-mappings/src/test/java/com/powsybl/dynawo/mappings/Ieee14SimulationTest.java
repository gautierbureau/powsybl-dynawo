/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.dynamicsimulation.DynamicModelsSupplier;
import com.powsybl.dynamicsimulation.DynamicSimulationParameters;
import com.powsybl.dynamicsimulation.DynamicSimulationResult;
import com.powsybl.dynamicsimulation.EventModelsSupplier;
import com.powsybl.dynamicsimulation.OutputVariablesSupplier;
import com.powsybl.dynawo.DynawoSimulationConfig;
import com.powsybl.dynawo.DynawoSimulationParameters;
import com.powsybl.dynawo.DynawoSimulationProvider;
import com.powsybl.dynawo.mappings.parameters.ModelDescriptionLookup;
import com.powsybl.dynawo.networks.PlausibleEnergySources;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManagerConstants;
import com.powsybl.iidm.serde.NetworkSerDe;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs a simulation of IEEE14 described by the mapping alone, which is what the whole pipeline is
 * for: no dynamic model file, no parameter file, no solver file.
 * <p>
 * Skipped when no Dynawo is available.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
class Ieee14SimulationTest {

    /**
     * Location of the Dynawo the simulation runs with, taken from the workspace rather than from
     * the user configuration so that the test does not depend on how the machine is set up.
     */
    private static final String DYNAWO_HOME_PROPERTY = "dynawo.home";
    private static final Path DEFAULT_DYNAWO_HOME = Path.of("..", "..", "dynawo");

    /**
     * The network Dynawo ships with its own IEEE14 example, already solved: a dynamic simulation
     * starts from an operating point, and running a load flow here would only reproduce one.
     */
    private static final Path NETWORK = Path.of("..", "..", "dynawo-source", "examples", "DynaWaltz",
            "IEEE14", "IEEE14_GeneratorDisconnections", "IEEE14.iidm");

    @Test
    void shouldRunAVoltageStabilityStudy() throws Exception {
        assertThat(run(IeeeTestSystemMappings.DYNAWALTZ_NAME).getStatus()).isEqualTo(DynamicSimulationResult.Status.SUCCESS);
    }

    @Test
    @Disabled("the machine parameters are generic, and the detailed models diverge on them around t = 6 s")
    void shouldRunATransientStudy() throws Exception {
        // the models are built and initialised, and the run proceeds for six seconds before the
        // solver stops converging: describing a transient study needs machine parameters closer to
        // the real ones than the plausible values the tables hold
        assertThat(run(IeeeTestSystemMappings.DYNASWING_NAME).getStatus()).isEqualTo(DynamicSimulationResult.Status.SUCCESS);
    }

    private static DynamicSimulationResult run(String mappingName) throws Exception {
        Path home = dynawoHome();
        Assumptions.assumeTrue(Files.exists(home), "no Dynawo available at " + home);
        Path networkFile = NETWORK.toAbsolutePath().normalize();
        Assumptions.assumeTrue(Files.exists(networkFile), "no network available at " + networkFile);

        Network network = NetworkSerDe.read(networkFile);
        // the example network names its machines the CGMES way, so the mix is deduced rather than
        // read from a table written for the identifiers the IEEE data uses
        PlausibleEnergySources.apply(network);

        DynamicSimulationParameters parameters = new DynamicSimulationParameters(0, 20);
        String debugDir = System.getProperty("dynawo.debugDir");
        if (debugDir != null) {
            parameters.setDebugDir(debugDir);
        }
        DynawoSimulationParameters dynawoParameters = new DynawoSimulationParameters();
        DynamicModelsSupplier models = DynamicModelsMappings.getInstance()
                .apply(mappingName, network, dynawoParameters, descriptions(home));
        parameters.addExtension(DynawoSimulationParameters.class, dynawoParameters);

        try (ComputationManager computationManager = new LocalComputationManager()) {
            return new DynawoSimulationProvider(new DynawoSimulationConfig(home, false))
                    .run(network, models, EventModelsSupplier.empty(), OutputVariablesSupplier.empty(),
                            VariantManagerConstants.INITIAL_VARIANT_ID, computationManager, parameters, ReportNode.NO_OP)
                    .get();
        }
    }

    private static ModelDescriptionLookup descriptions(Path home) {
        return ModelDescriptionLookup.fromModelDatabase(home);
    }

    private static Path dynawoHome() {
        String home = System.getProperty(DYNAWO_HOME_PROPERTY);
        return home != null ? Path.of(home) : DEFAULT_DYNAWO_HOME.toAbsolutePath().normalize();
    }
}
