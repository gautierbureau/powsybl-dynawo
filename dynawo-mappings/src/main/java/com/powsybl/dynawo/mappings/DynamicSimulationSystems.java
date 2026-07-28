/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dynawo.characteristics.DynamicSimulationSystemProvider;
import com.powsybl.iidm.network.Network;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * Registry of the {@link DynamicSimulationSystemProvider} found on the classpath, each adding every
 * extension a named system reads in one step.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public final class DynamicSimulationSystems {

    private static final DynamicSimulationSystems INSTANCE =
            new DynamicSimulationSystems(ServiceLoader.load(DynamicSimulationSystemProvider.class));

    private final Map<String, DynamicSimulationSystemProvider> systems = new LinkedHashMap<>();

    DynamicSimulationSystems(Iterable<DynamicSimulationSystemProvider> systems) {
        systems.forEach(s -> this.systems.put(s.getName(), s));
    }

    public static DynamicSimulationSystems getInstance() {
        return INSTANCE;
    }

    public Set<String> getSystemNames() {
        return systems.keySet();
    }

    public List<SystemInfo> getSystemInfos() {
        return systems.values().stream()
                .map(s -> new SystemInfo(s.getName(), s.getDescription()))
                .toList();
    }

    private DynamicSimulationSystemProvider getSystem(String name) {
        DynamicSimulationSystemProvider system = systems.get(name);
        if (system == null) {
            throw new PowsyblException("Dynamic simulation system " + name
                    + " not found, available systems are " + getSystemNames());
        }
        return system;
    }

    public void createExtensions(Network network, String name, MappingParameters parameters) {
        getSystem(name).createExtensions(network, parameters);
    }

    public record SystemInfo(String name, String description) {
    }
}
