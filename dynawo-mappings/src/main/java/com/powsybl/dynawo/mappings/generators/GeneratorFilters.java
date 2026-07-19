/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.generators;

import com.powsybl.iidm.network.Generator;

import java.util.function.Predicate;

/**
 * Which generators a mapping describes dynamically.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public final class GeneratorFilters {

    /**
     * Active power below which a machine is not considered to be generating, in MW.
     */
    public static final double GENERATION_THRESHOLD = 0.01;

    private GeneratorFilters() {
    }

    /**
     * Every connected machine, including the ones generating nothing.
     * <p>
     * Default: a synchronous machine holding no active power still takes part in the dynamics
     * through its voltage regulation, synchronous condensers being the obvious case. Three of the
     * five machines of the IEEE 14 bus network are condensers, so dropping them would leave the
     * test system almost undescribed.
     */
    public static Predicate<Generator> connected() {
        return g -> g.getTerminal().isConnected();
    }

    /**
     * Only the machines actually generating, the criterion the historical groovy and python
     * mappings used. Reads the load flow results, and falls back on the target when a network has
     * none.
     */
    public static Predicate<Generator> generating() {
        return connected().and(g -> {
            double p = g.getTerminal().getP();
            return Double.isNaN(p) ? g.getTargetP() > GENERATION_THRESHOLD : p < -GENERATION_THRESHOLD;
        });
    }
}
