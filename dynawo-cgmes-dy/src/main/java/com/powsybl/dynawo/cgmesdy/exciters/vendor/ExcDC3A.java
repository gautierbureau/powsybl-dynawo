/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.cgmesdy.exciters.vendor;
/** ExcDC3A – Non-IEEE DC exciter variant 3. CIM: ExcDC3A
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>
 */
public record ExcDC3A(
    String id, String synchronousMachineId,
    double efd1,
    double efd2,
    boolean efdlim,
    double efdmax,
    double efdmin,
    boolean exclim,
    double ke,
    double kr,
    double ks,
    double kv,
    double seefd1,
    double seefd2,
    double te,
    double trh,
    double vrmax,
    double vrmin
) { }
