/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.cgmesdy.governors.gas;
/** GovGASTWD – Woodward gas turbine (Rowen with PID governor). CIM: GovGASTWD
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>
 */
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public record GovGASTWD(
    String id, String synchronousMachineId,
    double a, double af1, double af2, double b, double bf1, double bf2,
    double c, double cf2, double ecr, double etd,
    double k3, double k4, double k5, double k6, double kd, double kdroop,
    double kf, double ki, double kp, double mwbase,
    double t, double t3, double t4, double t5, double tc, double tcd, double td, double tf,
    double tmax, double tmin, double tr, double trate, double tt
) { }
