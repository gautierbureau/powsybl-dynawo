/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.cgmesdy.asynchronous;
/**
 * AsynchronousMachineTimeConstantReactance – standard induction machine (time-constant/reactance
 * form). CIM: AsynchronousMachineTimeConstantReactance
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public record AsynchronousMachineTimeConstantReactance(
    String id,
    String asynchronousMachineId,
    // RotatingMachineDynamics
    double mBase,                   // Machine MVA rating (MVA)
    double damping,                 // Damping torque coefficient D
    double inertia,                 // Inertia constant H (s)
    double statorLeakageReactance,  // Stator leakage reactance Xl (PU)
    double statorResistance,        // Stator resistance Rs (PU)
    // AsynchronousMachineTimeConstantReactance
    double tpo,                     // Transient rotor time constant T' (s)
    double tppo,                    // Subtransient rotor time constant T'' (s)
    double xp,                      // Transient reactance X' (PU)
    double xpp,                     // Subtransient reactance X'' (PU)
    double xs                       // Synchronous reactance X (PU)
) { }
