/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.networks;

import com.powsybl.iidm.network.EnergySource;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;

import java.util.Map;

/**
 * Gives the IEEE 14 bus network the generation mix it lacks.
 * <p>
 * The network built from the IEEE CDF data declares every generator as {@link EnergySource#OTHER},
 * which leaves nothing to pick controls from. Assigning a plausible mix makes the mapping produce
 * the variety of models the test system is meant to illustrate.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public final class Ieee14EnergySources {

    private static final Map<String, EnergySource> ENERGY_SOURCES = Map.of(
            "B1-G", EnergySource.NUCLEAR,
            "B2-G", EnergySource.THERMAL,
            "B3-G", EnergySource.THERMAL,
            "B6-G", EnergySource.HYDRO,
            "B8-G", EnergySource.HYDRO);

    private Ieee14EnergySources() {
    }

    public static void apply(Network network) {
        ENERGY_SOURCES.forEach((id, energySource) -> {
            Generator generator = network.getGenerator(id);
            if (generator != null) {
                generator.setEnergySource(energySource);
            }
        });
    }
}
