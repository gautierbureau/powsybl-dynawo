/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.cgmesdy.protection;
/** UnderexcLimIEEE2 – IEEE underexcitation limiter type 2. CIM: UnderexcLimIEEE2
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>
 */
public record UnderexcLimIEEE2(
    String id, String excitationSystemId,
    double k1,
    double k2,
    double kfb,
    double kuf,
    double kui,
    double kul,
    double p0,
    double p1,
    double p2,
    double p3,
    double p4,
    double p5,
    double p6,
    double p7,
    double p8,
    double p9,
    double p10,
    double q0,
    double q1,
    double q2,
    double q3,
    double q4,
    double q5,
    double q6,
    double q7,
    double q8,
    double q9,
    double q10,
    double tu1,
    double tu2,
    double tu3,
    double tu4,
    double tul,
    double tup,
    double tuq,
    double tuv,
    double vuimax,
    double vuimin,
    double vulmax,
    double vulmin
) { }
