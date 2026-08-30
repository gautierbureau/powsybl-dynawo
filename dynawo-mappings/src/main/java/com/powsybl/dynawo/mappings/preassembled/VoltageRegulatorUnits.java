/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.preassembled;

import com.powsybl.dynawo.mappings.preassembled.MachineControlUnit.MachineQuantity;
import com.powsybl.dynawo.mappings.preassembled.MachineControlUnit.RegulatorInput;

/**
 * The voltage regulators a machine can be given, as Dynawo assembles them.
 * <p>
 * Each one states what it wires to the machine and nothing about the others, which is what lets
 * any of them be put on a machine beside any governor.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public final class VoltageRegulatorUnits {

    private static final String ID = "voltageRegulator";

    private VoltageRegulatorUnits() {
    }

    /**
     * VRDTRI8.
     */
    public static MachineControlUnit vrDtri8() {
        return new MachineControlUnit(ID, "Dynawo.Electrical.Controls.Machines.VoltageRegulators.Simplified.VRDTRI8", "Dynawo.Electrical.Controls.Machines.VoltageRegulators.Simplified.VRDTRI8_INIT")
                .reading("EfdPu", MachineQuantity.FIELD_VOLTAGE)
                .reading("UsPu", MachineQuantity.STATOR_VOLTAGE)
                .startingFrom("Efd0Pu", MachineQuantity.INIT_FIELD_VOLTAGE)
                .startingFrom("Us0Pu", MachineQuantity.INIT_STATOR_VOLTAGE);
    }

    /**
     * VRNordic.
     */
    public static MachineControlUnit vRNordic() {
        return new MachineControlUnit(ID, "Dynawo.Electrical.Controls.Machines.VoltageRegulators.Simplified.VRNordic", "Dynawo.Electrical.Controls.Machines.VoltageRegulators.Simplified.VRNordic_INIT")
                .reading("IrPu", MachineQuantity.ROTOR_CURRENT)
                .reading("UsPu", MachineQuantity.STATOR_VOLTAGE)
                .reading("efdPu", MachineQuantity.FIELD_VOLTAGE)
                .reading("omegaPu", MachineQuantity.SPEED)
                .startingFrom("Efd0Pu", MachineQuantity.INIT_FIELD_VOLTAGE)
                .startingFrom("Ir0Pu", MachineQuantity.INIT_ROTOR_CURRENT)
                .startingFrom("Us0Pu", MachineQuantity.INIT_STATOR_VOLTAGE);
    }

    /**
     * VRProportional.
     */
    public static MachineControlUnit vRProportional() {
        return new MachineControlUnit(ID,
                "Dynawo.Electrical.Controls.Machines.VoltageRegulators.Simplified.VRProportional",
                "Dynawo.Electrical.Controls.Machines.VoltageRegulators.Simplified.VRProportional_INIT")
                .reading("EfdPu", MachineQuantity.FIELD_VOLTAGE)
                .reading("UsPu", MachineQuantity.STATOR_VOLTAGE)
                .startingFrom("Efd0PuLF", MachineQuantity.INIT_FIELD_VOLTAGE)
                .startingFrom("Us0Pu", MachineQuantity.INIT_STATOR_VOLTAGE);
    }

    /**
     * VRProportionalIntegral.
     */
    public static MachineControlUnit vRProportionalIntegral() {
        return new MachineControlUnit(ID,
                "Dynawo.Electrical.Controls.Machines.VoltageRegulators.Simplified.VRProportionalIntegral",
                "Dynawo.Electrical.Controls.Machines.VoltageRegulators.Simplified.VRProportionalIntegral_INIT")
                .reading("EfdPu", MachineQuantity.FIELD_VOLTAGE)
                .reading("UsPu", MachineQuantity.STATOR_VOLTAGE)
                .startingFrom("Efd0PuLF", MachineQuantity.INIT_FIELD_VOLTAGE)
                .startingFrom("Us0Pu", MachineQuantity.INIT_STATOR_VOLTAGE);
    }

    /**
     * Ac1a.
     */
    public static MachineControlUnit ac1a() {
        return new MachineControlUnit(ID, "Dynawo.Electrical.Controls.Machines.VoltageRegulators.Standard.Ac1a", "Dynawo.Electrical.Controls.Machines.VoltageRegulators.Standard.Ac1a_INIT")
                .reading("EfdPu", MachineQuantity.FIELD_VOLTAGE)
                .reading("IrPu", MachineQuantity.ROTOR_CURRENT)
                .reading("UsPu", MachineQuantity.STATOR_VOLTAGE)
                .startingFrom("Efd0Pu", MachineQuantity.INIT_FIELD_VOLTAGE)
                .startingFrom("Ir0Pu", MachineQuantity.INIT_ROTOR_CURRENT)
                .startingFrom("Us0Pu", MachineQuantity.INIT_STATOR_VOLTAGE);
    }

    /**
     * Ac6a.
     */
    public static MachineControlUnit ac6a() {
        return new MachineControlUnit(ID, "Dynawo.Electrical.Controls.Machines.VoltageRegulators.Standard.Ac6a", "Dynawo.Electrical.Controls.Machines.VoltageRegulators.Standard.Ac168_INIT")
                .reading("EfdPu", MachineQuantity.FIELD_VOLTAGE)
                .reading("IrPu", MachineQuantity.ROTOR_CURRENT)
                .reading("UsPu", MachineQuantity.STATOR_VOLTAGE)
                .startingFrom("Efd0Pu", MachineQuantity.INIT_FIELD_VOLTAGE)
                .startingFrom("Ir0Pu", MachineQuantity.INIT_ROTOR_CURRENT)
                .startingFrom("Us0Pu", MachineQuantity.INIT_STATOR_VOLTAGE)
                .accepting(RegulatorInput.STABILISER, "UPssPu");
    }

    /**
     * Ac7b.
     */
    public static MachineControlUnit ac7b() {
        return new MachineControlUnit(ID, "Dynawo.Electrical.Controls.Machines.VoltageRegulators.Standard.Ac7b", "Dynawo.Electrical.Controls.Machines.VoltageRegulators.Standard.Ac7b_INIT")
                .reading("EfdPu", MachineQuantity.FIELD_VOLTAGE)
                .reading("IrPu", MachineQuantity.ROTOR_CURRENT)
                .reading("UsPu", MachineQuantity.STATOR_VOLTAGE)
                .reading("itPu", MachineQuantity.STATOR_CURRENT)
                .reading("running", MachineQuantity.RUNNING)
                .reading("utPu", MachineQuantity.TERMINAL_VOLTAGE)
                .startingFrom("Efd0Pu", MachineQuantity.INIT_FIELD_VOLTAGE)
                .startingFrom("Ir0Pu", MachineQuantity.INIT_ROTOR_CURRENT)
                .startingFrom("Us0Pu", MachineQuantity.INIT_STATOR_VOLTAGE)
                .startingFrom("it0Pu", MachineQuantity.INIT_CURRENT)
                .startingFrom("ut0Pu", MachineQuantity.INIT_TERMINAL_VOLTAGE)
                .accepting(RegulatorInput.STABILISER, "UPssPu");
    }

    /**
     * Ac7c.
     */
    public static MachineControlUnit ac7c() {
        return new MachineControlUnit(ID, "Dynawo.Electrical.Controls.Machines.VoltageRegulators.Standard.Ac7c", "Dynawo.Electrical.Controls.Machines.VoltageRegulators.Standard.Ac78c_INIT")
                .reading("EfdPu", MachineQuantity.FIELD_VOLTAGE)
                .reading("IrPu", MachineQuantity.ROTOR_CURRENT)
                .reading("UsPu", MachineQuantity.STATOR_VOLTAGE)
                .reading("itPu", MachineQuantity.STATOR_CURRENT)
                .reading("running", MachineQuantity.RUNNING)
                .reading("utPu", MachineQuantity.TERMINAL_VOLTAGE)
                .startingFrom("Efd0Pu", MachineQuantity.INIT_FIELD_VOLTAGE)
                .startingFrom("Ir0Pu", MachineQuantity.INIT_ROTOR_CURRENT)
                .startingFrom("Us0Pu", MachineQuantity.INIT_STATOR_VOLTAGE)
                .startingFrom("it0Pu", MachineQuantity.INIT_CURRENT)
                .startingFrom("ut0Pu", MachineQuantity.INIT_TERMINAL_VOLTAGE)
                .accepting(RegulatorInput.STABILISER, "UPssPu");
    }

    /**
     * Ac8b.
     */
    public static MachineControlUnit ac8b() {
        return new MachineControlUnit(ID, "Dynawo.Electrical.Controls.Machines.VoltageRegulators.Standard.Ac8b", "Dynawo.Electrical.Controls.Machines.VoltageRegulators.Standard.Ac168_INIT")
                .reading("EfdPu", MachineQuantity.FIELD_VOLTAGE)
                .reading("IrPu", MachineQuantity.ROTOR_CURRENT)
                .reading("UsPu", MachineQuantity.STATOR_VOLTAGE)
                .startingFrom("Efd0Pu", MachineQuantity.INIT_FIELD_VOLTAGE)
                .startingFrom("Ir0Pu", MachineQuantity.INIT_ROTOR_CURRENT)
                .startingFrom("Us0Pu", MachineQuantity.INIT_STATOR_VOLTAGE)
                .accepting(RegulatorInput.STABILISER, "UPssPu");
    }

    /**
     * BbSex1.
     */
    public static MachineControlUnit bbSex1() {
        return new MachineControlUnit(ID, "Dynawo.Electrical.Controls.Machines.VoltageRegulators.Standard.BbSex1", "Dynawo.Electrical.Controls.Machines.VoltageRegulators.Exciter_INIT")
                .reading("EfdPu", MachineQuantity.FIELD_VOLTAGE)
                .reading("UsPu", MachineQuantity.STATOR_VOLTAGE)
                .startingFrom("Efd0Pu", MachineQuantity.INIT_FIELD_VOLTAGE)
                .startingFrom("Us0Pu", MachineQuantity.INIT_STATOR_VOLTAGE);
    }

    /**
     * Dc1a.
     */
    public static MachineControlUnit dc1a() {
        return new MachineControlUnit(ID, "Dynawo.Electrical.Controls.Machines.VoltageRegulators.Standard.Dc1a", "Dynawo.Electrical.Controls.Machines.VoltageRegulators.Exciter_INIT")
                .reading("EfdPu", MachineQuantity.FIELD_VOLTAGE)
                .reading("UsPu", MachineQuantity.STATOR_VOLTAGE)
                .startingFrom("Efd0Pu", MachineQuantity.INIT_FIELD_VOLTAGE)
                .startingFrom("Us0Pu", MachineQuantity.INIT_STATOR_VOLTAGE);
    }

    /**
     * ExAc1.
     */
    public static MachineControlUnit exAc1() {
        return new MachineControlUnit(ID, "Dynawo.Electrical.Controls.Machines.VoltageRegulators.Standard.ExAc1", "Dynawo.Electrical.Controls.Machines.VoltageRegulators.Standard.ExAc1_INIT")
                .reading("EfdPu", MachineQuantity.FIELD_VOLTAGE)
                .reading("IrPu", MachineQuantity.ROTOR_CURRENT)
                .reading("UsPu", MachineQuantity.STATOR_VOLTAGE)
                .startingFrom("Efd0Pu", MachineQuantity.INIT_FIELD_VOLTAGE)
                .startingFrom("Ir0Pu", MachineQuantity.INIT_ROTOR_CURRENT)
                .startingFrom("Us0Pu", MachineQuantity.INIT_STATOR_VOLTAGE)
                .accepting(RegulatorInput.OVER_EXCITATION, "UOelPu")
                .accepting(RegulatorInput.STABILISER, "UPssPu");
    }

    /**
     * IEEET1.
     */
    public static MachineControlUnit iEEET1() {
        return new MachineControlUnit(ID, "Dynawo.Electrical.Controls.Machines.VoltageRegulators.Standard.IEEET1", "Dynawo.Electrical.Controls.Machines.VoltageRegulators.Exciter_INIT")
                .reading("EfdPu", MachineQuantity.FIELD_VOLTAGE)
                .reading("UsPu", MachineQuantity.STATOR_VOLTAGE)
                .startingFrom("Efd0Pu", MachineQuantity.INIT_FIELD_VOLTAGE)
                .startingFrom("Us0Pu", MachineQuantity.INIT_STATOR_VOLTAGE)
                .accepting(RegulatorInput.OVER_EXCITATION, "UOelPu")
                .accepting(RegulatorInput.STABILISER, "UPssPu");
    }

    /**
     * IEEX2A.
     */
    public static MachineControlUnit iEEX2A() {
        return new MachineControlUnit(ID, "Dynawo.Electrical.Controls.Machines.VoltageRegulators.Standard.IEEX2A", "Dynawo.Electrical.Controls.Machines.VoltageRegulators.Exciter_INIT")
                .reading("EfdPu", MachineQuantity.FIELD_VOLTAGE)
                .reading("UsPu", MachineQuantity.STATOR_VOLTAGE)
                .startingFrom("Efd0Pu", MachineQuantity.INIT_FIELD_VOLTAGE)
                .startingFrom("Us0Pu", MachineQuantity.INIT_STATOR_VOLTAGE);
    }

    /**
     * SCRX.
     */
    public static MachineControlUnit sCRX() {
        return new MachineControlUnit(ID, "Dynawo.Electrical.Controls.Machines.VoltageRegulators.Standard.SCRX", "Dynawo.Electrical.Controls.Machines.VoltageRegulators.Standard.SCRX_INIT")
                .reading("EfdPu", MachineQuantity.FIELD_VOLTAGE)
                .reading("IRotorPu", MachineQuantity.ROTOR_CURRENT)
                .reading("UStatorPu", MachineQuantity.STATOR_VOLTAGE)
                .reading("UtPu", MachineQuantity.VOLTAGE_MAGNITUDE)
                .startingFrom("Efd0Pu", MachineQuantity.INIT_FIELD_VOLTAGE)
                .startingFrom("IRotor0Pu", MachineQuantity.INIT_ROTOR_CURRENT)
                .startingFrom("UStator0Pu", MachineQuantity.INIT_STATOR_VOLTAGE)
                .startingFrom("Ut0Pu", MachineQuantity.INIT_VOLTAGE_MAGNITUDE)
                .accepting(RegulatorInput.OVER_EXCITATION, "UOelPu")
                .accepting(RegulatorInput.STABILISER, "UPssPu");
    }

    /**
     * SEXS.
     */
    public static MachineControlUnit sEXS() {
        return new MachineControlUnit(ID, "Dynawo.Electrical.Controls.Machines.VoltageRegulators.Standard.SEXS", "Dynawo.Electrical.Controls.Machines.VoltageRegulators.Exciter_INIT")
                .reading("EfdPu", MachineQuantity.FIELD_VOLTAGE)
                .reading("UsPu", MachineQuantity.STATOR_VOLTAGE)
                .startingFrom("Efd0Pu", MachineQuantity.INIT_FIELD_VOLTAGE)
                .startingFrom("Us0Pu", MachineQuantity.INIT_STATOR_VOLTAGE)
                .accepting(RegulatorInput.STABILISER, "UpssPu");
    }

    /**
     * St4b.
     */
    public static MachineControlUnit st4b() {
        return new MachineControlUnit(ID, "Dynawo.Electrical.Controls.Machines.VoltageRegulators.Standard.St4b", "Dynawo.Electrical.Controls.Machines.VoltageRegulators.Standard.St4b_INIT")
                .reading("EfdPu", MachineQuantity.FIELD_VOLTAGE)
                .reading("IrPu", MachineQuantity.ROTOR_CURRENT)
                .reading("UsPu", MachineQuantity.STATOR_VOLTAGE)
                .reading("itPu", MachineQuantity.STATOR_CURRENT)
                .reading("running", MachineQuantity.RUNNING)
                .reading("utPu", MachineQuantity.TERMINAL_VOLTAGE)
                .startingFrom("Efd0Pu", MachineQuantity.INIT_FIELD_VOLTAGE)
                .startingFrom("Ir0Pu", MachineQuantity.INIT_ROTOR_CURRENT)
                .startingFrom("Us0Pu", MachineQuantity.INIT_STATOR_VOLTAGE)
                .startingFrom("it0Pu", MachineQuantity.INIT_CURRENT)
                .startingFrom("ut0Pu", MachineQuantity.INIT_TERMINAL_VOLTAGE)
                .accepting(RegulatorInput.STABILISER, "UPssPu");
    }

    /**
     * St5b.
     */
    public static MachineControlUnit st5b() {
        return new MachineControlUnit(ID, "Dynawo.Electrical.Controls.Machines.VoltageRegulators.Standard.St5b", "Dynawo.Electrical.Controls.Machines.VoltageRegulators.Standard.St15c_INIT")
                .reading("EfdPu", MachineQuantity.FIELD_VOLTAGE)
                .reading("IrPu", MachineQuantity.ROTOR_CURRENT)
                .reading("UsPu", MachineQuantity.STATOR_VOLTAGE)
                .startingFrom("Efd0Pu", MachineQuantity.INIT_FIELD_VOLTAGE)
                .startingFrom("Ir0Pu", MachineQuantity.INIT_ROTOR_CURRENT)
                .startingFrom("Us0Pu", MachineQuantity.INIT_STATOR_VOLTAGE)
                .accepting(RegulatorInput.STABILISER, "UPssPu");
    }

    /**
     * St5c.
     */
    public static MachineControlUnit st5c() {
        return new MachineControlUnit(ID, "Dynawo.Electrical.Controls.Machines.VoltageRegulators.Standard.St5c", "Dynawo.Electrical.Controls.Machines.VoltageRegulators.Standard.St15c_INIT")
                .reading("EfdPu", MachineQuantity.FIELD_VOLTAGE)
                .reading("IrPu", MachineQuantity.ROTOR_CURRENT)
                .reading("UsPu", MachineQuantity.STATOR_VOLTAGE)
                .startingFrom("Efd0Pu", MachineQuantity.INIT_FIELD_VOLTAGE)
                .startingFrom("Ir0Pu", MachineQuantity.INIT_ROTOR_CURRENT)
                .startingFrom("Us0Pu", MachineQuantity.INIT_STATOR_VOLTAGE)
                .accepting(RegulatorInput.STABILISER, "UPssPu")
                .accepting(RegulatorInput.STATOR_CURRENT_OVER_EXCITATION, "USclOelPu")
                .accepting(RegulatorInput.STATOR_CURRENT_UNDER_EXCITATION, "USclUelPu");
    }

    /**
     * St6b.
     */
    public static MachineControlUnit st6b() {
        return new MachineControlUnit(ID, "Dynawo.Electrical.Controls.Machines.VoltageRegulators.Standard.St6b", "Dynawo.Electrical.Controls.Machines.VoltageRegulators.Standard.St6b_INIT")
                .reading("EfdPu", MachineQuantity.FIELD_VOLTAGE)
                .reading("IrPu", MachineQuantity.ROTOR_CURRENT)
                .reading("UsPu", MachineQuantity.STATOR_VOLTAGE)
                .reading("itPu", MachineQuantity.STATOR_CURRENT)
                .reading("running", MachineQuantity.RUNNING)
                .reading("utPu", MachineQuantity.TERMINAL_VOLTAGE)
                .startingFrom("Efd0Pu", MachineQuantity.INIT_FIELD_VOLTAGE)
                .startingFrom("Ir0Pu", MachineQuantity.INIT_ROTOR_CURRENT)
                .startingFrom("Us0Pu", MachineQuantity.INIT_STATOR_VOLTAGE)
                .startingFrom("it0Pu", MachineQuantity.INIT_CURRENT)
                .startingFrom("ut0Pu", MachineQuantity.INIT_TERMINAL_VOLTAGE)
                .accepting(RegulatorInput.STABILISER, "UPssPu");
    }

    /**
     * St6c.
     */
    public static MachineControlUnit st6c() {
        return new MachineControlUnit(ID, "Dynawo.Electrical.Controls.Machines.VoltageRegulators.Standard.St6c", "Dynawo.Electrical.Controls.Machines.VoltageRegulators.Standard.Stxc_INIT")
                .reading("EfdPu", MachineQuantity.FIELD_VOLTAGE)
                .reading("IrPu", MachineQuantity.ROTOR_CURRENT)
                .reading("UsPu", MachineQuantity.STATOR_VOLTAGE)
                .reading("itPu", MachineQuantity.STATOR_CURRENT)
                .reading("running", MachineQuantity.RUNNING)
                .reading("utPu", MachineQuantity.TERMINAL_VOLTAGE)
                .startingFrom("Efd0Pu", MachineQuantity.INIT_FIELD_VOLTAGE)
                .startingFrom("Ir0Pu", MachineQuantity.INIT_ROTOR_CURRENT)
                .startingFrom("Us0Pu", MachineQuantity.INIT_STATOR_VOLTAGE)
                .startingFrom("it0Pu", MachineQuantity.INIT_CURRENT)
                .startingFrom("ut0Pu", MachineQuantity.INIT_TERMINAL_VOLTAGE)
                .accepting(RegulatorInput.OVER_EXCITATION, "UOelPu")
                .accepting(RegulatorInput.STABILISER, "UPssPu");
    }

    /**
     * St7b.
     */
    public static MachineControlUnit st7b() {
        return new MachineControlUnit(ID, "Dynawo.Electrical.Controls.Machines.VoltageRegulators.Standard.St7b", "Dynawo.Electrical.Controls.Machines.VoltageRegulators.Standard.St7c_INIT")
                .reading("EfdPu", MachineQuantity.FIELD_VOLTAGE)
                .reading("UsPu", MachineQuantity.STATOR_VOLTAGE)
                .startingFrom("Efd0Pu", MachineQuantity.INIT_FIELD_VOLTAGE)
                .startingFrom("Us0Pu", MachineQuantity.INIT_STATOR_VOLTAGE)
                .accepting(RegulatorInput.FEEDBACK, "VFbPu")
                .accepting(RegulatorInput.INIT_VOLTAGE_REFERENCE, "UsRef0Pu")
                .accepting(RegulatorInput.STABILISER, "UPssPu")
                .accepting(RegulatorInput.UNDER_EXCITATION, "UUelPu");
    }

    /**
     * St9c.
     */
    public static MachineControlUnit st9c() {
        return new MachineControlUnit(ID, "Dynawo.Electrical.Controls.Machines.VoltageRegulators.Standard.St9c", "Dynawo.Electrical.Controls.Machines.VoltageRegulators.Standard.Stxc_INIT")
                .reading("EfdPu", MachineQuantity.FIELD_VOLTAGE)
                .reading("IrPu", MachineQuantity.ROTOR_CURRENT)
                .reading("UsPu", MachineQuantity.STATOR_VOLTAGE)
                .reading("itPu", MachineQuantity.STATOR_CURRENT)
                .reading("running", MachineQuantity.RUNNING)
                .reading("utPu", MachineQuantity.TERMINAL_VOLTAGE)
                .startingFrom("Efd0Pu", MachineQuantity.INIT_FIELD_VOLTAGE)
                .startingFrom("Ir0Pu", MachineQuantity.INIT_ROTOR_CURRENT)
                .startingFrom("Us0Pu", MachineQuantity.INIT_STATOR_VOLTAGE)
                .startingFrom("it0Pu", MachineQuantity.INIT_CURRENT)
                .startingFrom("ut0Pu", MachineQuantity.INIT_TERMINAL_VOLTAGE)
                .accepting(RegulatorInput.STABILISER, "UPssPu");
    }

    /**
     * VRKundur.
     */
    public static MachineControlUnit vRKundur() {
        return new MachineControlUnit(ID, "Dynawo.Electrical.Controls.Machines.VoltageRegulators.Standard.VRKundur", "Dynawo.Electrical.Controls.Machines.VoltageRegulators.Exciter_INIT")
                .reading("UsPu", MachineQuantity.STATOR_VOLTAGE)
                .reading("efdPu", MachineQuantity.FIELD_VOLTAGE)
                .startingFrom("Efd0Pu", MachineQuantity.INIT_FIELD_VOLTAGE)
                .startingFrom("Us0Pu", MachineQuantity.INIT_STATOR_VOLTAGE)
                .accepting(RegulatorInput.STABILISER, "UPssPu");
    }
}
