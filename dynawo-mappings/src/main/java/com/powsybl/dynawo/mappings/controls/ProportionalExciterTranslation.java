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
 * Simplifies every exciter to a proportional voltage regulation.
 * <p>
 * This is how the IEEE test systems are described: their reference models are the fully
 * proportional regulations, whichever exciter the machines are given in a transient study. It
 * departs from the general tables, where a detailed exciter keeps an integral term because a real
 * machine carrying one holds no steady state voltage error, so it is not contributed to the
 * classpath: a mapping that wants it passes it to its own
 * {@link com.powsybl.dynawo.mappings.generators.GeneratorLibResolver}.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public class ProportionalExciterTranslation implements ControlTranslation {

    @Override
    public int getPriority() {
        return 1;
    }

    @Override
    public Map<String, String> getVoltageRegulatorTranslations() {
        // named rather than a wildcard: only the detailed exciters are rewritten, so a machine
        // deliberately described with a simplified regulation keeps the one it was given
        return Map.of("St4b", DefaultControlTranslation.VR_PROPORTIONAL,
                "St5b", DefaultControlTranslation.VR_PROPORTIONAL,
                "St6b", DefaultControlTranslation.VR_PROPORTIONAL,
                "St7b", DefaultControlTranslation.VR_PROPORTIONAL,
                "IEEX2A", DefaultControlTranslation.VR_PROPORTIONAL);
    }

    @Override
    public Map<String, String> getGovernorTranslations() {
        return Map.of();
    }

}
