/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.tools;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dynawo.desc.ModelDescription;
import com.powsybl.dynawo.mappings.parameters.ModelDescriptionLookup;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Checks that a compiled model can be asked what it expects, against the Dynawo installed beside
 * this workspace.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
class DumpModelTest {

    private static final Path HOME = Path.of("..", "..", "dynawo");
    private static final String SHIPPED = "GeneratorSynchronousFourWindingsProportionalRegulations";

    @Test
    void shouldReadTheDescriptionOutOfALibraryDynawoShips(@TempDir Path modelsDir) throws IOException {
        Path library = HOME.resolve("ddb").resolve(SHIPPED + ".so");
        Assumptions.assumeTrue(Files.exists(library), "no Dynawo installation at " + HOME);

        // taken away from its own description, so that what comes out is read from the library
        // rather than found beside it
        Path copy = modelsDir.resolve(SHIPPED + ".so");
        Files.copy(library, copy);

        ModelDescriptionLookup lookup = ModelDescriptionLookup.fromCompiledModels(modelsDir, HOME);
        Optional<ModelDescription> description = lookup.find(SHIPPED);

        assertThat(description).isPresent();
        assertThat(description.get().name()).isEqualTo(SHIPPED);
        assertThat(description.get().parameters()).isNotEmpty();
        // and the description is left beside the library, the way a shipped one sits beside its own
        assertThat(modelsDir.resolve(SHIPPED + ".desc.xml")).exists();
    }

    @Test
    void shouldSayNothingAboutAModelTheDirectoryDoesNotHold(@TempDir Path modelsDir) {
        assertThat(ModelDescriptionLookup.fromCompiledModels(modelsDir, HOME).find("NoSuchModel"))
                .isEmpty();
    }

    @Test
    void shouldRefuseALibraryItCannotRead(@TempDir Path modelsDir) throws IOException {
        Assumptions.assumeTrue(Files.exists(HOME.resolve("dynawo.sh")),
                "no Dynawo installation at " + HOME);
        Files.writeString(modelsDir.resolve("Broken.so"), "not a library");

        assertThatThrownBy(() -> ModelDescriptionLookup.fromCompiledModels(modelsDir, HOME).find("Broken"))
                .isInstanceOf(PowsyblException.class)
                .hasMessageContaining("Broken");
    }

    @Test
    void shouldFallBackOnTheModelsDynawoShips(@TempDir Path modelsDir) {
        Assumptions.assumeTrue(Files.exists(HOME.resolve("ddb")), "no Dynawo installation at " + HOME);
        ModelDescriptionLookup lookup = ModelDescriptionLookup.fromCompiledModels(modelsDir, HOME)
                .orElse(ModelDescriptionLookup.fromModelDatabase(HOME));

        // the directory of our own holds nothing, so the answer can only come from the database
        assertThat(lookup.find(SHIPPED)).isPresent();
        assertThat(lookup.find("NoSuchModel")).isEmpty();
    }
}
