/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.parameters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * Merges every {@link GeneratorParameterDefaults} found on the classpath.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public final class GeneratorParameterDefaultsRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeneratorParameterDefaultsRegistry.class);
    private static final GeneratorParameterDefaultsRegistry INSTANCE =
            new GeneratorParameterDefaultsRegistry(ServiceLoader.load(GeneratorParameterDefaults.class));

    private final Map<String, String> references = new HashMap<>();
    private final Map<String, String> lowVoltageReferences = new HashMap<>();
    private final Map<String, String> highVoltageReferences = new HashMap<>();
    private final Map<String, String> values = new HashMap<>();
    private final Map<String, Map<String, String>> controlValues = new HashMap<>();

    GeneratorParameterDefaultsRegistry(Iterable<GeneratorParameterDefaults> defaults) {
        defaults.forEach(d -> {
            merge(references, d.getReferences(), "reference");
            merge(lowVoltageReferences, d.getLowVoltageReferences(), "low voltage reference");
            merge(highVoltageReferences, d.getHighVoltageReferences(), "high voltage reference");
            merge(values, d.getValues(), "value");
            d.getControlValues().forEach((control, controlValue) ->
                    merge(controlValues.computeIfAbsent(control, k -> new HashMap<>()), controlValue, control + " value"));
        });
    }

    public static GeneratorParameterDefaultsRegistry getInstance() {
        return INSTANCE;
    }

    private static void merge(Map<String, String> target, Map<String, String> contribution, String kind) {
        contribution.forEach((k, v) -> {
            String previous = target.putIfAbsent(k, v);
            if (previous != null && !previous.equals(v)) {
                LOGGER.warn("Conflicting {} for {}: {} already registered, {} ignored", kind, k, previous, v);
            }
        });
    }

    /**
     * Returns the IIDM name the given parameter refers to, if it is read from the network.
     *
     * @param highVoltage whether the generator is connected through a transformer, which changes
     *                    the voltages the model refers to
     */
    public Optional<String> getReference(String parameterName, boolean highVoltage) {
        Map<String, String> voltageReferences = highVoltage ? highVoltageReferences : lowVoltageReferences;
        return Optional.ofNullable(references.getOrDefault(parameterName, voltageReferences.get(parameterName)));
    }

    /**
     * Returns the value of the given parameter, looking first at the values specific to the
     * controls of the model, then at the shared ones.
     */
    public Optional<String> getValue(String parameterName, String modelName) {
        return controlValues.entrySet().stream()
                .filter(e -> modelName.contains(e.getKey()))
                .map(e -> e.getValue().get(parameterName))
                .filter(Objects::nonNull)
                .findFirst()
                .or(() -> Optional.ofNullable(values.get(parameterName)));
    }
}
