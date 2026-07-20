/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.tools;

import com.powsybl.dynawo.extensions.api.generator.SynchronousGeneratorProperties.Windings;
import com.powsybl.dynawo.mappings.parameters.ModelDescriptionLookup;
import com.powsybl.dynawo.mappings.preassembled.GeneratorAssembly;
import com.powsybl.dynawo.mappings.preassembled.GovernorUnits;
import com.powsybl.dynawo.mappings.preassembled.ModelNaming;
import com.powsybl.dynawo.mappings.preassembled.PreassembledModel;
import com.powsybl.dynawo.mappings.preassembled.VoltageRegulatorUnits;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Describes a model, compiles it and asks it what it expects, which is the whole way from
 * something we say to something a simulation can run.
 * <p>
 * Compiling calls the Modelica compiler, so this is minutes rather than milliseconds and is kept
 * out of the ordinary run.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
@Tag("compilation")
class PreassembledModelCompilerTest {

    private static final Path HOME = Path.of("..", "..", "dynawo");

    @Test
    void shouldCompileAModelWeDescribedAndReadBackWhatItExpects(@TempDir Path modelsDir) {
        Assumptions.assumeTrue(Files.exists(HOME.resolve("dynawo.sh")),
                "no Dynawo installation at " + HOME);

        PreassembledModel model = new GeneratorAssembly(Windings.FOUR_WINDINGS, false, false,
                ModelNaming.DYNAWO_1_7_0)
                .add(VoltageRegulatorUnits.vRProportional())
                .add(GovernorUnits.goverProportional())
                .build("GeneratorSynchronousFourWindingsProportionalRegulations");

        Path library = new PreassembledModelCompiler(HOME).compile(model, modelsDir);

        assertThat(library).exists();
        assertThat(modelsDir.resolve(model.getId() + ".xml")).exists();
        // nothing of the compilation is left behind but what was asked for
        assertThat(modelsDir.toFile().list()).containsExactlyInAnyOrder(
                model.getId() + ".so", model.getId() + ".xml");

        // and the library answers for itself, which is what the parameter generation reads
        var description = ModelDescriptionLookup.fromCompiledModels(modelsDir, HOME).find(model.getId());
        assertThat(description).isPresent();
        assertThat(description.get().name()).isEqualTo(model.getId());
        assertThat(description.get().parameters()).isNotEmpty();
    }

    @Test
    void shouldLeaveALibraryAlreadyThereAlone(@TempDir Path modelsDir) throws Exception {
        Path library = modelsDir.resolve("AlreadyBuilt.so");
        Files.writeString(library, "not really a library");
        // no Dynawo is needed to answer, since nothing is compiled
        assertThat(new PreassembledModelCompiler(Path.of("nowhere"))
                .compile(modelsDir.resolve("AlreadyBuilt.xml"), modelsDir)).isEqualTo(library);
    }
}
