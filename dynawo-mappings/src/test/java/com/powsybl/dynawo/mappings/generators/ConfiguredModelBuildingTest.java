/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.generators;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.commons.config.InMemoryPlatformConfig;
import com.powsybl.commons.config.MapModuleConfig;
import com.powsybl.dynawo.mappings.MappingConfig;
import com.powsybl.dynawo.mappings.preassembled.ModelNaming;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks that naming a directory to keep built models in is all a deployment has to say, and that
 * saying nothing asks nothing of it.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
class ConfiguredModelBuildingTest {

    private FileSystem fileSystem;
    private InMemoryPlatformConfig platformConfig;

    @BeforeEach
    void setUp() {
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
        platformConfig = new InMemoryPlatformConfig(fileSystem);
    }

    @AfterEach
    void tearDown() throws IOException {
        fileSystem.close();
    }

    @Test
    void shouldBuildNothingWhenNoDirectoryIsNamed() {
        MappingConfig config = MappingConfig.load(platformConfig);
        assertThat(config.getBuiltModelsDir()).isEmpty();
        // and the installation is never asked for, so a deployment building nothing needs no
        // Dynawo configured to say so
        assertThat(MissingModelBuilder.fromConfig(config, ConfiguredModelBuildingTest::noDynawo,
                ModelNaming.DYNAWO_1_7_0)).isEmpty();
    }

    @Test
    void shouldBuildWhereTheDirectoryIsNamed() {
        MapModuleConfig moduleConfig = platformConfig.createModuleConfig(MappingConfig.MODULE_NAME);
        moduleConfig.setStringProperty("builtModelsDir", "/models");

        MappingConfig config = MappingConfig.load(platformConfig);
        assertThat(config.getBuiltModelsDir()).isPresent();

        Optional<MissingModelBuilder> builder = MissingModelBuilder.fromConfig(config,
                () -> fileSystem.getPath("/dynawo"), ModelNaming.DYNAWO_1_7_0);
        assertThat(builder).isPresent();
        assertThat(builder.get().getModelsDir()).isEqualTo(fileSystem.getPath("/models"));
    }

    private static Path noDynawo() {
        throw new AssertionError("the installation was asked for although nothing is to be built");
    }
}
