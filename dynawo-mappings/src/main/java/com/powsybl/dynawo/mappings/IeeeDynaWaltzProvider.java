/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings;

import com.google.auto.service.AutoService;

/**
 * Registers the IEEE test systems mapped for a voltage stability study.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
@AutoService(DynamicMappingProvider.class)
public class IeeeDynaWaltzProvider implements DynamicMappingProvider {

    @Override
    public String getName() {
        return IeeeTestSystemMappings.DYNAWALTZ_NAME;
    }

    @Override
    public String getDescription() {
        return "Voltage stability study of the IEEE test systems (14, 57, Nordic).";
    }

    @Override
    public DynamicModelsMapping create(MappingParameters parameters) {
        return IeeeTestSystemMappings.dynaWaltz();
    }
}
