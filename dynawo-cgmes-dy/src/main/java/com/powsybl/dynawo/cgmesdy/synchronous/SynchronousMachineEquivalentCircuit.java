/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.cgmesdy.synchronous;

/**
 * Equivalent-circuit parameterisation of a synchronous machine.
 *
 * <p>CIM class: {@code SynchronousMachineEquivalentCircuit}
 * (sub-class of {@code SynchronousMachineDetailed → SynchronousMachineDynamics
 *  → RotatingMachineDynamics}).</p>
 *
 * <p>All reactances and resistances are in per-unit on the machine MVA base
 * ({@link #mBase}).  Two damper windings per axis are supported; set the
 * corresponding parameters to zero when only one damper is modelled.</p>
 *
 * <h3>d-axis circuit</h3>
 * <pre>
 *   stator ──Xl──┬──Xad──┬──Xf1d──Xfd──Rfd──┐  (field winding)
 *                │       └────────X1d──R1d──┐ │  (damper 1)
 *                │                X2d──R2d──┘ │  (damper 2, if present)
 *               Ra                            │
 * </pre>
 *
 * <h3>q-axis circuit</h3>
 * <pre>
 *   stator ──Xl──┬──Xaq──┬──X1q──R1q──┐  (damper 1)
 *                │       └──X2q──R2q──┘  (damper 2, if present)
 *               Ra
 * </pre>
 *
 * <h3>Parameter mapping to CIM attributes</h3>
 * <table>
 *   <tr><th>Java field</th><th>CIM attribute</th></tr>
 *   <tr><td>r1d, x1d</td><td>SynchronousMachineEquivalentCircuit.r1d / x1d</td></tr>
 *   <tr><td>rfd, xfd</td><td>SynchronousMachineEquivalentCircuit.rfd / xfd</td></tr>
 *   <tr><td>r1q, x1q</td><td>SynchronousMachineEquivalentCircuit.r1q / x1q</td></tr>
 *   <tr><td>r2q, x2q</td><td>SynchronousMachineEquivalentCircuit.r2q / x2q</td></tr>
 *   <tr><td>xad</td><td>SynchronousMachineEquivalentCircuit.xad  (d-axis mutual Xad)</td></tr>
 *   <tr><td>xaq</td><td>SynchronousMachineEquivalentCircuit.xaq  (q-axis mutual Xaq)</td></tr>
 *   <tr><td>xf1d</td><td>SynchronousMachineEquivalentCircuit.xf1d (field–damper1 mutual)</td></tr>
 * </table>
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>
 */
public record SynchronousMachineEquivalentCircuit(
    String id,
    String synchronousMachineId,

    // RotatingMachineDynamics
    double mBase,
    double damping,
    double inertia,
    double statorLeakageReactance,
    double statorResistance,

    // SynchronousMachineDetailed
    String ifdBaseType,
    double saturationFactor,
    double saturationFactor120,

    // d-axis parameters
    /** d-axis 1st damper resistance R1d (PU). */
    double r1d,
    /** d-axis 1st damper reactance X1d (PU). */
    double x1d,
    /** Field winding resistance Rfd (PU). */
    double rfd,
    /** Field winding reactance Xfd (PU). */
    double xfd,

    // q-axis parameters
    /** q-axis 1st damper resistance R1q (PU). */
    double r1q,
    /** q-axis 1st damper reactance X1q (PU). */
    double x1q,
    /** q-axis 2nd damper resistance R2q (PU); 0 if not present. */
    double r2q,
    /** q-axis 2nd damper reactance X2q (PU); 0 if not present. */
    double x2q,

    // mutual / magnetising reactances
    /** d-axis mutual reactance Xad (PU). */
    double xad,
    /** q-axis mutual reactance Xaq (PU). */
    double xaq,
    /** Mutual reactance between field and d-axis damper Xf1d (PU). */
    double xf1d
) { }
