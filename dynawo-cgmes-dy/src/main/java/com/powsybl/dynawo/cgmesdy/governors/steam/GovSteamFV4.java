/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.cgmesdy.governors.steam;
/** GovSteamFV4 – Detailed electro-hydraulic steam turbine governor. CIM: GovSteamFV4
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>
 */
public record GovSteamFV4(
    String id, String synchronousMachineId,
    double cpsmn, double cpsmx, double crmn, double crmx, double kdc,
    double kf1, double kf3, double khp, double kic, double kip, double kit,
    double kmp1, double kmp2, double kpc, double kpp, double kpt, double krc, double ksh,
    double lpi, double lps, double mnef, double mxef, double pr1, double pr2, double psmn,
    double rsmimn, double rsmimx, double rvgmn, double rvgmx,
    double srmn, double srmx, double srsmp, double svmn, double svmx,
    double ta, double tam, double tc, double tcm, double tdc, double tf1, double tf2,
    double thp, double tmp, double trh, double tv, double ty,
    double y, double yhpmn, double yhpmx, double ympmn, double ympmx
) { }
