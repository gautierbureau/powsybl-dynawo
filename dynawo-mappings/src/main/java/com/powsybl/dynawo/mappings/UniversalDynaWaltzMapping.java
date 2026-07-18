/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings;

import com.google.auto.service.AutoService;
import com.powsybl.dynawo.suppliers.dynamicmodels.DynamicModelConfig;
import com.powsybl.iidm.network.Network;

import java.util.List;

/**
 * Simplified flavour of {@link UniversalSynchronousGeneratorMapping}, registered so that it can be
 * selected by name.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
@AutoService(DynamicModelsMapping.class)
public class UniversalDynaWaltzMapping implements DynamicModelsMapping {

    private final UniversalSynchronousGeneratorMapping mapping = UniversalSynchronousGeneratorMapping.dynaWaltz();

    @Override
    public String getName() {
        return UniversalSynchronousGeneratorMapping.DYNAWALTZ_NAME;
    }

    @Override
    public void createExtensions(Network network) {
        mapping.createExtensions(network);
    }

    @Override
    public List<DynamicModelConfig> createModelConfigs(Network network) {
        return mapping.createModelConfigs(network);
    }
}
