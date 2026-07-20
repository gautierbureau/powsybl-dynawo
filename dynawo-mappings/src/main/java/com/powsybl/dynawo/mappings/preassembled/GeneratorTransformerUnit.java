/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.preassembled;

/**
 * The transformer a generating unit is connected through.
 * <p>
 * It takes an initialisation model of its own when auxiliaries hang on it, since their consumption
 * has to be accounted for while the operating point is worked out.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public class GeneratorTransformerUnit implements SeriesUnit {

    private final boolean auxiliaries;
    private final ModelNaming naming;

    public GeneratorTransformerUnit(boolean auxiliaries) {
        this(auxiliaries, ModelNaming.CURRENT);
    }

    public GeneratorTransformerUnit(boolean auxiliaries, ModelNaming naming) {
        this.auxiliaries = auxiliaries;
        this.naming = naming;
    }

    @Override
    public String getId() {
        return "transformer";
    }

    @Override
    public String getName() {
        return naming.getTransformerPackage() + "GeneratorTransformer";
    }

    @Override
    public String getInitName() {
        return naming.getTransformerPackage()
                + (auxiliaries ? "GeneratorTransformerAux_INIT" : "GeneratorTransformer_INIT");
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
