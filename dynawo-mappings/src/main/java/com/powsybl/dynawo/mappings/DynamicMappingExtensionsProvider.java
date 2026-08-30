/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings;

import com.powsybl.iidm.network.Network;

/**
 * Adds one kind of dynamic mapping extension to a network, under a name, so a study describes its
 * characteristics as a step of its own before a mapping reads them.
 * <p>
 * This is the one shape every such provider takes, whatever the extension, so a caller reaches any
 * of them through a single door, naming the extension and the provider, rather than through a method
 * of its own for each kind. That is what lets a deployment add a new kind of extension, the RTE ACMC
 * and SMACC among them, by putting a provider on the classpath and nothing else, and lets a binding
 * expose them all without a method per kind. The named registry is {@link DynamicMappingExtensions}.
 * <p>
 * It follows the two step design of {@link DynamicModelsMapping}: an extension the network already
 * carries is left as it is, so describing the characteristics and mapping them stay apart.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public interface DynamicMappingExtensionsProvider {

    /**
     * The kind of extension this adds, the name a caller asks for it by, the same for every provider
     * of that kind. Typically the IIDM extension's own name.
     */
    String getExtensionName();

    /**
     * The provider's name, unique among those adding the same kind, the name a caller chooses one by.
     */
    String getName();

    /**
     * One line saying what this provider adds, shown where the providers are listed.
     */
    default String getDescription() {
        return getName();
    }

    /**
     * The provider set up with the given settings, or this one where it takes none.
     */
    default DynamicMappingExtensionsProvider configured(MappingParameters parameters) {
        return this;
    }

    /**
     * Adds the extension to the network, leaving one already there untouched.
     */
    void createExtensions(Network network);
}
