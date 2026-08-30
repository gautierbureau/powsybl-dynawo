/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings;

import com.powsybl.commons.PowsyblException;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * The extra settings a mapping is created with, held as text.
 * <p>
 * Text is all a caller naming them loosely hands over: a python keyword argument, a configuration
 * entry, whatever a {@link DynamicMappingProvider} is called through. It is also all Dynawo reads,
 * so nothing is lost keeping them so and letting each provider read out what it knows.
 * <p>
 * A setting not named is answered with the default the provider gives, so a mapping created with
 * nothing still stands. A setting named as something it is not, a number that will not parse, is
 * refused here rather than carried into a study as a quiet default.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public final class MappingParameters {

    private static final MappingParameters EMPTY = new MappingParameters(Map.of());

    private final Map<String, String> values;

    private MappingParameters(Map<String, String> values) {
        this.values = Map.copyOf(values);
    }

    /**
     * What a mapping created with nothing reads: every setting left to its default.
     */
    public static MappingParameters empty() {
        return EMPTY;
    }

    public static MappingParameters of(Map<String, String> values) {
        return values.isEmpty() ? EMPTY : new MappingParameters(values);
    }

    public Optional<String> getString(String name) {
        return Optional.ofNullable(values.get(name));
    }

    public OptionalDouble getDouble(String name) {
        String value = values.get(name);
        if (value == null) {
            return OptionalDouble.empty();
        }
        try {
            return OptionalDouble.of(Double.parseDouble(value.trim()));
        } catch (NumberFormatException e) {
            throw new PowsyblException("Mapping parameter '" + name + "' is not a number: '" + value + "'");
        }
    }

    public double getDouble(String name, double defaultValue) {
        return getDouble(name).orElse(defaultValue);
    }

    public Optional<Boolean> getBoolean(String name) {
        String value = values.get(name);
        return value == null ? Optional.empty() : Optional.of(Boolean.parseBoolean(value.trim()));
    }

    public boolean getBoolean(String name, boolean defaultValue) {
        return getBoolean(name).orElse(defaultValue);
    }

    /**
     * The settings as they were given, for a provider reading them by some rule of its own.
     */
    public Map<String, String> asMap() {
        return values;
    }
}
