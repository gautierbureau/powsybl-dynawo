/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.preassembled;

/**
 * An element standing between the machine and the grid: the transformer a unit is connected
 * through, the switch coupling it to the network.
 * <p>
 * The one next to the machine initialises it, the machine reading its own operating point from
 * that element's machine side rather than from the grid. That is why adding auxiliaries alone
 * still changes how the machine is initialised: the switch they hang on has taken that place.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public interface SeriesUnit extends UnitModel {

    String getGridSideTerminalVarName();

    String getMachineSideTerminalVarName();

    String getSwitchOffSignalVarName();

    String getInitMachineSideVoltageVarName();

    String getInitMachineSideAngleVarName();

    String getInitMachineSideActivePowerVarName();

    String getInitMachineSideReactivePowerVarName();

    /**
     * Name of the voltage the auxiliaries hanging on the given side read.
     */
    String getInitAuxiliaryVoltageVarName(boolean gridSide);

    String getInitAuxiliaryActivePowerVarName(boolean gridSide);

    String getInitAuxiliaryReactivePowerVarName(boolean gridSide);
}
