/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.parameters;

import com.powsybl.dynawo.commons.DynawoConfig;
import com.powsybl.dynawo.commons.DynawoVersion;
import com.powsybl.dynawo.desc.FilteredDescriptionXml;
import com.powsybl.dynawo.desc.ModelDescription;
import com.powsybl.dynawo.desc.ModelDescriptionHandler;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

/**
 * Gives access to the description of a Dynawo model, which lists the parameters it expects.
 * <p>
 * Generating parameter sets requires those descriptions, hence an installed Dynawo. Going through
 * this interface keeps that dependency at the edge: a mapping can be exercised against
 * descriptions coming from anywhere else.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
@FunctionalInterface
public interface ModelDescriptionLookup {

    Optional<ModelDescription> find(String lib);

    /**
     * Reads the descriptions from the model database of the installed Dynawo.
     */
    static ModelDescriptionLookup fromDynawo(DynawoConfig config, DynawoVersion version) {
        Map<String, ModelDescription> descriptions = ModelDescriptionHandler.loadFrom(config, version).getModelDescriptions();
        return lib -> Optional.ofNullable(descriptions.get(lib));
    }

    /**
     * Reads a description straight from the model database, whichever version declared the model.
     * <p>
     * The catalog states from which Dynawo version a model is usable, and an installation can hold
     * models it does not claim to support: a build reporting 1.7.0 ships the description of a model
     * the catalog dates from 1.8.0. Going by the files present describes what the installation can
     * actually run.
     */
    static ModelDescriptionLookup fromModelDatabase(Path homeDir) {
        Path db = homeDir.resolve("ddb");
        return lib -> {
            Path descFile = db.resolve(lib + ".desc.xml");
            return Files.exists(descFile) ? Optional.of(FilteredDescriptionXml.load(descFile)) : Optional.empty();
        };
    }

    /**
     * Reads the descriptions from an already loaded map, keyed by library name.
     */
    static ModelDescriptionLookup from(Map<String, ModelDescription> descriptions) {
        return lib -> Optional.ofNullable(descriptions.get(lib));
    }
}
