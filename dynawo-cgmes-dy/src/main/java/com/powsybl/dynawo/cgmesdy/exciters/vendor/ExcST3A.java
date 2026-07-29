/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.cgmesdy.exciters.vendor;
/** ExcST3A – IEEE ST3A static exciter (vendor variant). CIM: ExcST3A
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public record ExcST3A(
    String id, String synchronousMachineId,
    double efdmax,
    double kc,
    double kg,
    double ki,
    double kj,
    double km,
    double kp,
    double ks,
    double ks1,
    double tb,
    double tc,
    double thetap,
    double tm,
    double vbmax,
    double vgmax,
    double vimax,
    double vimin,
    double vrmax,
    double vrmin,
    double xl
) { }
