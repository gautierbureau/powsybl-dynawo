/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.networks;

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

    /**
     * The machines by the bus they sit on, since the same system is named one way by the IEEE data
     * and another by the CGMES file Dynawo ships with its examples.
     */
    private static final Map<Integer, EnergySource> ENERGY_SOURCES_BY_BUS = Map.of(
            1, EnergySource.NUCLEAR,
            2, EnergySource.THERMAL,
            3, EnergySource.THERMAL,
            6, EnergySource.HYDRO,
            8, EnergySource.HYDRO);

    private Ieee14EnergySources() {
    }

    public static void apply(Network network) {
        ENERGY_SOURCES_BY_BUS.forEach((bus, energySource) -> {
            Generator generator = find(network, bus);
            if (generator != null) {
                generator.setEnergySource(energySource);
            }
        });
    }

    private static Generator find(Network network, int bus) {
        Generator generator = network.getGenerator("B" + bus + "-G");
        return generator != null ? generator
                : network.getGenerator("_GEN" + String.format("%5s", bus).replace(' ', '_') + "_SM");
    }
}
