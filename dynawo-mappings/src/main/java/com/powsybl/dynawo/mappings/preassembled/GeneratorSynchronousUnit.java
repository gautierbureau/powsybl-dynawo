/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.preassembled;

import com.powsybl.dynawo.extensions.api.generator.SynchronousGeneratorProperties.Windings;

/**
 * The synchronous machine every assembly is built around.
 * <p>
 * Which model computes its initial state depends on how it is wired: on the number of windings,
 * and on whether the assembly puts a transformer between it and the grid, since the machine is
 * then initialised from the far side of that transformer.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public class GeneratorSynchronousUnit implements MachineUnit {

    public static final String ID = "generator";
    private static final String PACKAGE = "Dynawo.Electrical.Machines.OmegaRef.";

    private final Windings windings;
    private final boolean transformer;
    private final ModelNaming naming;

    public GeneratorSynchronousUnit(Windings windings, boolean transformer) {
        this(windings, transformer, ModelNaming.CURRENT);
    }

    public GeneratorSynchronousUnit(Windings windings, boolean transformer, ModelNaming naming) {
        this.windings = windings;
        this.transformer = transformer;
        this.naming = naming;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return PACKAGE + "GeneratorSynchronous";
    }

    @Override
    public String getInitName() {
        return PACKAGE + "GeneratorSynchronousExt" + (transformer ? "Tfo" : "")
                + (windings == Windings.THREE_WINDINGS ? "3W" : "4W") + "_INIT";
    }

    @Override
    public String getStatorVoltageVarName() {
        return naming.exchanged("UStatorPu");
    }

    @Override
    public String getFieldVoltageVarName() {
        return naming.exchanged("efdPu");
    }

    @Override
    public String getRotorCurrentVarName() {
        return naming.exchanged("IRotorPu");
    }

    @Override
    public String getTerminalVoltageVarName() {
        return "uPu";
    }

    @Override
    public String getStatorCurrentVarName() {
        return "iStatorPu";
    }

    @Override
    public String getSpeedVarName() {
        return naming.exchanged("omegaPu");
    }

    @Override
    public String getMechanicalPowerVarName() {
        return naming.exchanged("PmPu");
    }

    @Override
    public String getActivePowerVarName() {
        return "PGenPu";
    }

    @Override
    public String getReactivePowerVarName() {
        return "QGenPu";
    }

    @Override
    public String getInitReactivePowerVarName() {
        return "QGen0Pu";
    }

    @Override
    public String getRunningVarName() {
        return naming.getRunning();
    }

    @Override
    public String getVoltageMagnitudeVarName() {
        return "UPu";
    }

    @Override
    public String getInitVoltageMagnitudeVarName() {
        return naming.getInitVoltageMagnitude();
    }

    @Override
    public String getInitStatorVoltageVarName() {
        return "UStator0Pu";
    }

    @Override
    public String getInitFieldVoltageVarName() {
        return "Efd0Pu";
    }

    @Override
    public String getInitRotorCurrentVarName() {
        return "IRotor0Pu";
    }

    @Override
    public String getInitTerminalVoltageVarName() {
        return "u0Pu";
    }

    @Override
    public String getInitCurrentVarName() {
        return naming.getInitCurrent();
    }

    @Override
    public String getInitMechanicalPowerVarName() {
        return "Pm0Pu";
    }

    @Override
    public String getInitActivePowerVarName() {
        return "PGen0Pu";
    }
}
