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
 * Registers the simplified whole network study: every synchronous generator on a model deduced
 * from its controls, every load on a voltage dependent one.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
@AutoService(DynamicMappingProvider.class)
public class UniversalDynaWaltzProvider implements DynamicMappingProvider {

    /**
     * The voltage below which a machine is taken to sit behind a transformer it does not carry
     * itself, so the transmission connected ones get one and the rest do not.
     */
    static final String TSO_VOLTAGE_MIN = "tso_voltage_min";

    @Override
    public String getName() {
        return UniversalSynchronousGeneratorMapping.DYNAWALTZ_NAME;
    }

    @Override
    public String getDescription() {
        return "Voltage stability study of any network: every synchronous generator on a simplified "
                + "model deduced from its controls, every load on a voltage dependent one.";
    }

    @Override
    public DynamicModelsMapping create(MappingParameters parameters) {
        double tsoVoltageMin = parameters.getDouble(TSO_VOLTAGE_MIN,
                EnergySourceSynchronousGeneratorPropertiesProvider.DEFAULT_TSO_VOLTAGE_MIN);
        return new UniversalMapping(UniversalSynchronousGeneratorMapping.DYNAWALTZ_NAME,
                UniversalSynchronousGeneratorMapping.dynaWaltz(tsoVoltageMin), new LoadMapping("DynaWaltz_"));
    }
}
