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
 * The GovCt2 turbine governor, reading the speed of the machine and the power it generates, and
 * driving its mechanical power.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public class GovCt2Unit implements ControlUnit {

    private static final String PACKAGE = "Dynawo.Electrical.Controls.Machines.Governors.";

    @Override
    public String getId() {
        return "governor";
    }

    @Override
    public String getName() {
        return PACKAGE + "Standard.Generic.GovCt2";
    }

    @Override
    public String getInitName() {
        return PACKAGE + "GovernorPmPGen_INIT";
    }

    @Override
    public List<UnitConnection> getConnectionsWith(MachineUnit machine) {
        return List.of(
                UnitConnection.of(machine, machine.getSpeedVarName(), this, "omegaPu"),
                UnitConnection.of(machine, machine.getMechanicalPowerVarName(), this, "PmPu"),
                UnitConnection.of(this, "PGenPu", machine, machine.getActivePowerVarName()));
    }

    @Override
    public List<UnitConnection> getInitConnectionsWith(MachineUnit machine) {
        return List.of(
                UnitConnection.ofInitialisation(this, "Pm0Pu", machine, machine.getInitMechanicalPowerVarName()),
                UnitConnection.ofInitialisation(this, "PGen0Pu", machine, machine.getInitActivePowerVarName()));
    }
}
