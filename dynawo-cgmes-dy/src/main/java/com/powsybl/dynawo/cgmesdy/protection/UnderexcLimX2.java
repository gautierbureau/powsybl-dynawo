/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.cgmesdy.protection;
/** UnderexcLimX2 – Extended underexcitation limiter variant 2. CIM: UnderexcLimX2
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>
 */
public record UnderexcLimX2(
    String id, String excitationSystemId,
    double kf2,
    double km,
    double melmax,
    double qo,
    double r,
    double tf2,
    double tm
) { }
