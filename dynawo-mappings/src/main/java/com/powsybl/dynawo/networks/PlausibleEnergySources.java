/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.networks;

import com.powsybl.dynawo.mappings.parameters.GeneratorSizing;
import com.powsybl.iidm.network.EnergySource;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;

/**
 * Gives a generation mix to a network that declares none, from the size of each machine.
 * <p>
 * Test systems rarely say what their machines burn: the IEEE and PEGASE cases label every one of
 * them {@link EnergySource#OTHER}, which leaves the controls with nothing to be deduced from. Size
 * is the only clue left, so a large unit is taken for a nuclear set, a medium one for a thermal set
 * and a small one for a hydro set.
 * <p>
 * This is a guess, and it is deliberately kept apart from the mapping: it fills in the network
 * description, then the extensions are built from the energy sources as usual. A study that knows
 * the real mix should set it instead of calling this, and {@link Ieee14EnergySources} shows the
 * shape of a mix written for one particular system.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public final class PlausibleEnergySources {

    /**
     * Nominal active power above which a machine is taken for a nuclear set, in MW.
     */
    public static final double LARGE_UNIT_POWER = 600.0;

    /**
     * Nominal active power above which a machine is taken for a thermal set rather than a hydro
     * one, in MW.
     */
    public static final double MEDIUM_UNIT_POWER = 100.0;

    private PlausibleEnergySources() {
    }

    /**
     * Sets an energy source on the machines that declare none, leaving the others untouched.
     */
    public static void apply(Network network) {
        network.getGeneratorStream()
                .filter(g -> g.getEnergySource() == EnergySource.OTHER)
                .forEach(g -> g.setEnergySource(energySourceOf(g)));
    }

    public static EnergySource energySourceOf(Generator generator) {
        double nominalP = GeneratorSizing.nominalActivePower(generator);
        if (nominalP >= LARGE_UNIT_POWER) {
            return EnergySource.NUCLEAR;
        }
        return nominalP >= MEDIUM_UNIT_POWER ? EnergySource.THERMAL : EnergySource.HYDRO;
    }
}
