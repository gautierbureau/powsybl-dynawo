/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A study putting a configuration of its own in force while its mappings resolve.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
class MappingConfigStudyScopeTest {

    @Test
    void shouldPutAStudysConfigurationInForceForItsScopeAlone() throws Exception {
        MappingConfig platform = MappingConfig.load();
        MappingConfig study = platform.withModelicaModelsDir(Path.of("/study/modelica"));
        MappingConfig inner = platform.withModelicaModelsDir(Path.of("/inner/modelica"));

        try (MappingConfig.Scope scope = study.useForStudy()) {
            assertThat(MappingConfig.load()).isSameAs(study);
            assertThat(MappingConfig.load().getModelicaModelsDir()).hasValue(Path.of("/study/modelica"));
            // what the deployment set is kept, only the sources are the study's
            assertThat(MappingConfig.load().getBuiltModelsDir()).isEqualTo(platform.getBuiltModelsDir());

            try (MappingConfig.Scope nested = inner.useForStudy()) {
                assertThat(MappingConfig.load()).isSameAs(inner);
            }
            assertThat(MappingConfig.load()).as("the enclosing study's, once the nested scope closes").isSameAs(study);

            // a study running beside it on another thread is not affected
            assertThat(CompletableFuture.supplyAsync(MappingConfig::load).get()).isEqualTo(platform);
        }
        assertThat(MappingConfig.load()).isEqualTo(platform);
    }
}
