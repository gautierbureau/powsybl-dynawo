/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.cgmesdy.governors.steam;
/** GovSteamEU – European simplified steam governor. CIM: GovSteamEU
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>
 */
public record GovSteamEU(
    String id, String synchronousMachineId,
    double chc, double cho, double cic, double cio, double db1, double db2, double hhpmax,
    double ke, double kfcor, double khp, double klp, double komegacor, double mwbase,
    double pmax, double prhmax, double simx,
    double tb, double tdp, double ten, double tf, double tfp, double thp, double tip, double tlp,
    double tp, double trh, double tvhp, double tvip, double tw,
    double wfmax, double wfmin, double wmax1, double wmax2, double wwmax, double wwmin
) { }
