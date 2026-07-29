/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.cgmesdy.asynchronous;
/**
 * AsynchronousMachineEquivalentCircuit – induction machine equivalent-circuit form.
 * CIM: AsynchronousMachineEquivalentCircuit
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public record AsynchronousMachineEquivalentCircuit(
    String id,
    String asynchronousMachineId,
    // RotatingMachineDynamics
    double mBase,                   // Machine MVA rating (MVA)
    double damping,                 // Damping torque coefficient D
    double inertia,                 // Inertia constant H (s)
    double statorLeakageReactance,  // Stator leakage reactance Xl (PU)
    double statorResistance,        // Stator resistance Rs (PU)
    // AsynchronousMachineEquivalentCircuit
    double rr1,                     // Damper 1 resistance Rr1 (PU)
    double rr2,                     // Damper 2 resistance Rr2 (PU)
    double xlr1,                    // Damper 1 leakage reactance Xlr1 (PU)
    double xlr2,                    // Damper 2 leakage reactance Xlr2 (PU)
    double xm                       // Magnetizing reactance Xm (PU)
) { }
