/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.preassembled;

import com.powsybl.dynawo.mappings.preassembled.MachineControlUnit.MachineQuantity;

/**
 * The governors a machine can be given, as Dynawo assembles them.
 * <p>
 * Each one states what it wires to the machine and nothing about the others, which is what lets
 * any of them be put on a machine beside any governor.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public final class GovernorUnits {

    private static final String ID = "governor";

    private GovernorUnits() {
    }

    /**
     * GoverDTRI8.
     */
    public static MachineControlUnit goverDTRI8() {
        return new MachineControlUnit(ID, "Dynawo.Electrical.Controls.Machines.Governors.Simplified.GoverDTRI8", "Dynawo.Electrical.Controls.Machines.Governors.Governor_INIT")
                .reading("PmPu", MachineQuantity.MECHANICAL_POWER)
                .reading("omegaPu", MachineQuantity.SPEED)
                .startingFrom("Pm0Pu", MachineQuantity.INIT_MECHANICAL_POWER);
    }

    /**
     * GoverNordic.
     */
    public static MachineControlUnit goverNordic() {
        return new MachineControlUnit(ID, "Dynawo.Electrical.Controls.Machines.Governors.Simplified.GoverNordic", "Dynawo.Electrical.Controls.Machines.Governors.Governor_INIT")
                .reading("PGenPu", MachineQuantity.ACTIVE_POWER)
                .reading("PmPu", MachineQuantity.MECHANICAL_POWER)
                .reading("omegaPu", MachineQuantity.SPEED)
                .startingFrom("Pm0Pu", MachineQuantity.INIT_MECHANICAL_POWER);
    }

    /**
     * GoverProportional.
     */
    public static MachineControlUnit goverProportional() {
        return new MachineControlUnit(ID, "Dynawo.Electrical.Controls.Machines.Governors.Simplified.GoverProportional", "Dynawo.Electrical.Controls.Machines.Governors.Governor_INIT")
                .reading("PmPu", MachineQuantity.MECHANICAL_POWER)
                .reading("omegaPu", MachineQuantity.SPEED)
                .startingFrom("Pm0Pu", MachineQuantity.INIT_MECHANICAL_POWER);
    }

    /**
     * GovCt2.
     */
    public static MachineControlUnit govCt2() {
        return new MachineControlUnit(ID, "Dynawo.Electrical.Controls.Machines.Governors.Standard.Generic.GovCt2", "Dynawo.Electrical.Controls.Machines.Governors.GovernorPmPGen_INIT")
                .reading("PGenPu", MachineQuantity.ACTIVE_POWER)
                .reading("PmPu", MachineQuantity.MECHANICAL_POWER)
                .reading("omegaPu", MachineQuantity.SPEED)
                .startingFrom("PGen0Pu", MachineQuantity.INIT_ACTIVE_POWER)
                .startingFrom("Pm0Pu", MachineQuantity.INIT_MECHANICAL_POWER);
    }

    /**
     * GovHydro4.
     */
    public static MachineControlUnit govHydro4() {
        return new MachineControlUnit(ID, "Dynawo.Electrical.Controls.Machines.Governors.Standard.Hydraulic.GovHydro4", "Dynawo.Electrical.Controls.Machines.Governors.Governor_INIT")
                .reading("PmPu", MachineQuantity.MECHANICAL_POWER)
                .reading("omegaPu", MachineQuantity.SPEED)
                .startingFrom("Pm0Pu", MachineQuantity.INIT_MECHANICAL_POWER);
    }

    /**
     * HyGov.
     */
    public static MachineControlUnit hyGov() {
        return new MachineControlUnit(ID, "Dynawo.Electrical.Controls.Machines.Governors.Standard.Hydraulic.HyGov", "Dynawo.Electrical.Controls.Machines.Governors.Governor_INIT")
                .reading("PmPu", MachineQuantity.MECHANICAL_POWER)
                .reading("omegaPu", MachineQuantity.SPEED)
                .startingFrom("Pm0Pu", MachineQuantity.INIT_MECHANICAL_POWER);
    }

    /**
     * GovSteam1.
     */
    public static MachineControlUnit govSteam1() {
        return new MachineControlUnit(ID, "Dynawo.Electrical.Controls.Machines.Governors.Standard.Steam.GovSteam1", "Dynawo.Electrical.Controls.Machines.Governors.Standard.Steam.GovSteam1_INIT")
                .reading("Pm1Pu", MachineQuantity.MECHANICAL_POWER)
                .reading("omegaPu", MachineQuantity.SPEED)
                .startingFrom("Pm0Pu", MachineQuantity.INIT_MECHANICAL_POWER);
    }

    /**
     * GovSteamEu.
     */
    public static MachineControlUnit govSteamEu() {
        return new MachineControlUnit(ID, "Dynawo.Electrical.Controls.Machines.Governors.Standard.Steam.GovSteamEu", "Dynawo.Electrical.Controls.Machines.Governors.GovernorPmPGen_INIT")
                .reading("PGenPu", MachineQuantity.ACTIVE_POWER)
                .reading("PmPu", MachineQuantity.MECHANICAL_POWER)
                .reading("fPu", MachineQuantity.SPEED)
                .reading("omegaPu", MachineQuantity.SPEED)
                .startingFrom("PGen0Pu", MachineQuantity.INIT_ACTIVE_POWER)
                .startingFrom("Pm0Pu", MachineQuantity.INIT_MECHANICAL_POWER);
    }

    /**
     * IEEEG1.
     * <p>
     * Wired this way on a machine with four windings, differently on the others.
     */
    public static MachineControlUnit iEEEG1() {
        return new MachineControlUnit(ID, "Dynawo.Electrical.Controls.Machines.Governors.Standard.Steam.IEEEG1", "Dynawo.Electrical.Controls.Machines.Governors.Governor_INIT")
                .reading("PmPu", MachineQuantity.MECHANICAL_POWER)
                .reading("omegaPu", MachineQuantity.SPEED)
                .startingFrom("Pm0Pu", MachineQuantity.INIT_MECHANICAL_POWER);
    }

    /**
     * IEEEG1.
     * <p>
     * Wired this way on a machine with three windings, differently on the others.
     */
    public static MachineControlUnit iEEEG1ThreeWindings() {
        return new MachineControlUnit(ID, "Dynawo.Electrical.Controls.Machines.Governors.Standard.Steam.IEEEG1", "Dynawo.Electrical.Controls.Machines.Governors.Governor_INIT")
                .reading("Pm1Pu", MachineQuantity.MECHANICAL_POWER)
                .reading("omegaPu", MachineQuantity.SPEED)
                .startingFrom("Pm0Pu", MachineQuantity.INIT_MECHANICAL_POWER);
    }

    /**
     * IEEEG2.
     */
    public static MachineControlUnit iEEEG2() {
        return new MachineControlUnit(ID, "Dynawo.Electrical.Controls.Machines.Governors.Standard.Steam.IEEEG2", "Dynawo.Electrical.Controls.Machines.Governors.Governor_INIT")
                .reading("PmPu", MachineQuantity.MECHANICAL_POWER)
                .reading("omegaPu", MachineQuantity.SPEED)
                .startingFrom("Pm0Pu", MachineQuantity.INIT_MECHANICAL_POWER);
    }

    /**
     * TGov1.
     */
    public static MachineControlUnit tGov1() {
        return new MachineControlUnit(ID, "Dynawo.Electrical.Controls.Machines.Governors.Standard.Steam.TGov1", "Dynawo.Electrical.Controls.Machines.Governors.Governor_INIT")
                .reading("PmPu", MachineQuantity.MECHANICAL_POWER)
                .reading("omegaPu", MachineQuantity.SPEED)
                .startingFrom("Pm0Pu", MachineQuantity.INIT_MECHANICAL_POWER);
    }

    /**
     * TGov3.
     */
    public static MachineControlUnit tGov3() {
        return new MachineControlUnit(ID, "Dynawo.Electrical.Controls.Machines.Governors.Standard.Steam.TGov3", "Dynawo.Electrical.Controls.Machines.Governors.Standard.Steam.TGov3_INIT")
                .reading("PmPu", MachineQuantity.MECHANICAL_POWER)
                .reading("omegaPu", MachineQuantity.SPEED)
                .startingFrom("Pm0Pu", MachineQuantity.INIT_MECHANICAL_POWER);
    }
}
