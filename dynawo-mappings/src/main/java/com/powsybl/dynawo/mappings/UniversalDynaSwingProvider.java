/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings;

import com.google.auto.service.AutoService;
import com.powsybl.dynawo.characteristics.EnergySourceSynchronousGeneratorPropertiesProvider;

/**
 * Registers the detailed whole network study: every synchronous generator on the model carrying
 * its full controls, every load on a voltage dependent one.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
@AutoService(DynamicMappingProvider.class)
public class UniversalDynaSwingProvider implements DynamicMappingProvider {

    @Override
    public String getName() {
        return UniversalSynchronousGeneratorMapping.DYNASWING_NAME;
    }

    @Override
    public String getDescription() {
        return "Transient study of any network: every synchronous generator on the model carrying "
                + "its detailed controls, every load on a voltage dependent one.";
    }

    @Override
    public DynamicModelsMapping create(MappingParameters parameters) {
        double tsoVoltageMin = parameters.getDouble(UniversalDynaWaltzProvider.TSO_VOLTAGE_MIN,
                EnergySourceSynchronousGeneratorPropertiesProvider.DEFAULT_TSO_VOLTAGE_MIN);
        return new UniversalMapping(UniversalSynchronousGeneratorMapping.DYNASWING_NAME,
                UniversalSynchronousGeneratorMapping.dynaSwing(tsoVoltageMin), new LoadMapping("DynaSwing_"));
    }
}
