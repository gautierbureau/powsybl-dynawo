/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.dynamicsimulation.DynamicModelsSupplier;
import com.powsybl.dynamicsimulation.DynamicSimulationParameters;
import com.powsybl.dynamicsimulation.DynamicSimulationResult;
import com.powsybl.dynamicsimulation.EventModelsSupplier;
import com.powsybl.dynamicsimulation.OutputVariable;
import com.powsybl.dynamicsimulation.OutputVariablesSupplier;
import com.powsybl.dynawo.DynawoSimulationConfig;
import com.powsybl.dynawo.DynawoSimulationParameters;
import com.powsybl.dynawo.DynawoSimulationProvider;
import com.powsybl.dynawo.builders.ModelConfigsHandler;
import com.powsybl.dynawo.characteristics.GeneratorFilters;
import com.powsybl.dynawo.characteristics.IidmSynchronousGeneratorPropertiesProvider;
import com.powsybl.dynawo.mappings.controls.ControlTranslations;
import com.powsybl.dynawo.mappings.generators.GeneratorLibResolver;
import com.powsybl.dynawo.mappings.generators.MissingModelBuilder;
import com.powsybl.dynawo.mappings.parameters.ModelDescriptionLookup;
import com.powsybl.dynawo.mappings.preassembled.ModelNaming;
import com.powsybl.dynawo.networks.Ieee14EnergySources;
import com.powsybl.dynawo.outputvariables.DynawoOutputVariablesBuilder;
import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManagerConstants;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks that a voltage stability study runs on the models it had to build, with the simplified
 * regulations it asked for.
 * <p>
 * Such a study describes every machine by proportional regulations, whatever detailed controls it
 * carries, and open source ships a model for only some of the winding and transformer combinations
 * that follow. The machines whose model was missing used to fall back on the detailed model closest
 * at hand, so a study asking for simplified regulations quietly ran detailed ones. Building the
 * missing models is what lets it have what it asked for, and this runs the whole way to check both
 * that the models are the simplified ones and that Dynawo solves them.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
@Tag("compilation")
class SimplifiedStudyRunsOnBuiltModelsTest {

    private static final Path HOME = Path.of("..", "..", "dynawo");

    @Test
    void shouldRunEveryMachineOnItsSimplifiedModel(@TempDir Path modelsDir) throws Exception {
        Assumptions.assumeTrue(Files.exists(HOME.resolve("dynawo.sh")), "no Dynawo installation at " + HOME);
        Path home = HOME.toAbsolutePath().normalize();
        Network network = IeeeCdfNetworkFactory.create14();
        Ieee14EnergySources.apply(network);

        MissingModelBuilder builder = new MissingModelBuilder(home, modelsDir, ModelNaming.DYNAWO_1_7_0);
        double tso = IidmSynchronousGeneratorPropertiesProvider.DEFAULT_TSO_VOLTAGE_MIN;
        UniversalSynchronousGeneratorMapping generators = new UniversalSynchronousGeneratorMapping(
                "DynaWaltz", true, tso,
                new IidmSynchronousGeneratorPropertiesProvider(tso, GeneratorFilters.connected()),
                new GeneratorLibResolver(ControlTranslations.getInstance(), builder));
        UniversalMapping mapping = new UniversalMapping("DynaWaltz", generators, new LoadMapping("DynaWaltz_"));

        DynawoSimulationParameters dynawoParameters = new DynawoSimulationParameters();
        DynamicModelsSupplier models = DynamicModelsMappings.getInstance()
                .apply(mapping, network, dynawoParameters, ModelDescriptionLookup.fromModelDatabase(home));
        ModelConfigsHandler.getInstance().addModels(dynawoParameters.getAdditionalModels());
        ModelConfigsHandler.getInstance().overrideModels(dynawoParameters.getAdditionalModelOverrides());

        // every machine runs proportional regulations, the detailed controls it carries having been
        // translated before any model was looked for
        assertThat(mapping.createModelConfigs(network))
                .filteredOn(model -> model.lib().startsWith("GeneratorSynchronous"))
                .isNotEmpty()
                .allSatisfy(model -> assertThat(model.lib()).contains("GoverProp"));

        DynamicSimulationParameters parameters = new DynamicSimulationParameters(0, 20);
        parameters.addExtension(DynawoSimulationParameters.class, dynawoParameters);

        DynamicSimulationResult result;
        try (ComputationManager computationManager = new LocalComputationManager()) {
            result = new DynawoSimulationProvider(new DynawoSimulationConfig(home, true))
                    .run(network, models, EventModelsSupplier.empty(), activePowerOfEveryMachine(),
                            VariantManagerConstants.INITIAL_VARIANT_ID, computationManager, parameters,
                            ReportNode.NO_OP)
                    .get();
        }

        assertThat(result.getStatus()).as(result.getStatusText()).isEqualTo(DynamicSimulationResult.Status.SUCCESS);
        assertThat(result.getCurves()).hasSize((int) machineCount(network));
    }

    /**
     * The active power of each machine, which is what says a machine held its share rather than
     * merely that the run reached its end.
     */
    private static OutputVariablesSupplier activePowerOfEveryMachine() {
        return (network, reportNode) -> {
            List<OutputVariable> variables = new ArrayList<>();
            network.getGeneratorStream().filter(g -> g.isVoltageRegulatorOn()).forEach(generator ->
                    new DynawoOutputVariablesBuilder().id(generator.getId())
                            .variables("generator_PGenPu")
                            .outputType(OutputVariable.OutputType.CURVE)
                            .add(variables::add));
            return variables;
        };
    }

    private static long machineCount(Network network) {
        return network.getGeneratorStream().filter(g -> g.isVoltageRegulatorOn()).count();
    }
}
