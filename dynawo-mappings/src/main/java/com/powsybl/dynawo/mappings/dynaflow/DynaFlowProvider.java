/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.dynaflow;

import com.google.auto.service.AutoService;
import com.powsybl.dynawo.mappings.DynamicMappingProvider;
import com.powsybl.dynawo.mappings.DynamicModelsMapping;
import com.powsybl.dynawo.mappings.MappingParameters;

/**
 * Registers the DynaFlow study: every equipment on the DynaFlow model the network calls for, the Java
 * replacement for the DynaFlow Launcher's model creation.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
@AutoService(DynamicMappingProvider.class)
public class DynaFlowProvider implements DynamicMappingProvider {

    @Override
    public String getName() {
        return DynaFlowMapping.NAME;
    }

    @Override
    public String getDescription() {
        return "DynaFlow steady-state study of any network: every equipment on the DynaFlow model the "
                + "network calls for, deduced as the DynaFlow Launcher does.";
    }

    @Override
    public DynamicModelsMapping create(MappingParameters parameters) {
        return new DynaFlowMapping(DynaFlowMapping.NAME);
    }
}
