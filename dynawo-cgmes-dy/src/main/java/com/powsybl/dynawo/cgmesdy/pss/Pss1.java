/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.cgmesdy.pss;
/** Pss1 – Italian three-input (speed / frequency / electric power) PSS. CIM: Pss1
 *
 * <p>{@code komega} carries the CIM17 speed-input gain (CIM16 name {@code kw}); both SPARQL paths
 * bind it. {@code vadat} is the signal selector (true = closed, false = open).
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>
 */
public record Pss1(
    String id, String excitationSystemId,
    double kf, double komega, double kpe, double ks, double pmin,
    double t5, double t6, double t7, double t8, double t9, double t10,
    double tpe, boolean vadat, double vsmn, double vsmx
) { }
