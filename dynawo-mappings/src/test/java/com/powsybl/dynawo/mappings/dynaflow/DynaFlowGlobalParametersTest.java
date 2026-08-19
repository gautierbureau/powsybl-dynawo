/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.dynaflow;

import com.powsybl.dynawo.parameters.ParametersSet;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The whole-simulation parameter sets a DynaFlow study reads beside its per-equipment sets — the
 * {@code Network} set (the DynaFlow Launcher's {@code Network} writer) and the {@code SimplifiedSolver}
 * set (its {@code Solver} writer).
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
class DynaFlowGlobalParametersTest {

    @Test
    void theNetworkSetCarriesTheStaticDefaultsAndTheStartingPoint() {
        ParametersSet set = DynaFlowGlobalParameters.networkParameters(DynaFlowConfig.defaults());
        assertEquals(DynaFlowGlobalParameters.NETWORK_SET_ID, set.getId());
        Map<String, String> params = values(set);
        assertEquals("300", params.get("capacitor_no_reclosing_delay"));
        assertEquals("90", params.get("line_currentLimit_maxTimeOperation"));
        assertEquals("false", params.get("load_isRestorative"));
        assertEquals("0.0149999997", params.get("transformer_tolV"));
        assertEquals("warm", params.get("startingPointMode"));   // warm start by default
    }

    @Test
    void theSolverSetCarriesTheStepBoundsFromTheConfiguration() {
        ParametersSet set = DynaFlowGlobalParameters.solverParameters(DynaFlowConfig.defaults());
        assertEquals(DynaFlowGlobalParameters.SOLVER_SET_ID, set.getId());
        Map<String, String> params = values(set);
        assertEquals("1e-4", params.get("fnormtol"));
        assertEquals("10.0", params.get("hMax"));   // default time step
        assertEquals("1.0", params.get("hMin"));    // default minimum time step
        assertEquals("15", params.get("mxiter"));
        assertEquals("ALGEBRAIC_J_UPDATE", params.get("minimumModeChangeTypeForAlgebraicRestoration"));
    }

    private static Map<String, String> values(ParametersSet set) {
        return set.getParameters().values().stream().collect(Collectors.toMap(p -> p.name(), p -> p.value()));
    }
}
