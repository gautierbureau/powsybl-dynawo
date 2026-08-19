/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.dynaflow;

import com.powsybl.dynawo.mappings.dynaflow.DynaFlowConfig.StartingPointMode;
import com.powsybl.dynawo.parameters.ParameterType;
import com.powsybl.dynawo.parameters.ParametersSet;

/**
 * The two whole-simulation parameter sets a DynaFlow study reads, beside the per-equipment sets: the
 * {@code Network} set every static component runs on, and the {@code SimplifiedSolver} set the fixed
 * time-step solver runs — the DynaFlow Launcher's {@code Network} and {@code Solver} writers.
 * <p>
 * Neither is network data, so they are not part of {@link DynaFlowMapping}'s per-equipment output; a
 * DynaFlow run sets them on its {@code DynawoSimulationParameters} (as the network and solver parameters)
 * so the job points at the right {@code Network} / {@code SimplifiedSolver} identifiers.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public final class DynaFlowGlobalParameters {

    /** The parameter set id the network model reads, matching the launcher and the Dynawo job. */
    public static final String NETWORK_SET_ID = "Network";

    /** The parameter set id the simplified (fixed time-step) solver reads. */
    public static final String SOLVER_SET_ID = "SimplifiedSolver";

    private static final ParameterType DOUBLE = ParameterType.DOUBLE;
    private static final ParameterType INT = ParameterType.INT;
    private static final ParameterType BOOL = ParameterType.BOOL;

    private DynaFlowGlobalParameters() {
    }

    /**
     * The {@code Network} set — the launcher's {@code Network::writeNetworkSet}: the static components'
     * fixed time constants and load behaviour, plus the starting point mode read from the configuration.
     */
    public static ParametersSet networkParameters(DynaFlowConfig config) {
        ParametersSet set = new ParametersSet(NETWORK_SET_ID);
        set.addParameter("capacitor_no_reclosing_delay", DOUBLE, "300");
        set.addParameter("dangling_line_currentLimit_maxTimeOperation", DOUBLE, "90");
        set.addParameter("line_currentLimit_maxTimeOperation", DOUBLE, "90");
        set.addParameter("load_Tp", DOUBLE, "90");
        set.addParameter("load_Tq", DOUBLE, "90");
        set.addParameter("load_alpha", DOUBLE, "0");
        set.addParameter("load_alphaLong", DOUBLE, "0");
        set.addParameter("load_beta", DOUBLE, "0");
        set.addParameter("load_betaLong", DOUBLE, "0");
        set.addParameter("load_isControllable", BOOL, "false");
        set.addParameter("load_isRestorative", BOOL, "false");
        set.addParameter("load_zPMax", DOUBLE, "100");
        set.addParameter("load_zQMax", DOUBLE, "100");
        set.addParameter("reactance_no_reclosing_delay", DOUBLE, "0");
        set.addParameter("transformer_currentLimit_maxTimeOperation", DOUBLE, "90");
        set.addParameter("transformer_t1st_HT", DOUBLE, "60");
        set.addParameter("transformer_t1st_THT", DOUBLE, "30");
        set.addParameter("transformer_tNext_HT", DOUBLE, "10");
        set.addParameter("transformer_tNext_THT", DOUBLE, "10");
        set.addParameter("transformer_tolV", DOUBLE, "0.0149999997");
        set.addParameter("startingPointMode", ParameterType.STRING,
                config.startingPointMode() == StartingPointMode.WARM ? "warm" : "flat");
        return set;
    }

    /**
     * The {@code SimplifiedSolver} set — the launcher's {@code Solver::writeSolverSet}: the fixed
     * time-step solver's tolerances, Newton limits and step bounds, with the time step read from the
     * configuration.
     */
    public static ParametersSet solverParameters(DynaFlowConfig config) {
        ParametersSet set = new ParametersSet(SOLVER_SET_ID);
        set.addParameter("fnormtol", DOUBLE, "1e-4");
        set.addParameter("fnormtolAlg", DOUBLE, "1e-4");
        set.addParameter("fnormtolAlgJ", DOUBLE, "1e-4");
        set.addParameter("hMax", DOUBLE, Double.toString(config.timeStep()));
        set.addParameter("hMin", DOUBLE, Double.toString(config.minTimeStep()));
        set.addParameter("initialaddtol", DOUBLE, "0.1");
        set.addParameter("initialaddtolAlg", DOUBLE, "0.1");
        set.addParameter("initialaddtolAlgJ", DOUBLE, "0.1");
        set.addParameter("kReduceStep", DOUBLE, "0.5");
        set.addParameter("maxNewtonTry", INT, "10");
        set.addParameter("msbset", INT, "0");
        set.addParameter("msbsetAlg", INT, "1");
        set.addParameter("msbsetAlgJ", INT, "1");
        set.addParameter("mxiter", INT, "15");
        set.addParameter("mxiterAlg", INT, "30");
        set.addParameter("mxiterAlgJ", INT, "50");
        set.addParameter("mxnewtstep", DOUBLE, "100000");
        set.addParameter("mxnewtstepAlg", DOUBLE, "100000");
        set.addParameter("mxnewtstepAlgJ", DOUBLE, "100000");
        set.addParameter("printfl", INT, "0");
        set.addParameter("printflAlg", INT, "0");
        set.addParameter("printflAlgJ", INT, "0");
        set.addParameter("scsteptol", DOUBLE, "1.e-4");
        set.addParameter("scsteptolAlg", DOUBLE, "1.e-4");
        set.addParameter("scsteptolAlgJ", DOUBLE, "1.e-4");
        set.addParameter("minimumModeChangeTypeForAlgebraicRestoration", ParameterType.STRING, "ALGEBRAIC_J_UPDATE");
        set.addParameter("minimumModeChangeTypeForAlgebraicRestorationInit", ParameterType.STRING, "ALGEBRAIC_J_UPDATE");
        return set;
    }
}
