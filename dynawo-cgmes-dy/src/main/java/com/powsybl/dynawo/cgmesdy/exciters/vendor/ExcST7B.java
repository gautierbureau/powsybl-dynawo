/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.cgmesdy.exciters.vendor;
/** ExcST7B – Static exciter ST7B (non-IEEE variant). CIM: ExcST7B
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>
 */
public record ExcST7B(
    String id, String synchronousMachineId,
    double kh,
    double kia,
    double kl,
    double kpa,
    String oelin,
    double tb,
    double tc,
    double tf,
    double tg,
    double tia,
    double ts,
    String uelin,
    double vmax,
    double vmin,
    double vrmax,
    double vrmin
) { }
