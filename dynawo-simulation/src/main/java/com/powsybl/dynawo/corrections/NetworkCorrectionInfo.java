/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.corrections;

import java.util.Objects;

/**
 * The identity of a {@link NetworkCorrection}: the name a study activates it by, and a description of
 * what it changes.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public record NetworkCorrectionInfo(String name, String description) {

    public NetworkCorrectionInfo(String name, String description) {
        this.name = Objects.requireNonNull(name);
        this.description = description;
    }
}
