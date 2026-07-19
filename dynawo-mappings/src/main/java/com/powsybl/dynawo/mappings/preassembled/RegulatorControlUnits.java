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
 * The stabilisers and limiters a machine can be given, as Dynawo assembles them.
 * <p>
 * Each watches the machine and drives an input of its voltage regulator, naming what it drives
 * rather than the variable holding it, so a regulator spelling that input its own way is followed
 * without any of them knowing.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public final class RegulatorControlUnits {

    private RegulatorControlUnits() {
    }

    /**
     * Oel2c.
     */
    public static RegulatorControlUnit oel2c() {
        return new RegulatorControlUnit("overExcitationLimiter",
                "Dynawo.Electrical.Controls.Machines.OverExcitationLimiters.Standard.Oel2c",
                "Dynawo.Electrical.Controls.Machines.OverExcitationLimiters.Standard.Oel23c_INIT")
                .reading("inputPu", MachineQuantity.ROTOR_CURRENT)
                .startingFrom("Input0Pu", MachineQuantity.INIT_ROTOR_CURRENT)
                .driving("UOelPu", RegulatorInput.OVER_EXCITATION);
    }

    /**
     * Oel3c.
     */
    public static RegulatorControlUnit oel3c() {
        return new RegulatorControlUnit("overExcitationLimiter",
                "Dynawo.Electrical.Controls.Machines.OverExcitationLimiters.Standard.Oel3c",
                "Dynawo.Electrical.Controls.Machines.OverExcitationLimiters.Standard.Oel23c_INIT")
                .reading("inputPu", MachineQuantity.ROTOR_CURRENT)
                .startingFrom("Input0Pu", MachineQuantity.INIT_ROTOR_CURRENT)
                .driving("UOelPu", RegulatorInput.OVER_EXCITATION);
    }

    /**
     * Oel4c.
     */
    public static RegulatorControlUnit oel4c() {
        return new RegulatorControlUnit("overExcitationLimiter",
                "Dynawo.Electrical.Controls.Machines.OverExcitationLimiters.Standard.Oel4c",
                "Dynawo.Electrical.Controls.Machines.OverExcitationLimiters.Standard.Oel4c_INIT")
                .reading("QGenPu", MachineQuantity.REACTIVE_POWER)
                .startingFrom("QGen0Pu", MachineQuantity.INIT_REACTIVE_POWER)
                .driving("UOelPu", RegulatorInput.OVER_EXCITATION);
    }

    /**
     * Oel5c.
     */
    public static RegulatorControlUnit oel5c() {
        return new RegulatorControlUnit("overExcitationLimiter",
                "Dynawo.Electrical.Controls.Machines.OverExcitationLimiters.Standard.Oel5c",
                "Dynawo.Electrical.Controls.Machines.OverExcitationLimiters.Standard.Oel23c_INIT")
                .reading("inputPu", MachineQuantity.ROTOR_CURRENT)
                .startingFrom("Input0Pu", MachineQuantity.INIT_ROTOR_CURRENT)
                .driving("UOelPu", RegulatorInput.OVER_EXCITATION);
    }

    /**
     * Pss2a.
     */
    public static RegulatorControlUnit pss2a() {
        return new RegulatorControlUnit("powerSystemStabilizer",
                "Dynawo.Electrical.Controls.Machines.PowerSystemStabilizers.Standard.Pss2a",
                "Dynawo.Electrical.Controls.Machines.PowerSystemStabilizers.Pss_INIT")
                .reading("PGenPu", MachineQuantity.ACTIVE_POWER)
                .reading("omegaPu", MachineQuantity.SPEED)
                .startingFrom("PGen0Pu", MachineQuantity.INIT_ACTIVE_POWER)
                .driving("VPssPu", RegulatorInput.STABILISER);
    }

    /**
     * Pss2a, named the older way in the assemblies using it.
     */
    public static RegulatorControlUnit pss2aAsPss() {
        return new RegulatorControlUnit("pss",
                "Dynawo.Electrical.Controls.Machines.PowerSystemStabilizers.Standard.Pss2a",
                "Dynawo.Electrical.Controls.Machines.PowerSystemStabilizers.Pss_INIT")
                .reading("PGenPu", MachineQuantity.ACTIVE_POWER)
                .reading("omegaPu", MachineQuantity.SPEED)
                .startingFrom("PGen0Pu", MachineQuantity.INIT_ACTIVE_POWER)
                .driving("VPssPu", RegulatorInput.STABILISER);
    }

    /**
     * Pss2b.
     */
    public static RegulatorControlUnit pss2b() {
        return new RegulatorControlUnit("powerSystemStabilizer",
                "Dynawo.Electrical.Controls.Machines.PowerSystemStabilizers.Standard.Pss2b",
                "Dynawo.Electrical.Controls.Machines.PowerSystemStabilizers.Pss_INIT")
                .reading("PGenPu", MachineQuantity.ACTIVE_POWER)
                .reading("omegaPu", MachineQuantity.SPEED)
                .startingFrom("PGen0Pu", MachineQuantity.INIT_ACTIVE_POWER)
                .driving("VPssPu", RegulatorInput.STABILISER);
    }

    /**
     * Pss2b, named the older way in the assemblies using it.
     */
    public static RegulatorControlUnit pss2bAsPss() {
        return new RegulatorControlUnit("pss",
                "Dynawo.Electrical.Controls.Machines.PowerSystemStabilizers.Standard.Pss2b",
                "Dynawo.Electrical.Controls.Machines.PowerSystemStabilizers.Pss_INIT")
                .reading("PGenPu", MachineQuantity.ACTIVE_POWER)
                .reading("omegaPu", MachineQuantity.SPEED)
                .startingFrom("PGen0Pu", MachineQuantity.INIT_ACTIVE_POWER)
                .driving("VPssPu", RegulatorInput.STABILISER);
    }

    /**
     * Pss2c.
     */
    public static RegulatorControlUnit pss2c() {
        return new RegulatorControlUnit("powerSystemStabilizer",
                "Dynawo.Electrical.Controls.Machines.PowerSystemStabilizers.Standard.Pss2c",
                "Dynawo.Electrical.Controls.Machines.PowerSystemStabilizers.Pss_INIT")
                .reading("PGenPu", MachineQuantity.ACTIVE_POWER)
                .reading("omegaPu", MachineQuantity.SPEED)
                .startingFrom("PGen0Pu", MachineQuantity.INIT_ACTIVE_POWER)
                .driving("VPssPu", RegulatorInput.STABILISER);
    }

    /**
     * Pss3b.
     */
    public static RegulatorControlUnit pss3b() {
        return new RegulatorControlUnit("powerSystemStabilizer",
                "Dynawo.Electrical.Controls.Machines.PowerSystemStabilizers.Standard.Pss3b",
                "Dynawo.Electrical.Controls.Machines.PowerSystemStabilizers.Pss_INIT")
                .reading("PGenPu", MachineQuantity.ACTIVE_POWER)
                .reading("omegaPu", MachineQuantity.SPEED)
                .startingFrom("PGen0Pu", MachineQuantity.INIT_ACTIVE_POWER)
                .driving("VPssPu", RegulatorInput.STABILISER);
    }

    /**
     * Pss6c.
     */
    public static RegulatorControlUnit pss6c() {
        return new RegulatorControlUnit("powerSystemStabilizer",
                "Dynawo.Electrical.Controls.Machines.PowerSystemStabilizers.Standard.Pss6c",
                "Dynawo.Electrical.Controls.Machines.PowerSystemStabilizers.Pss_INIT")
                .reading("PGenPu", MachineQuantity.ACTIVE_POWER)
                .reading("omegaPu", MachineQuantity.SPEED)
                .startingFrom("PGen0Pu", MachineQuantity.INIT_ACTIVE_POWER)
                .driving("VPssPu", RegulatorInput.STABILISER);
    }

    /**
     * PssKundur, named the older way in the assemblies using it.
     */
    public static RegulatorControlUnit pssKundurAsPss() {
        return new RegulatorControlUnit("pss",
                "Dynawo.Electrical.Controls.Machines.PowerSystemStabilizers.Standard.PssKundur",
                null)
                .reading("omegaPu", MachineQuantity.SPEED)
                .driving("UPssPu", RegulatorInput.STABILISER);
    }

    /**
     * Scl1c.
     */
    public static RegulatorControlUnit scl1c() {
        return new RegulatorControlUnit("statorCurrentLimiter",
                "Dynawo.Electrical.Controls.Machines.StatorCurrentLimiters.Standard.Scl1c",
                "Dynawo.Electrical.Controls.Machines.StatorCurrentLimiters.Standard.Scl1c_INIT")
                .reading("QGenPu", MachineQuantity.REACTIVE_POWER)
                .reading("itPu", MachineQuantity.STATOR_CURRENT)
                .reading("utPu", MachineQuantity.TERMINAL_VOLTAGE)
                .startingFrom("QGen0Pu", MachineQuantity.INIT_REACTIVE_POWER)
                .startingFrom("it0Pu", MachineQuantity.INIT_CURRENT)
                .startingFrom("ut0Pu", MachineQuantity.INIT_TERMINAL_VOLTAGE)
                .driving("USclOelPu", RegulatorInput.STATOR_CURRENT_OVER_EXCITATION)
                .driving("USclUelPu", RegulatorInput.STATOR_CURRENT_UNDER_EXCITATION);
    }

    /**
     * Scl2c.
     */
    public static RegulatorControlUnit scl2c() {
        return new RegulatorControlUnit("statorCurrentLimiter",
                "Dynawo.Electrical.Controls.Machines.StatorCurrentLimiters.Standard.Scl2c",
                "Dynawo.Electrical.Controls.Machines.StatorCurrentLimiters.Standard.Scl2c_INIT")
                .reading("PGenPu", MachineQuantity.ACTIVE_POWER)
                .reading("QGenPu", MachineQuantity.REACTIVE_POWER)
                .reading("itPu", MachineQuantity.STATOR_CURRENT)
                .reading("utPu", MachineQuantity.TERMINAL_VOLTAGE)
                .startingFrom("PGen0Pu", MachineQuantity.INIT_ACTIVE_POWER)
                .startingFrom("QGen0Pu", MachineQuantity.INIT_REACTIVE_POWER)
                .startingFrom("it0Pu", MachineQuantity.INIT_CURRENT)
                .startingFrom("ut0Pu", MachineQuantity.INIT_TERMINAL_VOLTAGE)
                .driving("USclOelPu", RegulatorInput.STATOR_CURRENT_OVER_EXCITATION)
                .driving("USclUelPu", RegulatorInput.STATOR_CURRENT_UNDER_EXCITATION);
    }

    /**
     * Uel1.
     */
    public static RegulatorControlUnit uel1() {
        return new RegulatorControlUnit("underExcitationLimiter",
                "Dynawo.Electrical.Controls.Machines.UnderExcitationLimiters.Standard.Uel1",
                "Dynawo.Electrical.Controls.Machines.UnderExcitationLimiters.Standard.Uel1_INIT")
                .reading("itPu", MachineQuantity.STATOR_CURRENT)
                .reading("utPu", MachineQuantity.TERMINAL_VOLTAGE)
                .startingFrom("it0Pu", MachineQuantity.INIT_CURRENT)
                .startingFrom("ut0Pu", MachineQuantity.INIT_TERMINAL_VOLTAGE)
                .driving("UUelPu", RegulatorInput.UNDER_EXCITATION);
    }

    /**
     * Uel2c.
     */
    public static RegulatorControlUnit uel2c() {
        return new RegulatorControlUnit("underExcitationLimiter",
                "Dynawo.Electrical.Controls.Machines.UnderExcitationLimiters.Standard.Uel2c",
                "Dynawo.Electrical.Controls.Machines.UnderExcitationLimiters.Standard.Uel2c_INIT")
                .reading("PGenPu", MachineQuantity.ACTIVE_POWER)
                .reading("QGenPu", MachineQuantity.REACTIVE_POWER)
                .reading("utPu", MachineQuantity.TERMINAL_VOLTAGE)
                .startingFrom("PGen0Pu", MachineQuantity.INIT_ACTIVE_POWER)
                .startingFrom("QGen0Pu", MachineQuantity.INIT_REACTIVE_POWER)
                .startingFrom("ut0Pu", MachineQuantity.INIT_TERMINAL_VOLTAGE)
                .driving("UUelPu", RegulatorInput.UNDER_EXCITATION)
                .driving("VFbPu", RegulatorInput.FEEDBACK)
                .startingFrom("UsRef0Pu", RegulatorInput.INIT_VOLTAGE_REFERENCE);
    }

    /**
     * MAXEX2.
     */
    public static RegulatorControlUnit mAXEX2() {
        return new RegulatorControlUnit("overExcitationLimiter",
                "Dynawo.Electrical.Controls.Machines.VoltageRegulators.Standard.MAXEX2",
                "Dynawo.Electrical.Controls.Machines.VoltageRegulators.Standard.MAXEX2_INIT")
                .reading("IfdPu", MachineQuantity.ROTOR_CURRENT)
                .startingFrom("Ifd0Pu", MachineQuantity.INIT_ROTOR_CURRENT)
                .driving("UOelPu", RegulatorInput.OVER_EXCITATION);
    }
}
