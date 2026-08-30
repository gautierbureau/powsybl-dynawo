/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.models.svc;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.dynawo.BlackBoxModelSupplier;
import com.powsybl.dynawo.builders.ModelConfig;
import com.powsybl.dynawo.models.BlackBoxModel;
import com.powsybl.dynawo.models.VarConnection;
import com.powsybl.dynawo.models.generators.SignalNGenerator;
import com.powsybl.dynawo.models.generators.SignalNGeneratorBuilder;
import com.powsybl.dynawo.models.macroconnections.MacroConnect;
import com.powsybl.dynawo.models.macroconnections.MacroConnectAttribute;
import com.powsybl.dynawo.models.macroconnections.MacroConnectionsAdder;
import com.powsybl.dynawo.models.macroconnections.MacroConnector;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The simplified secondary voltage control connecting its zone's machines: each machine's stator
 * reactive power and blocker read at its own index, the one shared level written to every machine, and
 * the pilot bus's voltage read from the {@code NETWORK} model.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
class SecondaryVoltageControlSimplifiedTest {

    @Test
    void itReadsEachMachineAtItsIndexAndTheOneSharedLevel() {
        Network network = EurostagTutorialExample1Factory.createWithMultipleConnectedComponents();
        SignalNGenerator gen1 = SignalNGeneratorBuilder.of(network, "GeneratorPVSignalN").staticId("GEN").build();
        SignalNGenerator gen2 = SignalNGeneratorBuilder.of(network, "GeneratorPVSignalN").staticId("GEN2").build();
        List<BlackBoxModel> dynamicModels = List.of(gen1, gen2);

        SecondaryVoltageControlSimplified svc = new SecondaryVoltageControlSimplified("SVC", "SVC",
                List.of(network.getGenerator("GEN"), network.getGenerator("GEN2")),
                network.getBusBreakerView().getBus("NHV1"),
                new ModelConfig("DYNModelSecondaryVoltageControlSimplified"));

        List<MacroConnect> macroConnects = new ArrayList<>();
        Map<String, MacroConnector> macroConnectors = new LinkedHashMap<>();
        MacroConnectionsAdder adder = new MacroConnectionsAdder(
                BlackBoxModelSupplier.createFrom(dynamicModels),
                macroConnects::add,
                macroConnectors::computeIfAbsent,
                ReportNode.NO_OP);

        svc.createMacroConnections(adder);

        // one connect per machine (indexed), plus one to the pilot bus
        assertEquals(3, macroConnects.size());
        assertThat(indexValues(macroConnects)).containsExactly("0", "1");

        MacroConnector toGenerator = macroConnectors.get("MC_DYNModelSecondaryVoltageControlSimplified-GeneratorPVSignalN");
        assertThat(toGenerator).usingRecursiveComparison().isEqualTo(
                new MacroConnector("MC_DYNModelSecondaryVoltageControlSimplified-GeneratorPVSignalN", List.of(
                        new VarConnection("QStator_@INDEX@_value", "generator_QStator"),
                        new VarConnection("blocker_@INDEX@_value", "generator_blocker"),
                        new VarConnection("level_value", "reactivePowerControlLoop_level"))));

        // the pilot bus has no dynamic model, so it resolves to the default action connection point on
        // NETWORK; @NAME@_Upu is the same network variable as the launcher's @NAME@_Upu_value
        MacroConnector toPilot = macroConnectors.get("MC_DYNModelSecondaryVoltageControlSimplified-DefaultActionConnectionPoint");
        assertThat(toPilot).usingRecursiveComparison().isEqualTo(
                new MacroConnector("MC_DYNModelSecondaryVoltageControlSimplified-DefaultActionConnectionPoint", List.of(
                        new VarConnection("UpPu_value", "@NAME@_Upu"))));
    }

    private static List<String> indexValues(List<MacroConnect> macroConnects) {
        return macroConnects.stream()
                .flatMap(mc -> mc.getAttributesFrom().stream())
                .filter(a -> a.name().equals("index1"))
                .map(MacroConnectAttribute::value)
                .toList();
    }
}
