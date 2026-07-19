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

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.StreamSupport;

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
        // highest priority first, so that a contribution knowing a particular fleet wins over the
        // general tables
        StreamSupport.stream(translations.spliterator(), false)
                .sorted(Comparator.comparingInt(ControlTranslation::getPriority).reversed())
                .forEach(t -> {
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
                LOGGER.debug("{} translation of {} to {} ignored, {} was registered with a higher priority",
                        kind, k, v, previous);
            }
        });
    }

    public String translateGovernor(String governor) {
        return translate(governors, governor, "governor");
    }

    public String translateVoltageRegulator(String voltageRegulator) {
        return translate(voltageRegulators, voltageRegulator, "voltage regulator");
    }

    /**
     * A control with no registered translation is already simple enough for a voltage stability
     * study and stands for itself, unless a contribution declared a wildcard.
     */
    private static String translate(Map<String, String> table, String control, String kind) {
        String translated = table.get(control);
        if (translated == null) {
            translated = table.getOrDefault(ControlTranslation.WILDCARD, control);
            LOGGER.debug("No {} translation registered for {}, {} used", kind, control, translated);
        }
        return translated;
    }

    /**
     * Returns the Dynawo model name fragment implementing the given controls once simplified.
     * <p>
     * The simplified regulations have names of their own rather than the concatenation of the two
     * controls, a proportional governor with a proportional regulator being named
     * {@code ProportionalRegulations}. Any other couple names itself, the way the detailed models
     * do.
     */
    public Optional<String> getSimplifiedFragment(String governor, String voltageRegulator) {
        String simplifiedGovernor = translateGovernor(governor);
        String simplifiedVoltageRegulator = translateVoltageRegulator(voltageRegulator);
        if (simplifiedGovernor == null || simplifiedVoltageRegulator == null) {
            return Optional.empty();
        }
        return Optional.of(fragments.getOrDefault(new SimplifiedControls(simplifiedGovernor, simplifiedVoltageRegulator),
                simplifiedGovernor + simplifiedVoltageRegulator));
    }
}
