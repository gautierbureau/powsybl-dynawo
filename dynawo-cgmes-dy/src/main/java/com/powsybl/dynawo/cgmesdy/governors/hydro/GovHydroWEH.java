/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.cgmesdy.governors.hydro;
/** GovHydroWEH – Woodward Electric Hydro governor. CIM: GovHydroWEH
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>
 */
public record GovHydroWEH(
    String id, String synchronousMachineId,
    double db, double dicn, double dpv, double dturb, boolean feedbackSignal,
    double fl1, double fl2, double fl3, double fl4, double fl5,
    double fp1, double fp2, double fp3, double fp4, double fp5,
    double fp6, double fp7, double fp8, double fp9, double fp10,
    double gmax, double gmin, double gtmxcl, double gtmxop,
    double gv1, double gv2, double gv3, double gv4, double gv5,
    double kd, double ki, double kp, double mwbase,
    double pmss1, double pmss2, double pmss3, double pmss4, double pmss5,
    double pmss6, double pmss7, double pmss8, double pmss9, double pmss10,
    double rpg, double rpp, double td, double tdv, double tg, double tp, double tpe, double tw
) { }
