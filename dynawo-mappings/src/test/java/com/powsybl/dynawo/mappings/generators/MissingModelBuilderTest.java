/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.generators;

import com.powsybl.dynawo.extensions.api.generator.RpclType;
import com.powsybl.dynawo.extensions.api.generator.SynchronousGeneratorProperties;
import com.powsybl.dynawo.extensions.api.generator.SynchronousGeneratorPropertiesAdder;
import com.powsybl.dynawo.mappings.TestNetworks;
import com.powsybl.dynawo.mappings.controls.ControlTranslations;
import com.powsybl.dynawo.mappings.preassembled.GeneratorModelDesigner;
import com.powsybl.dynawo.mappings.preassembled.ModelNaming;
import com.powsybl.dynawo.mappings.tools.PreassembledModelCompiler;
import com.powsybl.iidm.network.Generator;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks that a generator whose controls no installed model carries is answered with a model
 * built for it, without anything having been asked of the caller.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
class MissingModelBuilderTest {

    private static final Path HOME = Path.of("..", "..", "dynawo");
    private static final String UNBUILT = "GeneratorSynchronousFourWindingsGovCt2Ac6a";

    @Test
    void shouldLeaveAGeneratorUnmappedWhenNothingBuildsMissingModels() {
        // a governor and a regulator no installed model carries together. The catalog has no
        // near miss to offer either: a model is looked up by both its controls at once, so an
        // unknown pairing matches nothing and the generator went unmapped altogether
        assertThat(new GeneratorLibResolver().resolve(properties("GovCt2", "Ac6a"), false, false))
                .isEmpty();
    }

    @Test
    void shouldSayNothingOfAGeneratorItCannotBuildFor(@TempDir Path modelsDir) {
        MissingModelBuilder builder = new MissingModelBuilder(
                new GeneratorModelDesigner(ModelNaming.DYNAWO_1_7_0),
                new PreassembledModelCompiler(Path.of("nowhere")), modelsDir);
        assertThat(builder.build(properties("NoSuchGovernor", "Ac6a"), false)).isEmpty();
    }

    @Test
    void shouldCarryOnWhenBuildingFails(@TempDir Path modelsDir) {
        // no Dynawo where it is pointed, so building cannot work. It must leave things as they
        // were, the generator unmapped, rather than take the run down over it
        MissingModelBuilder builder = new MissingModelBuilder(Path.of("nowhere"), modelsDir,
                ModelNaming.DYNAWO_1_7_0);
        assertThat(new GeneratorLibResolver(ControlTranslations.getInstance(), builder)
                .resolve(properties("GovCt2", "Ac6a"), false, false))
                .isEmpty();
        // and nothing of the attempt is left behind, a definition for a model that was not built
        // describing nothing true
        assertThat(modelsDir.toFile().list()).isEmpty();
    }

    @Test
    @Tag("compilation")
    void shouldBuildWhatNoInstalledModelProvides(@TempDir Path modelsDir) {
        Assumptions.assumeTrue(Files.exists(HOME.resolve("dynawo.sh")), "no Dynawo installation at " + HOME);

        MissingModelBuilder builder = new MissingModelBuilder(HOME, modelsDir, ModelNaming.DYNAWO_1_7_0);
        Optional<String> lib = new GeneratorLibResolver(ControlTranslations.getInstance(), builder)
                .resolve(properties("GovCt2", "Ac6a"), false, false);

        // the generator gets the model its extension asked for, not the nearest catalogued one
        assertThat(lib).hasValue(UNBUILT);
        assertThat(modelsDir.resolve(UNBUILT + ".so")).exists();
    }

    @Test
    @Tag("compilation")
    void shouldLeaveTheCatalogToAnswerWhatItAlreadyHas(@TempDir Path modelsDir) {
        Assumptions.assumeTrue(Files.exists(HOME.resolve("dynawo.sh")), "no Dynawo installation at " + HOME);

        MissingModelBuilder builder = new MissingModelBuilder(HOME, modelsDir, ModelNaming.DYNAWO_1_7_0);
        Optional<String> lib = new GeneratorLibResolver(ControlTranslations.getInstance(), builder)
                .resolve(properties("GovCt2", "St4b"), false, false);

        assertThat(lib).isPresent();
        // nothing was built, the catalog holding that one already
        assertThat(modelsDir.toFile().list()).isEmpty();
    }

    private static SynchronousGeneratorProperties properties(String governor, String voltageRegulator) {
        Generator generator = TestNetworks.singleGenerator(400.0);
        generator.newExtension(SynchronousGeneratorPropertiesAdder.class)
                .withNumberOfWindings(SynchronousGeneratorProperties.Windings.FOUR_WINDINGS)
                .withGovernor(governor)
                .withVoltageRegulator(voltageRegulator)
                .withPss("")
                .withAuxiliaries(false)
                .withInternalTransformer(false)
                .withRpcl(RpclType.NONE)
                .withUva(SynchronousGeneratorProperties.Uva.LOCAL)
                .withAggregated(false)
                .withQlim(false)
                .add();
        return generator.getExtension(SynchronousGeneratorProperties.class);
    }
}
