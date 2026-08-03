/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings;

import com.powsybl.dynawo.extensions.api.generator.SynchronousGeneratorProperties;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.extensions.TapChangerBlockings;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks that the Nordic 32 study is chosen by name like any mapping, and that its tap changer
 * blocking is added only where the caller asks for it, the machines described either way.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
class Nordic32MappingProviderTest {

    private static final Path NORDIC = Path.of("..", "..", "dynawo", "examples", "DynaWaltz", "Nordic", "Nordic.xiidm");

    @Test
    void shouldRegisterNordic32AmongTheMappings() {
        assertThat(DynamicModelsMappings.getInstance().getMappingInfos())
                .anySatisfy(info -> {
                    assertThat(info.name()).isEqualTo("Nordic32");
                    assertThat(info.description()).isNotBlank();
                });
    }

    @Test
    void shouldDescribeTheMachinesWithoutTheBlockingByDefault() {
        Assumptions.assumeTrue(Files.exists(NORDIC), "no Nordic network at " + NORDIC);
        Network network = Network.read(NORDIC);

        DynamicModelsMapping mapping = new Nordic32MappingProvider().create(MappingParameters.empty());
        mapping.createExtensions(network);

        // the machines carry their controls
        assertThat((Object) network.getGenerator("g01").getExtension(SynchronousGeneratorProperties.class)).isNotNull();
        // but nothing asked for the blocking, so there is none, and no automaton mapped for it
        assertThat((Object) network.getExtension(TapChangerBlockings.class)).isNull();
        assertThat(mapping.createModelConfigs(network))
                .noneSatisfy(model -> assertThat(model.staticId()).isEqualTo("TCB"));
    }

    @Test
    void shouldAddTheBlockingWhenAskedFor() {
        Assumptions.assumeTrue(Files.exists(NORDIC), "no Nordic network at " + NORDIC);
        Network network = Network.read(NORDIC);

        DynamicModelsMapping mapping = new Nordic32MappingProvider()
                .create(MappingParameters.of(Map.of(Nordic32MappingProvider.WITH_TCB, "true")));
        mapping.createExtensions(network);

        // the blocking is named, and the universal mapping maps it to its automaton beside the machines
        assertThat((Object) network.getExtension(TapChangerBlockings.class)).isNotNull();
        assertThat(mapping.createModelConfigs(network))
                .anySatisfy(model -> assertThat(model.staticId()).isEqualTo("TCB"));
    }
}
