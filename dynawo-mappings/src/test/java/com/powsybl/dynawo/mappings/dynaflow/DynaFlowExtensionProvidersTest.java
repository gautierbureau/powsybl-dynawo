/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.dynaflow;

import com.powsybl.dynawo.mappings.DynamicMappingExtensions;
import com.powsybl.dynawo.mappings.MappedModelsSupplier.MappedModel;
import com.powsybl.dynawo.mappings.MappingParameters;
import com.powsybl.iidm.network.EnergySource;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TopologyKind;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.iidm.network.extensions.SecondaryVoltageControl;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * The two providers a study points at to declare the secondary voltage control DynaFlow cannot deduce:
 * one JSON writing the zones into the {@code secondaryVoltageControl} extension, one CSV naming the
 * {@code Rpcl2} machines on the {@code synchronizedGeneratorProperties} extension. Together they drive the
 * generator tree onto the control-loop models and the zone onto its control.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
class DynaFlowExtensionProvidersTest {

    @Test
    void theProvidersDeclareTheZoneAndTheSecondLoopFromTheirResources() {
        Network network = twoMachines();

        DynamicMappingExtensions.getInstance().createExtensions(network, SecondaryVoltageControl.NAME,
                DynaFlowSecondaryVoltageControlProvider.NAME,
                MappingParameters.of(Map.of("svc_zones_resource", "/svcZones.json")));
        DynamicMappingExtensions.getInstance().createExtensions(network,
                com.powsybl.dynawo.extensions.api.generator.SynchronizedGeneratorProperties.NAME,
                DynaFlowRpcl2Provider.NAME,
                MappingParameters.of(Map.of("rpcl2_generators_resource", "/rpcl2Generators.csv")));

        SecondaryVoltageControl svc = network.getExtension(SecondaryVoltageControl.class);
        assertNotNull(svc, "the JSON should have added the secondaryVoltageControl extension");
        assertEquals(1, svc.getControlZones().size());

        Map<String, String> libsById = new DynaFlowMapping(DynaFlowMapping.NAME).createModelConfigs(network).stream()
                .collect(Collectors.toMap(MappedModel::staticId, MappedModel::lib));
        assertEquals(DynaFlowGeneratorMapping.PV_RPCL_SIGNALN, libsById.get("GEN1"));
        assertEquals(DynaFlowGeneratorMapping.PV_RPCL2_SIGNALN, libsById.get("GEN2"));
        assertEquals("DYNModelSecondaryVoltageControlSimplified", libsById.get("ZONE"));
    }

    private static Network twoMachines() {
        Network network = Network.create("svc", "test");
        machine(network, "VL1", "B1", "GEN1");
        machine(network, "VL2", "B2", "GEN2");
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
