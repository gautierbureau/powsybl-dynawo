/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.dynaflow;

import com.powsybl.dynawo.mappings.MappedModelsSupplier.MappedModel;
import com.powsybl.dynawo.parameters.ParametersSet;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TopologyKind;
import com.powsybl.iidm.network.VoltageLevel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * The DynaFlow load rule (the DynaFlow Launcher's {@code LoadDefinitionAlgorithm}): a load runs the
 * restorative model unless it is fictitious, below the distribution voltage or injecting nothing — then
 * it keeps the static network model. Every modelled load reads one shared parameter set.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
class DynaFlowLoadMappingTest {

    @Test
    void onlyAnInjectingTransmissionLoadRunsTheRestorativeModel() {
        Network network = Network.create("loads", "test");
        load(network, "VL_HV", 90, "L_HV", 10, 5, false);      // transmission, injecting -> modelled
        load(network, "VL_LV", 20, "L_LV", 10, 5, false);      // below 45 kV -> network
        load(network, "VL_Z", 90, "L_ZERO", 0, 0, false);      // injects nothing -> network
        load(network, "VL_F", 90, "L_FICT", 10, 5, true);      // fictitious -> network (not restored)

        List<MappedModel> models = new DynaFlowMapping(DynaFlowMapping.NAME).createModelConfigs(network);
        Map<String, MappedModel> byId = models.stream().collect(Collectors.toMap(MappedModel::staticId, m -> m));

        assertEquals(1, models.size());
        MappedModel modelled = byId.get("L_HV");
        assertEquals(DynaFlowLoadMapping.LIB, modelled.lib());
        assertEquals(DynaFlowLoadMapping.PARAMETER_SET, modelled.parameterSetId());
        assertFalse(byId.containsKey("L_LV"));
        assertFalse(byId.containsKey("L_ZERO"));
        assertFalse(byId.containsKey("L_FICT"));
    }

    @Test
    void theModelledLoadsShareOneParameterSet() {
        Network network = Network.create("loads", "test");
        load(network, "VL1", 90, "L1", 10, 5, false);
        load(network, "VL2", 90, "L2", 20, 8, false);

        List<ParametersSet> sets = new DynaFlowMapping(DynaFlowMapping.NAME).createParameters(network, null);
        assertEquals(1, sets.size());
        ParametersSet set = sets.get(0);
        assertEquals(DynaFlowLoadMapping.PARAMETER_SET, set.getId());
        Map<String, String> params = set.getParameters().values().stream()
                .collect(Collectors.toMap(p -> p.name(), p -> p.value()));
        assertEquals("1.5", params.get("load_Alpha"));
        assertEquals("2.5", params.get("load_Beta"));
        assertEquals("0.01", params.get("load_UDeadBandPu"));
        Map<String, String> refs = set.getReferences().values().stream()
                .collect(Collectors.toMap(r -> r.name(), r -> r.origName()));
        assertEquals("p0_pu", refs.get("load_P0Pu"));
        assertEquals("v_pu", refs.get("load_U0Pu"));   // warm start by default
    }

    private static void load(Network network, String vlId, double nominalV, String loadId,
                             double p0, double q0, boolean fictitious) {
        VoltageLevel vl = network.newSubstation().setId("S_" + vlId).add()
                .newVoltageLevel().setId(vlId).setNominalV(nominalV).setTopologyKind(TopologyKind.BUS_BREAKER).add();
        vl.getBusBreakerView().newBus().setId("B_" + vlId).add();
        Load load = vl.newLoad().setId(loadId).setBus("B_" + vlId).setConnectableBus("B_" + vlId)
                .setP0(p0).setQ0(q0).add();
        load.setFictitious(fictitious);
    }
}
