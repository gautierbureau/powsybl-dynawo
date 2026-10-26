/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.generators;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dynawo.extensions.api.generator.RpclType;
import com.powsybl.dynawo.extensions.api.generator.SynchronousGeneratorProperties;
import com.powsybl.dynawo.extensions.api.generator.SynchronousGeneratorProperties.Windings;
import com.powsybl.dynawo.extensions.api.generator.SynchronousGeneratorPropertiesAdder;
import com.powsybl.dynawo.mappings.TestNetworks;
import com.powsybl.dynawo.mappings.controls.ControlTranslations;
import com.powsybl.dynawo.mappings.preassembled.ControlUnitCatalog;
import com.powsybl.dynawo.mappings.preassembled.ControlUnitProvider;
import com.powsybl.dynawo.mappings.preassembled.GeneratorControls;
import com.powsybl.dynawo.mappings.preassembled.GeneratorModelDesigner;
import com.powsybl.dynawo.mappings.preassembled.MachineControlUnit;
import com.powsybl.dynawo.mappings.preassembled.MachineControlUnit.MachineQuantity;
import com.powsybl.dynawo.mappings.preassembled.ModelNaming;
import com.powsybl.dynawo.mappings.preassembled.PreassembledModel;
import com.powsybl.dynawo.mappings.tools.PreassembledModelCompiler;
import com.powsybl.iidm.network.Generator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
    private static final String ALSO_UNBUILT = "GeneratorSynchronousFourWindingsGovCt2Ac1a";

    @AfterEach
    void resetCatalog() {
        ControlUnitCatalog.getInstance().resetToBase();
    }

    /**
     * Stands in for Dynawo: fails the models it is told to, and says whether the installation
     * compiles at all as it is told to, recording what it was asked.
     */
    private static final class ScriptedCompiler extends PreassembledModelCompiler {

        private final Set<String> failing;
        private final boolean installationCompiles;
        private final List<String> compiled = new ArrayList<>();
        private int probes;

        private ScriptedCompiler(Set<String> failing, boolean installationCompiles) {
            super(Path.of("nowhere"));
            this.failing = failing;
            this.installationCompiles = installationCompiles;
        }

        @Override
        public Path compile(PreassembledModel model, Path modelsDir) {
            compiled.add(model.getId());
            if (failing.contains(model.getId())) {
                throw new PowsyblException("--generate-preassembled failed: " + model.getId());
            }
            return modelsDir.resolve(model.getId() + ".so");
        }

        @Override
        public boolean canCompile() {
            probes++;
            return installationCompiles;
        }
    }

    private static MissingModelBuilder builder(ScriptedCompiler compiler, Path modelsDir) {
        return new MissingModelBuilder(new GeneratorModelDesigner(ModelNaming.DYNAWO_1_7_0), compiler, modelsDir);
    }

    @Test
    void shouldGiveUpOnlyOnTheModelThatDoesNotCompile(@TempDir Path modelsDir) {
        ScriptedCompiler compiler = new ScriptedCompiler(Set.of(UNBUILT), true);
        MissingModelBuilder builder = builder(compiler, modelsDir);

        assertThat(builder.build(new GeneratorControls("GovCt2", "Ac6a"), Windings.FOUR_WINDINGS, false, false)).isEmpty();
        assertThat(builder.getLastFailure()).hasValueSatisfying(reason -> assertThat(reason).contains(UNBUILT));
        // another machine asking for the same model is told at once, not by failing it again
        assertThat(builder.build(new GeneratorControls("GovCt2", "Ac6a"), Windings.FOUR_WINDINGS, false, false)).isEmpty();
        assertThat(builder.getLastFailure()).hasValueSatisfying(reason -> assertThat(reason).contains(UNBUILT));
        // and the installation, which compiles, still builds the models that do
        assertThat(builder.build(new GeneratorControls("GovCt2", "Ac1a"), Windings.FOUR_WINDINGS, false, false))
                .hasValue(ALSO_UNBUILT);

        assertThat(compiler.compiled).containsExactly(UNBUILT, ALSO_UNBUILT);
        assertThat(compiler.probes).as("the installation is asked once").isEqualTo(1);
    }

    @Test
    void shouldGiveUpOnTheInstallationThatCompilesNothing(@TempDir Path modelsDir) {
        ScriptedCompiler compiler = new ScriptedCompiler(Set.of(UNBUILT, ALSO_UNBUILT), false);
        MissingModelBuilder builder = builder(compiler, modelsDir);

        assertThat(builder.build(new GeneratorControls("GovCt2", "Ac6a"), Windings.FOUR_WINDINGS, false, false)).isEmpty();
        assertThat(builder.build(new GeneratorControls("GovCt2", "Ac1a"), Windings.FOUR_WINDINGS, false, false)).isEmpty();

        // the rest of the network does not pay a compilation each to be told the same thing
        assertThat(compiler.compiled).containsExactly(UNBUILT);
    }

    @Test
    @Tag("compilation")
    void shouldBuildTheRestWhenARegisteredControlDoesNotCompile(@TempDir Path modelsDir) {
        Assumptions.assumeTrue(Files.exists(HOME.resolve("dynawo.sh")), "no Dynawo installation at " + HOME);

        // GovCt2 under a name of its own, its speed input misspelt, as a study registering it might
        String governors = "Dynawo.Electrical.Controls.Machines.Governors.";
        MachineControlUnit misspelt = new MachineControlUnit("governor", governors + "Standard.Generic.GovCt2",
                governors + "GovernorPmPGen_INIT")
                .named("MyGov")
                .reading("PGenPu", MachineQuantity.ACTIVE_POWER)
                .reading("PmPu", MachineQuantity.MECHANICAL_POWER)
                .reading("omegaPuu", MachineQuantity.SPEED)
                .startingFrom("PGen0Pu", MachineQuantity.INIT_ACTIVE_POWER)
                .startingFrom("Pm0Pu", MachineQuantity.INIT_MECHANICAL_POWER);
        ControlUnitCatalog.getInstance().register(new ControlUnitProvider() {
            @Override
            public List<MachineControlUnit> getGovernors() {
                return List.of(misspelt);
            }
        });
        MissingModelBuilder builder = new MissingModelBuilder(HOME, modelsDir, ModelNaming.DYNAWO_1_7_0);

        assertThat(builder.build(new GeneratorControls("MyGov", "Ac6a"), Windings.FOUR_WINDINGS, false, false)).isEmpty();
        assertThat(builder.getLastFailure()).hasValueSatisfying(reason -> assertThat(reason).contains("omegaPuu"));

        assertThat(builder.build(new GeneratorControls("GovCt2", "Ac6a"), Windings.FOUR_WINDINGS, false, false))
                .hasValue(UNBUILT);
        assertThat(modelsDir.resolve(UNBUILT + ".so")).exists();
    }

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
        assertThat(builder.build(new GeneratorControls("NoSuchGovernor", "Ac6a"), Windings.FOUR_WINDINGS, false, false)).isEmpty();
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
        // and what the attempt wrote is kept, since it is the whole of the evidence of why the
        // model was not built, with no library beside it to be mistaken for one that was
        assertThat(modelsDir.toFile().list()).containsExactly(".GeneratorSynchronousFourWindingsGovCt2Ac6a.work");
        assertThat(builder.getLastFailure()).isPresent();
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
