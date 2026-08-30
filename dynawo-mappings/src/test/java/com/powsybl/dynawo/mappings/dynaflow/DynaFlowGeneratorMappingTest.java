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
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TopologyKind;
import com.powsybl.iidm.network.VoltageLevel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The DynaFlow generator rule (the DynaFlow Launcher's {@code GeneratorDefinitionAlgorithm}, SVC / RPCL
 * branches aside): a machine that holds a voltage on a valid operating point runs one of the
 * {@code GeneratorPV*SignalN} / {@code GeneratorPQProp*SignalN} models — chosen by local vs remote
 * regulation, one vs several regulators, the transformer-voltage threshold and the reactive-diagram shape
 * — while a machine that regulates no voltage or has a degenerate diagram keeps the static network model.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
class DynaFlowGeneratorMappingTest {

    @Test
    void aHighVoltageLocalMachineRunsTheTransformerModel() {
        // 400 kV >= the 100 kV threshold: its transformer is assumed absent from the static model
        assertEquals(DynaFlowGeneratorMapping.PV_TFO_SIGNALN, libFor(localMachine(400)));
    }

    @Test
    void aLowVoltageLocalMachineRunsThePlainModel() {
        // 20 kV < the threshold: its transformer is in the static model, no Tfo variant
        assertEquals(DynaFlowGeneratorMapping.PV_SIGNALN, libFor(localMachine(20)));
    }

    @Test
    void aLocalMachineWithAGenuinePqCurveRunsTheDiagramModel() {
        Network network = network();
        Generator gen = machine(network, "VL", 20, "GEN", true);
        gen.newReactiveCapabilityCurve()
                .beginPoint().setP(0).setMinQ(-100).setMaxQ(100).endPoint()
                .beginPoint().setP(100).setMinQ(-40).setMaxQ(60).endPoint()
                .add();
        assertEquals(DynaFlowGeneratorMapping.PV_DIAGRAM_PQ_SIGNALN, libFor(network, "GEN"));
    }

    @Test
    void aMachineAloneOnARemoteBusRunsTheRemoteModel() {
        Network network = network();
        Generator gen = machine(network, "VL_GEN", 20, "GEN", true);
        Load anchor = anchorLoad(network, "VL_REG", 20, "LD");
        gen.setRegulatingTerminal(anchor.getTerminal());
        assertEquals(DynaFlowGeneratorMapping.PV_REMOTE_SIGNALN, libFor(network, "GEN"));
    }

    @Test
    void severalMachinesOnOneBusShareTheReactivePowerProportionally() {
        Network network = network();
        VoltageLevel vl = voltageLevel(network, "VL", 20);
        machine(vl, "GEN1", true);
        machine(vl, "GEN2", true);
        Map<String, String> libs = libsById(network);
        assertEquals(DynaFlowGeneratorMapping.PQ_PROP_SIGNALN, libs.get("GEN1"));
        assertEquals(DynaFlowGeneratorMapping.PQ_PROP_SIGNALN, libs.get("GEN2"));
    }

    @Test
    void aMachineRegulatingNoVoltageIsLeftOnTheNetworkModel() {
        assertNull(libFor(localMachine(400, false)));
    }

    @Test
    void aMachineOffItsOperatingPointIsLeftOnTheNetworkModel() {
        Network network = network();
        // -targetP = -200 falls below minP (0): the operating point is invalid
        Generator gen = machine(network, "VL", 20, "GEN", true);
        gen.setMinP(0).setMaxP(100).setTargetP(200);
        assertNull(libFor(network, "GEN"));
    }

    @Test
    void aMachineWithADegenerateDiagramIsLeftOnTheNetworkModel() {
        Network network = network();
        Generator gen = machine(network, "VL", 20, "GEN", true);
        gen.newMinMaxReactiveLimits().setMinQ(50).setMaxQ(50).add();
        assertNull(libFor(network, "GEN"));
    }

    @Test
    void theMappingIsRegisteredAndRunsTheSimplifiedSolver() {
        assertTrue(DynamicModelsMappings.getInstance().getMappingNames().contains(DynaFlowMapping.NAME),
                "the DynaFlow mapping should be reachable by name");
        assertEquals(DynawoSimulationParameters.SolverType.SIM,
                DynamicModelsMappings.getInstance().create(DynaFlowMapping.NAME, MappingParameters.empty()).getSolverType());
    }

    // --- helpers ---

    private static String libFor(Network network, String generatorId) {
        return libsById(network).get(generatorId);
    }

    private static Map<String, String> libsById(Network network) {
        List<MappedModel> models = new DynaFlowMapping(DynaFlowMapping.NAME).createModelConfigs(network);
        // every model here is a generator's, keyed by its static id, its parameter set named after it
        models.forEach(m -> assertEquals(m.staticId(), m.parameterSetId()));
        return models.stream().collect(Collectors.toMap(MappedModel::staticId, MappedModel::lib));
    }

    /** A single-generator network whose one machine regulates its own bus, so {@link #libFor} can read it. */
    private static Network localMachine(double nominalV) {
        return localMachine(nominalV, true);
    }

    private static Network localMachine(double nominalV, boolean voltageRegulatorOn) {
        Network network = network();
        machine(network, "VL", nominalV, "GEN", voltageRegulatorOn);
        return network;
    }

    private static String libFor(Network network) {
        return libsById(network).get("GEN");
    }

    private static Network network() {
        return Network.create("test", "test");
    }

    private static VoltageLevel voltageLevel(Network network, String id, double nominalV) {
        VoltageLevel vl = network.newSubstation().setId("S_" + id).add()
                .newVoltageLevel().setId(id).setNominalV(nominalV).setTopologyKind(TopologyKind.BUS_BREAKER).add();
        vl.getBusBreakerView().newBus().setId("B_" + id).add();
        return vl;
    }

    private static Generator machine(Network network, String vlId, double nominalV, String genId, boolean voltageRegulatorOn) {
        return machine(voltageLevel(network, vlId, nominalV), genId, voltageRegulatorOn);
    }

    private static Generator machine(VoltageLevel vl, String genId, boolean voltageRegulatorOn) {
        String busId = "B_" + vl.getId();
        return vl.newGenerator()
                .setId(genId)
                .setBus(busId)
                .setConnectableBus(busId)
                .setMinP(-1000)
                .setMaxP(1000)
                .setTargetP(500)
                .setEnergySource(EnergySource.OTHER)
                .setVoltageRegulatorOn(voltageRegulatorOn)
                .setTargetV(voltageRegulatorOn ? vl.getNominalV() : Double.NaN)
                .setTargetQ(voltageRegulatorOn ? Double.NaN : 0)
                .add();
    }

    private static Load anchorLoad(Network network, String vlId, double nominalV, String loadId) {
        VoltageLevel vl = voltageLevel(network, vlId, nominalV);
        String busId = "B_" + vl.getId();
        return vl.newLoad().setId(loadId).setBus(busId).setConnectableBus(busId).setP0(0).setQ0(0).add();
    }
}
