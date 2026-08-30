/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.parameters;

import com.powsybl.dynawo.parameters.ParametersSet;
import com.powsybl.dynawo.xml.ParametersXml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The parameters a known system's models expect, held ready rather than generated.
 * <p>
 * A model deduced from a machine's characteristics can be valued from them, but a known system's
 * models want the values the system was built with, which are not derivable and are shipped with
 * it instead. The Nordic 32 system is the first: its parameters travel as a resource, under the
 * set names this mapping gives its machines, so a model of the system finds its set already
 * written where a generated one would fall short.
 * <p>
 * A set is offered only for a model of the system it belongs to, told by the controls the model
 * carries, so the same machine name in another network is not given the wrong values.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public final class ReferenceGeneratorParameters {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceGeneratorParameters.class);

    private static final String NORDIC32_RESOURCE = "/parameters/nordic32.par";
    // the Nordic models all carry the Nordic regulator, so their names all hold this
    private static final String NORDIC = "Nordic";

    private static final ReferenceGeneratorParameters INSTANCE = load();

    private final Map<String, ParametersSet> sets;

    private ReferenceGeneratorParameters(Map<String, ParametersSet> sets) {
        this.sets = sets;
    }

    public static ReferenceGeneratorParameters getInstance() {
        return INSTANCE;
    }

    private static ReferenceGeneratorParameters load() {
        Map<String, ParametersSet> sets = new LinkedHashMap<>();
        loadResource(NORDIC32_RESOURCE, sets);
        return new ReferenceGeneratorParameters(sets);
    }

    private static void loadResource(String resource, Map<String, ParametersSet> into) {
        try (InputStream is = ReferenceGeneratorParameters.class.getResourceAsStream(resource)) {
            if (is == null) {
                LOGGER.warn("No reference parameters at {}, a model that wanted them is valued by "
                        + "generation instead", resource);
                return;
            }
            ParametersXml.load(is).forEach(set -> into.put(set.getId(), set));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * The reference set for a model under the id asked for, where one is held for it.
     * <p>
     * Held for it means the model is a known system's, told by the controls its name carries, and
     * a set of the system is named for the machine, told by the id matching. The set is copied
     * under that id, so the model finds it by the name the mapping gave it and the reference is
     * left for the next machine.
     */
    public Optional<ParametersSet> forModel(String setId, String lib) {
        if (!lib.contains(NORDIC)) {
            return Optional.empty();
        }
        ParametersSet reference = sets.get(setId);
        return reference == null ? Optional.empty() : Optional.of(new ParametersSet(setId, reference));
    }
}
