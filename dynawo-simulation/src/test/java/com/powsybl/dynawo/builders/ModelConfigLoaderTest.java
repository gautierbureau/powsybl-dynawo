/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com/)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.builders;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.dynawo.commons.DynawoVersion;
import com.powsybl.dynawo.models.VarMapping;
import com.powsybl.dynawo.models.generators.BaseGeneratorBuilder;
import com.powsybl.dynawo.models.lines.LineBuilder;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.test.NoEquipmentNetworkFactory;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
 */
class ModelConfigLoaderTest {

    @Test
    void loadConfigTest() throws IOException {
        String json = """
                {
                    "miscGenerators": {
                        "defaultLib": "WT4BWeccCurrentSource",
                        "libs": [
                            {
                              "lib": "PhotovoltaicsWeccCurrentSource",
                              "alias": "Wecc",
                              "internalModelPrefix": "WTG4A",
                              "properties": [
                                "SYNCHRONIZED"
                              ],
                              "minVersion": "1.3.0",
                              "maxVersion": "1.4.0",
                              "endCause": "Deleted",
                              "doc": "Photovoltaics Wecc generator",
                              "macroStaticRef": [
                                {
                                  "dynamicVar":"wecc_state",
                                  "staticVar":"state"
                                }
                              ],
                              "variablePrefix": [
                                {
                                  "variable":"terminal",
                                  "prefix":"WT"
                                }
                              ]
                            },
                            {
                              "lib": "WT4BWeccCurrentSource",
                              "properties": [
                                "SYNCHRONIZED",
                                "CONTROLLABLE"
                              ]
                            },
                            {
                              "lib": "WT4AWeccCurrentSource",
                              "doc": "WT4A Wecc generator",
                              "minVersion": "1.6.0"
                            }
                        ]
                    }
                }""";

        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Map.class, new ModelConfigsJsonDeserializer());
        objectMapper.registerModule(module);
        Map<String, ModelConfigs> configs = objectMapper.readValue(json, new TypeReference<>() {
        });
        assertThat(configs.keySet()).containsExactly("miscGenerators");
        ModelConfigs synchroGens = configs.get("miscGenerators");
        assertThat(synchroGens.getModelsName()).containsExactly(
                "WT4AWeccCurrentSource",
                "WT4BWeccCurrentSource",
                "Wecc");

        // Expected models
        ModelConfig defaultModel = new ModelConfig("WT4BWeccCurrentSource", List.of("SYNCHRONIZED", "CONTROLLABLE"));
        ModelConfig baseModel = new ModelConfig("WT4AWeccCurrentSource", null, null, Collections.emptyList(), "WT4A Wecc generator", new VersionInterval(new DynawoVersion(1, 6, 0)));
        ModelConfig completeModel = new ModelConfig("PhotovoltaicsWeccCurrentSource", "Wecc", "WTG4A", List.of("SYNCHRONIZED"), "Photovoltaics Wecc generator",
                new VersionInterval(new DynawoVersion(1, 3, 0), new DynawoVersion(1, 4, 0), "Deleted"),
                List.of(new VarMapping("wecc_state", "state")),
                Map.of("terminal", "WT_terminal"));

        assertEquals(defaultModel, synchroGens.getDefaultModelConfig());
        assertThat(synchroGens.getModelInfos())
                .containsExactly(baseModel, defaultModel, completeModel)
                // Check formatted info
                .map(ModelInfo::formattedInfo)
                .containsExactly(
                    "WT4AWeccCurrentSource: WT4A Wecc generator (Dynawo Version 1.6.0)",
                    "WT4BWeccCurrentSource (Dynawo Version 1.5.0)",
                    "Wecc (PhotovoltaicsWeccCurrentSource): Photovoltaics Wecc generator (Dynawo Version 1.3.0 - 1.4.0 (Deleted))");
        assertThat(synchroGens.getModelInfos(DynawoVersion.createFromString("1.5.0")))
                .map(ModelInfo::name)
                .hasSize(1)
                .containsExactly("WT4BWeccCurrentSource");
    }

    @Test
    void mergeModelConfigs() {
        ModelConfig defaultModel = new ModelConfig("AA");
        ModelConfigs modelConfigs1 = new ModelConfigs(new TreeMap<>(Map.of(defaultModel.name(), defaultModel)), defaultModel.name());

        ModelConfig mc1 = new ModelConfig("BB");
        ModelConfig mc2 = new ModelConfig("CC");
        ModelConfigs modelConfigs2 = new ModelConfigs(new TreeMap<>(Map.of(mc1.name(), mc1, mc2.name(), mc2)), mc1.name());

        modelConfigs1.addModelConfigs(modelConfigs2);
        assertThat(modelConfigs1.getModelInfos()).containsExactly(
                defaultModel,
                new ModelConfig("BB"),
                mc2);
        assertEquals(defaultModel, modelConfigs1.getDefaultModelConfig());

        ModelConfigs modelConfigs3 = new ModelConfigs(new TreeMap<>(Map.of(mc2.name(), mc2)), null);
        ModelConfigs modelConfigs4 = new ModelConfigs(new TreeMap<>(Map.of(defaultModel.name(), defaultModel)), defaultModel.name());
        modelConfigs3.addModelConfigs(modelConfigs4);
        assertEquals(defaultModel, modelConfigs3.getDefaultModelConfig());
    }

    @Test
    void loadAdditionalModels() throws URISyntaxException {
        Path additionalModels = Path.of(Objects.requireNonNull(getClass().getResource("/additionalModels.json")).toURI());
        Network network = NoEquipmentNetworkFactory.create();
        ModelConfigsHandler handler = ModelConfigsHandler.getInstance();
        int baseGenNumber = BaseGeneratorBuilder.getSupportedModelInfos().size();
        int baseLineNumber = LineBuilder.getSupportedModelInfos().size();
        handler.addModels(new AdditionalModelConfigLoader(additionalModels));

        assertThat(BaseGeneratorBuilder.getSupportedModelInfos())
                .hasSize(baseGenNumber + 2)
                .contains(new ModelConfig("AdditionalGenerator1"), new ModelConfig("AdditionalGenerator2"));
        assertNotNull(handler.getModelBuilder(network, "AdditionalGenerator1", ReportNode.NO_OP));
        assertNotNull(handler.getModelBuilder(network, "AdditionalGenerator2", ReportNode.NO_OP));

        assertThat(LineBuilder.getSupportedModelInfos())
                .hasSize(baseLineNumber + 1)
                .contains(new ModelConfig("AdditionalLine"));
        assertNotNull(handler.getModelBuilder(network, "AdditionalLine", ReportNode.NO_OP));
    }

    @Test
    void additionalModelsFileNotFound() {
        ModelConfigsHandler handler = ModelConfigsHandler.getInstance();
        AdditionalModelConfigLoader loader = new AdditionalModelConfigLoader(Path.of("wrongPath"));
        assertThatThrownBy(() -> handler.addModels(loader))
                .isInstanceOf(PowsyblException.class)
                .hasMessage("Additional dynamic models configuration file not found");

    }

    @Test
    void addModelsProgrammatically() {
        Network network = NoEquipmentNetworkFactory.create();
        ModelConfigsHandler handler = ModelConfigsHandler.getInstance();
        int baseGenNumber = BaseGeneratorBuilder.getSupportedModelInfos().size();
        int baseLineNumber = LineBuilder.getSupportedModelInfos().size();

        ModelConfig gen1 = new ModelConfig("ProgrammaticGenerator1");
        ModelConfig gen2 = new ModelConfig("ProgrammaticGenerator2", List.of("CONTROLLABLE"));
        ModelConfig line = new ModelConfig("ProgrammaticLine");
        handler.addModels(Map.of(
                "BASE_GENERATOR", List.of(gen1, gen2),
                "BASE_LINE", List.of(line)));

        assertThat(BaseGeneratorBuilder.getSupportedModelInfos())
                .hasSize(baseGenNumber + 2)
                .contains(gen1, gen2);
        assertNotNull(handler.getModelBuilder(network, "ProgrammaticGenerator1", ReportNode.NO_OP));
        assertNotNull(handler.getModelBuilder(network, "ProgrammaticGenerator2", ReportNode.NO_OP));

        assertThat(LineBuilder.getSupportedModelInfos())
                .hasSize(baseLineNumber + 1)
                .contains(line);
        assertNotNull(handler.getModelBuilder(network, "ProgrammaticLine", ReportNode.NO_OP));
    }

    @Test
    void addModelsProgrammaticallyUnknownCategoryIsSkipped() {
        Network network = NoEquipmentNetworkFactory.create();
        ModelConfigsHandler handler = ModelConfigsHandler.getInstance();
        handler.addModels(Map.of("UNKNOWN_CATEGORY", List.of(new ModelConfig("ShouldBeSkipped"))));
        assertNull(handler.getModelBuilder(network, "ShouldBeSkipped", ReportNode.NO_OP));
    }

    @Test
    void scopedRegistrationsAreUndoneOnClose() {
        Network network = NoEquipmentNetworkFactory.create();
        ModelConfigsHandler handler = ModelConfigsHandler.getInstance();
        int baseGenNumber = BaseGeneratorBuilder.getSupportedModelInfos().size();

        ModelConfig scoped = new ModelConfig("ScopedGenerator");
        try (ModelConfigsHandler.Scope scope = handler.openScope()) {
            handler.addModels(Map.of("BASE_GENERATOR", List.of(scoped)));
            // inside the scope the model is registered, catalog and builders alike
            assertThat(BaseGeneratorBuilder.getSupportedModelInfos()).hasSize(baseGenNumber + 1).contains(scoped);
            assertNotNull(handler.getModelBuilder(network, "ScopedGenerator", ReportNode.NO_OP));
        }

        // closing the scope puts the catalog back as it was, the model gone from both
        assertThat(BaseGeneratorBuilder.getSupportedModelInfos()).hasSize(baseGenNumber).doesNotContain(scoped);
        assertNull(handler.getModelBuilder(network, "ScopedGenerator", ReportNode.NO_OP));
        assertThat(handler.findModelConfig("ScopedGenerator")).isEmpty();
    }

    @Test
    void resetToBaseDropsRuntimeRegistrations() {
        Network network = NoEquipmentNetworkFactory.create();
        ModelConfigsHandler handler = ModelConfigsHandler.getInstance();
        // reset first so an earlier test's leaked registration is not mistaken for the base catalog
        handler.resetToBase();
        int baseGenCount = BaseGeneratorBuilder.getSupportedModelInfos().size();

        handler.addModels(Map.of("BASE_GENERATOR", List.of(new ModelConfig("ResetGenerator"))));
        assertThat(BaseGeneratorBuilder.getSupportedModelInfos()).hasSize(baseGenCount + 1);
        assertNotNull(handler.getModelBuilder(network, "ResetGenerator", ReportNode.NO_OP));

        handler.resetToBase();

        // the runtime addition is gone and the base catalog is whole again
        assertThat(BaseGeneratorBuilder.getSupportedModelInfos()).hasSize(baseGenCount);
        assertNull(handler.getModelBuilder(network, "ResetGenerator", ReportNode.NO_OP));
        assertThat(handler.findModelConfig("ResetGenerator")).isEmpty();
    }

    @Test
    void scopeRestoresAConfigurationAnOverrideReplaced() {
        ModelConfigsHandler handler = ModelConfigsHandler.getInstance();
        String lib = BaseGeneratorBuilder.getSupportedModelInfos().stream().findFirst().orElseThrow().lib();
        ModelConfig original = handler.findModelConfig(lib).orElseThrow();

        try (ModelConfigsHandler.Scope scope = handler.openScope()) {
            handler.overrideModels(Map.of("BASE_GENERATOR", List.of(new ModelConfig(lib, List.of("OVERRIDDEN")))));
            assertThat(handler.findModelConfig(lib).orElseThrow().properties()).contains("OVERRIDDEN");
        }

        // an override corrects a configuration in place; the scope brings the original one back
        assertThat(handler.findModelConfig(lib)).contains(original);
    }
}
