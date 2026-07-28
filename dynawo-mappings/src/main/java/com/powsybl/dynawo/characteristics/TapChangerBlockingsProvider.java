/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.characteristics;

import com.powsybl.dynawo.mappings.MappingParameters;
import com.powsybl.iidm.network.Network;

/**
 * Adds the {@code tcbs} extension a mapping reads its tap changer blockings from.
 * <p>
 * Discovered with a {@link java.util.ServiceLoader} and chosen by name, as the generator controls
 * are, so a study sets the blockings of a known system as a step of its own before a model is
 * chosen for them. A network already carrying the extension is left as it is.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public interface TapChangerBlockingsProvider {

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
    default TapChangerBlockingsProvider configured(MappingParameters parameters) {
        return this;
    }

    /**
     * Adds the tap changer blockings to the network, leaving one already there untouched.
     */
    void createExtensions(Network network);
}
