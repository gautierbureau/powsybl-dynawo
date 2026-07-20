/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.preassembled;

import com.powsybl.dynawo.extensions.api.generator.SynchronousGeneratorProperties;
import com.powsybl.dynawo.extensions.api.generator.SynchronousGeneratorProperties.Windings;

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
     * The model a generator wants, made of the controls its extension names.
     *
     * @param transformer whether it is connected through a transformer, which the extension alone
     *                    does not say since it depends on the voltage level the generator sits on
     */
    public Optional<PreassembledModel> design(SynchronousGeneratorProperties properties, boolean transformer) {
        Optional<MachineControlUnit> governor = catalog.getGovernor(properties.getGovernor());
        Optional<MachineControlUnit> voltageRegulator = catalog.getVoltageRegulator(properties.getVoltageRegulator());
        if (governor.isEmpty() || voltageRegulator.isEmpty()) {
            return Optional.empty();
        }
        Optional<RegulatorControlUnit> pss = Optional.empty();
        if (properties.getPss() != null && !properties.getPss().isEmpty()) {
            pss = catalog.getRegulatorControl(properties.getPss());
            if (pss.isEmpty()) {
                return Optional.empty();
            }
        }

        boolean auxiliaries = properties.isAuxiliaries();
        boolean withTransformer = transformer && !properties.isInternalTransformer();
        GeneratorAssembly assembly = new GeneratorAssembly(properties.getNumberOfWindings(),
                withTransformer, auxiliaries, naming);
        assembly.add(governor.get());
        assembly.add(voltageRegulator.get());
        pss.ifPresent(assembly::add);
        return Optional.of(assembly.build(name(properties, withTransformer, auxiliaries)));
    }

    /**
     * The name such a model carries, which is what it is made of read in order: the machine, its
     * controls, then what stands between it and the grid.
     */
    public String name(SynchronousGeneratorProperties properties, boolean transformer, boolean auxiliaries) {
        StringBuilder name = new StringBuilder(LIB_PREFIX)
                .append(properties.getNumberOfWindings() == Windings.THREE_WINDINGS ? THREE_WINDINGS : FOUR_WINDINGS)
                .append(properties.getGovernor())
                .append(properties.getVoltageRegulator());
        if (properties.getPss() != null) {
            name.append(properties.getPss());
        }
        if (transformer) {
            name.append("Tfo");
        }
        if (auxiliaries) {
            name.append("Aux");
        }
        return name.toString();
    }
}
