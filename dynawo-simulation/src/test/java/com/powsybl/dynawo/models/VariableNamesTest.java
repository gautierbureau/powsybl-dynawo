/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.models;

import com.powsybl.dynawo.commons.DynawoVersion;
import org.junit.jupiter.api.Test;

import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
class VariableNamesTest {

    @Test
    void testBeforeDynawo2() {
        UnaryOperator<String> variableNames = VariableNames.of(new DynawoVersion(1, 8, 0));
        assertEquals("@@NAME@@@NODE@_phi_value", variableNames.apply("@@NAME@@@NODE@_phi_value"));
        assertEquals("omegaRef_grp_@INDEX@", variableNames.apply("omegaRef_grp_@INDEX@"));
        assertEquals("event_state1_value", VariableNames.of(null).apply("event_state1_value"));
    }

    @Test
    void testDynawo2() {
        UnaryOperator<String> variableNames = VariableNames.of(new DynawoVersion(2, 0, 0));
        // variables of the C++ models and connectors of the Modelica models, no more structures with a value
        assertEquals("@@NAME@@@NODE@_phi", variableNames.apply("@@NAME@@@NODE@_phi_value"));
        assertEquals("event_state1", variableNames.apply("event_state1_value"));
        assertEquals("@STATIC_ID@@NODE@_switchOff", variableNames.apply("@STATIC_ID@@NODE@_switchOff"));
        assertEquals("generator_terminal", variableNames.apply("generator_terminal"));
        // indexed variables of the C++ models, ending with an underscore
        assertEquals("omegaRef_grp_@INDEX@_", variableNames.apply("omegaRef_grp_@INDEX@_value"));
        assertEquals("numcc_node_@INDEX@_", variableNames.apply("numcc_node_@INDEX@"));
        assertEquals("omega_grp_@INDEX@_", VariableNames.of(new DynawoVersion(2, 1, 0)).apply("omega_grp_@INDEX@"));
    }
}
