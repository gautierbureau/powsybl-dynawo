/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.cgmesdy.governors.hydro;
/** GovHydroR – Hydro turbine and governor (4th-order lead-lag). CIM: GovHydroR
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>
 */
public record GovHydroR(
    String id, String synchronousMachineId,
    double at, double db1, double db2, double dturb, double eps,
    double gmax, double gmin, double gv1, double gv2, double gv3, double gv4, double gv5, double gv6,
    double h0, boolean inputSignal, double kg, double ki, double mwbase,
    double pgv1, double pgv2, double pgv3, double pgv4, double pgv5, double pgv6,
    double pmax, double pmin, double qnl, double r,
    double t1, double t2, double t3, double t4, double t5, double t6, double t7, double t8,
    double td, double tp, double tt, double tw, double velcl, double velop
) { }
