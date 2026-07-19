/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.controls;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.stream.StreamSupport;

/**
 * Resolves the controls of a machine into the Dynawo model name fragment implementing them once
 * simplified, from the {@link ControlTranslation} tables it was given.
 * <p>
 * Tables are consulted whole, in decreasing priority: the first one saying anything about a
 * control answers for it, whether by an entry of its own or by its wildcard. A table that knows a
 * fleet therefore overrides a general one even where the general one is more specific, which is
 * what lets the IEEE test systems simplify every exciter to a proportional regulation while the
 * general tables keep the integral term a real machine needs.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public final class ControlTranslations {

    private static final ControlTranslations INSTANCE = new ControlTranslations(ServiceLoader.load(ControlTranslation.class));

    private final List<ControlTranslation> translations;

    /**
     * Tables of a mapping that does not translate the way the classpath says.
     * <p>
     * The contributed tables describe how a control is usually simplified, which is what a mapping
     * meant for any network wants. A mapping written for one particular system, whose reference
     * models are known, passes its own tables to
     * {@link com.powsybl.dynawo.mappings.generators.GeneratorLibResolver} instead.
     */
    public static ControlTranslations of(ControlTranslation... translations) {
        return new ControlTranslations(List.of(translations));
    }

    ControlTranslations(Iterable<ControlTranslation> translations) {
        this.translations = StreamSupport.stream(translations.spliterator(), false)
                .sorted(Comparator.comparingInt(ControlTranslation::getPriority).reversed())
                .toList();
    }

    public static ControlTranslations getInstance() {
        return INSTANCE;
    }

    public String translateGovernor(String governor) {
        return translate(ControlTranslation::getGovernorTranslations, governor);
    }

    public String translateVoltageRegulator(String voltageRegulator) {
        return translate(ControlTranslation::getVoltageRegulatorTranslations, voltageRegulator);
    }

    /**
     * A control no table speaks about is already simple enough for a voltage stability study and
     * stands for itself.
     */
    private String translate(Function<ControlTranslation, Map<String, String>> table, String control) {
        return translations.stream()
                .map(t -> {
                    Map<String, String> entries = table.apply(t);
                    return entries.getOrDefault(control, entries.get(ControlTranslation.WILDCARD));
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(control);
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
        SimplifiedControls simplified = new SimplifiedControls(translateGovernor(governor),
                translateVoltageRegulator(voltageRegulator));
        return Optional.of(translations.stream()
                .map(t -> t.getSimplifiedControlsFragments().get(simplified))
                .filter(Objects::nonNull)
                .findFirst()
                .orElseGet(() -> simplified.governor() + simplified.voltageRegulator()));
    }
}
