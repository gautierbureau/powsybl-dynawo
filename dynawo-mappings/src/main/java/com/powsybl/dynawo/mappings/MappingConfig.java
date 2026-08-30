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
     * Where a model built for a generator no installed model suits is kept, as the deployment set
     * it, or nothing where it said nothing and the default below is used.
     */
    public Optional<Path> getBuiltModelsDir() {
        return Optional.ofNullable(builtModelsDir);
    }

    /**
     * Where such a model is kept, whether or not the deployment said anything.
     * <p>
     * A machine asking for controls no installed model implements is a machine we can compile a
     * model for, so it gets one: needing a study to be configured before it can have the model it
     * asked for only means that a study nobody configured quietly ran on the wrong models. What a
     * deployment chooses is where they are kept, not whether they exist, so an unset directory
     * falls back here rather than turning building off.
     * <p>
     * The default sits beside the rest of the user's powsybl state and is kept between runs, since
     * a model costs half a minute to compile and none at all to find already built.
     */
    public Path getOrCreateBuiltModelsDir() {
        return builtModelsDir != null ? builtModelsDir : defaultBuiltModelsDir();
    }

    private static Path defaultBuiltModelsDir() {
        return Path.of(System.getProperty("user.home"), ".powsybl", "dynawo-built-models");
    }
}
