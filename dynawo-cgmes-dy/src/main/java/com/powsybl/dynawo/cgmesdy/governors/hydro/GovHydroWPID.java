/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.cgmesdy.governors.hydro;
/** GovHydroWPID – Woodward PID hydro governor. CIM: GovHydroWPID
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>
 */
public record GovHydroWPID(
    String id, String synchronousMachineId,
    double mwbase, double reg, double treg, double tw, double ta, double tb,
    double pmax, double pmin, double gatmax, double gatmin,
    double d, double kd, double ki, double kp, double velmax, double velmin,
    double gv1, double pgv1, double gv2, double pgv2, double gv3, double pgv3
) { }
