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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks what settles whether a missing model is built: the installation, not the configuration.
 * <p>
 * A deployment that says nothing still gets its models built, somewhere sensible. What stops
 * building is an installation that cannot do it, which is asked rather than assumed.
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
    void shouldBuildSomewhereByDefaultWhenNoDirectoryIsNamed() throws IOException {
        MappingConfig config = MappingConfig.load(platformConfig);
        assertThat(config.getBuiltModelsDir()).isEmpty();

        // a deployment that says nothing still builds what a machine asks for, since needing a
        // study to be configured before it can have its model only means an unconfigured study
        // quietly ran on the wrong one
        Optional<MissingModelBuilder> builder = MissingModelBuilder.fromConfig(config,
                this::installationThatBuilds, ModelNaming.DYNAWO_1_7_0);
        assertThat(builder).isPresent();
        assertThat(builder.get().getModelsDir()).isEqualTo(config.getOrCreateBuiltModelsDir());
    }

    @Test
    void shouldBuildWhereTheDirectoryIsNamed() throws IOException {
        MapModuleConfig moduleConfig = platformConfig.createModuleConfig(MappingConfig.MODULE_NAME);
        moduleConfig.setStringProperty("builtModelsDir", "/models");

        MappingConfig config = MappingConfig.load(platformConfig);
        assertThat(config.getBuiltModelsDir()).isPresent();

        Optional<MissingModelBuilder> builder = MissingModelBuilder.fromConfig(config,
                this::installationThatBuilds, ModelNaming.DYNAWO_1_7_0);
        assertThat(builder).isPresent();
        assertThat(builder.get().getModelsDir()).isEqualTo(fileSystem.getPath("/models"));
    }

    @Test
    void shouldBuildNothingWhereTheInstallationCannot() throws IOException {
        MappingConfig config = MappingConfig.load(platformConfig);

        // an installation whose launcher does not carry the option that builds a model, which is
        // the only thing that stops a machine from getting the model it asked for
        Path homeDir = fileSystem.getPath("/old-dynawo");
        Files.createDirectories(homeDir);
        Files.writeString(homeDir.resolve("dynawo.sh"), "#!/bin/bash\necho jobs --dump-model\n");

        assertThat(MissingModelBuilder.fromConfig(config, () -> homeDir, ModelNaming.DYNAWO_1_7_0))
                .isEmpty();
    }

    @Test
    void shouldBuildNothingWhereThereIsNoInstallationAtAll() {
        assertThat(MissingModelBuilder.fromConfig(MappingConfig.load(platformConfig),
                () -> fileSystem.getPath("/nowhere"), ModelNaming.DYNAWO_1_7_0)).isEmpty();
    }

    /**
     * An installation whose launcher carries the option that builds a preassembled model.
     */
    private Path installationThatBuilds() {
        Path homeDir = fileSystem.getPath("/dynawo");
        try {
            Files.createDirectories(homeDir);
            Files.writeString(homeDir.resolve("dynawo.sh"),
                    "#!/bin/bash\necho jobs --generate-preassembled --dump-model\n");
        } catch (IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
        return homeDir;
    }
}
