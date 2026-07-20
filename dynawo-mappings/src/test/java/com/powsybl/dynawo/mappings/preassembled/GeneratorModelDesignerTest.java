/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.preassembled;

import com.powsybl.dynawo.extensions.api.generator.RpclType;
import com.powsybl.dynawo.extensions.api.generator.SynchronousGeneratorProperties;
import com.powsybl.dynawo.extensions.api.generator.SynchronousGeneratorPropertiesAdder;
import com.powsybl.dynawo.mappings.TestNetworks;
import com.powsybl.dynawo.mappings.parameters.ModelDescriptionLookup;
import com.powsybl.dynawo.mappings.tools.PreassembledModelCompiler;
import com.powsybl.iidm.network.Generator;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Designs a model nobody has ever built and builds it.
 * <p>
 * Of the combinations of a governor and a regulator we describe, all but a handful have no model:
 * Dynawo ships the ones somebody needed. Asking for one of the others is the case a catalogue
 * cannot answer, and the reason describing a model out of its parts is worth anything.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
class GeneratorModelDesignerTest {

    private static final Path HOME = Path.of("..", "..", "dynawo");

    /**
     * A governor and a regulator Dynawo both ships, on a machine it never puts them on together.
     */
    private static final String GOVERNOR = "GovCt2";
    private static final String REGULATOR = "Ac6a";
    private static final String WANTED = "GeneratorSynchronousFourWindingsGovCt2Ac6a";

    @Test
    void shouldDesignAModelNobodyHasBuilt() {
        Optional<PreassembledModel> model = new GeneratorModelDesigner(ModelNaming.DYNAWO_1_7_0)
                .design(properties(GOVERNOR, REGULATOR, null), false);

        assertThat(model).isPresent();
        assertThat(model.get().getId()).isEqualTo(WANTED);
        assertThat(model.get().getUnits()).extracting(UnitModel::getId)
                .containsExactlyInAnyOrder("generator", "governor", "voltageRegulator");
        assertThat(model.get().getUnits()).extracting(ControlUnitCatalog::nameOf)
                .contains(GOVERNOR, REGULATOR);
    }

    @Test
    void shouldSayNothingOfAControlItDoesNotKnow() {
        GeneratorModelDesigner designer = new GeneratorModelDesigner(ModelNaming.DYNAWO_1_7_0);
        assertThat(designer.design(properties("NoSuchGovernor", REGULATOR, null), false)).isEmpty();
        assertThat(designer.design(properties(GOVERNOR, "NoSuchRegulator", null), false)).isEmpty();
        // a stabiliser we do not know is not quietly left out either
        assertThat(designer.design(properties(GOVERNOR, REGULATOR, "NoSuchStabiliser"), false)).isEmpty();
    }

    @Test
    void shouldCarryAStabiliserAndATransformerIntoTheName() {
        GeneratorModelDesigner designer = new GeneratorModelDesigner(ModelNaming.DYNAWO_1_7_0);
        Optional<PreassembledModel> model = designer.design(properties(GOVERNOR, REGULATOR, "Pss2b"), true);

        assertThat(model).isPresent();
        assertThat(model.get().getId()).isEqualTo(WANTED + "Pss2b" + "Tfo");
        assertThat(model.get().getUnits()).extracting(UnitModel::getId)
                .contains("powerSystemStabilizer", "transformer");
    }

    @Test
    @Tag("compilation")
    void shouldBuildTheModelItDesigned(@TempDir Path modelsDir) {
        Assumptions.assumeTrue(Files.exists(HOME.resolve("dynawo.sh")), "no Dynawo installation at " + HOME);
        // nothing in the installation goes by that name, which is the whole point of building it
        assertThat(HOME.resolve("ddb").resolve(WANTED + ".so")).doesNotExist();

        PreassembledModel model = new GeneratorModelDesigner(ModelNaming.DYNAWO_1_7_0)
                .design(properties(GOVERNOR, REGULATOR, null), false)
                .orElseThrow();
        Path library = new PreassembledModelCompiler(HOME).compile(model, modelsDir);

        assertThat(library).exists();
        // and it answers for itself, holding what both controls expect, so parameters can be
        // generated for it like for any model Dynawo ships
        var description = ModelDescriptionLookup.fromCompiledModels(modelsDir, HOME).find(WANTED);
        assertThat(description).isPresent();
        assertThat(description.get().name()).isEqualTo(WANTED);
        assertThat(description.get().parameters()).extracting(p -> p.name())
                .anyMatch(name -> name.startsWith("governor_"))
                .anyMatch(name -> name.startsWith("voltageRegulator_"))
                .anyMatch(name -> name.startsWith("generator_"));
    }

    private static SynchronousGeneratorProperties properties(String governor, String voltageRegulator, String pss) {
        Generator generator = TestNetworks.singleGenerator(400.0);
        generator.newExtension(SynchronousGeneratorPropertiesAdder.class)
                .withNumberOfWindings(SynchronousGeneratorProperties.Windings.FOUR_WINDINGS)
                .withGovernor(governor)
                .withVoltageRegulator(voltageRegulator)
                .withPss(pss)
                .withAuxiliaries(false)
                .withInternalTransformer(false)
                .withRpcl(RpclType.NONE)
                .withUva(SynchronousGeneratorProperties.Uva.LOCAL)
                .withAggregated(false)
                .withQlim(false)
                .add();
        return generator.getExtension(SynchronousGeneratorProperties.class);
    }
}
