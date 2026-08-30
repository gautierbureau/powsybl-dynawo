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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Builds a model Dynawo already ships and checks it comes out the same, which is what says the
 * unit models and their connections describe an assembly faithfully.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
class PreassembledModelTest {

    private static final String MODEL = "GeneratorSynchronousFourWindingsGovCt2St4b";

    /**
     * The definitions Dynawo builds its models from, read from a source checkout when there is one.
     */
    private static final Path CORPUS = Path.of("..", "..", "dynawo-source", "dynawo", "sources", "Models",
            "Modelica", "PreassembledModels");

    private static final Pattern UNIT = Pattern.compile(
            "<dyn:unitDynamicModel\\s+id=\"([^\"]+)\"\\s+name=\"([^\"]+)\"(?:\\s+initName\\s*=\"([^\"]*)\")?");
    private static final Pattern CONNECTION = Pattern.compile(
            "<dyn:(initConnect|connect)\\s+id1=\"([^\"]+)\"\\s+var1=\"([^\"]+)\"\\s+id2=\"([^\"]+)\"\\s+var2=\"([^\"]+)\"");

    @ParameterizedTest
    @CsvSource({
        // the machine and its two controls
        "'', false, false",
        // through its transformer, which then initialises it
        "Tfo, true, false",
        // with what it consumes to run itself, the switch they hang on initialising it
        "Aux, false, true",
        // both, the transformer initialising it and the switch carrying the grid side ones
        "TfoAux, true, true"
    })
    void shouldBuildTheModelsDynawoAlreadyShips(String variant, boolean transformer, boolean auxiliaries) throws IOException {
        String model = MODEL + variant;
        Path reference = CORPUS.resolve(model + ".xml").toAbsolutePath().normalize();
        Assumptions.assumeTrue(Files.exists(reference), "no model definitions available at " + CORPUS);
        String shipped = Files.readString(reference);

        String built = PreassembledModelXml.toXml(
                new GeneratorAssembly(Windings.FOUR_WINDINGS, transformer, auxiliaries)
                        .add(new St4bUnit())
                        .add(new GovCt2Unit())
                        .build(model));

        assertThat(units(built)).containsExactlyInAnyOrderElementsOf(units(shipped));
        assertThat(connections(built)).containsExactlyInAnyOrderElementsOf(connections(shipped));
    }

    /**
     * The units of a definition, as the text says them: which Modelica model stands under each
     * name of the assembly, and which one computes its initial state.
     */
    private static List<String> units(String definition) {
        Matcher matcher = UNIT.matcher(definition);
        return matcher.results()
                .map(result -> result.group(1) + " " + result.group(2) + " " + String.valueOf(result.group(3)))
                .toList();
    }

    /**
     * The connections of a definition, each one read whichever way round it was written.
     */
    private static List<UnitConnection> connections(String definition) {
        Matcher matcher = CONNECTION.matcher(definition);
        return matcher.results()
                .map(result -> new UnitConnection(result.group(2), result.group(3), result.group(4), result.group(5),
                        "initConnect".equals(result.group(1))))
                .toList();
    }
}
