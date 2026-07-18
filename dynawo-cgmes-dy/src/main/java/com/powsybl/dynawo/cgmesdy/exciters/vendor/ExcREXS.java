/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.cgmesdy.exciters.vendor;
/** ExcREXS – REXS rotating excitation system. CIM: ExcREXS
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>
 */
public record ExcREXS(
    String id, String synchronousMachineId,
    double e1, double e2, String fbf, double flimf, double kc, double kd,
    double ke, double kefd, double kf, double kh, double kii, double kip,
    double ks, double kvi, double kvp, double kvphz, double nvphz,
    double se1, double se2, double ta, double tb1, double tb2, double tc1,
    double tc2, double te, double tf, double tf1, double tf2, double tp,
    double vcmax, double vfmax, double vfmin, double vimax, double vrmax,
    double vrmin, double xc
) { }
