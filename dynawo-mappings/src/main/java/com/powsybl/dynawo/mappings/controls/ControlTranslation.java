/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.controls;

import java.util.Map;

/**
 * Translates the detailed controls carried by the generator extensions (DynaSwing world) into
 * their simplified counterpart (DynaWaltz world).
 * <p>
 * Contributions are discovered with a {@link java.util.ServiceLoader} and merged by
 * {@link ControlTranslations}, the same way model catalogs are contributed through
 * {@link com.powsybl.dynawo.builders.ModelConfigLoader}. Open source ships the base tables and
 * a jar providing additional models (RTE ones for instance) contributes the corresponding
 * additional entries, without any change to the mapping algorithm.
 * <p>
 * Governors and voltage regulators are translated symmetrically: both are plain tables, so a
 * contribution can refine either one. {@link #WILDCARD} declares the fallback used when a
 * control has no explicit entry.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public interface ControlTranslation {

    /**
     * Key matching any control without an explicit entry.
     */
    String WILDCARD = "*";

    /**
     * Detailed governor name (a Dynawo model name fragment, e.g. {@code GovCt2}) to its
     * simplified counterpart.
     */
    Map<String, String> getGovernorTranslations();

    /**
     * Detailed voltage regulator name (a Dynawo model name fragment, e.g. {@code St4b}) to its
     * simplified counterpart.
     */
    Map<String, String> getVoltageRegulatorTranslations();

    /**
     * Simplified controls couple to the Dynawo model name fragment implementing it. The
     * simplified libraries do not concatenate both control names (a
     * {@code (Proportional, Proportional)} couple is named {@code ProportionalRegulations}),
     * hence this third table.
     */
    Map<SimplifiedControls, String> getSimplifiedControlsFragments();
}
