/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.cgmesdy.exciters.vendor;
/** ExcELIN2 – ELIN exciter model 2. CIM: ExcELIN2
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>
 */
public record ExcELIN2(
    String id, String synchronousMachineId,
    double efdbas, double iefmax, double iefmax2, double iefmin, double k1,
    double k1ec, double k2, double k3, double k4, double kd1,
    double ke2, double ketb, double pid1max, double seve1, double seve2,
    double tb1, double te, double te2, double ti1, double ti3,
    double ti4, double tr4, double upmax, double upmin, double ve1,
    double ve2, double xp
) { }
