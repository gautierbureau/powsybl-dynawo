/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.parameters;

import com.powsybl.dynawo.desc.Cardinality;
import com.powsybl.dynawo.desc.ModelDescription;
import com.powsybl.dynawo.desc.ModifiableParameter;
import com.powsybl.dynawo.parameters.ParameterType;
import com.powsybl.dynawo.parameters.ParametersSet;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TopologyKind;
import com.powsybl.iidm.network.VoltageLevel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
class ParametersSetCompleterTest {

    private final ParametersSetCompleter completer = new ParametersSetCompleter();

    @Test
    void shouldTellWhichParametersTheSetLacks() {
        ParametersSet set = new ParametersSet("set");
        set.addParameter("generator_H", ParameterType.DOUBLE, "5.4");

        assertThat(ParametersSetCompleter.missingParameters(set, description("generator_H", "voltageRegulator_tIntegral")))
                .containsExactly("voltageRegulator_tIntegral");
    }

    @Test
    void shouldKeepWhatTheMachineAlreadySaysAndAddWhatTheModelAsks() {
        // the machine kept its inertia, chosen by hand, and its regulator kept its gain: giving it
        // an integral term adds the integral time beside them
        ParametersSet written = new ParametersSet("DynaWaltz_g");
        written.addParameter("generator_H", ParameterType.DOUBLE, "5.4");
        written.addParameter("voltageRegulator_Gain", ParameterType.DOUBLE, "42");

        ParametersSet completed = completer.complete("DynaWaltz_g_2", written,
                description("generator_H", "voltageRegulator_Gain", "voltageRegulator_tIntegral"),
                generator(), false);

        assertThat(completed.getId()).isEqualTo("DynaWaltz_g_2");
        assertThat(completed.getDouble("generator_H")).isEqualTo(5.4);
        assertThat(completed.getDouble("voltageRegulator_Gain")).isEqualTo(42.0);
        assertThat(completed.getDouble("voltageRegulator_tIntegral")).isEqualTo(5.0);
    }

    @Test
    void shouldDropWhatTheModelNoLongerDeclares() {
        ParametersSet written = new ParametersSet("DynaWaltz_g");
        written.addParameter("generator_H", ParameterType.DOUBLE, "5.4");
        written.addParameter("voltageRegulator_tIntegral", ParameterType.DOUBLE, "5");

        ParametersSet completed = completer.complete("DynaWaltz_g_2", written,
                description("generator_H"), generator(), false);

        assertThat(completed.getParameters()).containsOnlyKeys("generator_H");
    }

    private static ModelDescription description(String... parameters) {
        return new ModelDescription("AModel",
                List.of(parameters).stream()
                        .map(name -> new ModifiableParameter(name, ParameterType.DOUBLE, Cardinality.ONE))
                        .toList(),
                List.of());
    }

    private static Generator generator() {
        Network network = Network.create("test", "test");
        VoltageLevel voltageLevel = network.newSubstation().setId("s").add()
                .newVoltageLevel().setId("vl").setNominalV(20.0).setTopologyKind(TopologyKind.BUS_BREAKER).add();
        voltageLevel.getBusBreakerView().newBus().setId("bus").add();
        Generator generator = voltageLevel.newGenerator().setId("g")
                .setBus("bus").setConnectableBus("bus")
                .setTargetP(100).setMinP(0).setMaxP(120).setTargetV(20).setVoltageRegulatorOn(true)
                .add();
        generator.getTerminal().setP(-100.0).setQ(0.0);
        return generator;
    }
}
