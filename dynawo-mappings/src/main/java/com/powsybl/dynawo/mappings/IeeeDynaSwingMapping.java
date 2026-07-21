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
 * Mapping of the IEEE test systems for a transient study, registered so that it can be selected by
 * name.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
@AutoService(DynamicModelsMapping.class)
public class IeeeDynaSwingMapping extends AbstractForwardingDynamicModelsMapping {

    private final UniversalSynchronousGeneratorMapping mapping = IeeeTestSystemMappings.dynaSwing();

    @Override
    protected DynamicModelsMapping delegate() {
        return mapping;
    }
}
