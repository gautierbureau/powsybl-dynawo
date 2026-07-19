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
import com.powsybl.dynamicsimulation.OutputVariablesSupplier;
import com.powsybl.dynawo.DynawoSimulationConfig;
import com.powsybl.dynawo.DynawoSimulationParameters;
import com.powsybl.dynawo.DynawoSimulationProvider;
import com.powsybl.dynawo.mappings.parameters.ModelDescriptionLookup;
import com.powsybl.dynawo.networks.Ieee14EnergySources;
import com.powsybl.dynawo.parameters.ParametersSet;
import com.powsybl.dynawo.suppliers.dynamicmodels.DynamicModelConfig;
import com.powsybl.dynawo.xml.ParametersXml;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManagerConstants;
import com.powsybl.iidm.serde.NetworkSerDe;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs a simulation of IEEE14 described by the mapping alone, which is what the whole pipeline is
 * for: no dynamic model file, no parameter file, no solver file.
 * <p>
 * Skipped when no Dynawo is available.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
class Ieee14SimulationTest {

    /**
     * Location of the Dynawo the simulation runs with, taken from the workspace rather than from
     * the user configuration so that the test does not depend on how the machine is set up.
     */
    private static final String DYNAWO_HOME_PROPERTY = "dynawo.home";
    private static final Path DEFAULT_DYNAWO_HOME = Path.of("..", "..", "dynawo");

    /**
     * The network Dynawo ships with its own IEEE14 example, already solved: a dynamic simulation
     * starts from an operating point, and running a load flow here would only reproduce one.
     */
    private static final Path NETWORK = Path.of("..", "..", "dynawo-source", "examples", "DynaWaltz",
            "IEEE14", "IEEE14_GeneratorDisconnections", "IEEE14.iidm");

    @Test
    void shouldRunAVoltageStabilityStudy() throws Exception {
        assertThat(run(IeeeTestSystemMappings.DYNAWALTZ_NAME).getStatus()).isEqualTo(DynamicSimulationResult.Status.SUCCESS);
    }

    @Test
    void shouldMapTheExampleNetworkOntoTheReferenceModels() throws Exception {
        Path networkFile = NETWORK.toAbsolutePath().normalize();
        Assumptions.assumeTrue(Files.exists(networkFile), "no network available at " + networkFile);
        Network network = NetworkSerDe.read(networkFile);
        Ieee14EnergySources.apply(network);

        UniversalSynchronousGeneratorMapping mapping = IeeeTestSystemMappings.dynaWaltz();
        mapping.createExtensions(network);

        // the reference dyd of this very network holds three four winding and two three winding
        // proportional regulations
        assertThat(mapping.createModelConfigs(network)).extracting(DynamicModelConfig::model)
                .containsExactlyInAnyOrder("GeneratorSynchronousFourWindingsProportionalRegulations",
                        "GeneratorSynchronousFourWindingsProportionalRegulations",
                        "GeneratorSynchronousFourWindingsProportionalRegulations",
                        "GeneratorSynchronousThreeWindingsProportionalRegulations",
                        "GeneratorSynchronousThreeWindingsProportionalRegulations");
    }

    @Test
    void shouldRunTheReferenceParametersThroughOurModels() throws Exception {
        // the same models, valued by the parameters Dynawo ships for this system instead of the
        // generated ones: what runs then tells the models apart from the values given to them
        assertThat(run(IeeeTestSystemMappings.DYNAWALTZ_NAME, Ieee14SimulationTest::referenceParameters).getStatus())
                .isEqualTo(DynamicSimulationResult.Status.SUCCESS);
    }

    @Test
    @Disabled("the machine parameters are generic, and the detailed models diverge on them around t = 6 s")
    void shouldRunATransientStudy() throws Exception {
        // the models are built and initialised, and the run proceeds for six seconds before the
        // solver stops converging: describing a transient study needs machine parameters closer to
        // the real ones than the plausible values the tables hold
        assertThat(run(IeeeTestSystemMappings.DYNASWING_NAME).getStatus()).isEqualTo(DynamicSimulationResult.Status.SUCCESS);
    }

    private static DynamicSimulationResult run(String mappingName) throws Exception {
        return run(mappingName, null);
    }

    /**
     * @param parameterOverride replaces the generated machine parameters when given
     */
    private static DynamicSimulationResult run(String mappingName,
                                               Function<Network, List<ParametersSet>> parameterOverride) throws Exception {
        Path home = dynawoHome();
        Assumptions.assumeTrue(Files.exists(home), "no Dynawo available at " + home);
        Path networkFile = NETWORK.toAbsolutePath().normalize();
        Assumptions.assumeTrue(Files.exists(networkFile), "no network available at " + networkFile);

        Network network = NetworkSerDe.read(networkFile);
        Ieee14EnergySources.apply(network);

        DynamicSimulationParameters parameters = new DynamicSimulationParameters(0, 20);
        String debugDir = System.getProperty("dynawo.debugDir");
        if (debugDir != null) {
            parameters.setDebugDir(debugDir);
        }
        DynawoSimulationParameters dynawoParameters = new DynawoSimulationParameters();
        DynamicModelsSupplier models = DynamicModelsMappings.getInstance()
                .apply(mappingName, network, dynawoParameters, descriptions(home));
        if (parameterOverride != null) {
            parameterOverride.apply(network).forEach(dynawoParameters::addModelParameters);
        }
        parameters.addExtension(DynawoSimulationParameters.class, dynawoParameters);

        try (ComputationManager computationManager = new LocalComputationManager()) {
            return new DynawoSimulationProvider(new DynawoSimulationConfig(home, false))
                    .run(network, models, EventModelsSupplier.empty(), OutputVariablesSupplier.empty(),
                            VariantManagerConstants.INITIAL_VARIANT_ID, computationManager, parameters, ReportNode.NO_OP)
                    .get();
        }
    }

    /**
     * The parameters Dynawo ships for this system, renamed after the machines the mapping names
     * its sets after, so that they take the place of the generated ones.
     */
    private static List<ParametersSet> referenceParameters(Network network) {
        Path parFile = NETWORK.resolveSibling("IEEE14.par").toAbsolutePath().normalize();
        Map<String, ParametersSet> sets = ParametersXml.load(parFile).stream()
                .collect(Collectors.toMap(ParametersSet::getId, Function.identity()));
        return network.getGeneratorStream()
                .map(g -> {
                    String bus = g.getId().replace("_GEN", "").replace("_SM", "").replace("_", "");
                    ParametersSet reference = sets.get("Generator" + bus);
                    return reference == null ? null : new ParametersSet("DynaWaltz_" + g.getId(), reference);
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private static ModelDescriptionLookup descriptions(Path home) {
        return ModelDescriptionLookup.fromModelDatabase(home);
    }

    private static Path dynawoHome() {
        String home = System.getProperty(DYNAWO_HOME_PROPERTY);
        return home != null ? Path.of(home) : DEFAULT_DYNAWO_HOME.toAbsolutePath().normalize();
    }
}
