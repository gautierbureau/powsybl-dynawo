/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings;

import com.powsybl.dynawo.characteristics.EnergySourceSynchronousGeneratorPropertiesProvider;
import com.powsybl.dynawo.characteristics.GeneratorFilters;
import com.powsybl.dynawo.mappings.controls.ControlTranslations;
import com.powsybl.dynawo.mappings.generators.GeneratorLibResolver;
import com.powsybl.dynawo.mappings.generators.MissingModelBuilder;
import com.powsybl.dynawo.mappings.parameters.ModelDescriptionLookup;
import com.powsybl.dynawo.mappings.preassembled.ModelNaming;
import com.powsybl.dynawo.networks.Ieee14EnergySources;
import com.powsybl.dynawo.parameters.ParametersSet;
import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Maps IEEE14 for a transient study and lets the pipeline build the models its machines ask for.
 * <p>
 * A machine on the transmission grid is connected through a transformer, and no such detailed
 * model is installed: this is where a mapping stops settling for the nearest thing and builds
 * exactly what the machine wants. This proves the library is compiled and describes itself; that
 * a model of a name Dynawo does not ship can then be instantiated in a dyd is a further step,
 * needing the catalog to carry it, which is not yet done.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
@Tag("compilation")
class BuildMissingModelsOnIeee14Test {

    private static final Path HOME = Path.of("..", "..", "dynawo");

    @Test
    void shouldBuildTheTransformerModelsTheHighVoltageMachinesAskFor(@TempDir Path modelsDir) {
        Assumptions.assumeTrue(Files.exists(HOME.resolve("dynawo.sh")), "no Dynawo installation at " + HOME);
        Network network = IeeeCdfNetworkFactory.create14();
        Ieee14EnergySources.apply(network);

        MissingModelBuilder builder = new MissingModelBuilder(HOME, modelsDir, ModelNaming.DYNAWO_1_7_0);
        // a transient study, whose detailed controls name a model that is what a unit is assembled
        // from, so what a machine wants can be built
        UniversalSynchronousGeneratorMapping mapping = new UniversalSynchronousGeneratorMapping(
                "DynaSwing", false, EnergySourceSynchronousGeneratorPropertiesProvider.DEFAULT_TSO_VOLTAGE_MIN,
                new EnergySourceSynchronousGeneratorPropertiesProvider(
                        EnergySourceSynchronousGeneratorPropertiesProvider.DEFAULT_TSO_VOLTAGE_MIN,
                        GeneratorFilters.connected()),
                new GeneratorLibResolver(ControlTranslations.getInstance(), builder));

        mapping.createExtensions(network);
        List<MappedModelsSupplier.MappedModel> models = mapping.createModelConfigs(network);

        // the machines above the transmission voltage are given a model carrying their transformer,
        // which nothing installed provided and this built
        List<String> withTransformer = models.stream().map(MappedModelsSupplier.MappedModel::lib)
                .filter(lib -> lib.contains("Tfo")).distinct().toList();
        assertThat(withTransformer).as("models built for the transmission connected machines").isNotEmpty();
        withTransformer.forEach(lib -> assertThat(modelsDir.resolve(lib + ".so")).as(lib).exists());

        // every machine is still described, those below the transmission voltage by an installed
        // model as before
        assertThat(models).extracting(MappedModelsSupplier.MappedModel::lib)
                .allMatch(lib -> lib.startsWith("GeneratorSynchronous"));

        // and each built model answers for its own parameters out of the library, so the mapping
        // values it like any Dynawo ships
        List<ParametersSet> sets = mapping.createParameters(network,
                builder.describe(ModelDescriptionLookup.fromModelDatabase(HOME)));
        assertThat(sets).hasSameSizeAs(models);
        assertThat(sets).allSatisfy(set -> assertThat(set.getParameters()).isNotEmpty());
    }
}
