/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.controls;

import java.util.Objects;

/**
 * A governor / voltage regulator couple, once translated to its simplified counterpart.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public record SimplifiedControls(String governor, String voltageRegulator) {

    public SimplifiedControls {
        Objects.requireNonNull(governor);
        Objects.requireNonNull(voltageRegulator);
    }
}
