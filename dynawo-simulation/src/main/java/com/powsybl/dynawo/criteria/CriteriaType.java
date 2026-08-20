/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com/)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.criteria;

/**
 * How a criteria aggregates its components — Dynawo's {@code CriteriaParams::CriteriaType_t}. Its labels
 * are those the criteria file uses ({@code LOCAL_VALUE}, {@code SUM}).
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public enum CriteriaType {
    /** Each component is checked on its own value. */
    LOCAL_VALUE,
    /** The components' values are summed and the sum is checked. */
    SUM
}
