/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings;

import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks that every static var compensator becomes the model standing for it, its working point
 * read from the load flow and the rest of what the model expects valued from the reference defaults,
 * and that the universal mapping carries it beside the machines and loads.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
class SvarcMappingTest {

    @Test
    void shouldMapEverySvarcToItsModel() {
        Network network = TestNetworks.singleStaticVarCompensator(225).getNetwork();
        SvarcMapping mapping = new SvarcMapping("Study_");

        assertThat(mapping.createModelConfigs(network)).singleElement().satisfies(model -> {
            assertThat(model.lib()).isEqualTo("StaticVarCompensator");
            assertThat(model.staticId()).isEqualTo("svc");
            assertThat(model.parameterSetId()).isEqualTo("Study_svc");
        });

        assertThat(mapping.createParameters(network)).singleElement().satisfies(set -> {
            assertThat(set.getId()).isEqualTo("Study_svc");
            // the working point is read from the load flow of the compensator the model stands for
            assertThat(set.getReferences()).containsKeys("SVarC_P0Pu", "SVarC_Q0Pu", "SVarC_U0Pu", "SVarC_UPhase0");
            // the rest are values, the starting mode and the susceptance limits among them
            assertThat(set.hasParameter("SVarC_Mode0")).isTrue();
            assertThat(set.getParameters().get("SVarC_BMaxPu").value()).isEqualTo("1.07");
        });
    }

    @Test
    void universalMappingCarriesTheSvarcBesideTheRest() {
        Network network = TestNetworks.singleStaticVarCompensator(225).getNetwork();
        var models = new UniversalDynaWaltzProvider().create(MappingParameters.empty()).createModelConfigs(network);
        assertThat(models).anySatisfy(model -> {
            assertThat(model.lib()).isEqualTo("StaticVarCompensator");
            assertThat(model.staticId()).isEqualTo("svc");
        });
    }
}
