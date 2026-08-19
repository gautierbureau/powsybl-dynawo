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
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.StaticVarCompensator;
import com.powsybl.iidm.network.StaticVarCompensator.RegulationMode;
import com.powsybl.iidm.network.TopologyKind;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.iidm.network.extensions.StandbyAutomatonAdder;
import com.powsybl.iidm.network.extensions.VoltagePerReactivePowerControlAdder;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The DynaFlow static var compensator rule (the DynaFlow Launcher's {@code SVarCDefinitionAlgorithm}): a
 * compensator holding no voltage keeps the static network model, and one that does runs a flavour of the
 * {@code StaticVarCompensatorPV} family read from the network — {@code Prop} for a slope, {@code Remote}
 * for a distant regulated bus, {@code ModeHandling} for a standby automaton.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
class DynaFlowSvarcMappingTest {

    @Test
    void onlyVoltageRegulatingCompensatorsAreModelledAndTheirFlavourComposes() {
        Network network = Network.create("svarcs", "test");
        VoltageLevel vl = busBreakerVl(network, "VL", 225);
        VoltageLevel vlRemote = busBreakerVl(network, "VL_R", 90);

        svarc(vl, "SVC_OFF", false, RegulationMode.REACTIVE_POWER);     // not regulating voltage -> NETWORK
        svarc(vl, "SVC_PV", true, RegulationMode.VOLTAGE);              // plain PV
        StaticVarCompensator prop = svarc(vl, "SVC_PROP", true, RegulationMode.VOLTAGE);
        prop.newExtension(VoltagePerReactivePowerControlAdder.class).withSlope(0.01).add();
        StaticVarCompensator mode = svarc(vl, "SVC_MODE", true, RegulationMode.VOLTAGE);
        mode.newExtension(StandbyAutomatonAdder.class).withStandbyStatus(true).withB0(-0.5)
                .withLowVoltageSetpoint(218).withHighVoltageSetpoint(232)
                .withLowVoltageThreshold(210).withHighVoltageThreshold(240).add();
        StaticVarCompensator remote = svarc(vl, "SVC_REMOTE", true, RegulationMode.VOLTAGE);
        remote.setRegulatingTerminal(remoteTerminal(vlRemote));

        List<MappedModel> models = new DynaFlowMapping(DynaFlowMapping.NAME).createModelConfigs(network);
        Map<String, String> libById = models.stream().collect(Collectors.toMap(MappedModel::staticId, MappedModel::lib));

        assertFalse(libById.containsKey("SVC_OFF"));
        assertEquals("StaticVarCompensatorPV", libById.get("SVC_PV"));
        assertEquals("StaticVarCompensatorPVProp", libById.get("SVC_PROP"));
        assertEquals("StaticVarCompensatorPVModeHandling", libById.get("SVC_MODE"));
        assertEquals("StaticVarCompensatorPVRemote", libById.get("SVC_REMOTE"));
    }

    @Test
    void aModeHandlingCompensatorReadsItsOwnSetWithTheThresholds() {
        Network network = Network.create("svarcs", "test");
        VoltageLevel vl = busBreakerVl(network, "VL", 225);
        StaticVarCompensator svarc = svarc(vl, "SVC", true, RegulationMode.VOLTAGE); // Bmin -0.02, Bmax 0.02, vSet 226
        svarc.newExtension(StandbyAutomatonAdder.class).withStandbyStatus(true).withB0(-0.5)
                .withLowVoltageSetpoint(218).withHighVoltageSetpoint(232)
                .withLowVoltageThreshold(210).withHighVoltageThreshold(240).add();

        List<ParametersSet> sets = new DynaFlowMapping(DynaFlowMapping.NAME).createParameters(network, null);
        assertEquals(1, sets.size());
        ParametersSet set = sets.get(0);
        assertEquals("SVC", set.getId());
        Map<String, String> params = set.getParameters().values().stream()
                .collect(Collectors.toMap(p -> p.name(), p -> p.value()));
        Map<String, String> refs = set.getReferences().values().stream()
                .collect(Collectors.toMap(r -> r.name(), r -> r.origName()));

        assertEquals("225.0", params.get("SVarC_UNom"));
        assertEquals(Double.toString(226.0 / 225.0), params.get("SVarC_URef0Pu"));
        // computeBPU(b, 225) = b * 225^2 / 100
        assertEquals(Double.toString(-0.5 * 225 * 225 / 100), params.get("SVarC_BShuntPu"));
        assertEquals(Double.toString(0.02 * 225 * 225 / 100), params.get("SVarC_BMaxPu"));
        assertEquals("218.0", params.get("SVarC_URefDown"));
        assertEquals("232.0", params.get("SVarC_URefUp"));
        assertEquals("210.0", params.get("SVarC_UThresholdDown"));
        assertEquals("240.0", params.get("SVarC_UThresholdUp"));
        assertEquals("60", params.get("SVarC_tThresholdUp"));
        assertTrue(refs.containsKey("SVarC_Mode0"));
        assertEquals("regulatingMode", refs.get("SVarC_Mode0"));
        assertEquals("v_pu", refs.get("SVarC_U0Pu"));   // warm start by default
    }

    private static VoltageLevel busBreakerVl(Network network, String id, double nominalV) {
        VoltageLevel vl = network.newSubstation().setId("S_" + id).add()
                .newVoltageLevel().setId(id).setNominalV(nominalV).setTopologyKind(TopologyKind.BUS_BREAKER).add();
        vl.getBusBreakerView().newBus().setId("B_" + id).add();
        return vl;
    }

    private static StaticVarCompensator svarc(VoltageLevel vl, String id, boolean regulating, RegulationMode mode) {
        return vl.newStaticVarCompensator().setId(id).setBus("B_" + vl.getId()).setConnectableBus("B_" + vl.getId())
                .setBmin(-0.02).setBmax(0.02).setVoltageSetpoint(226).setReactivePowerSetpoint(0)
                .setRegulationMode(mode).setRegulating(regulating).add();
    }

    private static com.powsybl.iidm.network.Terminal remoteTerminal(VoltageLevel vlRemote) {
        // a load in the remote voltage level gives a terminal on its bus to regulate
        return vlRemote.newLoad().setId("L_" + vlRemote.getId()).setBus("B_" + vlRemote.getId())
                .setConnectableBus("B_" + vlRemote.getId()).setP0(1).setQ0(0).add().getTerminal();
    }
}
