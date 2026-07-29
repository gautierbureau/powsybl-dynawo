/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.cgmesdy.exciters.vendor;
/** ExcPIC – Proportional-integral controller exciter. CIM: ExcPIC
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>
 */
public record ExcPIC(
    String id, String synchronousMachineId,
    double e1, double e2, double efdmax, double efdmin,
    double ka, double kc, double ke, double kf, double ki, double kp,
    double se1, double se2,
    double ta1, double ta2, double ta3, double ta4, double te, double tf1, double tf2,
    double vr1, double vr2, double vrmax, double vrmin
) { }
