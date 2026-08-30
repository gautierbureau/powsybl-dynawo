/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings;

import com.powsybl.dynawo.characteristics.Nordic32SystemProvider;
import com.powsybl.dynawo.extensions.api.generator.SynchronousGeneratorProperties;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.extensions.TapChangerBlocking;
import com.powsybl.iidm.network.extensions.TapChangerBlockings;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks a whole system set up in one step: naming Nordic 32 gives its machines their controls and
 * its network its tap changer blocking, both from the system's reference, without the caller
 * touching either.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
class DynamicSimulationSystemsTest {

    private static final Path NORDIC = Path.of("..", "..", "dynawo", "examples", "DynaWaltz", "Nordic", "Nordic.xiidm");

    @Test
    void shouldRegisterEverySystemUnderANameAndDescription() {
        assertThat(DynamicSimulationSystems.getInstance().getSystemNames()).contains("Nordic32", "IEEE");
        DynamicSimulationSystems.getInstance().getSystemInfos()
                .forEach(info -> assertThat(info.description()).as(info.name()).isNotBlank());
        assertThat(TapChangerBlockingsProviders.getInstance().getProviderNames()).contains("Nordic32");
    }

    @Test
    void shouldSetUpTheWholeNordicSystemAtOnce() {
        Assumptions.assumeTrue(Files.exists(NORDIC), "no Nordic network at " + NORDIC);
        Network network = Network.read(NORDIC);

        DynamicSimulationSystems.getInstance().createExtensions(network, Nordic32SystemProvider.NAME,
                MappingParameters.empty());

        // the machines are described
        assertThat((Object) network.getGenerator("g01").getExtension(SynchronousGeneratorProperties.class)).isNotNull();

        // and the blocking is named, watching the reference bus and holding the reference
        // transformers by the levels they sit in
        TapChangerBlockings tcbs = network.getExtension(TapChangerBlockings.class);
        assertThat(tcbs).isNotNull();
        TapChangerBlocking tcb = tcbs.getTapChangerBlocking("TCB").orElseThrow();
        assertThat(tcb.getMeasurementPoints()).singleElement().satisfies(point -> {
            assertThat(point.getBuses()).singleElement().satisfies(bus -> {
                assertThat(bus.busId()).isEqualTo("1042_131");
                assertThat(bus.voltageLevelId()).isEqualTo("1042_130");
            });
            assertThat(point.getBusbarSectionIds()).isEmpty();
        });
        // the twenty two transformers of the reference sit in twenty two levels
        assertThat(tcb.getControlVoltageLevels()).hasSize(22);
    }

    @Test
    void shouldMapNordicWithItsBlockingEndToEnd() {
        Assumptions.assumeTrue(Files.exists(NORDIC), "no Nordic network at " + NORDIC);
        Network network = Network.read(NORDIC);
        DynamicSimulationSystems.getInstance().createExtensions(network, Nordic32SystemProvider.NAME,
                MappingParameters.empty());

        // the universal mapping, reading the extensions the system set, resolves a model for the
        // blocking beside the machines and loads
        var models = new UniversalDynaWaltzProvider().create(MappingParameters.empty())
                .createModelConfigs(network);
        assertThat(models).anySatisfy(model ->
                assertThat(model.staticId()).isEqualTo("TCB"));
    }
}
