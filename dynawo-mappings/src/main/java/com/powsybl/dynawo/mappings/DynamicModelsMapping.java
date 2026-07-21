/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings;
import com.powsybl.dynawo.DynawoSimulationParameters;
import com.powsybl.dynawo.builders.ModelConfig;
import com.powsybl.dynawo.mappings.parameters.ModelDescriptionLookup;
import com.powsybl.dynawo.parameters.ParametersSet;
import com.powsybl.iidm.network.Network;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A ready to use mapping between the equipments of a network and the dynamic models representing
 * them, so that running a simulation does not require writing a mapping script.
 * <p>
 * Implementations are discovered with a {@link java.util.ServiceLoader} and selected by name, the
 * way model simplifiers are, which makes a mapping usable from a configuration file, from itools
 * or from the python API.
 * <p>
 * A mapping is applied in two steps, kept apart because a network may already carry its dynamic
 * characteristics, in which case the first step is skipped:
 * <ol>
 *     <li>{@link #createExtensions(Network)} describes the dynamic characteristics of the
 *     equipments as IIDM extensions,</li>
 *     <li>{@link #createModelConfigs(Network)} turns those characteristics into dynamic model
 *     configurations.</li>
 * </ol>
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public interface DynamicModelsMapping {

    String getName();

    /**
     * The solver the models of this mapping are meant to run with.
     * <p>
     * Simplified models integrate well with the fixed time step solver, which is what makes a long
     * simulation affordable, while detailed ones need the variable time step solver to follow their
     * faster dynamics. The choice belongs to the mapping rather than to the user, since it follows
     * from the models it produces.
     */
    DynawoSimulationParameters.SolverType getSolverType();

    /**
     * Creates the extensions describing the dynamic characteristics of the equipments, leaving
     * untouched those the network already carries.
     */
    void createExtensions(Network network);

    /**
     * Resolves the model of every equipment the mapping covers, from the extensions the network
     * carries.
     */
    List<MappedModelsSupplier.MappedModel> createModelConfigs(Network network);

    /**
     * Builds the parameter sets the models need, one per mapped equipment, from the parameters the
     * models declare and the characteristics of the equipments.
     */
    List<ParametersSet> createParameters(Network network, ModelDescriptionLookup descriptions);

    /**
     * Where this mapping leaves the models it had to build, which a simulation is to look in
     * besides the ones Dynawo ships, or nothing where it builds none.
     */
    default Optional<Path> getBuiltModelsDir() {
        return Optional.empty();
    }

    /**
     * The descriptions to generate parameters from, given those of the installed models. A
     * mapping building models of its own reads theirs out of the libraries it built, nothing else
     * describing them.
     */
    default ModelDescriptionLookup describeBuiltModels(ModelDescriptionLookup installed) {
        return installed;
    }

    /**
     * The models this mapping had to build, under the category their builder is declared for, so a
     * simulation can be told they exist and stand them up. Empty where it builds none.
     */
    default Map<String, List<ModelConfig>> getBuiltModelConfigs() {
        return Map.of();
    }
}
