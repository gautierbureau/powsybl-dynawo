/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.parameters;

import com.powsybl.dynawo.parameters.ParametersSet;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks the parameters a known system's models are valued from, shipped rather than generated:
 * offered under the name a machine's set is given, and only for a model of the system they belong
 * to.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
class ReferenceGeneratorParametersTest {

    private static final String NORDIC_LIB = "GeneratorSynchronousThreeWindingsGoverNordicVRNordic";

    private final ReferenceGeneratorParameters reference = ReferenceGeneratorParameters.getInstance();

    @Test
    void shouldValueANordicModelFromTheShippedParameters() {
        Optional<ParametersSet> set = reference.forModel("DynaWaltz_g09", NORDIC_LIB);
        assertThat(set).isPresent();
        // the exact reference value the system was built with, which generation cannot derive
        assertThat(set.get().getDouble("generator_H")).isEqualTo(3.0);
        assertThat(set.get().getDouble("generator_SNom")).isEqualTo(1000.0);
        // the references onto the load flow are kept, so the model still reads its operating point
        assertThat(set.get().getReferences()).containsKey("generator_P0Pu");
    }

    @Test
    void shouldNameTheSetAsAsked() {
        // the set is offered under the id the mapping gives the machine, so the model finds it
        assertThat(reference.forModel("DynaWaltz_g09", NORDIC_LIB)).get()
                .extracting(ParametersSet::getId).isEqualTo("DynaWaltz_g09");
    }

    @Test
    void shouldNotTouchTheHeldReference() {
        // each call is a copy, so valuing one machine does not disturb the next
        ParametersSet first = reference.forModel("DynaWaltz_g09", NORDIC_LIB).orElseThrow();
        first.replaceParameter("generator_H", com.powsybl.dynawo.parameters.ParameterType.DOUBLE, "99");
        assertThat(reference.forModel("DynaWaltz_g09", NORDIC_LIB).orElseThrow()
                .getDouble("generator_H")).isEqualTo(3.0);
    }

    @Test
    void shouldOfferNothingForAModelOfAnotherKind() {
        // the same machine name in a network that is not the system gets no reference: its model
        // does not carry the system's controls
        assertThat(reference.forModel("DynaWaltz_g09", "GeneratorSynchronousFourWindingsGovCt2St4b")).isEmpty();
    }

    @Test
    void shouldOfferNothingForAMachineTheSystemDoesNotHold() {
        assertThat(reference.forModel("DynaWaltz_gNoSuch", NORDIC_LIB)).isEmpty();
    }
}
