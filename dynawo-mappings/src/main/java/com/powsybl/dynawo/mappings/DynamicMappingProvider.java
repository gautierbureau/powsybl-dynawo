/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings;

/**
 * Registers a mapping under a name, and makes one to order from the settings a caller gives.
 * <p>
 * A mapping that does the work is configured: the voltage below which a machine is taken to sit
 * behind a transformer, the flavour it runs in, the region a hybrid study runs detailed. That is
 * not something a {@link java.util.ServiceLoader} makes on its own from a constructor taking
 * nothing, which is why {@link DynamicModelsMapping} was registered through a thin class holding
 * the settings frozen at their defaults. The provider is what a service loader can make: it
 * carries the name and the one line a caller chooses a mapping by, and builds the configured
 * mapping when asked, reading what it knows out of {@link MappingParameters}.
 * <p>
 * Implementations are discovered with a {@code ServiceLoader}, so a deployment adds a mapping by
 * putting a provider on the classpath and nothing else. This is also how the RTE mappings are
 * reached beside the open source ones.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public interface DynamicMappingProvider {

    /**
     * The name a mapping is created by, unique among the registered ones.
     */
    String getName();

    /**
     * One line saying what the mapping is for, shown where the registered mappings are listed so a
     * caller can tell them apart without reading the code behind them.
     */
    String getDescription();

    /**
     * The configured mapping, built from the settings given, each left to its default where the
     * caller said nothing.
     */
    DynamicModelsMapping create(MappingParameters parameters);
}
