/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.preassembled;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
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
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public final class ControlUnitCatalog {

    private static final ControlUnitCatalog INSTANCE = new ControlUnitCatalog();

    private final Map<String, MachineControlUnit> governors = new LinkedHashMap<>();
    private final Map<String, MachineControlUnit> voltageRegulators = new LinkedHashMap<>();
    private final Map<String, RegulatorControlUnit> regulatorControls = new LinkedHashMap<>();

    private ControlUnitCatalog() {
        declare(GovernorUnits.class, MachineControlUnit.class, governors);
        declare(VoltageRegulatorUnits.class, MachineControlUnit.class, voltageRegulators);
        declare(RegulatorControlUnits.class, RegulatorControlUnit.class, regulatorControls);
    }

    public static ControlUnitCatalog getInstance() {
        return INSTANCE;
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
     * The name a model built with this control carries, which is the last part of the Modelica
     * model it stands for.
     */
    static String nameOf(UnitModel unit) {
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
    }

    private static Object invoke(Method factory) {
        try {
            return factory.invoke(null);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Could not read the declared controls", e);
        }
    }
}
