/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.cgmesdy.wind;
/** WindPlantReactiveControlIEC – Plant-level reactive power control. CIM: WindPlantReactiveControlIEC
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>
 */
public record WindPlantReactiveControlIEC(
    String id,
    double kiwpx,
    double kpwpx,
    double kwpqu,
    double mwppf,
    double mwpu,
    double twppfilt,
    double twpqfilt,
    double twpufilt,
    double txft,
    double txfv,
    double uwpqdip,
    double xrefmax,
    double xrefmin
) { }
