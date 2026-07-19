/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings;

import com.google.auto.service.AutoService;
import com.powsybl.dynawo.DynawoSimulationParameters;
import com.powsybl.dynawo.mappings.parameters.ModelDescriptionLookup;
import com.powsybl.dynawo.parameters.ParametersSet;
import com.powsybl.iidm.network.Network;

import java.util.List;

/**
 * Mapping of the IEEE test systems, registered so that it can be selected by name.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
@AutoService(DynamicModelsMapping.class)
public class IeeeDynaWaltzMapping implements DynamicModelsMapping {

    private final UniversalSynchronousGeneratorMapping mapping = IeeeTestSystemMappings.dynaWaltz();

    @Override
    public String getName() {
        return mapping.getName();
    }

    @Override
    public DynawoSimulationParameters.SolverType getSolverType() {
        return mapping.getSolverType();
    }

    @Override
    public void createExtensions(Network network) {
        mapping.createExtensions(network);
    }

    @Override
    public List<MappedModelsSupplier.MappedModel> createModelConfigs(Network network) {
        return mapping.createModelConfigs(network);
    }

    @Override
    public List<ParametersSet> createParameters(Network network, ModelDescriptionLookup descriptions) {
        return mapping.createParameters(network, descriptions);
    }
}
