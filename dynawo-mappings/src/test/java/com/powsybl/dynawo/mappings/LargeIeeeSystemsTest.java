/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings;

import com.powsybl.dynawo.mappings.parameters.ModelDescriptionLookup;
import com.powsybl.dynawo.networks.PlausibleEnergySources;
import com.powsybl.dynawo.parameters.ParametersSet;
import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Describes the larger test systems, which nothing says anything about: their machines carry no
 * declared energy source, so what they are is guessed from their size, and there are enough of
 * them for a gap in the resolver or the parameters to show.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
class LargeIeeeSystemsTest {

    private static final Path HOME = Path.of("..", "..", "dynawo");

    static Stream<Arguments> systems() {
        return Stream.of(
                Arguments.of("IEEE118", (Supplier<Network>) IeeeCdfNetworkFactory::create118),
                Arguments.of("IEEE300", (Supplier<Network>) IeeeCdfNetworkFactory::create300));
    }

    @ParameterizedTest(name = "{0} for a steady state study")
    @MethodSource("systems")
    void shouldDescribeEveryMachineOfASystemNothingIsSaidAbout(String name, Supplier<Network> factory) {
        Network network = described(factory);
        UniversalSynchronousGeneratorMapping mapping = UniversalSynchronousGeneratorMapping.dynaWaltz();
        mapping.createExtensions(network);
        List<MappedModelsSupplier.MappedModel> models = mapping.createModelConfigs(network);

        // every machine the mapping covers gets a model, none left behind for want of one
        assertThat(models).as(name).hasSize((int) covered(network));
        assertThat(models).extracting(MappedModelsSupplier.MappedModel::lib)
                .allMatch(lib -> lib.startsWith("GeneratorSynchronous"));
        // and a set to value it, each named after the machine it belongs to
        assertThat(models).extracting(MappedModelsSupplier.MappedModel::parameterSetId)
                .doesNotContainNull().doesNotHaveDuplicates();
    }

    @ParameterizedTest(name = "{0} for a transient study")
    @MethodSource("systems")
    void shouldDescribeEveryMachineForATransientStudy(String name, Supplier<Network> factory) {
        Network network = described(factory);
        UniversalSynchronousGeneratorMapping mapping = UniversalSynchronousGeneratorMapping.dynaSwing();
        mapping.createExtensions(network);
        assertThat(mapping.createModelConfigs(network)).as(name).hasSize((int) covered(network));
    }

    @ParameterizedTest(name = "{0} parameters")
    @MethodSource("systems")
    void shouldValueEveryModelItGives(String name, Supplier<Network> factory) {
        Assumptions.assumeTrue(Files.exists(HOME.resolve("ddb")), "no Dynawo installation at " + HOME);
        Network network = described(factory);
        UniversalSynchronousGeneratorMapping mapping = UniversalSynchronousGeneratorMapping.dynaWaltz();
        mapping.createExtensions(network);
        List<MappedModelsSupplier.MappedModel> models = mapping.createModelConfigs(network);

        List<ParametersSet> sets = mapping.createParameters(network,
                ModelDescriptionLookup.fromModelDatabase(HOME));

        // one set per model, and none of them empty: a model given a set that values nothing is
        // a model the simulation cannot run
        assertThat(sets).as(name).hasSameSizeAs(models);
        assertThat(sets).allSatisfy(set -> assertThat(set.getParameters()).isNotEmpty());
        assertThat(sets).extracting(ParametersSet::getId)
                .containsExactlyInAnyOrderElementsOf(
                        models.stream().map(MappedModelsSupplier.MappedModel::parameterSetId).toList());
    }

    /**
     * The machines a mapping covers: those the network holds, since nothing here is disconnected.
     */
    private static long covered(Network network) {
        return network.getGeneratorStream().filter(Generator::isVoltageRegulatorOn).count();
    }

    private static Network described(Supplier<Network> factory) {
        Network network = factory.get();
        PlausibleEnergySources.apply(network);
        return network;
    }
}
