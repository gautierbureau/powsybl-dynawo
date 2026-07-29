/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.cgmesdy.exciters.vendor;
/** ExcAC6A – Non-IEEE AC6A stationary rectifier variant. CIM: ExcAC6A
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>
 */
public record ExcAC6A(
    String id, String synchronousMachineId,
    double ka,
    double kc,
    double kd,
    double ke,
    double kh,
    double ks,
    double seve1,
    double seve2,
    double ta,
    double tb,
    double tc,
    double te,
    double th,
    double tj,
    double tk,
    double vamax,
    double vamin,
    double ve1,
    double ve2,
    double vfelim,
    double vhmax,
    double vrmax,
    double vrmin
) { }
