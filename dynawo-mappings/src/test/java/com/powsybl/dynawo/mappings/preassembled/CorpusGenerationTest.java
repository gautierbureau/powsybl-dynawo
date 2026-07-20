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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Writes out the whole corpus as we describe it, named the way a given Dynawo release has things,
 * so that what we generate can be compiled and held against what Dynawo ships.
 * <p>
 * The round trip says our description matches the definitions; only compiling what we write says
 * it means the same thing. The definitions are read for what each model is made of, and everything
 * after that is ours.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
class CorpusGenerationTest {

    private static final Path CORPUS = Path.of("..", "..", "dynawo-source", "dynawo", "sources", "Models",
            "Modelica", "PreassembledModels");
    private static final Path OUTPUT = Path.of("target", "generated-preassembled");

    private static final Pattern UNIT = Pattern.compile(
            "<dyn:unitDynamicModel\\s+id=\"([^\"]+)\"\\s+name=\"([^\"]+)\"(?:\\s+initName\\s*=\"([^\"]*)\")?");

    /**
     * The families we write but do not yet describe rightly. Their controls are declared, so a
     * model comes out for them, but not the model Dynawo holds: one keeps its parameters
     * internally, and the other drives its own speed reference, wired to itself rather than to
     * another unit, which an assembly of units has no way to say. They are named so that what
     * comes of them is expected rather than discovered.
     */
    static final List<String> KNOWN_WRONG = List.of(
            "GeneratorSynchronousProportionalRegulationsInternalParameters",
            "GeneratorSynchronousFourWindingsTGov1Sexs");

    @Test
    void shouldWriteTheWholeCorpusAsDynawo170HasIt() throws IOException {
        Assumptions.assumeTrue(Files.exists(CORPUS), "no model definitions available at " + CORPUS);
        Files.createDirectories(OUTPUT);
        Map<String, MachineControlUnit> controls = declaredControls();

        List<String> written = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        for (Path definition : models()) {
            String name = definition.getFileName().toString().replace(".xml", "");
            Map<String, String> units = unitsOf(Files.readString(definition));
            Map<String, String> inits = initsOf(Files.readString(definition));
            boolean threeWindings = String.valueOf(inits.get("generator")).contains("3W");

            GeneratorAssembly assembly = new GeneratorAssembly(
                    threeWindings ? Windings.THREE_WINDINGS : Windings.FOUR_WINDINGS,
                    units.containsKey("transformer"), units.containsKey("auxLV"),
                    ModelNaming.DYNAWO_1_7_0);
            boolean describable = true;
            for (Map.Entry<String, String> unit : units.entrySet()) {
                String role = unit.getKey();
                if (!"voltageRegulator".equals(role) && !"governor".equals(role)) {
                    continue;
                }
                MachineControlUnit control = controls.getOrDefault(key(role, unit.getValue(), threeWindings),
                        controls.get(key(role, unit.getValue(), false)));
                if (control == null) {
                    describable = false;
                } else {
                    assembly.add(control);
                }
            }
            if (!describable) {
                skipped.add(name);
                continue;
            }
            Files.writeString(OUTPUT.resolve(name + ".xml"), PreassembledModelXml.toXml(assembly.build(name)));
            written.add(name);
        }

        // every model of the corpus comes out, the two we cannot describe rightly included, so
        // that what is wrong with them is caught by compiling rather than passed over here
        assertThat(skipped).isEmpty();
        assertThat(written).hasSameSizeAs(models());
        assertThat(written).containsAll(KNOWN_WRONG);
        // named the way 1.7.0 has them, which is what says the release was taken into account
        String anyModel = Files.readString(OUTPUT.resolve("GeneratorSynchronousFourWindingsProportionalRegulations.xml"));
        assertThat(anyModel).contains("efdPu.value").doesNotContain("\"efdPu\"");
    }

    private static List<Path> models() throws IOException {
        try (Stream<Path> definitions = Files.list(CORPUS)) {
            return definitions.filter(p -> p.getFileName().toString().startsWith("GeneratorSynchronous"))
                    .sorted().toList();
        }
    }

    private static Map<String, MachineControlUnit> declaredControls() {
        Map<String, MachineControlUnit> controls = new LinkedHashMap<>();
        Stream.of(VoltageRegulatorUnits.class, GovernorUnits.class).forEach(declaring -> {
            for (Method factory : declaring.getDeclaredMethods()) {
                if (factory.getReturnType() != MachineControlUnit.class) {
                    continue;
                }
                MachineControlUnit control = invoke(factory);
                controls.put(key(control.getId(), control.getName(), factory.getName().endsWith("ThreeWindings")),
                        control);
                controls.putIfAbsent(key(control.getId(), control.getName(), false), control);
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
}
