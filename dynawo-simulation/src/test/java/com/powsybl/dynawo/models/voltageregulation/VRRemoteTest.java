/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.models.voltageregulation;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.dynawo.BlackBoxModelSupplier;
import com.powsybl.dynawo.models.VarConnection;
import com.powsybl.dynawo.models.macroconnections.MacroConnect;
import com.powsybl.dynawo.models.macroconnections.MacroConnectAttribute;
import com.powsybl.dynawo.models.macroconnections.MacroConnectionsAdder;
import com.powsybl.dynawo.models.macroconnections.MacroConnector;
import com.powsybl.dynawo.parameters.ParametersSet;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TopologyKind;
import com.powsybl.iidm.network.VoltageLevel;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The {@code VRRemote} coordinating the machines that remotely regulate one bus: named after that bus,
 * summing every machine's reactive injection while reading each one's reactive limits at its own index,
 * and reading the regulated bus's voltage from the {@code NETWORK} model.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
class VRRemoteTest {

    /** A regulating machine seen by the {@code VRRemote}: fixed var names and the bus it regulates. */
    private record StubRegulatingMachine(String staticId, Bus regulatedBus, double uRef0Pu) implements VRRemoteModel {
        @Override
        public Bus getRegulatedBus() {
            return regulatedBus;
        }

        @Override
        public double getURef0Pu() {
            return uRef0Pu;
        }

        @Override
        public String getNQVarName() {
            return "generator_NQ";
        }

        @Override
        public String getLimUQUpVarName() {
            return "generator_limUQUp";
        }

        @Override
        public String getLimUQDownVarName() {
            return "generator_limUQDown";
        }

        @Override
        public String getName() {
            return "GeneratorPVRemote";
        }

        @Override
        public List<MacroConnectAttribute> getMacroConnectToAttributes() {
            return List.of(MacroConnectAttribute.of("id2", staticId));
        }
    }

    private static Bus regulatedBus() {
        Network network = Network.create("test", "test");
        VoltageLevel vl = network.newSubstation().setId("S").add()
                .newVoltageLevel().setId("VL").setNominalV(400).setTopologyKind(TopologyKind.BUS_BREAKER).add();
        return vl.getBusBreakerView().newBus().setId("REGULATED_BUS").add();
    }

    @Test
    void itIsNamedAfterTheRegulatedBusAndRunsTheVrRemoteLib() {
        Bus bus = regulatedBus();
        VRRemote vrRemote = new VRRemote(bus.getId(),
                List.of(new StubRegulatingMachine("GEN1", bus, 1.05)), "test.par");

        assertEquals("Model_Signal_NQ_REGULATED_BUS", vrRemote.getDynamicModelId());
        assertEquals("Model_Signal_NQ_REGULATED_BUS", vrRemote.getParameterSetId());
        assertEquals("VRRemote", vrRemote.getLib());
        assertEquals("test.par", vrRemote.getDefaultParFile());
    }

    @Test
    void itSumsEveryMachinesReactiveInjectionAndReadsEachLimitAtItsIndex() {
        Bus bus = regulatedBus();
        VRRemote vrRemote = new VRRemote(bus.getId(), List.of(
                new StubRegulatingMachine("GEN1", bus, 1.05),
                new StubRegulatingMachine("GEN2", bus, 1.05)), "test.par");

        List<MacroConnect> macroConnects = new ArrayList<>();
        Map<String, MacroConnector> macroConnectors = new LinkedHashMap<>();
        MacroConnectionsAdder adder = new MacroConnectionsAdder(
                BlackBoxModelSupplier.createFrom(List.of()),
                macroConnects::add,
                macroConnectors::computeIfAbsent,
                ReportNode.NO_OP);

        vrRemote.createMacroConnections(adder);

        // one connect per machine (indexed), plus one to the regulated bus
        assertEquals(3, macroConnects.size());
        assertThat(indexValues(macroConnects)).containsExactly("0", "1");

        // the two machines share one templated connector; the regulated bus, with no dynamic model,
        // resolves to the default action connection point on NETWORK
        assertThat(macroConnectors.keySet())
                .containsExactly("MC_VRRemote-GeneratorPVRemote", "MC_VRRemote-DefaultActionConnectionPoint");
        assertThat(macroConnectors.get("MC_VRRemote-GeneratorPVRemote"))
                .usingRecursiveComparison()
                .isEqualTo(new MacroConnector("MC_VRRemote-GeneratorPVRemote", List.of(
                        new VarConnection("vrremote_NQ", "generator_NQ"),
                        new VarConnection("vrremote_limUQUp_@INDEX@_", "generator_limUQUp"),
                        new VarConnection("vrremote_limUQDown_@INDEX@_", "generator_limUQDown"))));
        assertThat(macroConnectors.get("MC_VRRemote-DefaultActionConnectionPoint"))
                .usingRecursiveComparison()
                .isEqualTo(new MacroConnector("MC_VRRemote-DefaultActionConnectionPoint", List.of(
                        new VarConnection("vrremote_URegulatedPu", "@NAME@_Upu"))));
    }

    @Test
    void itFreezesAtTheRegulatedBusVoltageSetpoint() {
        Bus bus = regulatedBus();
        VRRemote vrRemote = new VRRemote(bus.getId(),
                List.of(new StubRegulatingMachine("GEN1", bus, 1.05)), "test.par");

        List<ParametersSet> sets = new ArrayList<>();
        vrRemote.createDynamicModelParameters(sets::add);

        assertEquals(1, sets.size());
        ParametersSet set = sets.getFirst();
        assertEquals("Model_Signal_NQ_REGULATED_BUS", set.getId());
        assertThat(set.getParameters().values().stream().map(p -> p.name() + "=" + p.value()))
                .contains("vrremote_U0Pu=1.05", "vrremote_URef0Pu=1.05", "vrremote_Frozen0=true");
    }

    private static List<String> indexValues(List<MacroConnect> macroConnects) {
        return macroConnects.stream()
                .flatMap(mc -> mc.getAttributesFrom().stream())
                .filter(a -> a.name().equals("index1"))
                .map(MacroConnectAttribute::value)
                .toList();
    }
}
