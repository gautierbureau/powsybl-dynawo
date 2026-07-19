/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.preassembled;

import java.util.List;

/**
 * The ST4B exciter of the IEEE standard, a voltage regulator reading the stator voltage, the
 * terminal voltage and current and the rotor current, and driving the field voltage.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public class St4bUnit implements ControlUnit {

    private static final String PACKAGE = "Dynawo.Electrical.Controls.Machines.VoltageRegulators.Standard.";

    @Override
    public String getId() {
        return "voltageRegulator";
    }

    @Override
    public String getName() {
        return PACKAGE + "St4b";
    }

    @Override
    public String getInitName() {
        return PACKAGE + "St4b_INIT";
    }

    @Override
    public List<UnitConnection> getConnectionsWith(MachineUnit machine) {
        return List.of(
                UnitConnection.of(machine, machine.getStatorVoltageVarName(), this, "UsPu"),
                UnitConnection.of(machine, machine.getFieldVoltageVarName(), this, "EfdPu"),
                UnitConnection.of(this, "running", machine, machine.getRunningVarName()),
                UnitConnection.of(this, "utPu", machine, machine.getTerminalVoltageVarName()),
                UnitConnection.of(this, "itPu", machine, machine.getStatorCurrentVarName()),
                UnitConnection.of(this, "IrPu", machine, machine.getRotorCurrentVarName()));
    }

    @Override
    public List<UnitConnection> getInitConnectionsWith(MachineUnit machine) {
        return List.of(
                UnitConnection.ofInitialisation(this, "Us0Pu", machine, machine.getInitStatorVoltageVarName()),
                UnitConnection.ofInitialisation(this, "Efd0Pu", machine, machine.getInitFieldVoltageVarName()),
                UnitConnection.ofInitialisation(this, "ut0Pu", machine, machine.getInitTerminalVoltageVarName()),
                UnitConnection.ofInitialisation(this, "it0Pu", machine, machine.getInitCurrentVarName()),
                UnitConnection.ofInitialisation(this, "Ir0Pu", machine, machine.getInitRotorCurrentVarName()));
    }
}
