/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com/)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.criteria;

/**
 * When a criteria is checked over the run — Dynawo's {@code CriteriaParams::CriteriaScope_t}. Its labels
 * are those the criteria file uses ({@code FINAL}, {@code DYNAMIC}).
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public enum CriteriaScope {
    /** Checked once, on the final steady state. */
    FINAL,
    /** Checked throughout the dynamic simulation. */
    DYNAMIC
}
