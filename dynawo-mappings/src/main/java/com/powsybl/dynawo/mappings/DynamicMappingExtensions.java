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
import com.powsybl.dynawo.characteristics.TapChangerBlockingsProvider;
import com.powsybl.iidm.network.Network;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * The one registry every kind of dynamic mapping extension is reached through, each provider named
 * by the extension it adds and by itself.
 * <p>
 * A study describes an equipment's characteristics before a mapping reads them; the providers that
 * write those extensions used to be reached each through a registry and a binding method of its own,
 * one per kind. Here they are reached through a single door: the caller names the extension and the
 * provider, and the same call serves the generator controls, the tap changer blockings, and the RTE
 * ACMC and SMACC alike. The public API keeps a method of its own for each kind as sugar, but it runs
 * this underneath, so the public and the RTE sides, the latter having no such sugar, describe an
 * extension the very same way.
 * <p>
 * Providers of a kind that has a {@link java.util.ServiceLoader} SPI of its own (the generator
 * controls, the tap changer blockings) are gathered from it; any other kind is a plain
 * {@link DynamicMappingExtensionsProvider} on the classpath, which is how a deployment adds one, the
 * RTE extensions among them, without touching this.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public final class DynamicMappingExtensions {

    private static final DynamicMappingExtensions INSTANCE = new DynamicMappingExtensions(load());

    // extension name -> (provider name -> provider), both kept in the order they were found
    private final Map<String, Map<String, DynamicMappingExtensionsProvider>> byExtension = new LinkedHashMap<>();

    DynamicMappingExtensions(Iterable<? extends DynamicMappingExtensionsProvider> providers) {
        providers.forEach(p -> byExtension
                .computeIfAbsent(p.getExtensionName(), k -> new LinkedHashMap<>())
                .putIfAbsent(p.getName(), p));
    }

    private static List<DynamicMappingExtensionsProvider> load() {
        List<DynamicMappingExtensionsProvider> all = new ArrayList<>();
        // the kinds with an SPI of their own, kept so their existing providers need no change
        ServiceLoader.load(SynchronousGeneratorPropertiesProvider.class).forEach(all::add);
        ServiceLoader.load(TapChangerBlockingsProvider.class).forEach(all::add);
        // any other kind, the RTE ACMC and SMACC among them, added as a plain provider
        ServiceLoader.load(DynamicMappingExtensionsProvider.class).forEach(all::add);
        return all;
    }

    public static DynamicMappingExtensions getInstance() {
        return INSTANCE;
    }

    /**
     * The kinds of extension a caller can ask for, each the name of one to give
     * {@link #createExtensions}.
     */
    public Set<String> getExtensionNames() {
        return byExtension.keySet();
    }

    /**
     * The providers adding the named kind, each with the one line it is chosen by.
     */
    public List<ProviderInfo> getProviderInfos(String extensionName) {
        return providers(extensionName).values().stream()
                .map(p -> new ProviderInfo(p.getName(), p.getDescription()))
                .toList();
    }

    /**
     * Adds the named extension to the network with the named provider, set up with the settings
     * given, leaving one already there untouched.
     */
    public void createExtensions(Network network, String extensionName, String providerName, MappingParameters parameters) {
        DynamicMappingExtensionsProvider provider = providers(extensionName).get(providerName);
        if (provider == null) {
            throw new PowsyblException("Provider " + providerName + " for extension " + extensionName
                    + " not found, available providers are " + providers(extensionName).keySet());
        }
        provider.configured(parameters).createExtensions(network);
    }

    private Map<String, DynamicMappingExtensionsProvider> providers(String extensionName) {
        Map<String, DynamicMappingExtensionsProvider> providers = byExtension.get(extensionName);
        if (providers == null) {
            throw new PowsyblException("Dynamic mapping extension " + extensionName
                    + " not found, available extensions are " + getExtensionNames());
        }
        return providers;
    }

    public record ProviderInfo(String name, String description) {
    }
}
