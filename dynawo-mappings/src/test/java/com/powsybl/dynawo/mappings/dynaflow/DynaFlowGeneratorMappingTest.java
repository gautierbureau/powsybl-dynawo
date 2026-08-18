/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.dynaflow;

import com.powsybl.dynawo.DynawoSimulationParameters;
import com.powsybl.dynawo.mappings.DynamicModelsMappings;
import com.powsybl.dynawo.mappings.MappedModelsSupplier.MappedModel;
import com.powsybl.dynawo.mappings.MappingParameters;
import com.powsybl.iidm.network.EnergySource;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TopologyKind;
import com.powsybl.iidm.network.VoltageLevel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The start of the DynaFlow generator rule: a machine that holds a voltage on a valid operating point
 * runs the plain {@code GeneratorPVSignalN}, a machine that regulates no voltage keeps the static
 * network model. The mapping is registered and reachable by its name.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
class DynaFlowGeneratorMappingTest {

    @Test
    void aVoltageHoldingMachineRunsThePvSignalNModel() {
        Network network = oneMachine(true);
        List<MappedModel> models = new DynaFlowMapping(DynaFlowMapping.NAME).createModelConfigs(network);

        assertEquals(1, models.size());
        assertEquals("GEN", models.get(0).staticId());
        assertEquals(DynaFlowGeneratorMapping.SIGNALN_INFINITE, models.get(0).lib());
        assertEquals(DynaFlowGeneratorMapping.SIGNALN_GENERATOR_SET, models.get(0).parameterSetId());
    }

    @Test
    void aMachineRegulatingNoVoltageIsLeftOnTheNetworkModel() {
        Network network = oneMachine(false);
        assertTrue(new DynaFlowMapping(DynaFlowMapping.NAME).createModelConfigs(network).isEmpty(),
                "a machine that holds no voltage runs no dynamic model");
    }

    /** A one-bus network with a single machine, holding a voltage or not. */
    private static Network oneMachine(boolean voltageRegulatorOn) {
        Network network = Network.create("test", "test");
        VoltageLevel voltageLevel = network.newSubstation().setId("S").add()
                .newVoltageLevel().setId("VL").setNominalV(400).setTopologyKind(TopologyKind.BUS_BREAKER).add();
        voltageLevel.getBusBreakerView().newBus().setId("B").add();
        // limits bracket -targetP (-500), so the DynaFlow gate holds when the machine regulates voltage
        voltageLevel.newGenerator()
                .setId("GEN")
                .setBus("B")
                .setConnectableBus("B")
                .setMinP(-1000)
                .setMaxP(1000)
                .setTargetP(500)
                .setEnergySource(EnergySource.OTHER)
                .setVoltageRegulatorOn(voltageRegulatorOn)
                .setTargetV(voltageRegulatorOn ? 400 : Double.NaN)
                .setTargetQ(voltageRegulatorOn ? Double.NaN : 0)
                .add();
        return network;
    }

    @Test
    void theMappingIsRegisteredAndRunsTheSimplifiedSolver() {
        assertTrue(DynamicModelsMappings.getInstance().getMappingNames().contains(DynaFlowMapping.NAME),
                "the DynaFlow mapping should be reachable by name");
        assertEquals(DynawoSimulationParameters.SolverType.SIM,
                DynamicModelsMappings.getInstance().create(DynaFlowMapping.NAME, MappingParameters.empty()).getSolverType());
    }
}
