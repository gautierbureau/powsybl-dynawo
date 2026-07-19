/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.parameters;

import com.powsybl.dynawo.desc.FilteredDescriptionXml;
import com.powsybl.dynawo.desc.ModelDescription;
import com.powsybl.dynawo.parameters.ParametersSet;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TopologyKind;
import com.powsybl.iidm.network.VoltageLevel;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
class SynchronousGeneratorParametersGeneratorTest {

    private static final String LIB = "GeneratorSynchronousFourWindingsGoverPropVRPropInt";

    private final SynchronousGeneratorParametersGenerator generator = new SynchronousGeneratorParametersGenerator();

    @Test
    void shouldValueEveryParameterOfARealModel() {
        ModelDescription description = description();
        ParametersSet set = generator.generate("set", description, generator(400.0, 250.0, 220.0), false);

        // every modifiable parameter the model declares is either read from the network or valued
        int described = description.parameters().size();
        assertThat(set.getParameters().size() + set.getReferences().size()).isEqualTo(described);
        assertThat(described).isPositive();
    }

    @Test
    void shouldReadTheOperatingPointFromTheNetwork() {
        ParametersSet set = generator.generate("set", description(), generator(400.0, 250.0, 220.0), false);

        assertThat(set.getReferences()).containsKey("generator_P0Pu");
        assertThat(set.getReferences().get("generator_P0Pu").origName()).isEqualTo("p_pu");
        assertThat(set.getReferences().get("generator_U0Pu").origName()).isEqualTo("v_pu");
    }

    @Test
    void shouldSizeTheMachineFromItsRatedApparentPower() {
        Generator gen = generator(400.0, 250.0, 220.0);
        gen.setRatedS(300.0);
        ParametersSet set = generator.generate("set", description(), gen, false);

        assertThat(set.getDouble("generator_SNom")).isEqualTo(300.0);
    }

    @Test
    void shouldIgnoreAPlaceholderMaximumActivePower() {
        // the IEEE test systems declare 9999 MW for every machine, sizing a 232 MW unit as a 11 GW
        // one would make the simulation meaningless
        Generator gen = generator(400.0, 9999.0, 232.4);
        ParametersSet set = generator.generate("set", description(), gen, false);

        assertThat(set.getDouble("generator_SNom")).isEqualTo(232.4 / GeneratorSizing.LOAD_FACTOR);
    }

    @Test
    void shouldUseAConsistentMaximumActivePower() {
        Generator gen = generator(400.0, 250.0, 220.0);
        ParametersSet set = generator.generate("set", description(), gen, false);

        assertThat(set.getDouble("generator_SNom")).isEqualTo(1.1 * 250.0);
    }

    @Test
    void shouldAddALeakageReactanceWhenTheModelHasNoTransformer() {
        ParametersSet set = generator.generate("set", description(), generator(400.0, 250.0, 220.0), false);

        assertThat(set.getDouble("generator_XTfPu")).isEqualTo(0.01);
    }

    @Test
    void shouldReferToTheNominalVoltageOfALowVoltageMachine() {
        // directly connected to a generation voltage level, the machine voltages follow the network
        ParametersSet set = generator.generate("set", description(), generator(20.0, 250.0, 220.0), false);

        assertThat(set.getReferences()).containsKey("generator_UNom");
        assertThat(set.getReferences().get("generator_UNom").origName()).isEqualTo("vNom");
        // no transformer stands between the machine and the network, nothing to stand in for
        assertThat(set.getDouble("generator_XTfPu")).isZero();
    }

    private static ModelDescription description() {
        try (InputStream is = SynchronousGeneratorParametersGeneratorTest.class.getResourceAsStream("/" + LIB + ".desc.xml")) {
            return FilteredDescriptionXml.load(is);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static Generator generator(double nominalV, double maxP, double targetP) {
        Network network = Network.create("test", "test");
        VoltageLevel voltageLevel = network.newSubstation().setId("s").add()
                .newVoltageLevel().setId("vl").setNominalV(nominalV).setTopologyKind(TopologyKind.BUS_BREAKER).add();
        voltageLevel.getBusBreakerView().newBus().setId("bus").add();
        Generator generator = voltageLevel.newGenerator().setId("g")
                .setBus("bus").setConnectableBus("bus")
                .setTargetP(targetP).setMinP(0).setMaxP(maxP).setTargetV(nominalV).setVoltageRegulatorOn(true)
                .add();
        // a load flow is a prerequisite of a dynamic simulation, its results are available
        generator.getTerminal().setP(-targetP).setQ(0.0);
        return generator;
    }
}
