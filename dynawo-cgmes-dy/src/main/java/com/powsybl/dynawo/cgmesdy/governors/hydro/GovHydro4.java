/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.cgmesdy.governors.hydro;
/** GovHydro4 – Kaplan/Francis hydro turbine-governor (HYGOV4). CIM: GovHydro4
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>
 */
public record GovHydro4(
    String id, String synchronousMachineId,
    double at,
    double bgv0, double bgv1, double bgv2, double bgv3, double bgv4, double bgv5, double bmax,
    double db1, double db2, double dturb, double eps, double gmax, double gmin,
    double gv0, double gv1, double gv2, double gv3, double gv4, double gv5,
    double hdam, double mwbase,
    double pgv0, double pgv1, double pgv2, double pgv3, double pgv4, double pgv5,
    double qn1, double qnl, double rperm, double rtemp, double tblade,
    double tg, double tp, double tr, double tw, double uc, double uo
) { }
