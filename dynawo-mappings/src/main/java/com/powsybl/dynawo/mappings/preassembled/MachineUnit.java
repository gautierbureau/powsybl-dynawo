/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.preassembled;

/**
 * The synchronous machine of an assembly, which every control it carries connects to.
 * <p>
 * It names the quantities a control reads or drives rather than letting each control spell them,
 * so that a machine model naming one of them differently is described once, where it belongs.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public interface MachineUnit extends UnitModel {

    String getStatorVoltageVarName();

    String getFieldVoltageVarName();

    String getRotorCurrentVarName();

    String getTerminalVoltageVarName();

    String getStatorCurrentVarName();

    String getStatorCurrentMagnitudeVarName();

    String getSpeedVarName();

    String getMechanicalPowerVarName();

    String getActivePowerVarName();

    String getReactivePowerVarName();

    /**
     * The reactive power at its stator, which a regulator with reactive limits reads apart from the
     * reactive power seen at the terminal.
     */
    String getStatorReactivePowerVarName();

    /**
     * The stator reactive power over its nominal apparent power, which a reactive power control loop
     * reads to hold the machine's reactive output to a share of what it can give.
     */
    String getNominalStatorReactivePowerVarName();

    /**
     * The rotor angle, which a few regulators read to place their quantities on the machine's axes.
     */
    String getRotorAngleVarName();

    /**
     * The direct-axis current, which a regulator resolving its action along the machine's axes reads.
     */
    String getDirectAxisCurrentVarName();

    String getRunningVarName();

    /**
     * The magnitude of the voltage at its terminal, which some exciters read instead of the
     * complex voltage.
     */
    String getVoltageMagnitudeVarName();

    String getInitStatorVoltageVarName();

    String getInitFieldVoltageVarName();

    String getInitRotorCurrentVarName();

    String getInitTerminalVoltageVarName();

    String getInitCurrentVarName();

    String getInitStatorCurrentMagnitudeVarName();

    String getInitMechanicalPowerVarName();

    String getInitActivePowerVarName();

    String getInitReactivePowerVarName();

    String getInitStatorReactivePowerVarName();

    String getInitNominalStatorReactivePowerVarName();

    String getInitRotorAngleVarName();

    String getInitDirectAxisCurrentVarName();

    String getInitVoltageMagnitudeVarName();
}
