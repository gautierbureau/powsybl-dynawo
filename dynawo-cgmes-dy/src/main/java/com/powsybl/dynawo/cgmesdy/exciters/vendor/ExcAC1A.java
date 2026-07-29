/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.cgmesdy.exciters.vendor;
/** ExcAC1A – Non-IEEE AC1A rotating exciter variant. CIM: ExcAC1A
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>
 */
public record ExcAC1A(
    String id, String synchronousMachineId,
    boolean hvlvgates,
    double ka,
    double kc,
    double kd,
    double ke,
    double kf,
    double kf1,
    double kf2,
    double ks,
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
    double vrmax,
    double vrmin
) { }
