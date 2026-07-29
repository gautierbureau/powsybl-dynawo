/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.cgmesdy.exciters.vendor;
/** ExcAC2A – Non-IEEE AC2A high initial response variant. CIM: ExcAC2A
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>
 */
public record ExcAC2A(
    String id, String synchronousMachineId,
    boolean hvgate,
    double ka,
    double kb,
    double kb1,
    double kc,
    double kd,
    double ke,
    double kf,
    double kh,
    double kl,
    double kl1,
    double ks,
    boolean lvgate,
    double seve1,
    double seve2,
    double ta,
    double tb,
    double tc,
    double te,
    double tf,
    double vamax,
    double vamin,
    double ve1,
    double ve2,
    double vfemax,
    double vlr,
    double vrmax,
    double vrmin
) { }
