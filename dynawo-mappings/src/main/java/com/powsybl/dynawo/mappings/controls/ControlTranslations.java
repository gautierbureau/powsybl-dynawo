/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.controls;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * Merges every {@link ControlTranslation} found on the classpath and resolves detailed controls
 * into the Dynawo model name fragment implementing their simplified counterpart.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public final class ControlTranslations {

    private static final Logger LOGGER = LoggerFactory.getLogger(ControlTranslations.class);
    private static final ControlTranslations INSTANCE = new ControlTranslations(ServiceLoader.load(ControlTranslation.class));

    private final Map<String, String> governors = new HashMap<>();
    private final Map<String, String> voltageRegulators = new HashMap<>();
    private final Map<SimplifiedControls, String> fragments = new HashMap<>();

    ControlTranslations(Iterable<ControlTranslation> translations) {
        translations.forEach(t -> {
            merge(governors, t.getGovernorTranslations(), "governor");
            merge(voltageRegulators, t.getVoltageRegulatorTranslations(), "voltage regulator");
            merge(fragments, t.getSimplifiedControlsFragments(), "simplified controls");
        });
    }

    public static ControlTranslations getInstance() {
        return INSTANCE;
    }

    private static <K> void merge(Map<K, String> target, Map<K, String> contribution, String kind) {
        contribution.forEach((k, v) -> {
            String previous = target.putIfAbsent(k, v);
            if (previous != null && !previous.equals(v)) {
                LOGGER.warn("Conflicting {} translation for {}: {} already registered, {} ignored", kind, k, previous, v);
            }
        });
    }

    public String translateGovernor(String governor) {
        return translate(governors, governor, "governor");
    }

    public String translateVoltageRegulator(String voltageRegulator) {
        return translate(voltageRegulators, voltageRegulator, "voltage regulator");
    }

    private static String translate(Map<String, String> table, String control, String kind) {
        String translated = table.get(control);
        if (translated == null) {
            translated = table.get(ControlTranslation.WILDCARD);
            LOGGER.debug("No {} translation registered for {}, falling back on {}", kind, control, translated);
        }
        return translated;
    }

    /**
     * Returns the Dynawo model name fragment implementing the given detailed controls once
     * simplified, or an empty optional when the couple has no known simplified implementation.
     */
    public Optional<String> getSimplifiedFragment(String governor, String voltageRegulator) {
        String simplifiedGovernor = translateGovernor(governor);
        String simplifiedVoltageRegulator = translateVoltageRegulator(voltageRegulator);
        if (simplifiedGovernor == null || simplifiedVoltageRegulator == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(fragments.get(new SimplifiedControls(simplifiedGovernor, simplifiedVoltageRegulator)));
    }
}
