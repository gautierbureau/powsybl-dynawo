/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.preassembled;

import com.powsybl.dynawo.extensions.api.generator.RpclType;
import com.powsybl.dynawo.extensions.api.generator.SynchronousGeneratorProperties.Windings;

import java.util.Map;
import java.util.Optional;

/**
 * Describes the model a generator is asking for, whether or not anything has ever built it.
 * <p>
 * A model is named after what it is made of, so the name a generator wants and the model that
 * would bear it are two readings of the same thing: the controls its extension carries, on a
 * machine of so many windings, with or without a transformer and auxiliaries. Reading the name
 * back into an assembly is what lets a combination nobody has assembled be built on demand,
 * rather than settling for whichever catalogued model comes closest.
 * <p>
 * Only combinations of controls we describe can be designed. One naming a control we do not know
 * is answered with nothing, which is the honest answer: better no model than one quietly missing
 * a control the extension asked for.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public class GeneratorModelDesigner {

    private static final String LIB_PREFIX = "GeneratorSynchronous";
    private static final String THREE_WINDINGS = "ThreeWindings";
    private static final String FOUR_WINDINGS = "FourWindings";
    private static final String REACTIVE_LIMITS = "ReactiveLimits";
    private static final String QLIM = "Qlim";
    private static final String RPCL = "Rpcl";
    private static final String RPCL2 = "Rpcl2";

    /**
     * The control pairs whose model is not named after the two of them run together.
     */
    private static final Map<String, String> NAMED_PAIRS = Map.of(
            "GoverProportional VRProportional", "ProportionalRegulations",
            "GoverProportional VRProportionalIntegral", "GoverPropVRPropInt");

    private final ControlUnitCatalog catalog;
    private final ModelNaming naming;

    public GeneratorModelDesigner(ModelNaming naming) {
        this(ControlUnitCatalog.getInstance(), naming);
    }

    public GeneratorModelDesigner(ControlUnitCatalog catalog, ModelNaming naming) {
        this.catalog = catalog;
        this.naming = naming;
    }

    /**
     * The model made of those controls on a machine of that shape.
     *
     * @param controls    the controls settled on, already what the study is to run
     * @param windings    how many windings the machine is described with
     * @param transformer whether a transformer stands between it and the grid
     * @param auxiliaries whether auxiliaries draw from it
     */
    public Optional<PreassembledModel> design(GeneratorControls controls, Windings windings,
                                              boolean transformer, boolean auxiliaries) {
        return design(controls, windings, transformer, auxiliaries, false, RpclType.NONE);
    }

    /**
     * The model made of those controls on a machine of that shape, with reactive limits and a
     * reactive power control loop where a voltage stability study asks for them.
     *
     * @param qlim whether the regulator holds its reactive output within limits, which it does by
     *             standing on its reactive-limits variant
     * @param rpcl the reactive power control loop wrapped around the regulator, or {@link
     *             RpclType#NONE none}
     */
    public Optional<PreassembledModel> design(GeneratorControls controls, Windings windings,
                                              boolean transformer, boolean auxiliaries, boolean qlim, RpclType rpcl) {
        Optional<MachineControlUnit> governor = catalog.getGovernor(controls.governor());
        // reactive limits stand the regulator on its reactive-limits variant, named for the model it is
        String regulatorName = qlim ? controls.voltageRegulator() + REACTIVE_LIMITS : controls.voltageRegulator();
        Optional<MachineControlUnit> voltageRegulator = catalog.getVoltageRegulator(regulatorName);
        if (governor.isEmpty() || voltageRegulator.isEmpty()) {
            return Optional.empty();
        }
        Optional<RegulatorControlUnit> pss = Optional.empty();
        if (controls.hasPss()) {
            pss = catalog.getRegulatorControl(controls.pss());
            if (pss.isEmpty()) {
                return Optional.empty();
            }
        }
        Optional<RegulatorControlUnit> reactivePowerControlLoop = Optional.empty();
        if (rpcl != RpclType.NONE) {
            reactivePowerControlLoop = catalog.getRegulatorControl(reactivePowerControlLoopName(rpcl));
            if (reactivePowerControlLoop.isEmpty()) {
                return Optional.empty();
            }
        }

        GeneratorAssembly assembly = new GeneratorAssembly(windings, transformer, auxiliaries, naming);
        assembly.add(governor.get());
        assembly.add(voltageRegulator.get());
        pss.ifPresent(assembly::add);
        reactivePowerControlLoop.ifPresent(assembly::add);
        return Optional.of(assembly.build(name(controls, windings, transformer, auxiliaries, qlim, rpcl)));
    }

    /**
     * The name such a model carries, which is what it is made of read in order: the machine, its
     * controls, then what stands between it and the grid.
     */
    public static String name(GeneratorControls controls, Windings windings,
                              boolean transformer, boolean auxiliaries) {
        return name(controls, windings, transformer, auxiliaries, false, RpclType.NONE);
    }

    /**
     * The name such a model carries, its reactive limits and reactive power control loop named after
     * its controls, before what stands between it and the grid.
     */
    public static String name(GeneratorControls controls, Windings windings,
                              boolean transformer, boolean auxiliaries, boolean qlim, RpclType rpcl) {
        StringBuilder name = new StringBuilder(LIB_PREFIX)
                .append(windings == Windings.THREE_WINDINGS ? THREE_WINDINGS : FOUR_WINDINGS)
                .append(controlsFragment(controls.governor(), controls.voltageRegulator()));
        if (controls.hasPss()) {
            name.append(controls.pss());
        }
        if (qlim) {
            name.append(QLIM);
        }
        if (rpcl == RpclType.RPCL1) {
            name.append(RPCL);
        } else if (rpcl == RpclType.RPCL2) {
            name.append(RPCL2);
        }
        if (transformer) {
            name.append("Tfo");
        }
        if (auxiliaries) {
            name.append("Aux");
        }
        return name.toString();
    }

    private static String reactivePowerControlLoopName(RpclType rpcl) {
        return rpcl == RpclType.RPCL2 ? "ReactivePowerControlLoop2" : "ReactivePowerControlLoop";
    }

    /**
     * What a governor and a voltage regulator are called together in a model's name, which is the
     * two of them run together: a Nordic governor with a Nordic regulator makes
     * {@code GoverNordicVRNordic}.
     * <p>
     * Two pairs are named otherwise, and there is no rule behind it to derive, so they are simply
     * named here: the simplified regulations Dynawo ships were christened before the convention
     * settled.
     */
    private static String controlsFragment(String governor, String voltageRegulator) {
        String named = NAMED_PAIRS.get(governor + " " + voltageRegulator);
        return named != null ? named : governor + voltageRegulator;
    }
}
