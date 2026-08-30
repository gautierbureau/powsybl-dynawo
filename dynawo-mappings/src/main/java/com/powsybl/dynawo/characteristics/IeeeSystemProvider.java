/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.characteristics;

import com.google.auto.service.AutoService;
import com.powsybl.dynawo.mappings.MappingParameters;
import com.powsybl.dynawo.mappings.SynchronousGeneratorPropertiesProviders;
import com.powsybl.iidm.network.Network;

/**
 * The IEEE test systems: their machines described from their energy source, which is all they
 * carry, and no automaton.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
@AutoService(DynamicSimulationSystemProvider.class)
public class IeeeSystemProvider implements DynamicSimulationSystemProvider {

    public static final String NAME = "IEEE";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "The IEEE test systems: machine controls deduced from the energy source.";
    }

    @Override
    public void createExtensions(Network network, MappingParameters parameters) {
        SynchronousGeneratorPropertiesProviders.getInstance()
                .createExtensions(network, EnergySourceSynchronousGeneratorPropertiesProvider.NAME, parameters);
    }
}
