/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dynawo.characteristics.TapChangerBlockingsProvider;
import com.powsybl.iidm.network.Network;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * Registry of the {@link TapChangerBlockingsProvider} found on the classpath, each a named way of
 * giving a network its tap changer blockings.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public final class TapChangerBlockingsProviders {

    private static final TapChangerBlockingsProviders INSTANCE =
            new TapChangerBlockingsProviders(ServiceLoader.load(TapChangerBlockingsProvider.class));

    private final Map<String, TapChangerBlockingsProvider> providers = new LinkedHashMap<>();

    TapChangerBlockingsProviders(Iterable<TapChangerBlockingsProvider> providers) {
        providers.forEach(p -> this.providers.put(p.getName(), p));
    }

    public static TapChangerBlockingsProviders getInstance() {
        return INSTANCE;
    }

    public Set<String> getProviderNames() {
        return providers.keySet();
    }

    public List<ProviderInfo> getProviderInfos() {
        return providers.values().stream()
                .map(p -> new ProviderInfo(p.getName(), p.getDescription()))
                .toList();
    }

    private TapChangerBlockingsProvider getProvider(String name) {
        TapChangerBlockingsProvider provider = providers.get(name);
        if (provider == null) {
            throw new PowsyblException("Tap changer blockings provider " + name
                    + " not found, available providers are " + getProviderNames());
        }
        return provider;
    }

    public void createExtensions(Network network, String name, MappingParameters parameters) {
        getProvider(name).configured(parameters).createExtensions(network);
    }

    public record ProviderInfo(String name, String description) {
    }
}
