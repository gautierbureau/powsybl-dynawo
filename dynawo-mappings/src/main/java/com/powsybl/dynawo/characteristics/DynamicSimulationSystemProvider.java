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
 * Adds, in one step, every extension a named system's study reads.
 * <p>
 * A study of a system needs its machines described and its automatons named, each an extension of
 * its own kind. Naming the system rather than each extension adds them all: this one says which
 * provider of each kind the system uses, so a caller has the fast way of setting a whole system up
 * and the fine way of setting one extension at a time both.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public interface DynamicSimulationSystemProvider {

    String getName();

    default String getDescription() {
        return getName();
    }

    /**
     * Adds every extension the system reads, each left untouched where the network already carries
     * it.
     */
    void createExtensions(Network network, MappingParameters parameters);
}
