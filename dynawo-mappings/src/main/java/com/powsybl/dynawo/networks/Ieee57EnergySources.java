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

import java.util.List;

/**
 * Gives the IEEE 57 bus network a generation mix, as {@link Ieee14EnergySources} does for the
 * smaller system.
 * <p>
 * Dynawo describes its seven machines with four winding models, so all of them are steam sets
 * here. The IEEE data itself says nothing about what they burn.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public final class Ieee57EnergySources {

    private static final List<String> GENERATORS = List.of("B1-G", "B2-G", "B3-G", "B6-G", "B8-G", "B9-G", "B12-G");

    private Ieee57EnergySources() {
    }

    public static void apply(Network network) {
        GENERATORS.forEach(id -> {
            Generator generator = network.getGenerator(id);
            if (generator != null) {
                generator.setEnergySource(EnergySource.THERMAL);
            }
        });
    }
}
