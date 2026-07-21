/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.dynawo.DynawoSimulationParameters;
import com.powsybl.dynawo.builders.ModelConfig;
import com.powsybl.dynawo.mappings.parameters.ModelDescriptionLookup;
import com.powsybl.dynawo.parameters.ParametersSet;
import com.powsybl.iidm.network.Network;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A whole network described for a study, each kind of equipment by its own part.
 * <p>
 * A transient study needs a model behind every machine and every load, a source and a sink, or the
 * network it solves has nothing holding its voltage. One mapping stands for the study and hands
 * each kind of equipment to the part that knows it: the machines to the one resolving their
 * controls, the loads to the one giving them a voltage dependent model. What each part makes is
 * gathered, so a study is one thing to apply though it is made of several.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public class UniversalMapping implements DynamicModelsMapping {

    private final String name;
    private final UniversalSynchronousGeneratorMapping generators;
    private final LoadMapping loads;

    protected UniversalMapping(String name, UniversalSynchronousGeneratorMapping generators, LoadMapping loads) {
        this.name = Objects.requireNonNull(name);
        this.generators = Objects.requireNonNull(generators);
        this.loads = Objects.requireNonNull(loads);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public DynawoSimulationParameters.SolverType getSolverType() {
        // the study runs one solver, which follows from its models rather than from any one part
        return generators.getSolverType();
    }

    @Override
    public void createExtensions(Network network) {
        // only the machines carry the extensions a mapping reads; a load is described from itself
        generators.createExtensions(network);
    }

    @Override
    public List<MappedModelsSupplier.MappedModel> createModelConfigs(Network network) {
        return createModelConfigs(network, ReportNode.NO_OP);
    }

    @Override
    public List<MappedModelsSupplier.MappedModel> createModelConfigs(Network network, ReportNode reportNode) {
        List<MappedModelsSupplier.MappedModel> models =
                new ArrayList<>(generators.createModelConfigs(network, reportNode));
        models.addAll(loads.createModelConfigs(network));
        return models;
    }

    @Override
    public List<ParametersSet> createParameters(Network network, ModelDescriptionLookup descriptions) {
        List<ParametersSet> sets = new ArrayList<>(generators.createParameters(network, descriptions));
        sets.addAll(loads.createParameters(network));
        return sets;
    }

    // the models built for the machines are the mapping's, carried through so a study applied by
    // name reaches them as one applied by hand does

    @Override
    public Optional<Path> getBuiltModelsDir() {
        return generators.getBuiltModelsDir();
    }

    @Override
    public Map<String, List<ModelConfig>> getBuiltModelConfigs() {
        return generators.getBuiltModelConfigs();
    }

    @Override
    public Map<String, List<ModelConfig>> getModelConfigOverrides() {
        return generators.getModelConfigOverrides();
    }

    @Override
    public ModelDescriptionLookup describeBuiltModels(ModelDescriptionLookup installed) {
        return generators.describeBuiltModels(installed);
    }
}
