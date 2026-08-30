/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dynawo.characteristics.EnergySourceSynchronousGeneratorPropertiesProvider;
import com.powsybl.dynawo.characteristics.Nordic32SynchronousGeneratorPropertiesProvider;
import com.powsybl.dynawo.extensions.api.generator.SynchronousGeneratorProperties;
import com.powsybl.dynawo.extensions.api.generator.SynchronousGeneratorProperties.Windings;
import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Checks the registry a fleet's controls are described through, chosen by name as a step of its
 * own: the machine by machine controls of a known system, and the ones deduced from a rule, both
 * writing the same extension a mapping then reads.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
class SynchronousGeneratorPropertiesProvidersTest {

    private final SynchronousGeneratorPropertiesProviders registry = SynchronousGeneratorPropertiesProviders.getInstance();

    @Test
    void shouldRegisterEveryProviderUnderANameAndDescription() {
        assertThat(registry.getProviderNames())
                .contains(EnergySourceSynchronousGeneratorPropertiesProvider.NAME,
                        Nordic32SynchronousGeneratorPropertiesProvider.NAME);
        assertThat(registry.getProviderInfos()).allSatisfy(info -> {
            assertThat(info.name()).isNotBlank();
            assertThat(info.description()).as(info.name() + " has a description").isNotBlank();
        });
    }

    @Test
    void shouldDescribeAKnownSystemMachineByMachine() {
        Network network = IeeeCdfNetworkFactory.create14();
        network.getGenerator("B1-G").setId("g01");
        network.getGenerator("B2-G").setId("g06");

        registry.createExtensions(network, Nordic32SynchronousGeneratorPropertiesProvider.NAME,
                MappingParameters.empty());

        SynchronousGeneratorProperties goverNordic = properties(network, "g01");
        assertThat(goverNordic.getGovernor()).isEqualTo("GoverNordic");
        assertThat(goverNordic.getVoltageRegulator()).isEqualTo("VRNordic");
        assertThat(goverNordic.getNumberOfWindings()).isEqualTo(Windings.THREE_WINDINGS);

        SynchronousGeneratorProperties pmConst = properties(network, "g06");
        assertThat(pmConst.getGovernor()).isEqualTo("PmConst");
        assertThat(pmConst.getNumberOfWindings()).isEqualTo(Windings.FOUR_WINDINGS);
    }

    @Test
    void shouldLeaveAMachineTheSystemDoesNotNameForAnotherProvider() {
        Network network = IeeeCdfNetworkFactory.create14();
        // no generator is renamed to a Nordic id, so the Nordic provider describes none of them
        registry.createExtensions(network, Nordic32SynchronousGeneratorPropertiesProvider.NAME,
                MappingParameters.empty());
        assertThat(network.getGeneratorStream())
                .allMatch(g -> g.getExtension(SynchronousGeneratorProperties.class) == null);
    }

    @Test
    void shouldNotDescribeAMachineAlreadyDescribed() {
        Network network = IeeeCdfNetworkFactory.create14();
        network.getGenerator("B1-G").setId("g01");
        // the energy source rule describes it first, and the Nordic provider leaves it be
        registry.createExtensions(network, EnergySourceSynchronousGeneratorPropertiesProvider.NAME,
                MappingParameters.empty());
        String beforeGovernor = properties(network, "g01").getGovernor();
        registry.createExtensions(network, Nordic32SynchronousGeneratorPropertiesProvider.NAME,
                MappingParameters.empty());
        assertThat(properties(network, "g01").getGovernor()).isEqualTo(beforeGovernor);
    }

    @Test
    void shouldCarryASettingToTheProvider() {
        Network network = IeeeCdfNetworkFactory.create14();
        // a machine at 135 kV: below a tso voltage of 400 it carries no auxiliaries, above 63 it
        // would, so the setting given here is what decides it
        registry.createExtensions(network, EnergySourceSynchronousGeneratorPropertiesProvider.NAME,
                MappingParameters.of(Map.of(
                        EnergySourceSynchronousGeneratorPropertiesProvider.TSO_VOLTAGE_MIN_PARAM, "400")));
        assertThat(properties(network, "B1-G").isAuxiliaries()).isFalse();
    }

    @Test
    void shouldRefuseAnUnknownProvider() {
        Network network = IeeeCdfNetworkFactory.create14();
        assertThatThrownBy(() -> registry.createExtensions(network, "NoSuchProvider", MappingParameters.empty()))
                .isInstanceOf(PowsyblException.class)
                .hasMessageContaining("NoSuchProvider");
    }

    private static SynchronousGeneratorProperties properties(Network network, String generatorId) {
        Generator generator = network.getGenerator(generatorId);
        return generator.getExtension(SynchronousGeneratorProperties.class);
    }
}
