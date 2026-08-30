/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings;

import com.powsybl.dynawo.builders.ModelConfig;
import com.powsybl.dynawo.mappings.parameters.ModelDescriptionLookup;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks that a forwarding mapping passes every method on, the built-model ones among them, so a
 * mapping applied by name reaches what one applied by hand does. A method left out here is one a
 * registered mapping would quietly drop.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
class ForwardingMappingTest {

    /**
     * A delegate that remembers being asked, so forwarding can be told from a default answer.
     */
    private static final class Spy implements DynamicModelsMapping {

        private boolean builtModelsDirAsked;
        private boolean builtModelConfigsAsked;
        private boolean describeBuiltModelsAsked;

        @Override
        public String getName() {
            return "spy";
        }

        @Override
        public com.powsybl.dynawo.DynawoSimulationParameters.SolverType getSolverType() {
            return com.powsybl.dynawo.DynawoSimulationParameters.SolverType.SIM;
        }

        @Override
        public void createExtensions(com.powsybl.iidm.network.Network network) {
            // nothing
        }

        @Override
        public List<MappedModelsSupplier.MappedModel> createModelConfigs(com.powsybl.iidm.network.Network network) {
            return List.of();
        }

        @Override
        public List<com.powsybl.dynawo.parameters.ParametersSet> createParameters(
                com.powsybl.iidm.network.Network network, ModelDescriptionLookup descriptions) {
            return List.of();
        }

        @Override
        public Optional<Path> getBuiltModelsDir() {
            builtModelsDirAsked = true;
            return Optional.of(Path.of("built"));
        }

        @Override
        public Map<String, List<ModelConfig>> getBuiltModelConfigs() {
            builtModelConfigsAsked = true;
            return Map.of("SYNCHRONOUS_GENERATOR", List.of(new ModelConfig("Built", List.of())));
        }

        @Override
        public ModelDescriptionLookup describeBuiltModels(ModelDescriptionLookup installed) {
            describeBuiltModelsAsked = true;
            return installed;
        }
    }

    @Test
    void shouldForwardTheBuiltModelMethodsToItsDelegate() {
        Spy spy = new Spy();
        AbstractForwardingDynamicModelsMapping forwarding = new AbstractForwardingDynamicModelsMapping() {
            @Override
            protected DynamicModelsMapping delegate() {
                return spy;
            }
        };

        assertThat(forwarding.getBuiltModelsDir()).hasValue(Path.of("built"));
        assertThat(forwarding.getBuiltModelConfigs()).containsKey("SYNCHRONOUS_GENERATOR");
        assertThat(forwarding.describeBuiltModels(lib -> Optional.empty())).isNotNull();

        assertThat(spy.builtModelsDirAsked).isTrue();
        assertThat(spy.builtModelConfigsAsked).isTrue();
        assertThat(spy.describeBuiltModelsAsked).isTrue();
    }

    @Test
    void shouldForwardEveryMethodTheMappingDeclares() {
        // nothing on the interface is left unforwarded, so a method added later is caught here
        // rather than dropped by a registered mapping
        for (Method method : DynamicModelsMapping.class.getMethods()) {
            if (method.isDefault() || Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            assertThat(isOverriddenBy(method, AbstractForwardingDynamicModelsMapping.class))
                    .as("AbstractForwardingDynamicModelsMapping forwards " + method.getName())
                    .isTrue();
        }
    }

    private static boolean isOverriddenBy(Method method, Class<?> type) {
        try {
            Method found = type.getDeclaredMethod(method.getName(), method.getParameterTypes());
            return !Modifier.isAbstract(found.getModifiers());
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}
