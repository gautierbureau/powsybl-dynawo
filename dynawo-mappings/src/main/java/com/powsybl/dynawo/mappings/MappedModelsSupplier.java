/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.dynamicsimulation.DynamicModel;
import com.powsybl.dynamicsimulation.DynamicModelsSupplier;
import com.powsybl.dynawo.DynawoSimulationProvider;
import com.powsybl.dynawo.builders.AbstractEquipmentModelBuilder;
import com.powsybl.dynawo.builders.ModelBuilder;
import com.powsybl.dynawo.builders.ModelConfigsHandler;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Builds the models a mapping resolved.
 * <p>
 * The models are described by a library, a parameter set and the equipment they stand for, and
 * nothing else, so the builders are called for what they are instead of by reflection the way a
 * model read from a file has to be. That keeps a mapping usable from a natively compiled image,
 * where a method reached by reflection has to be declared beforehand.
 * <p>
 * A model that is not one equipment's, an automation system watching several, is set up by a hand
 * of its own rather than by a static id: it names what it watches and what it acts on, which a
 * bare equipment id cannot say. The mapping that resolved it gives that hand, casting the builder
 * to the one it knows, so a native image still reaches the methods without reflection.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public class MappedModelsSupplier implements DynamicModelsSupplier {

    private static final Logger LOGGER = LoggerFactory.getLogger(MappedModelsSupplier.class);

    /**
     * @param lib            library of the model
     * @param staticId       identifier of the equipment the model stands for, or a name for a model
     *                       that stands for none
     * @param parameterSetId identifier of the set holding its parameters
     * @param configurer     sets the builder up where a static id and a set do not say enough, an
     *                       automation system's for one, or {@code null} for an equipment's model
     */
    public record MappedModel(String lib, String staticId, String parameterSetId,
                              Consumer<ModelBuilder<DynamicModel>> configurer) {

        public MappedModel(String lib, String staticId, String parameterSetId) {
            this(lib, staticId, parameterSetId, null);
        }
    }

    private final List<MappedModel> models;

    public MappedModelsSupplier(List<MappedModel> models) {
        this.models = Objects.requireNonNull(models);
    }

    @Override
    public String getName() {
        return DynawoSimulationProvider.NAME;
    }

    @Override
    public List<DynamicModel> get(Network network, ReportNode reportNode) {
        return models.stream()
                .map(model -> build(model, network, reportNode))
                .filter(Objects::nonNull)
                .toList();
    }

    private static DynamicModel build(MappedModel model, Network network, ReportNode reportNode) {
        ModelBuilder<DynamicModel> builder = ModelConfigsHandler.getInstance()
                .getModelBuilder(network, model.lib(), reportNode);
        if (builder == null) {
            LOGGER.warn("No builder found for model {}, {} will not be modelled", model.lib(), model.staticId());
            return null;
        }
        if (model.configurer() != null) {
            model.configurer().accept(builder);
            return builder.build();
        }
        if (builder instanceof AbstractEquipmentModelBuilder<?, ?> equipmentBuilder) {
            equipmentBuilder.staticId(model.staticId());
            equipmentBuilder.parameterSetId(model.parameterSetId());
        } else {
            LOGGER.warn("Model {} does not stand for an equipment, {} will not be modelled", model.lib(), model.staticId());
            return null;
        }
        return builder.build();
    }
}
