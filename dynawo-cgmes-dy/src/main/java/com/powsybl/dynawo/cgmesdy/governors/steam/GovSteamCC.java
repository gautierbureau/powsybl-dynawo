/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.cgmesdy.governors.steam;
/** GovSteamCC – Combined cycle (cross-compound) steam governor. CIM: GovSteamCC.
 *
 * <p>Two turbine-governor trains (HP and LP) act in parallel: each has its own droop
 * ({@code rhp}/{@code rlp}), governor time constant ({@code t1hp}/{@code t1lp}), three turbine
 * time constants ({@code t3,t4,t5}), output fraction ({@code fhp}/{@code flp}, {@code fhp+flp=1}),
 * damping ({@code dhp}/{@code dlp}) and maximum valve position ({@code pmaxhp}/{@code pmaxlp}).
 * The two mechanical powers are summed. Attribute set is identical in CGMES 2.4.15 and 3.0.0.
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>
 */
public record GovSteamCC(
    String id, String synchronousMachineId,
    double mwbase,
    double rhp, double t1hp, double t3hp, double t4hp, double t5hp,
    double fhp, double dhp, double pmaxhp,
    double rlp, double t1lp, double t3lp, double t4lp, double t5lp,
    double flp, double dlp, double pmaxlp
) { }
