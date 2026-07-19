/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings;

import com.powsybl.dynawo.extensions.api.generator.RpclType;
import com.powsybl.dynawo.extensions.api.generator.SynchronousGeneratorProperties;
import com.powsybl.dynawo.extensions.api.generator.SynchronousGeneratorPropertiesAdder;
import com.powsybl.dynawo.mappings.generators.GeneratorLibResolver;
import com.powsybl.iidm.network.Generator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
class GeneratorLibResolverTest {

    private final GeneratorLibResolver resolver = new GeneratorLibResolver();

    @ParameterizedTest
    @CsvSource({
        // detailed models: the controls are concatenated as is
        "FOUR_WINDINGS, GovCt2, St4b, false, GeneratorSynchronousFourWindingsGovCt2St4b",
        "FOUR_WINDINGS, GovSteam1, St4b, false, GeneratorSynchronousFourWindingsGovSteam1St4b",
        "THREE_WINDINGS, GovHydro4, St4b, false, GeneratorSynchronousThreeWindingsGovHydro4St4b",
        // simplified models: the governor reduces to proportional, the exciter keeps its integral term
        "FOUR_WINDINGS, GovCt2, St4b, true, GeneratorSynchronousFourWindingsGoverPropVRPropInt",
        "THREE_WINDINGS, GovHydro4, St4b, true, GeneratorSynchronousThreeWindingsGoverPropVRPropInt",
        // a machine already described with proportional regulations keeps them
        "THREE_WINDINGS, GovHydro4, Proportional, true, GeneratorSynchronousThreeWindingsProportionalRegulations"
    })
    void shouldResolveLibFromControls(String windings, String governor, String voltageRegulator, boolean simplified, String expectedLib) {
        SynchronousGeneratorProperties properties = properties(windings, governor, voltageRegulator, false);
        assertThat(resolver.resolve(properties, simplified, false)).contains(expectedLib);
    }

    @ParameterizedTest
    @CsvSource({
        // the Nordic system runs the same controls in both kinds of study, they have no
        // simplified counterpart to translate to and stand for themselves
        "FOUR_WINDINGS, PmConst, VRNordic, GeneratorSynchronousFourWindingsPmConstVRNordic",
        "THREE_WINDINGS, GoverNordic, VRNordic, GeneratorSynchronousThreeWindingsGoverNordicVRNordic",
        "THREE_WINDINGS, PmConst, VRNordic, GeneratorSynchronousThreeWindingsPmConstVRNordic",
        // a machine already described by proportional regulations keeps them
        "FOUR_WINDINGS, Proportional, Proportional, GeneratorSynchronousFourWindingsProportionalRegulations",
        "THREE_WINDINGS, Proportional, Proportional, GeneratorSynchronousThreeWindingsProportionalRegulations"
    })
    void shouldLeaveAlreadySimpleControlsAlone(String windings, String governor, String voltageRegulator, String expectedLib) {
        SynchronousGeneratorProperties properties = properties(windings, governor, voltageRegulator, false);
        assertThat(resolver.resolve(properties, true, false)).contains(expectedLib);
    }

    @Test
    void shouldDropCapabilitiesMissingFromTheCatalog() {
        // no auxiliary nor transformer variant of this model exists in the open source catalog:
        // the mapping degrades to the plain model instead of building a name that does not exist
        SynchronousGeneratorProperties properties = properties("FOUR_WINDINGS", "GovCt2", "St4b", true);
        assertThat(resolver.resolve(properties, false, true)).contains("GeneratorSynchronousFourWindingsGovCt2St4b");
    }

    @Test
    void shouldNotSelectAModelProvidingUnwantedCapabilities() {
        // a transformer model exists for those controls, it must not be picked when no transformer
        // is wanted
        SynchronousGeneratorProperties properties = properties("THREE_WINDINGS", "PmConst", "Scrx", false);
        assertThat(resolver.resolve(properties, false, false)).contains("GeneratorSynchronousThreeWindingsPmConstScrx");
    }

    @Test
    void shouldSelectTheTransformerVariantWhenItExists() {
        SynchronousGeneratorProperties properties = properties("THREE_WINDINGS", "PmConst", "Scrx", false);
        assertThat(resolver.resolve(properties, false, true)).contains("GeneratorSynchronousThreeWindingsPmConstScrxTfo");
    }

    @Test
    void shouldReturnEmptyForUnknownControls() {
        SynchronousGeneratorProperties properties = properties("FOUR_WINDINGS", "NoSuchGovernor", "NoSuchRegulator", false);
        assertThat(resolver.resolve(properties, false, false)).isEmpty();
    }

    private static SynchronousGeneratorProperties properties(String windings, String governor, String voltageRegulator, boolean auxiliaries) {
        Generator generator = TestNetworks.singleGenerator(400.0);
        generator.newExtension(SynchronousGeneratorPropertiesAdder.class)
                .withNumberOfWindings(SynchronousGeneratorProperties.Windings.valueOf(windings))
                .withGovernor(governor)
                .withVoltageRegulator(voltageRegulator)
                .withPss("")
                .withAuxiliaries(auxiliaries)
                .withInternalTransformer(false)
                .withRpcl(RpclType.NONE)
                .withUva(SynchronousGeneratorProperties.Uva.LOCAL)
                .withAggregated(false)
                .withQlim(false)
                .add();
        return generator.getExtension(SynchronousGeneratorProperties.class);
    }
}
