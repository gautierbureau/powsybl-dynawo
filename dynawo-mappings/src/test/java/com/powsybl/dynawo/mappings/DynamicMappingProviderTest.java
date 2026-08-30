/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings;

import com.powsybl.commons.PowsyblException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Checks the registry a mapping is chosen and created through: a name and a description to choose
 * by, and settings that reach the mapping without a network being named.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
class DynamicMappingProviderTest {

    private final DynamicModelsMappings registry = DynamicModelsMappings.getInstance();

    @Test
    void shouldRegisterEveryMappingUnderAName() {
        assertThat(registry.getMappingNames())
                .contains(UniversalSynchronousGeneratorMapping.DYNAWALTZ_NAME,
                        UniversalSynchronousGeneratorMapping.DYNASWING_NAME,
                        IeeeTestSystemMappings.DYNAWALTZ_NAME,
                        IeeeTestSystemMappings.DYNASWING_NAME);
    }

    @Test
    void shouldDescribeEveryMappingForChoosing() {
        assertThat(registry.getMappingInfos())
                .allSatisfy(info -> {
                    assertThat(info.name()).isNotBlank();
                    assertThat(info.description()).as(info.name() + " has a description").isNotBlank();
                })
                .extracting(DynamicModelsMappings.MappingInfo::name)
                .contains(UniversalSynchronousGeneratorMapping.DYNAWALTZ_NAME);
    }

    @Test
    void shouldCreateAMappingWithoutAnySetting() {
        DynamicModelsMapping mapping = registry.create(UniversalSynchronousGeneratorMapping.DYNAWALTZ_NAME,
                MappingParameters.empty());
        assertThat(mapping.getName()).isEqualTo(UniversalSynchronousGeneratorMapping.DYNAWALTZ_NAME);
    }

    @Test
    void shouldCreateAFreshMappingEachTime() {
        // a mapping carries the state of one application, the models it built among it, so a
        // second study is given its own rather than what the first left behind
        DynamicModelsMapping first = registry.create(UniversalSynchronousGeneratorMapping.DYNASWING_NAME,
                MappingParameters.empty());
        DynamicModelsMapping second = registry.create(UniversalSynchronousGeneratorMapping.DYNASWING_NAME,
                MappingParameters.empty());
        assertThat(first).isNotSameAs(second);
    }

    @Test
    void shouldCarryASettingToTheMapping() {
        // tso_voltage_min reaches the mapping as text and settles which machines are taken to sit
        // behind a transformer; here it only has to be read without a network in hand
        DynamicModelsMapping mapping = registry.create(UniversalSynchronousGeneratorMapping.DYNAWALTZ_NAME,
                MappingParameters.of(Map.of(UniversalDynaWaltzProvider.TSO_VOLTAGE_MIN, "63")));
        assertThat(mapping.getName()).isEqualTo(UniversalSynchronousGeneratorMapping.DYNAWALTZ_NAME);
    }

    @Test
    void shouldRefuseAnUnknownMapping() {
        assertThatThrownBy(() -> registry.create("NoSuchMapping", MappingParameters.empty()))
                .isInstanceOf(PowsyblException.class)
                .hasMessageContaining("NoSuchMapping");
    }

    @Test
    void shouldRefuseASettingThatIsNotWhatItIsReadAs() {
        MappingParameters parameters = MappingParameters.of(
                Map.of(UniversalDynaWaltzProvider.TSO_VOLTAGE_MIN, "not-a-number"));
        assertThatThrownBy(() -> registry.create(UniversalSynchronousGeneratorMapping.DYNAWALTZ_NAME, parameters))
                .isInstanceOf(PowsyblException.class)
                .hasMessageContaining(UniversalDynaWaltzProvider.TSO_VOLTAGE_MIN);
    }
}
