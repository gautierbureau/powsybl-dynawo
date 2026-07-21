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
 * A mapping that stands in for another, passing everything on to it.
 * <p>
 * A mapping is registered under a name by a class with a constructor taking nothing, which the one
 * doing the work is not, being configured. So a thin class carries the name and hands the work
 * over. Forwarding every method in one place rather than each class picking the ones it remembers
 * is what keeps a method added to a mapping from being quietly dropped by a class that never heard
 * of it, which is how a model built on the fly once reached a mapping applied by hand but not one
 * applied by name.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public abstract class AbstractForwardingDynamicModelsMapping implements DynamicModelsMapping {

    /**
     * The mapping the work is passed to.
     */
    protected abstract DynamicModelsMapping delegate();

    @Override
    public String getName() {
        return delegate().getName();
    }

    @Override
    public DynawoSimulationParameters.SolverType getSolverType() {
        return delegate().getSolverType();
    }

    @Override
    public void createExtensions(Network network) {
        delegate().createExtensions(network);
    }

    @Override
    public List<MappedModelsSupplier.MappedModel> createModelConfigs(Network network) {
        return delegate().createModelConfigs(network);
    }

    @Override
    public List<ParametersSet> createParameters(Network network, ModelDescriptionLookup descriptions) {
        return delegate().createParameters(network, descriptions);
    }

    @Override
    public Optional<Path> getBuiltModelsDir() {
        return delegate().getBuiltModelsDir();
    }

    @Override
    public Map<String, List<ModelConfig>> getBuiltModelConfigs() {
        return delegate().getBuiltModelConfigs();
    }

    @Override
    public Map<String, List<ModelConfig>> getModelConfigOverrides() {
        return delegate().getModelConfigOverrides();
    }

    @Override
    public ModelDescriptionLookup describeBuiltModels(ModelDescriptionLookup installed) {
        return delegate().describeBuiltModels(installed);
    }
}
