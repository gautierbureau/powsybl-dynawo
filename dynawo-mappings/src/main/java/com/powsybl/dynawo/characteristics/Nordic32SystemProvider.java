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
import com.powsybl.dynawo.mappings.TapChangerBlockingsProviders;
import com.powsybl.iidm.network.Network;

/**
 * The Nordic 32 system: its machines described by their known controls, and its tap changer
 * blocking named, added together.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
@AutoService(DynamicSimulationSystemProvider.class)
public class Nordic32SystemProvider implements DynamicSimulationSystemProvider {

    public static final String NAME = "Nordic32";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "The Nordic 32 test system: its machines' controls and its tap changer blocking.";
    }

    @Override
    public void createExtensions(Network network, MappingParameters parameters) {
        SynchronousGeneratorPropertiesProviders.getInstance().createExtensions(network, NAME, parameters);
        TapChangerBlockingsProviders.getInstance().createExtensions(network, NAME, parameters);
    }
}
