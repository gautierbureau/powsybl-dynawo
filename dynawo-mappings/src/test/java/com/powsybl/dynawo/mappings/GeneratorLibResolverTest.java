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
import com.powsybl.dynawo.extensions.api.generator.SynchronousGeneratorProperties.Windings;
import com.powsybl.dynawo.extensions.api.generator.SynchronousGeneratorPropertiesAdder;
import com.powsybl.dynawo.mappings.controls.ControlTranslations;
import com.powsybl.dynawo.mappings.generators.GeneratorLibResolver;
import com.powsybl.dynawo.mappings.generators.MissingModelBuilder;
import com.powsybl.dynawo.mappings.preassembled.GeneratorControls;
import com.powsybl.dynawo.mappings.preassembled.GeneratorModelDesigner;
import com.powsybl.dynawo.mappings.preassembled.ModelNaming;
import com.powsybl.dynawo.mappings.tools.PreassembledModelCompiler;
import com.powsybl.iidm.network.Generator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.file.Path;
import java.util.Optional;

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
        // a machine already described with a simplified regulation keeps it, named as its model is
        "THREE_WINDINGS, GovHydro4, VRProportional, true, GeneratorSynchronousThreeWindingsProportionalRegulations"
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
        // a machine already described by the simplified regulations keeps them, named as their models are
        "FOUR_WINDINGS, GoverProportional, VRProportional, GeneratorSynchronousFourWindingsProportionalRegulations",
        "THREE_WINDINGS, GoverProportional, VRProportional, GeneratorSynchronousThreeWindingsProportionalRegulations"
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

    @Test
    void shouldBuildASimplifiedMachineWithItsReactiveLimitsAndControlLoop() {
        CapturingBuilder builder = new CapturingBuilder();
        GeneratorLibResolver resolverWithBuilder = new GeneratorLibResolver(ControlTranslations.getInstance(), builder);
        SynchronousGeneratorProperties properties = properties("FOUR_WINDINGS", "GovHydro4", "VRProportional", true, true, RpclType.RPCL2);
        // no installed model provides the reactive limits and the loop, so one is built with them
        assertThat(resolverWithBuilder.resolve(properties, true, false))
                .contains("GeneratorSynchronousFourWindingsProportionalRegulationsQlimRpcl2");
        assertThat(builder.qlim).isTrue();
        assertThat(builder.rpcl).isEqualTo(RpclType.RPCL2);
    }

    @Test
    void shouldBuildADetailedMachineWithNeitherReactiveLimitsNorControlLoop() {
        CapturingBuilder builder = new CapturingBuilder();
        GeneratorLibResolver resolverWithBuilder = new GeneratorLibResolver(ControlTranslations.getInstance(), builder);
        SynchronousGeneratorProperties properties = properties("FOUR_WINDINGS", "GovHydro4", "VRProportional", true, true, RpclType.RPCL2);
        // detailed: the reactive limits and the loop only exist on the simplified models, so a
        // detailed machine asks the builder for neither, whatever its flags say
        resolverWithBuilder.resolve(properties, false, false);
        assertThat(builder.qlim).isFalse();
        assertThat(builder.rpcl).isEqualTo(RpclType.NONE);
    }

    /**
     * A builder that records what it was asked to build rather than compiling anything, so that what
     * the resolver hands it can be read off directly.
     */
    private static final class CapturingBuilder extends MissingModelBuilder {

        private Boolean qlim;
        private RpclType rpcl;

        private CapturingBuilder() {
            super(new GeneratorModelDesigner(ModelNaming.DYNAWO_1_7_0),
                    new PreassembledModelCompiler(Path.of("nowhere")), Path.of("target"));
        }

        @Override
        public Optional<String> build(GeneratorControls controls, Windings windings, boolean transformer,
                                      boolean auxiliaries, boolean qlim, RpclType rpcl) {
            this.qlim = qlim;
            this.rpcl = rpcl;
            return Optional.of("GeneratorSynchronousFourWindingsProportionalRegulationsQlimRpcl2");
        }
    }

    private static SynchronousGeneratorProperties properties(String windings, String governor, String voltageRegulator, boolean auxiliaries) {
        return properties(windings, governor, voltageRegulator, auxiliaries, false, RpclType.NONE);
    }

    private static SynchronousGeneratorProperties properties(String windings, String governor, String voltageRegulator,
                                                             boolean auxiliaries, boolean qlim, RpclType rpcl) {
        Generator generator = TestNetworks.singleGenerator(400.0);
        generator.newExtension(SynchronousGeneratorPropertiesAdder.class)
                .withNumberOfWindings(SynchronousGeneratorProperties.Windings.valueOf(windings))
                .withGovernor(governor)
                .withVoltageRegulator(voltageRegulator)
                .withPss("")
                .withAuxiliaries(auxiliaries)
                .withInternalTransformer(false)
                .withRpcl(rpcl)
                .withUva(SynchronousGeneratorProperties.Uva.LOCAL)
                .withAggregated(false)
                .withQlim(qlim)
                .add();
        return generator.getExtension(SynchronousGeneratorProperties.class);
    }
}
