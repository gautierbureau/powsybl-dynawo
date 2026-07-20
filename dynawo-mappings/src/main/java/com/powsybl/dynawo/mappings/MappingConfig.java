/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings;

import com.powsybl.commons.config.PlatformConfig;

import java.nio.file.Path;
import java.util.Optional;

/**
 * How far a deployment lets a mapping go on its own.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public record MappingConfig(boolean strict, Path builtModelsDir) {

    public static final String MODULE_NAME = "dynawo-mappings";
    private static final String STRICT = "strict";
    private static final String BUILT_MODELS_DIR = "builtModelsDir";

    /**
     * A strict deployment never lets a parameter appear on its own: a model given to an equipment
     * has to be valued by a set that already suits it, and one that does not is refused rather than
     * completed. Studies run this way where the parameters are held in a database and expected to
     * be seen and edited there, rather than written by whoever asked for the simulation.
     */
    public static final boolean DEFAULT_STRICT = false;

    public static MappingConfig load() {
        return load(PlatformConfig.defaultConfig());
    }

    public static MappingConfig load(PlatformConfig platformConfig) {
        Optional<com.powsybl.commons.config.ModuleConfig> config =
                platformConfig.getOptionalModuleConfig(MODULE_NAME);
        return new MappingConfig(
                config.flatMap(c -> c.getOptionalBooleanProperty(STRICT)).orElse(DEFAULT_STRICT),
                config.flatMap(c -> c.getOptionalPathProperty(BUILT_MODELS_DIR)).orElse(null));
    }

    public boolean isStrict() {
        return strict;
    }

    /**
     * Where a model built for a generator no installed model suits is kept, or nothing where a
     * deployment would rather make do with what is installed.
     * <p>
     * Naming one turns on building: a generator whose controls the catalog cannot answer gets a
     * model compiled for it there, which the simulation is then told to look in besides the models
     * Dynawo ships. Left unset, such a generator goes unmapped as before.
     */
    public Optional<Path> getBuiltModelsDir() {
        return Optional.ofNullable(builtModelsDir);
    }
}
