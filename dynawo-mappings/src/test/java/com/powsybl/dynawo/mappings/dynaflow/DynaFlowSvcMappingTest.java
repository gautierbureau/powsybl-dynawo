/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.dynaflow;

import com.powsybl.dynawo.extensions.api.generator.SynchronizedGeneratorPropertiesAdder;
import com.powsybl.dynawo.mappings.MappedModelsSupplier.MappedModel;
import com.powsybl.iidm.network.EnergySource;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TopologyKind;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.iidm.network.extensions.PilotPoint;
import com.powsybl.iidm.network.extensions.SecondaryVoltageControlAdder;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The secondary voltage control branches of the DynaFlow generator tree, and the control model itself: a
 * machine in a control zone runs a reactive-power-control-loop model — the second loop where a study
 * marks it so — and every zone gets one simplified secondary voltage control coordinating its machines.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
class DynaFlowSvcMappingTest {

    @Test
    void machinesInAZoneRunTheControlLoopModelAndTheZoneGetsItsControl() {
        Network network = twoMachineZone();
        List<MappedModel> models = new DynaFlowMapping(DynaFlowMapping.NAME).createModelConfigs(network);
        Map<String, String> libsById = models.stream().collect(Collectors.toMap(MappedModel::staticId, MappedModel::lib));

        // GEN1 is in the zone -> the plain control loop; GEN2 is flagged Rpcl2 -> the second loop
        assertEquals(DynaFlowGeneratorMapping.PV_RPCL_SIGNALN, libsById.get("GEN1"));
        assertEquals(DynaFlowGeneratorMapping.PV_RPCL2_SIGNALN, libsById.get("GEN2"));

        // the zone gets one simplified secondary voltage control, set up by a hand of its own
        MappedModel svc = models.stream().filter(m -> m.lib().equals(DynaFlowSvcMapping.LIB)).findFirst().orElseThrow();
        assertEquals("ZONE", svc.staticId());
        assertEquals("ZONE", svc.parameterSetId());
        assertEquals(svc.configurer() != null, true);
    }

    @Test
    void withoutTheZoneAMachineKeepsThePlainModel() {
        Network network = twoMachineZone();
        network.removeExtension(com.powsybl.iidm.network.extensions.SecondaryVoltageControl.class);
        network.getGenerator("GEN2").removeExtension(com.powsybl.dynawo.extensions.api.generator.SynchronizedGeneratorProperties.class);
        Map<String, String> libsById = new DynaFlowMapping(DynaFlowMapping.NAME).createModelConfigs(network).stream()
                .collect(Collectors.toMap(MappedModel::staticId, MappedModel::lib));
        // no zone: the machines fall back to the plain local model, and no SVC model is emitted
        assertEquals(DynaFlowGeneratorMapping.PV_SIGNALN, libsById.get("GEN1"));
        assertEquals(DynaFlowGeneratorMapping.PV_SIGNALN, libsById.get("GEN2"));
        assertEquals(false, libsById.containsValue(DynaFlowSvcMapping.LIB));
    }

    @Test
    void aZoneWhosePilotTheNetworkDoesNotHoldLeavesItsMachinesOnThePlainModel() {
        Network network = Network.create("svc", "test");
        machine(network, "VL1", "B1", "GEN1");
        machine(network, "VL2", "B2", "GEN2");
        network.newExtension(SecondaryVoltageControlAdder.class)
                .newControlZone()
                    .withName("ZONE")
                    // the pilot busbar section is not in the network, so the control cannot reach it
                    .newPilotPoint()
                        .withBusbarSectionIds(List.of("GHOST_BBS"))
                        .withBuses(List.of())
                        .withTargetV(20)
                        .add()
                    .newControlUnit().withId("GEN1").withParticipate(true).add()
                    .newControlUnit().withId("GEN2").withParticipate(true).add()
                    .add()
                .add();

        Map<String, String> libs = new DynaFlowMapping(DynaFlowMapping.NAME).createModelConfigs(network).stream()
                .collect(Collectors.toMap(MappedModel::staticId, MappedModel::lib));
        // the zone is dropped: no SVC model, and the machines stay plain (removeRpclFromModel)
        assertEquals(DynaFlowGeneratorMapping.PV_SIGNALN, libs.get("GEN1"));
        assertEquals(DynaFlowGeneratorMapping.PV_SIGNALN, libs.get("GEN2"));
        assertEquals(false, libs.containsValue(DynaFlowSvcMapping.LIB));
    }

    /** Two 20 kV machines, each alone on its bus, both in one control zone; GEN2 carries a second loop. */
    private static Network twoMachineZone() {
        Network network = Network.create("svc", "test");
        machine(network, "VL1", "B1", "GEN1");
        machine(network, "VL2", "B2", "GEN2");
        network.getGenerator("GEN2").newExtension(SynchronizedGeneratorPropertiesAdder.class)
                .withType("PV").withRpcl2(true).add();
        network.newExtension(SecondaryVoltageControlAdder.class)
                .newControlZone()
                    .withName("ZONE")
                    .newPilotPoint()
                        .withBuses(List.of(new PilotPoint.BusRef("VL1", "B1")))
                        .withTargetV(20)
                        .add()
                    .newControlUnit().withId("GEN1").withParticipate(true).add()
                    .newControlUnit().withId("GEN2").withParticipate(true).add()
                    .add()
                .add();
        return network;
    }

    private static void machine(Network network, String vlId, String busId, String genId) {
        VoltageLevel vl = network.newSubstation().setId("S_" + vlId).add()
                .newVoltageLevel().setId(vlId).setNominalV(20).setTopologyKind(TopologyKind.BUS_BREAKER).add();
        vl.getBusBreakerView().newBus().setId(busId).add();
        vl.newGenerator().setId(genId).setBus(busId).setConnectableBus(busId)
                .setMinP(-100).setMaxP(100).setTargetP(50).setTargetV(20)
                .setEnergySource(EnergySource.OTHER).setVoltageRegulatorOn(true).add();
    }
}
