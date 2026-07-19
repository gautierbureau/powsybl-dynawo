/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.preassembled;

import com.powsybl.dynawo.extensions.api.generator.SynchronousGeneratorProperties.Windings;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks every control against the definitions Dynawo ships: what each one says it wires to the
 * machine has to be what the models using it hold.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
class ControlUnitsCorpusTest {

    private static final Path CORPUS = Path.of("..", "..", "dynawo-source", "dynawo", "sources", "Models",
            "Modelica", "PreassembledModels");

    private static final Set<String> ATTACHED = Set.of("powerSystemStabilizer", "pss",
            "overExcitationLimiter", "underExcitationLimiter", "statorCurrentLimiter");

    private static final Pattern UNIT = Pattern.compile(
            "<dyn:unitDynamicModel\\s+id=\"([^\"]+)\"\\s+name=\"([^\"]+)\"(?:\\s+initName\\s*=\"([^\"]*)\")?");
    private static final Pattern CONNECTION = Pattern.compile(
            "<dyn:(initConnect|connect)\\s+id1=\"([^\"]+)\"\\s+var1=\"([^\"]+)\"\\s+id2=\"([^\"]+)\"\\s+var2=\"([^\"]+)\"");

    @Test
    void shouldWireEveryControlTheWayTheShippedModelsDo() throws IOException {
        Assumptions.assumeTrue(Files.exists(CORPUS), "no model definitions available at " + CORPUS);
        Map<String, MachineControlUnit> controls = declaredControls();
        assertThat(controls).hasSizeGreaterThan(30);

        List<String> disagreements = new ArrayList<>();
        List<String> undeclared = new ArrayList<>();
        try (Stream<Path> definitions = Files.list(CORPUS)) {
            for (Path definition : definitions.filter(p -> p.getFileName().toString().startsWith("GeneratorSynchronous"))
                    .toList()) {
                String shipped = Files.readString(definition);
                boolean threeWindings = definition.getFileName().toString().contains("ThreeWindings");
                MachineUnit machine = new GeneratorSynchronousUnit(
                        threeWindings ? com.powsybl.dynawo.extensions.api.generator.SynchronousGeneratorProperties.Windings.THREE_WINDINGS
                                : com.powsybl.dynawo.extensions.api.generator.SynchronousGeneratorProperties.Windings.FOUR_WINDINGS,
                        false);
                for (Map.Entry<String, String> unit : machineControls(shipped).entrySet()) {
                    // a control wired differently on a three winding machine is declared apart,
                    // the others stand for both
                    MachineControlUnit control = controls.getOrDefault(
                            key(unit.getKey(), unit.getValue(), threeWindings),
                            controls.get(key(unit.getKey(), unit.getValue(), false)));
                    if (control == null) {
                        undeclared.add(definition.getFileName() + " " + unit.getValue());
                        continue;
                    }
                    List<UnitConnection> declared = new ArrayList<>(control.getConnectionsWith(machine));
                    declared.addAll(control.getInitConnectionsWith(machine));
                    List<UnitConnection> held = connectionsBetween(shipped, unit.getKey());
                    if (!declared.containsAll(held) || !held.containsAll(declared)) {
                        disagreements.add(definition.getFileName() + " " + unit.getKey());
                    }
                }
            }
        }
        // nothing skipped, so that a control the corpus holds and this does not cannot pass for
        // agreement, which is how a lookup that quietly matches nothing would read
        assertThat(undeclared).isEmpty();
        assertThat(disagreements).isEmpty();
    }

    @Test
    void shouldWireEveryStabiliserAndLimiterTheWayTheShippedModelsDo() throws IOException {
        Assumptions.assumeTrue(Files.exists(CORPUS), "no model definitions available at " + CORPUS);
        Map<String, MachineControlUnit> regulators = declaredControls();
        Map<String, RegulatorControlUnit> attached = declaredRegulatorControls();

        List<String> disagreements = new ArrayList<>();
        List<String> undeclared = new ArrayList<>();
        try (Stream<Path> definitions = Files.list(CORPUS)) {
            for (Path definition : definitions.filter(p -> p.getFileName().toString().startsWith("GeneratorSynchronous"))
                    .toList()) {
                String shipped = Files.readString(definition);
                boolean threeWindings = definition.getFileName().toString().contains("ThreeWindings");
                MachineUnit machine = machine(threeWindings);
                Map<String, String> units = unitsOf(shipped);
                String regulatorModel = units.get("voltageRegulator");
                for (Map.Entry<String, String> unit : units.entrySet()) {
                    if (!ATTACHED.contains(unit.getKey())) {
                        continue;
                    }
                    RegulatorControlUnit control = attached.get(key(unit.getKey(), unit.getValue(), false));
                    MachineControlUnit regulator = regulators.getOrDefault(
                            key("voltageRegulator", regulatorModel, threeWindings),
                            regulators.get(key("voltageRegulator", regulatorModel, false)));
                    if (control == null || regulator == null) {
                        undeclared.add(definition.getFileName() + " " + unit.getValue());
                        continue;
                    }
                    List<UnitConnection> declared = control.getConnectionsWith(machine, regulator);
                    List<UnitConnection> held = connectionsOf(shipped, unit.getKey());
                    if (!declared.containsAll(held) || !held.containsAll(declared)) {
                        disagreements.add(definition.getFileName() + " " + unit.getKey());
                    }
                }
            }
        }
        assertThat(undeclared).isEmpty();
        assertThat(disagreements).isEmpty();
    }

    private static Map<String, RegulatorControlUnit> declaredRegulatorControls() {
        Map<String, RegulatorControlUnit> controls = new LinkedHashMap<>();
        for (Method factory : RegulatorControlUnits.class.getDeclaredMethods()) {
            if (factory.getReturnType() != RegulatorControlUnit.class) {
                continue;
            }
            try {
                RegulatorControlUnit control = (RegulatorControlUnit) factory.invoke(null);
                controls.put(key(control.getId(), control.getName(), false), control);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new AssertionError(e);
            }
        }
        return controls;
    }

    private static MachineUnit machine(boolean threeWindings) {
        return new GeneratorSynchronousUnit(threeWindings ? Windings.THREE_WINDINGS : Windings.FOUR_WINDINGS, false);
    }

    /**
     * Every unit of a definition, by the name the assembly gives it.
     */
    private static Map<String, String> unitsOf(String definition) {
        Map<String, String> units = new LinkedHashMap<>();
        Matcher matcher = UNIT.matcher(definition);
        while (matcher.find()) {
            units.put(matcher.group(1), matcher.group(2));
        }
        return units;
    }

    /**
     * Every connection a unit takes part in, whichever way round it was written.
     */
    private static List<UnitConnection> connectionsOf(String definition, String role) {
        return CONNECTION.matcher(definition).results()
                .filter(result -> List.of(result.group(2), result.group(4)).contains(role))
                .map(result -> new UnitConnection(result.group(2), result.group(3), result.group(4), result.group(5),
                        "initConnect".equals(result.group(1))))
                .toList();
    }

    /**
     * Every control declared, under the model it stands for and the machine it is wired to when
     * that makes a difference.
     */
    private static Map<String, MachineControlUnit> declaredControls() {
        Map<String, MachineControlUnit> controls = new LinkedHashMap<>();
        Stream.of(VoltageRegulatorUnits.class, GovernorUnits.class).forEach(declaring -> {
            for (Method factory : declaring.getDeclaredMethods()) {
                if (factory.getReturnType() != MachineControlUnit.class) {
                    continue;
                }
                MachineControlUnit control = invoke(factory);
                controls.put(key(control.getId(), control.getName(),
                        factory.getName().endsWith("ThreeWindings")), control);
            }
        });
        return controls;
    }

    private static MachineControlUnit invoke(Method factory) {
        try {
            return (MachineControlUnit) factory.invoke(null);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new AssertionError(e);
        }
    }

    private static String key(String role, String model, boolean threeWindings) {
        return role + " " + model + (threeWindings ? " 3W" : "");
    }

    /**
     * The controls a definition wires straight to the machine, by the name the assembly gives them.
     */
    private static Map<String, String> machineControls(String definition) {
        Map<String, String> controls = new LinkedHashMap<>();
        Matcher matcher = UNIT.matcher(definition);
        while (matcher.find()) {
            if (matcher.group(1).equals("voltageRegulator") || matcher.group(1).equals("governor")) {
                controls.put(matcher.group(1), matcher.group(2));
            }
        }
        return controls;
    }

    private static List<UnitConnection> connectionsBetween(String definition, String role) {
        return CONNECTION.matcher(definition).results()
                .filter(result -> List.of(result.group(2), result.group(4)).contains(role)
                        && List.of(result.group(2), result.group(4)).contains("generator"))
                .map(result -> new UnitConnection(result.group(2), result.group(3), result.group(4), result.group(5),
                        "initConnect".equals(result.group(1))))
                .toList();
    }
}
