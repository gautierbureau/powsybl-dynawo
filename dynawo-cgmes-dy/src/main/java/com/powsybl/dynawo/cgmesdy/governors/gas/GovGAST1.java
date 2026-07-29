/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.cgmesdy.governors.gas;
/** GovGAST1 – Detailed gas turbine governor. CIM: GovGAST1
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>
 */
public record GovGAST1(
    String id, String synchronousMachineId,
    double mwbase, double r, double db1, double db2, double eps,
    double t1, double t2, double t3, double t4, double t5, double tltr,
    double ka, double kt, double a, double b,
    double lmax, double loadinc, double ltrate, double rmax, double fidle,
    double gv1, double pgv1, double gv2, double pgv2, double gv3, double pgv3,
    double gv4, double pgv4, double gv5, double pgv5, double gv6, double pgv6,
    double vmax, double vmin
) { }
