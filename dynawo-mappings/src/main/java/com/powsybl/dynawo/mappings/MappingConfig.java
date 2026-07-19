/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings;

import com.powsybl.commons.config.PlatformConfig;

/**
 * How far a deployment lets a mapping go on its own.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public record MappingConfig(boolean strict) {

    public static final String MODULE_NAME = "dynawo-mappings";
    private static final String STRICT = "strict";

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
        return new MappingConfig(platformConfig.getOptionalModuleConfig(MODULE_NAME)
                .flatMap(config -> config.getOptionalBooleanProperty(STRICT))
                .orElse(DEFAULT_STRICT));
    }

    public boolean isStrict() {
        return strict;
    }
}
