/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings;

import com.powsybl.dynawo.DynawoSimulationParameters;
import com.powsybl.dynawo.mappings.generators.MissingModelBuilder;
import com.powsybl.dynawo.mappings.parameters.ModelDescriptionLookup;
import com.powsybl.dynawo.mappings.preassembled.ModelNaming;
import com.powsybl.dynawo.parameters.ParametersSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks that a model the mapping had to build reaches the simulation: the directory it went into
 * is named in the parameters, so the jobs file points at it, and the model answers for its own
 * parameters.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
class BuiltModelsReachTheSimulationTest {

    private static final Path HOME = Path.of("..", "..", "dynawo");

    @Test
    void shouldNameTheDirectoryOnlyOnceSomethingIsInIt(@TempDir Path modelsDir) {
        DynawoSimulationParameters parameters = DynawoSimulationParameters.load();
        MissingModelBuilder builder = new MissingModelBuilder(HOME, modelsDir, ModelNaming.DYNAWO_1_7_0);

        // an empty directory is not named: Dynawo refuses one holding nothing, and a run over
        // models that all exist builds nothing
        DynamicModelsMappings.addBuiltModelsDir(mappingBuilding(builder), parameters);
        assertThat(parameters.getPrecompiledModelsDirs()).isEmpty();
    }

    @Test
    void shouldReadTheParametersOfAModelOutOfTheLibraryItWasBuiltInto(@TempDir Path modelsDir) throws Exception {
        // a library of our own, and a lookup that knows nothing of it
        Files.createFile(modelsDir.resolve("SomethingWeBuilt.so"));
        ModelDescriptionLookup installed = lib -> java.util.Optional.empty();

        MissingModelBuilder builder = new MissingModelBuilder(HOME, modelsDir, ModelNaming.DYNAWO_1_7_0);
        // the lookup it gives back looks in the directory first, so a built model is described
        // from its own library rather than being unknown
        assertThat(builder.describe(installed)).isNotSameAs(installed);
    }

    private static DynamicModelsMapping mappingBuilding(MissingModelBuilder builder) {
        return new DynamicModelsMapping() {
            @Override
            public String getName() {
                return "test";
            }

            @Override
            public DynawoSimulationParameters.SolverType getSolverType() {
                return DynawoSimulationParameters.SolverType.SIM;
            }

            @Override
            public void createExtensions(com.powsybl.iidm.network.Network network) {
                // nothing to create
            }

            @Override
            public List<MappedModelsSupplier.MappedModel> createModelConfigs(com.powsybl.iidm.network.Network network) {
                return List.of();
            }

            @Override
            public List<ParametersSet> createParameters(com.powsybl.iidm.network.Network network,
                                                        ModelDescriptionLookup descriptions) {
                return List.of();
            }

            @Override
            public java.util.Optional<Path> getBuiltModelsDir() {
                return java.util.Optional.of(builder.getModelsDir());
            }
        };
    }
}
