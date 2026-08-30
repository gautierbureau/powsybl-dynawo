/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.parameters;

import com.powsybl.dynawo.desc.ModelDescription;
import com.powsybl.dynawo.desc.ModifiableParameter;
import com.powsybl.dynawo.parameters.Parameter;
import com.powsybl.dynawo.parameters.ParametersSet;
import com.powsybl.iidm.network.Generator;

import java.util.List;
import java.util.Objects;

/**
 * Completes a parameter set for a model other than the one it was written for.
 * <p>
 * Giving a machine another model, an exciter with an integral term in place of a proportional one
 * for instance, leaves its parameters describing the model it had. What the new model asks for and
 * the set has not got is generated; the rest stays. The machine is the same machine and its
 * regulator the same regulator, so the inertia, the reactances and the gain a study may have
 * chosen by hand are kept, and only the integral time is added beside them.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public class ParametersSetCompleter {

    private final SynchronousGeneratorParametersGenerator generator;

    public ParametersSetCompleter() {
        this(new SynchronousGeneratorParametersGenerator());
    }

    public ParametersSetCompleter(SynchronousGeneratorParametersGenerator generator) {
        this.generator = Objects.requireNonNull(generator);
    }

    /**
     * The parameters the model declares that the set holds no value for, neither of its own nor
     * read from the network.
     */
    public static List<String> missingParameters(ParametersSet set, ModelDescription description) {
        return description.parameters().stream()
                .map(ModifiableParameter::name)
                .filter(name -> !set.getParameters().containsKey(name) && !set.getReferences().containsKey(name))
                .toList();
    }

    /**
     * A set valuing the given model, keeping every value the old set already held for a parameter
     * the model still declares.
     *
     * @param setId       identifier of the set to build, distinct from the old one since a set may
     *                    value more than one equipment and completing it in place would reach
     *                    equipments nobody asked to change
     * @param existing    the set written for the model the equipment had
     * @param description description of the model it is given
     */
    public ParametersSet complete(String setId, ParametersSet existing, ModelDescription description,
                                  Generator equipment, boolean transformer) {
        ParametersSet completed = generator.generate(setId, description, equipment, transformer);
        existing.getParameters().forEach((name, parameter) -> keep(completed, parameter));
        return completed;
    }

    private static void keep(ParametersSet completed, Parameter parameter) {
        if (completed.hasParameter(parameter.name())) {
            completed.replaceParameter(parameter.name(), parameter.type(), parameter.value());
        }
    }
}
