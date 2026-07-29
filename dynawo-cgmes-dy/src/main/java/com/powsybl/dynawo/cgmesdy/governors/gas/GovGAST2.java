/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.cgmesdy.governors.gas;
/** GovGAST2 – Gas turbine (Rowen GAST2A) with temperature control. CIM: GovGAST2
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>
 */
public record GovGAST2(
    String id, String synchronousMachineId,
    double a, double af1, double af2, double b, double bf1, double bf2,
    double c, double cf2, double ecr, double etd,
    double k3, double k4, double k5, double k6, double kf, double mwbase,
    double t, double t3, double t4, double t5, double tc, double tcd, double tf,
    double tmax, double tmin, double tr, double trate, double tt,
    double w, double x, double y, boolean z
) { }
