/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.characteristics;

import com.powsybl.dynawo.mappings.DynamicMappingExtensionsProvider;
import com.powsybl.dynawo.mappings.MappingParameters;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;

/**
 * Creates the {@code synchronousGeneratorProperties} extensions a mapping relies on.
 * <p>
 * Implementations are discovered with a {@link java.util.ServiceLoader} and selected by name,
 * the way a mapping is, and reached from a study as its own step so the controls can be set and
 * looked at before a model is chosen for them. One derives the controls from the IIDM
 * characteristics of each machine; another holds the controls of a known system by name; another
 * may read them from a control database. All write the very same extension, only the control
 * names differ, so nothing downstream changes.
 * <p>
 * This is the {@code synchronousGeneratorProperties} kind of {@link DynamicMappingExtensionsProvider},
 * so it is reached through the same door as any other extension, by naming the extension and the
 * provider, on top of the method of its own the public API offers for it.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public interface SynchronousGeneratorPropertiesProvider extends DynamicMappingExtensionsProvider {

    /** The kind a caller asks these providers for through the extension registry. */
    String EXTENSION_NAME = "synchronousGeneratorProperties";

    @Override
    default String getExtensionName() {
        return EXTENSION_NAME;
    }

    @Override
    String getName();

    /**
     * One line saying what this provider describes, shown where the providers are listed so a
     * study can tell them apart.
     */
    default String getDescription() {
        return getName();
    }

    /**
     * The provider set up with the given settings, for one selected by name and given a study's
     * own values. A provider reading a setting, the voltage a machine is taken to sit behind a
     * transformer from, returns one holding it; a provider whose controls are fixed returns
     * itself.
     */
    @Override
    default SynchronousGeneratorPropertiesProvider configured(MappingParameters parameters) {
        return this;
    }

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
