/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.characteristics;

import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;

/**
 * Creates the {@code synchronousGeneratorProperties} extensions a mapping relies on.
 * <p>
 * Implementations are discovered with a {@link java.util.ServiceLoader} and selected by name.
 * The open source implementation derives the controls from the IIDM characteristics of each
 * generator; another implementation may read them from a control database instead. Both write
 * the very same extension, only the control names differ, so nothing downstream changes.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public interface SynchronousGeneratorPropertiesProvider {

    String getName();

    /**
     * Creates the extension of every generator it can describe. Generators already carrying the
     * extension must be left untouched, so that properties coming from the network file or from
     * an earlier call always win.
     */
    void createExtensions(Network network);

    /**
     * Tells whether the given generator is meant to be modelled dynamically.
     */
    boolean isEligible(Generator generator);
}
