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
import com.powsybl.dynawo.desc.FilteredDescriptionXml;
import com.powsybl.dynawo.desc.ModelDescription;
import com.powsybl.dynawo.extensions.api.generator.RpclType;
import com.powsybl.dynawo.extensions.api.generator.SynchronousGeneratorProperties;
import com.powsybl.dynawo.extensions.api.generator.SynchronousGeneratorPropertiesAdder;
import com.powsybl.dynawo.mappings.generators.GeneratorFilters;
import com.powsybl.dynawo.mappings.generators.IidmSynchronousGeneratorPropertiesProvider;
import com.powsybl.dynawo.mappings.networks.Ieee14EnergySources;
import com.powsybl.dynawo.parameters.ParametersSet;
import com.powsybl.dynawo.suppliers.Property;
import com.powsybl.dynawo.suppliers.dynamicmodels.DynamicModelConfig;
import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
class UniversalSynchronousGeneratorMappingTest {

    @Test
    void shouldMapIeee14WithSimplifiedModels() {
        Network network = ieee14();
        UniversalSynchronousGeneratorMapping mapping = UniversalSynchronousGeneratorMapping.dynaWaltz();
        mapping.createExtensions(network);

        // the simplified models keep the number of windings but collapse the controls, and the
        // capabilities absent from the open source catalog are dropped
        assertThat(libsByStaticId(mapping.createModelConfigs(network))).containsExactlyInAnyOrderEntriesOf(Map.of(
                "B1-G", "GeneratorSynchronousFourWindingsGoverPropVRPropInt",
                "B2-G", "GeneratorSynchronousFourWindingsGoverPropVRPropInt",
                "B3-G", "GeneratorSynchronousFourWindingsGoverPropVRPropInt",
                "B6-G", "GeneratorSynchronousThreeWindingsGoverPropVRPropInt",
                "B8-G", "GeneratorSynchronousThreeWindingsGoverPropVRPropInt"));
    }

    @Test
    void shouldMapIeee14WithDetailedModels() {
        Network network = ieee14();
        UniversalSynchronousGeneratorMapping mapping = UniversalSynchronousGeneratorMapping.dynaSwing();
        mapping.createExtensions(network);

        // same extensions, but the detailed controls are kept
        assertThat(libsByStaticId(mapping.createModelConfigs(network))).containsExactlyInAnyOrderEntriesOf(Map.of(
                "B1-G", "GeneratorSynchronousFourWindingsGovCt2St4b",
                "B2-G", "GeneratorSynchronousFourWindingsGovSteam1St4b",
                "B3-G", "GeneratorSynchronousFourWindingsGovSteam1St4b",
                "B6-G", "GeneratorSynchronousThreeWindingsGovHydro4St4b",
                "B8-G", "GeneratorSynchronousThreeWindingsGovHydro4St4b"));
    }

    @Test
    void shouldDeduceControlsFromTheEnergySource() {
        Network network = ieee14();
        UniversalSynchronousGeneratorMapping.dynaSwing().createExtensions(network);

        SynchronousGeneratorProperties nuclear = network.getGenerator("B1-G").getExtension(SynchronousGeneratorProperties.class);
        assertThat(nuclear.getGovernor()).isEqualTo("GovCt2");
        assertThat(nuclear.getVoltageRegulator()).isEqualTo("St4b");
        assertThat(nuclear.getNumberOfWindings()).isEqualTo(SynchronousGeneratorProperties.Windings.FOUR_WINDINGS);
        // 135 kV is above the transformer threshold
        assertThat(nuclear.isAuxiliaries()).isTrue();

        SynchronousGeneratorProperties hydro = network.getGenerator("B6-G").getExtension(SynchronousGeneratorProperties.class);
        assertThat(hydro.getGovernor()).isEqualTo("GovHydro4");
        assertThat(hydro.getNumberOfWindings()).isEqualTo(SynchronousGeneratorProperties.Windings.THREE_WINDINGS);
        // 12 kV is below the transformer threshold
        assertThat(hydro.isAuxiliaries()).isFalse();
    }

    @Test
    void shouldKeepExtensionsAlreadyCarriedByTheNetwork() {
        Network network = ieee14();
        Generator generator = network.getGenerator("B1-G");
        generator.newExtension(SynchronousGeneratorPropertiesAdder.class)
                .withNumberOfWindings(SynchronousGeneratorProperties.Windings.THREE_WINDINGS)
                .withGovernor("GovHydro4")
                .withVoltageRegulator("St4b")
                .withPss("")
                .withAuxiliaries(false)
                .withInternalTransformer(false)
                .withRpcl(RpclType.NONE)
                .withUva(SynchronousGeneratorProperties.Uva.LOCAL)
                .withAggregated(false)
                .withQlim(false)
                .add();

        UniversalSynchronousGeneratorMapping mapping = UniversalSynchronousGeneratorMapping.dynaSwing();
        mapping.createExtensions(network);

        // the nuclear rule did not overwrite what the network already declared
        assertThat(generator.getExtension(SynchronousGeneratorProperties.class).getGovernor()).isEqualTo("GovHydro4");
        assertThat(libsByStaticId(mapping.createModelConfigs(network)))
                .containsEntry("B1-G", "GeneratorSynchronousThreeWindingsGovHydro4St4b");
    }

    @Test
    void shouldCoverSynchronousCondensersByDefault() {
        // three of the five IEEE14 machines hold no active power, dropping them would leave the
        // test system almost undescribed
        Network network = ieee14();
        assertThat(network.getGenerator("B3-G").getTargetP()).isZero();

        UniversalSynchronousGeneratorMapping mapping = UniversalSynchronousGeneratorMapping.dynaWaltz();
        mapping.createExtensions(network);

        assertThat(libsByStaticId(mapping.createModelConfigs(network))).containsKeys("B3-G", "B6-G", "B8-G");
    }

    @Test
    void shouldKeepOnlyGeneratingMachinesWhenAsked() {
        Network network = ieee14();
        UniversalSynchronousGeneratorMapping mapping = UniversalSynchronousGeneratorMapping.dynaWaltz(
                IidmSynchronousGeneratorPropertiesProvider.DEFAULT_TSO_VOLTAGE_MIN, GeneratorFilters.generating());
        mapping.createExtensions(network);

        // no load flow has run here, so the targets decide: only B1-G and B2-G generate
        assertThat(libsByStaticId(mapping.createModelConfigs(network))).containsOnlyKeys("B1-G", "B2-G");
    }

    @Test
    void shouldIgnoreDisconnectedMachines() {
        Network network = ieee14();
        network.getGenerator("B6-G").getTerminal().disconnect();

        UniversalSynchronousGeneratorMapping mapping = UniversalSynchronousGeneratorMapping.dynaWaltz();
        mapping.createExtensions(network);

        assertThat(libsByStaticId(mapping.createModelConfigs(network))).doesNotContainKey("B6-G");
    }

    @Test
    void shouldRegisterBothMappings() {
        assertThat(DynamicModelsMappings.getInstance().getMappingNames())
                .contains(UniversalSynchronousGeneratorMapping.DYNAWALTZ_NAME, UniversalSynchronousGeneratorMapping.DYNASWING_NAME);
    }

    @Test
    void shouldFeedTheGeneratedParametersToTheSimulation() {
        Network network = ieee14();
        DynawoSimulationParameters parameters = new DynawoSimulationParameters();
        ModelDescription description = FilteredDescriptionXml.load(
                UniversalSynchronousGeneratorMappingTest.class.getResourceAsStream("/GeneratorSynchronousFourWindingsGoverPropVRPropInt.desc.xml"));

        DynamicModelsSupplier supplier = DynamicModelsMappings.getInstance().apply(
                UniversalSynchronousGeneratorMapping.DYNAWALTZ_NAME, network, parameters,
                lib -> lib.equals(description.name()) ? Optional.of(description) : Optional.empty());

        // the three four winding machines get a set, named after the model they are mapped to
        assertThat(supplier.get(network, ReportNode.NO_OP)).hasSize(5);
        assertThat(parameters.getModelParameters()).extracting(ParametersSet::getId)
                .containsExactlyInAnyOrder("DynaWaltz_B1-G", "DynaWaltz_B2-G", "DynaWaltz_B3-G");
        assertThat(parameters.getModelParameters("DynaWaltz_B1-G").getDouble("generator_H")).isEqualTo(3.0);
    }

    @Test
    void shouldApplyMappingByName() {
        Network network = ieee14();
        assertThat(DynamicModelsMappings.getInstance().apply(UniversalSynchronousGeneratorMapping.DYNAWALTZ_NAME, network)
                .get(network, ReportNode.NO_OP))
                .hasSize(5);
    }

    private static Network ieee14() {
        Network network = IeeeCdfNetworkFactory.create14();
        Ieee14EnergySources.apply(network);
        return network;
    }

    private static Map<String, String> libsByStaticId(List<DynamicModelConfig> configs) {
        return configs.stream().collect(Collectors.toMap(
                c -> (String) c.properties().stream()
                        .filter(p -> "staticId".equals(p.name()))
                        .map(Property::value)
                        .findFirst()
                        .orElseThrow(),
                DynamicModelConfig::model,
                (a, b) -> a,
                LinkedHashMap::new));
    }
}
