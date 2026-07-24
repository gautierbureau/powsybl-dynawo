/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.dynamicsimulation.DynamicModelsSupplier;
import com.powsybl.dynawo.DynawoSimulationParameters;
import com.powsybl.dynawo.builders.ModelConfig;
import com.powsybl.dynawo.builders.ModelConfigsHandler;
import com.powsybl.dynawo.characteristics.EnergySourceSynchronousGeneratorPropertiesProvider;
import com.powsybl.dynawo.characteristics.GeneratorFilters;
import com.powsybl.dynawo.mappings.controls.ControlTranslations;
import com.powsybl.dynawo.mappings.generators.GeneratorLibResolver;
import com.powsybl.dynawo.mappings.generators.MissingModelBuilder;
import com.powsybl.dynawo.mappings.parameters.ModelDescriptionLookup;
import com.powsybl.dynawo.mappings.preassembled.ModelNaming;
import com.powsybl.dynawo.networks.Ieee14EnergySources;
import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks that a model the mapping had to build is stood up in the dyd, not passed over. Applying
 * the mapping registers what it built, so the simulation knows the library exists and connects it
 * like any model Dynawo ships.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
@Tag("compilation")
class BuiltModelsEnterTheDydTest {

    private static final Path HOME = Path.of("..", "..", "dynawo");

    @Test
    void shouldStandUpTheBuiltModelsInTheDyd(@TempDir Path modelsDir) {
        Assumptions.assumeTrue(Files.exists(HOME.resolve("dynawo.sh")), "no Dynawo installation at " + HOME);
        Path home = HOME.toAbsolutePath().normalize();
        Network network = IeeeCdfNetworkFactory.create14();
        Ieee14EnergySources.apply(network);

        MissingModelBuilder builder = new MissingModelBuilder(home, modelsDir, ModelNaming.DYNAWO_1_7_0);
        double tso = EnergySourceSynchronousGeneratorPropertiesProvider.DEFAULT_TSO_VOLTAGE_MIN;
        UniversalSynchronousGeneratorMapping mapping = new UniversalSynchronousGeneratorMapping(
                "DynaSwing", false, tso,
                new EnergySourceSynchronousGeneratorPropertiesProvider(tso, GeneratorFilters.connected()),
                new GeneratorLibResolver(ControlTranslations.getInstance(), builder));

        DynawoSimulationParameters parameters = new DynawoSimulationParameters();
        DynamicModelsSupplier supplier = DynamicModelsMappings.getInstance()
                .apply(mapping, network, parameters, ModelDescriptionLookup.fromModelDatabase(home));

        // the mapping put what it built into the parameters, under the generator category
        Map<String, List<ModelConfig>> built = parameters.getAdditionalModels();
        assertThat(built).containsKey("SYNCHRONOUS_GENERATOR");
        assertThat(built.get("SYNCHRONOUS_GENERATOR")).extracting(ModelConfig::name)
                .anyMatch(name -> name.contains("Tfo"));

        // registering them, as a run does, is what lets the builder stand them up
        ModelConfigsHandler.getInstance().addModels(built);
        var models = supplier.get(network, ReportNode.NO_OP);

        // every machine is now a model in the dyd, the built ones among them: before they were
        // registered, the models built for the transmission machines were dropped for want of a
        // builder and the dyd came out short
        long machines = network.getGeneratorStream().filter(g -> g.isVoltageRegulatorOn()).count();
        assertThat(models).as("no machine dropped for want of a builder").hasSize((int) machines);
    }
}
