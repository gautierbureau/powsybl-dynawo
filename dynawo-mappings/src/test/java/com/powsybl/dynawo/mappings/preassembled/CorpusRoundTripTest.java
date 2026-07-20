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
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Rebuilds every model Dynawo ships from the elements it is made of, and checks it comes out the
 * same. Reproducing what already exists is what says the description holds before anything new is
 * assembled from it.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
class CorpusRoundTripTest {

    private static final Path CORPUS = Path.of("..", "..", "dynawo-source", "dynawo", "sources", "Models",
            "Modelica", "PreassembledModels");

    private static final Pattern UNIT = Pattern.compile(
            "<dyn:unitDynamicModel\\s+id=\"([^\"]+)\"\\s+name=\"([^\"]+)\"(?:\\s+initName\\s*=\"([^\"]*)\")?");
    private static final Pattern CONNECTION = Pattern.compile(
            "<dyn:(initConnect|connect)\\s+id1=\"([^\"]+)\"\\s+var1=\"([^\"]+)\"\\s+id2=\"([^\"]+)\"\\s+var2=\"([^\"]+)\"");

    private static final Set<String> REGULATOR_ATTACHED = Set.of("powerSystemStabilizer", "pss",
            "overExcitationLimiter", "underExcitationLimiter", "statorCurrentLimiter");

    /**
     * The models Dynawo ships with a transformer they do not initialise the machine through. None
     * of them is used by any test case, which is how it went unnoticed.
     */
    private static final Set<String> MIS_INITIALISED_UPSTREAM = Set.of(
            "GeneratorSynchronousFourWindingsPmConstVRNordicTfo",
            "GeneratorSynchronousThreeWindingsGoverNordicVRNordicTfo",
            "GeneratorSynchronousThreeWindingsHyGovScrxTfo",
            "GeneratorSynchronousThreeWindingsIeeeG1IeeeT1Tfo",
            "GeneratorSynchronousThreeWindingsIeeeG1ScrxTfo",
            "GeneratorSynchronousThreeWindingsPmConstExAc1Tfo",
            "GeneratorSynchronousThreeWindingsPmConstScrxTfo",
            "GeneratorSynchronousThreeWindingsPmConstVRNordicTfo");

    @Test
    void shouldRebuildEveryModelDynawoShips() throws IOException {
        Assumptions.assumeTrue(Files.exists(CORPUS), "no model definitions available at " + CORPUS);
        Map<String, MachineControlUnit> machineControls = declared(VoltageRegulatorUnits.class, GovernorUnits.class);
        Map<String, RegulatorControlUnit> regulatorControls = declaredRegulatorControls();

        Map<String, String> different = new TreeMap<>();
        List<String> rebuilt = new ArrayList<>();
        try (Stream<Path> definitions = Files.list(CORPUS)) {
            for (Path definition : definitions.filter(p -> p.getFileName().toString().startsWith("GeneratorSynchronous"))
                    .sorted().toList()) {
                String name = definition.getFileName().toString().replace(".xml", "");
                String shipped = Files.readString(definition);
                Map<String, String> units = unitsOf(shipped);
                Map<String, String> inits = initsOf(shipped);

                String machineInit = String.valueOf(inits.get("generator"));
                GeneratorAssembly assembly = new GeneratorAssembly(
                        machineInit.contains("3W") ? Windings.THREE_WINDINGS : Windings.FOUR_WINDINGS,
                        units.containsKey("transformer"), units.containsKey("auxLV"));
                List<String> notDeclared = new ArrayList<>();
                for (Map.Entry<String, String> unit : units.entrySet()) {
                    String role = unit.getKey();
                    if ("voltageRegulator".equals(role) || "governor".equals(role)) {
                        // a control wired differently on a three winding machine is declared
                        // apart, the others stand for both
                        MachineControlUnit control = machineControls.getOrDefault(
                                key(role, unit.getValue(), String.valueOf(inits.get("generator")).contains("3W")),
                                machineControls.get(key(role, unit.getValue(), false)));
                        if (control == null) {
                            notDeclared.add(role + " " + unit.getValue());
                        } else {
                            assembly.add(control);
                        }
                    } else if (REGULATOR_ATTACHED.contains(role)) {
                        if (!regulatorControls.containsKey(key(role, unit.getValue(), false))) {
                            notDeclared.add(role + " " + unit.getValue());
                        }
                    }
                }
                if (!notDeclared.isEmpty()) {
                    different.put(name, "not declared: " + notDeclared);
                    continue;
                }
                rebuilt.add(name);
                String built = PreassembledModelXml.toXml(assembly.build(name));
                String difference = difference(built, shipped, units.keySet());
                if (difference != null) {
                    different.put(name, difference);
                }
            }
        }
        // Everything the rebuilt model does not match, named so that nothing can join it
        // unnoticed. Two families are not described here yet: a machine holding its parameters
        // internally, and one driving its own speed reference. The eight others are the ones
        // Dynawo ships wrong, and we build them the way every other transformer connected model
        // is written, which is to say deliberately unlike the reference until it is corrected.
        assertThat(different.keySet()).containsExactlyInAnyOrder(
                "GeneratorSynchronousProportionalRegulationsInternalParameters",
                "GeneratorSynchronousFourWindingsTGov1Sexs",
                "GeneratorSynchronousFourWindingsPmConstVRNordicTfo",
                "GeneratorSynchronousThreeWindingsGoverNordicVRNordicTfo",
                "GeneratorSynchronousThreeWindingsHyGovScrxTfo",
                "GeneratorSynchronousThreeWindingsIeeeG1IeeeT1Tfo",
                "GeneratorSynchronousThreeWindingsIeeeG1ScrxTfo",
                "GeneratorSynchronousThreeWindingsPmConstExAc1Tfo",
                "GeneratorSynchronousThreeWindingsPmConstScrxTfo",
                "GeneratorSynchronousThreeWindingsPmConstVRNordicTfo");
        // and each of those eight differs by nothing but the four connections carrying the
        // operating point across the transformer, so that a real regression cannot hide among them
        MIS_INITIALISED_UPSTREAM.forEach(name -> assertThat(different.get(name))
                .as(name)
                .startsWith("missing [] extra [")
                .contains("var1=P0Pu, id2=transformer, var2=P20Pu")
                .contains("var1=Q0Pu, id2=transformer, var2=Q20Pu")
                .contains("var1=U0Pu, id2=transformer, var2=U20Pu")
                .contains("var1=UPhase0, id2=transformer, var2=U2Phase0"));
        assertThat(rebuilt).hasSameSizeAs(models());
    }

    /**
     * Every model of the corpus, so that rebuilding all but a few cannot pass for rebuilding them
     * all.
     */
    private static List<Path> models() throws IOException {
        try (Stream<Path> definitions = Files.list(CORPUS)) {
            return definitions.filter(p -> p.getFileName().toString().startsWith("GeneratorSynchronous"))
                    .sorted().toList();
        }
    }

    /**
     * What the rebuilt model says that the shipped one does not, or the other way round, looking
     * only at the elements both hold: the stabilisers and limiters are declared but not yet placed
     * in an assembly.
     */
    private static String difference(String built, String shipped, Set<String> roles) {
        Set<String> compared = Set.of("generator", "voltageRegulator", "governor", "transformer", "coupling",
                "auxLV", "auxHV");
        List<UnitConnection> builtConnections = connectionsOf(built, compared);
        List<UnitConnection> shippedConnections = connectionsOf(shipped, compared);
        List<UnitConnection> missing = new ArrayList<>(shippedConnections);
        missing.removeAll(builtConnections);
        List<UnitConnection> extra = new ArrayList<>(builtConnections);
        extra.removeAll(shippedConnections);
        if (!missing.isEmpty() || !extra.isEmpty()) {
            return "missing " + missing + " extra " + extra;
        }
        Map<String, String> builtUnits = new LinkedHashMap<>(unitsOf(built));
        builtUnits.keySet().retainAll(compared);
        Map<String, String> shippedUnits = new LinkedHashMap<>(unitsOf(shipped));
        shippedUnits.keySet().retainAll(compared);
        if (!builtUnits.equals(shippedUnits)) {
            return "units " + builtUnits + " against " + shippedUnits;
        }
        Map<String, String> builtInits = new LinkedHashMap<>(initsOf(built));
        builtInits.keySet().retainAll(compared);
        Map<String, String> shippedInits = new LinkedHashMap<>(initsOf(shipped));
        shippedInits.keySet().retainAll(compared);
        return builtInits.equals(shippedInits) ? null : "initialisation " + builtInits + " against " + shippedInits;
    }

    private static Map<String, MachineControlUnit> declared(Class<?>... declaring) {
        Map<String, MachineControlUnit> controls = new LinkedHashMap<>();
        for (Class<?> type : declaring) {
            for (Method factory : type.getDeclaredMethods()) {
                if (factory.getReturnType() == MachineControlUnit.class) {
                    MachineControlUnit control = (MachineControlUnit) invoke(factory);
                    controls.put(key(control.getId(), control.getName(), factory.getName().endsWith("ThreeWindings")),
                            control);
                    controls.putIfAbsent(key(control.getId(), control.getName(), false), control);
                }
            }
        }
        return controls;
    }

    private static Map<String, RegulatorControlUnit> declaredRegulatorControls() {
        Map<String, RegulatorControlUnit> controls = new LinkedHashMap<>();
        for (Method factory : RegulatorControlUnits.class.getDeclaredMethods()) {
            if (factory.getReturnType() == RegulatorControlUnit.class) {
                RegulatorControlUnit control = (RegulatorControlUnit) invoke(factory);
                controls.put(key(control.getId(), control.getName(), false), control);
            }
        }
        return controls;
    }

    private static Object invoke(Method factory) {
        try {
            return factory.invoke(null);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new AssertionError(e);
        }
    }

    private static String key(String role, String model, boolean threeWindings) {
        return role + " " + model + (threeWindings ? " 3W" : "");
    }

    private static Map<String, String> unitsOf(String definition) {
        Map<String, String> units = new LinkedHashMap<>();
        Matcher matcher = UNIT.matcher(definition);
        while (matcher.find()) {
            units.put(matcher.group(1), matcher.group(2));
        }
        return units;
    }

    private static Map<String, String> initsOf(String definition) {
        Map<String, String> inits = new LinkedHashMap<>();
        Matcher matcher = UNIT.matcher(definition);
        while (matcher.find()) {
            inits.put(matcher.group(1), String.valueOf(matcher.group(3)));
        }
        return inits;
    }

    private static List<UnitConnection> connectionsOf(String definition, Set<String> roles) {
        return CONNECTION.matcher(definition).results()
                .filter(result -> roles.contains(result.group(2)) && roles.contains(result.group(4)))
                .map(result -> new UnitConnection(result.group(2), result.group(3), result.group(4), result.group(5),
                        "initConnect".equals(result.group(1))))
                .toList();
    }
}
