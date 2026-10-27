/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.preassembled;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Stream;

/**
 * Every control we can put on a machine, under the name a model is known by.
 * <p>
 * A model is named after the controls it carries, {@code GeneratorSynchronousFourWindingsGovCt2St4b}
 * being a machine with the {@code GovCt2} governor and the {@code St4b} regulator, and an
 * extension says which controls a generator has in those same terms. So a control is looked up
 * here by the last part of the Modelica model it stands for, which is the part a name is built
 * from.
 * <p>
 * Nothing is looked up by position or by guessing where one name ends and the next begins: the
 * extension says which control plays which part, and each is asked for separately.
 * <p>
 * Besides the controls declared here and those a {@link ControlUnitProvider} brings, a study may
 * {@link #register} controls of its own at run time, for a machine whose extension names a control
 * nothing ships a description of. The catalog is a JVM-wide singleton in a process that may run
 * study after study, so such a study starts from {@link #resetToBase}, as it does for the model
 * catalog, rather than from whatever an earlier study left behind.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public final class ControlUnitCatalog {

    private static final Logger LOGGER = LoggerFactory.getLogger(ControlUnitCatalog.class);

    private static final ControlUnitCatalog INSTANCE = new ControlUnitCatalog();

    private final Map<String, MachineControlUnit> governors = new LinkedHashMap<>();
    private final Map<String, MachineControlUnit> voltageRegulators = new LinkedHashMap<>();
    private final Map<String, RegulatorControlUnit> regulatorControls = new LinkedHashMap<>();
    // the catalog as the declarations and the providers leave it, before any study adds to it
    private final Map<String, MachineControlUnit> baseGovernors;
    private final Map<String, MachineControlUnit> baseVoltageRegulators;
    private final Map<String, RegulatorControlUnit> baseRegulatorControls;

    private ControlUnitCatalog() {
        declare(GovernorUnits.class, MachineControlUnit.class, governors);
        declare(VoltageRegulatorUnits.class, MachineControlUnit.class, voltageRegulators);
        declare(RegulatorControlUnits.class, RegulatorControlUnit.class, regulatorControls);
        // the units a deployment brings of its own, the RTE controls among them, added over the open
        // ones: a deployment's unit stands in for the open default of the same name, so a control the
        // deployment models its own way, RTE's Astre simplified regulations for one, is the one built
        ServiceLoader.load(ControlUnitProvider.class).forEach(this::declare);
        baseGovernors = new LinkedHashMap<>(governors);
        baseVoltageRegulators = new LinkedHashMap<>(voltageRegulators);
        baseRegulatorControls = new LinkedHashMap<>(regulatorControls);
    }

    private void declare(ControlUnitProvider provider) {
        provider.getGovernors().forEach(unit -> governors.put(nameOf(unit), unit));
        provider.getVoltageRegulators().forEach(unit -> voltageRegulators.put(nameOf(unit), unit));
        provider.getRegulatorControls().forEach(unit -> regulatorControls.put(nameOf(unit), unit));
    }

    public static ControlUnitCatalog getInstance() {
        return INSTANCE;
    }

    /**
     * A catalog of its own holding what the declarations and the providers bring, and nothing any
     * study registered in the shared one: what ships, to be listed or designed from apart from
     * whichever study last ran in the process.
     */
    public static ControlUnitCatalog shipped() {
        return new ControlUnitCatalog();
    }

    /**
     * Adds the controls a study describes of its own, beside those already known. A name already
     * known keeps the control it has, as {@link com.powsybl.dynawo.builders.ModelConfigsHandler#addModels}
     * keeps a model already there: a study adds controls, and redefining one is {@link #override}.
     */
    public void register(ControlUnitProvider additions) {
        merge(additions, false);
    }

    /**
     * Puts in the controls a study describes its own way, a name already known standing aside for
     * the one brought in. For a study that means to wire a known control differently, not to add one.
     */
    public void override(ControlUnitProvider replacements) {
        merge(replacements, true);
    }

    /**
     * Drops every control registered at run time, putting the catalog back as the declarations and
     * the providers left it.
     */
    public void resetToBase() {
        restore(governors, baseGovernors);
        restore(voltageRegulators, baseVoltageRegulators);
        restore(regulatorControls, baseRegulatorControls);
    }

    private void merge(ControlUnitProvider provider, boolean override) {
        Objects.requireNonNull(provider);
        merge(provider.getGovernors(), governors, "governor", override);
        merge(provider.getVoltageRegulators(), voltageRegulators, "voltage regulator", override);
        merge(provider.getRegulatorControls(), regulatorControls, "regulator control", override);
    }

    private static <T extends UnitModel> void merge(List<T> units, Map<String, T> into, String kind,
                                                    boolean override) {
        units.forEach(unit -> {
            String name = nameOf(Objects.requireNonNull(unit));
            if (override) {
                into.put(name, unit);
            } else if (into.putIfAbsent(name, unit) != null) {
                LOGGER.warn("The {} {} is already known, the one already there is kept", kind, name);
            }
        });
    }

    private static <T> void restore(Map<String, T> current, Map<String, T> base) {
        current.clear();
        current.putAll(base);
    }

    /**
     * Every governor known, keyed by the name a model carries.
     */
    public Map<String, MachineControlUnit> getGovernors() {
        return Collections.unmodifiableMap(governors);
    }

    /**
     * Every voltage regulator known, keyed by the name a model carries.
     */
    public Map<String, MachineControlUnit> getVoltageRegulators() {
        return Collections.unmodifiableMap(voltageRegulators);
    }

    /**
     * Every stabiliser, limiter and reactive power control loop known, keyed by the name a model
     * carries.
     */
    public Map<String, RegulatorControlUnit> getRegulatorControls() {
        return Collections.unmodifiableMap(regulatorControls);
    }

    /**
     * The governor of that name, {@code GovCt2} and the like.
     */
    public Optional<MachineControlUnit> getGovernor(String name) {
        return Optional.ofNullable(governors.get(name));
    }

    /**
     * The voltage regulator of that name, {@code St4b} and the like.
     */
    public Optional<MachineControlUnit> getVoltageRegulator(String name) {
        return Optional.ofNullable(voltageRegulators.get(name));
    }

    /**
     * The stabiliser or limiter of that name, {@code Pss2b} and the like.
     */
    public Optional<RegulatorControlUnit> getRegulatorControl(String name) {
        return Optional.ofNullable(regulatorControls.get(name));
    }

    /**
     * The name a model built with this control carries, which is the {@link
     * ControlUnit#getCatalogName() name the control goes by} where it is one, its Modelica model's
     * last part otherwise, the two being the same unless the control stands for a model under a name
     * of its own.
     */
    static String nameOf(UnitModel unit) {
        if (unit instanceof ControlUnit control) {
            return control.getCatalogName();
        }
        if (unit instanceof RegulatorControlUnit control) {
            return control.getCatalogName();
        }
        String model = unit.getName();
        return model.substring(model.lastIndexOf('.') + 1);
    }

    private static <T> void declare(Class<?> declaring, Class<T> kind, Map<String, T> into) {
        Stream.of(declaring.getDeclaredMethods())
                .filter(factory -> factory.getReturnType() == kind)
                .map(factory -> kind.cast(invoke(factory)))
                // a control wired differently on a three winding machine is declared apart under
                // the same model, so the first declared stands for the name and the other is
                // reached through the assembly that knows how many windings it has
                .forEach(control -> into.putIfAbsent(nameOf((UnitModel) control), control));
        if (into.isEmpty()) {
            // the controls are read off the class that declares them, which a native image strips
            // unless it is told to keep them: without the reflect-config beside this class the
            // catalog comes up empty and every machine is quietly answered with the nearest
            // installed model instead of the one built for it, which is a whole study running on
            // models nobody chose. So it is said here rather than found out from the results
            throw new IllegalStateException("No control was read from " + declaring.getName()
                    + ". Where this is a native image, the class has to be registered for "
                    + "reflection, see META-INF/native-image in this module");
        }
    }

    private static Object invoke(Method factory) {
        try {
            return factory.invoke(null);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Could not read the declared controls", e);
        }
    }
}
