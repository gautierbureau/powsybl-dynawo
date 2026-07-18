/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.controls;

import com.google.auto.service.AutoService;

import java.util.Map;

/**
 * Open source translation tables.
 * <p>
 * Every detailed governor collapses to {@code Proportional}, which is the only simplified
 * governor available. Voltage regulators keep their own table since they do not all collapse to
 * the same simplified regulator.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
@AutoService(ControlTranslation.class)
public class DefaultControlTranslation implements ControlTranslation {

    public static final String PROPORTIONAL = "Proportional";
    public static final String PROPORTIONAL_INTEGRAL = "ProportionalIntegral";

    @Override
    public Map<String, String> getGovernorTranslations() {
        return Map.of(WILDCARD, PROPORTIONAL);
    }

    @Override
    public Map<String, String> getVoltageRegulatorTranslations() {
        return Map.of("St4b", PROPORTIONAL_INTEGRAL,
                PROPORTIONAL, PROPORTIONAL,
                WILDCARD, PROPORTIONAL_INTEGRAL);
    }

    @Override
    public Map<SimplifiedControls, String> getSimplifiedControlsFragments() {
        return Map.of(new SimplifiedControls(PROPORTIONAL, PROPORTIONAL), "ProportionalRegulations",
                new SimplifiedControls(PROPORTIONAL, PROPORTIONAL_INTEGRAL), "GoverPropVRPropInt");
    }
}
