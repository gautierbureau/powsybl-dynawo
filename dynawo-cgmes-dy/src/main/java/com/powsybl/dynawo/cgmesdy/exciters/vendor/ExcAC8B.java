/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.cgmesdy.exciters.vendor;
/** ExcAC8B – Non-IEEE AC8B PID exciter variant. CIM: ExcAC8B
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>
 */
public record ExcAC8B(
    String id, String synchronousMachineId,
    boolean inlim,
    double ka,
    double kc,
    double kd,
    double kdr,
    double ke,
    double kir,
    double kpr,
    double ks,
    boolean pidlim,
    double seve1,
    double seve2,
    double ta,
    double tdr,
    double te,
    boolean telim,
    double ve1,
    double ve2,
    double vemin,
    double vfemax,
    double vimax,
    double vimin,
    double vpidmax,
    double vpidmin,
    double vrmax,
    double vrmin,
    boolean vtmult
) { }
