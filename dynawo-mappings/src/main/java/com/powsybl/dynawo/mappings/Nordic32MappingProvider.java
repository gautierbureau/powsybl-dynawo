/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings;

import com.google.auto.service.AutoService;
import com.powsybl.dynawo.characteristics.Nordic32SystemProvider;

/**
 * Registers the Nordic 32 voltage stability study, chosen by name the way any mapping is.
 * <p>
 * The tap changer blocking is the study's to ask for, so it is a setting the caller gives,
 * {@code withTCB}, off by default; the voltage telling a machine behind a transformer from one that
 * is not can be set too, and stands high by default so the Nordic machines, not taken to sit behind
 * one, keep the plain models the reference uses.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
@AutoService(DynamicMappingProvider.class)
public class Nordic32MappingProvider implements DynamicMappingProvider {

    static final String WITH_TCB = "withTCB";
    static final String TSO_VOLTAGE_MIN = "tso_voltage_min";
    // above the Nordic machines, so none is taken behind a transformer, as the reference study runs
    static final double DEFAULT_TSO_VOLTAGE_MIN = 1000;

    @Override
    public String getName() {
        return Nordic32SystemProvider.NAME;
    }

    @Override
    public String getDescription() {
        return "The Nordic 32 test system as a voltage stability study: its machines on the models "
                + "their known controls name, valued from the parameters the system ships, with its "
                + "tap changer blocking added when withTCB is set.";
    }

    @Override
    public DynamicModelsMapping create(MappingParameters parameters) {
        double tsoVoltageMin = parameters.getDouble(TSO_VOLTAGE_MIN, DEFAULT_TSO_VOLTAGE_MIN);
        boolean withTapChangerBlockings = parameters.getBoolean(WITH_TCB, false);
        return new Nordic32Mapping(tsoVoltageMin, withTapChangerBlockings);
    }
}
