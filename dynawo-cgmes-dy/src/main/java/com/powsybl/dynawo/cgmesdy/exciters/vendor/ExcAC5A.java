/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.cgmesdy.exciters.vendor;
/** ExcAC5A – Non-IEEE simplified rotating exciter variant. CIM: ExcAC5A
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>
 */
public record ExcAC5A(
    String id, String synchronousMachineId,
    double a,
    double efd1,
    double efd2,
    double ka,
    double ke,
    double kf,
    double ks,
    double seefd1,
    double seefd2,
    double ta,
    double tb,
    double tc,
    double te,
    double tf1,
    double tf2,
    double tf3,
    double vrmax,
    double vrmin
) { }
