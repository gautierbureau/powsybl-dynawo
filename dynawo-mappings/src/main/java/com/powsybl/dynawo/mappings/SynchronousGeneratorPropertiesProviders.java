/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dynawo.characteristics.SynchronousGeneratorPropertiesProvider;
import com.powsybl.iidm.network.Network;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * Registry of the {@link SynchronousGeneratorPropertiesProvider} found on the classpath, each a
 * named way of describing the controls of a fleet.
 * <p>
 * A study reaches these as a step of its own, before a mapping chooses a model for the controls:
 * it names a provider and creates the extensions, and the mapping then reads them and leaves them
 * be. A machine already described is never described again, so a study may describe part of a
 * fleet this way, from a known system say, and let the mapping's own provider deduce the rest.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public final class SynchronousGeneratorPropertiesProviders {

    private static final SynchronousGeneratorPropertiesProviders INSTANCE =
            new SynchronousGeneratorPropertiesProviders(ServiceLoader.load(SynchronousGeneratorPropertiesProvider.class));

    private final Map<String, SynchronousGeneratorPropertiesProvider> providers = new LinkedHashMap<>();

    SynchronousGeneratorPropertiesProviders(Iterable<SynchronousGeneratorPropertiesProvider> providers) {
        providers.forEach(p -> this.providers.put(p.getName(), p));
    }

    public static SynchronousGeneratorPropertiesProviders getInstance() {
        return INSTANCE;
    }

    public Set<String> getProviderNames() {
        return providers.keySet();
    }

    /**
     * What each registered provider is: its name and the one line it is chosen by.
     */
    public List<ProviderInfo> getProviderInfos() {
        return providers.values().stream()
                .map(p -> new ProviderInfo(p.getName(), p.getDescription()))
                .toList();
    }

    private SynchronousGeneratorPropertiesProvider getProvider(String name) {
        SynchronousGeneratorPropertiesProvider provider = providers.get(name);
        if (provider == null) {
            throw new PowsyblException("Generator properties provider " + name
                    + " not found, available providers are " + getProviderNames());
        }
        return provider;
    }

    /**
     * Describes the fleet with the named provider, set up with the given settings. A machine
     * already carrying its controls is left as it is.
     */
    public void createExtensions(Network network, String name, MappingParameters parameters) {
        getProvider(name).configured(parameters).createExtensions(network);
    }

    /**
     * The name and description of one registered provider.
     */
    public record ProviderInfo(String name, String description) {
    }
}
