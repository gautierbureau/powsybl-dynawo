/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.preassembled;

/**
 * What a generating unit consumes to run itself, hanging either on the machine side or on the grid
 * side of its transformer.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public class AuxiliaryUnit implements UnitModel {

    private static final String PACKAGE = "Dynawo.Electrical.Loads.";

    private final boolean gridSide;

    public AuxiliaryUnit(boolean gridSide) {
        this.gridSide = gridSide;
    }

    public boolean isGridSide() {
        return gridSide;
    }

    @Override
    public String getId() {
        return gridSide ? "auxHV" : "auxLV";
    }

    @Override
    public String getName() {
        return PACKAGE + "LoadAlphaBeta";
    }

    @Override
    public String getInitName() {
        return PACKAGE + "LoadAuxiliaries_INIT";
    }

    public String getTerminalVarName() {
        return "terminal";
    }

    public String getSwitchOffSignalVarName() {
        return "switchOffSignal1";
    }

    public String getInitVoltageVarName() {
        return "u0Pu";
    }

    public String getInitActivePowerVarName() {
        return "P0Pu";
    }

    public String getInitReactivePowerVarName() {
        return "Q0Pu";
    }
}
