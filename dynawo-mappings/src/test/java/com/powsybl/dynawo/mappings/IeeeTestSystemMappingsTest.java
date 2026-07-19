/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings;

import com.powsybl.dynawo.extensions.api.generator.SynchronousGeneratorProperties;
import com.powsybl.dynawo.networks.Ieee14EnergySources;
import com.powsybl.dynawo.networks.Ieee57EnergySources;
import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks that the mapping of the IEEE test systems produces the models Dynawo ships for them.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
class IeeeTestSystemMappingsTest {

    private static final String FOUR_WINDINGS = "GeneratorSynchronousFourWindingsProportionalRegulations";
    private static final String THREE_WINDINGS = "GeneratorSynchronousThreeWindingsProportionalRegulations";

    @Test
    void shouldProduceTheIeee14ReferenceModels() {
        // the reference dyd holds three four winding and two three winding proportional regulations
        assertThat(libs(ieee14(), IeeeTestSystemMappings.dynaWaltz()))
                .containsExactlyInAnyOrder(FOUR_WINDINGS, FOUR_WINDINGS, FOUR_WINDINGS, THREE_WINDINGS, THREE_WINDINGS);
    }

    @Test
    void shouldProduceTheIeee57ReferenceModels() {
        // the reference dyd holds seven four winding proportional regulations
        assertThat(libs(ieee57(), IeeeTestSystemMappings.dynaWaltz()))
                .containsExactly(FOUR_WINDINGS, FOUR_WINDINGS, FOUR_WINDINGS, FOUR_WINDINGS,
                        FOUR_WINDINGS, FOUR_WINDINGS, FOUR_WINDINGS);
    }

    @Test
    void shouldKeepTheDetailedModelsForATransientStudy() {
        // the same systems described for a transient study keep the regulations they were given
        assertThat(libs(ieee14(), IeeeTestSystemMappings.dynaSwing()))
                .containsExactlyInAnyOrder("GeneratorSynchronousFourWindingsGovCt2St4b",
                        "GeneratorSynchronousFourWindingsGovSteam1St4b",
                        "GeneratorSynchronousFourWindingsGovSteam1St4b",
                        "GeneratorSynchronousThreeWindingsGovHydro4St4b",
                        "GeneratorSynchronousThreeWindingsGovHydro4St4b");
    }

    @Test
    void shouldKeepTheIntegralTermInTheUniversalMapping() {
        // a real machine carrying such an exciter holds no steady state voltage error, which the
        // test systems do not model
        assertThat(libs(ieee14(), UniversalSynchronousGeneratorMapping.dynaWaltz()))
                .containsOnly("GeneratorSynchronousFourWindingsGoverPropVRPropInt",
                        "GeneratorSynchronousThreeWindingsGoverPropVRPropInt");
    }

    @Test
    void shouldDeduceTheSimplifiedModelsFromTheExtensionsOfATransientStudy() {
        // a transient study described this network first, writing the real regulations
        Network network = ieee14();
        IeeeTestSystemMappings.dynaSwing().createExtensions(network);
        SynchronousGeneratorProperties written = network.getGenerator("B1-G").getExtension(SynchronousGeneratorProperties.class);
        assertThat(written.getGovernor()).isEqualTo("GovCt2");
        assertThat(written.getVoltageRegulator()).isEqualTo("St4b");

        // asking for a voltage stability study on that same network deduces the simplified models
        UniversalSynchronousGeneratorMapping voltageStability = IeeeTestSystemMappings.dynaWaltz();
        voltageStability.createExtensions(network);
        assertThat(libs(network, voltageStability))
                .containsExactlyInAnyOrder(FOUR_WINDINGS, FOUR_WINDINGS, FOUR_WINDINGS, THREE_WINDINGS, THREE_WINDINGS);

        // and the network still describes the machines by their real regulations
        assertThat(written.getGovernor()).isEqualTo("GovCt2");
        assertThat(written.getVoltageRegulator()).isEqualTo("St4b");
    }

    @Test
    void shouldRegisterBothFlavours() {
        assertThat(DynamicModelsMappings.getInstance().getMappingNames())
                .contains(IeeeTestSystemMappings.DYNAWALTZ_NAME, IeeeTestSystemMappings.DYNASWING_NAME);
    }

    private static List<String> libs(Network network, UniversalSynchronousGeneratorMapping mapping) {
        mapping.createExtensions(network);
        return mapping.createModelConfigs(network).stream().map(MappedModelsSupplier.MappedModel::lib).toList();
    }

    private static Network ieee14() {
        Network network = IeeeCdfNetworkFactory.create14();
        Ieee14EnergySources.apply(network);
        return network;
    }

    private static Network ieee57() {
        Network network = IeeeCdfNetworkFactory.create57();
        Ieee57EnergySources.apply(network);
        return network;
    }
}
