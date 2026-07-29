/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.cgmesdy.exciters.vendor;
/** ExcDC1A – Non-IEEE DC exciter variant 1. CIM: ExcDC1A
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>
 */
public record ExcDC1A(
    String id, String synchronousMachineId,
    double efd1,
    double efd2,
    double efdmax,
    double efdmin,
    boolean exclim,
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
    double tf,
    double vrmax,
    double vrmin
) { }
