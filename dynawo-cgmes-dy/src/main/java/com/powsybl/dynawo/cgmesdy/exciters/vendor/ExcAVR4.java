/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.cgmesdy.exciters.vendor;
/** ExcAVR4 – European AVR Model 4. CIM: ExcAVR4
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>
 */
public record ExcAVR4(
    String id, String synchronousMachineId,
    boolean imul, double ka, double ke, double kif, double t1, double t1if,
    double t2, double t3, double t4, double tif, double vfmn, double vfmx,
    double vrmn, double vrmx
) { }
