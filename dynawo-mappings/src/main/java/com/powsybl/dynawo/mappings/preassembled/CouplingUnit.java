/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.preassembled;

/**
 * The switch coupling a generating unit to the network.
 * <p>
 * Standing next to the machine, with auxiliaries hanging on it and no transformer between, it is
 * what initialises the machine and needs a model saying so. Behind a transformer it has nothing to
 * work out and needs none.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public class CouplingUnit implements SeriesUnit {

    private static final String PACKAGE = "Dynawo.Electrical.Switches.";

    private final boolean initialisesMachine;

    public CouplingUnit(boolean initialisesMachine) {
        this.initialisesMachine = initialisesMachine;
    }

    @Override
    public String getId() {
        return "coupling";
    }

    @Override
    public String getName() {
        return PACKAGE + (initialisesMachine ? "IdealSwitch2" : "IdealSwitch");
    }

    @Override
    public String getInitName() {
        return initialisesMachine ? PACKAGE + "IdealSwitchGeneratorAux_INIT" : null;
    }

    @Override
    public String getGridSideTerminalVarName() {
        return "terminal1";
    }

    @Override
    public String getMachineSideTerminalVarName() {
        return "terminal2";
    }

    @Override
    public String getSwitchOffSignalVarName() {
        return "switchOffSignal1";
    }

    @Override
    public String getInitMachineSideVoltageVarName() {
        return "U20Pu";
    }

    @Override
    public String getInitMachineSideAngleVarName() {
        return "U2Phase0";
    }

    @Override
    public String getInitMachineSideActivePowerVarName() {
        return "P20Pu";
    }

    @Override
    public String getInitMachineSideReactivePowerVarName() {
        return "Q20Pu";
    }

    @Override
    public String getInitAuxiliaryVoltageVarName(boolean gridSide) {
        return gridSide ? "u10Pu" : "u20Pu";
    }

    @Override
    public String getInitAuxiliaryActivePowerVarName(boolean gridSide) {
        return gridSide ? "PAuxHV0Pu" : "PAuxLV0Pu";
    }

    @Override
    public String getInitAuxiliaryReactivePowerVarName(boolean gridSide) {
        return gridSide ? "QAuxHV0Pu" : "QAuxLV0Pu";
    }
}
