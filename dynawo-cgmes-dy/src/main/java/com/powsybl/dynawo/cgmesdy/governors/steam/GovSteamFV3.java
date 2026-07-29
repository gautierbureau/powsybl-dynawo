/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.cgmesdy.governors.steam;
/** GovSteamFV3 – Simplified GovSteamIEEE1 with Prmax and fast valving. CIM: GovSteamFV3
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>
 */
public record GovSteamFV3(
    String id, String synchronousMachineId,
    double k, double k1, double k2, double k3, double mwbase,
    double pmax, double pmin, double prmax,
    double t1, double t2, double t3, double t4, double t5, double t6,
    double ta, double tb, double tc, double uc, double uo
) { }
