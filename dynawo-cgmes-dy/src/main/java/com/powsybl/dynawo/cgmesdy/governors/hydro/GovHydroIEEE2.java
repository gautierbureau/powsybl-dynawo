/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.cgmesdy.governors.hydro;
/** GovHydroIEEE2 – IEEE type 3 speed-governor model (IEEEG3). CIM: GovHydroIEEE2
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>
 */
public record GovHydroIEEE2(
    String id, String synchronousMachineId,
    double aturb, double bturb,
    double gv1, double gv2, double gv3, double gv4, double gv5, double gv6,
    double kturb, double mwbase,
    double pgv1, double pgv2, double pgv3, double pgv4, double pgv5, double pgv6,
    double pmax, double pmin, double rperm, double rtemp,
    double tg, double tp, double tr, double tw, double uc, double uo
) { }
