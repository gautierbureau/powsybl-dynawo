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
 * The simplified study, its machines described for a voltage stability run and its loads given a
 * voltage dependent model, registered so that it can be selected by name.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
@AutoService(DynamicModelsMapping.class)
public class UniversalDynaWaltzMapping extends UniversalMapping {

    public UniversalDynaWaltzMapping() {
        super(UniversalSynchronousGeneratorMapping.DYNAWALTZ_NAME,
                UniversalSynchronousGeneratorMapping.dynaWaltz(), new LoadMapping("DynaWaltz_"));
    }
}
