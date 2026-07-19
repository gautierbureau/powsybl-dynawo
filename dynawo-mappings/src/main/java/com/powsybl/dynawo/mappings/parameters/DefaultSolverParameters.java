/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.parameters;

import com.powsybl.dynawo.parameters.ParametersSet;

import static com.powsybl.dynawo.parameters.ParameterType.BOOL;
import static com.powsybl.dynawo.parameters.ParameterType.DOUBLE;
import static com.powsybl.dynawo.parameters.ParameterType.INT;
import static com.powsybl.dynawo.parameters.ParameterType.STRING;

/**
 * Solver settings a simulation runs with when the user provides none.
 * <p>
 * Values are held here rather than in a parameter file so that running a mapping needs no
 * prerequisite on disk, and so that a study starting from them can read and override them
 * programmatically.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public final class DefaultSolverParameters {

    public static final String SIM_ID = "SIM";
    public static final String SIM_FINAL_STEP_ID = "SIM_FINAL_STEP";
    public static final String IDA_ID = "IDA";

    private DefaultSolverParameters() {
    }

    /**
     * Fixed time step solver, the usual choice for a long simulation.
     */
    public static ParametersSet sim() {
        return simulationSolver(SIM_ID, 5.0, 2.0);
    }

    /**
     * The same solver tuned for the final step of a two step simulation, where the dynamics are
     * slower and the steps can be shorter.
     */
    public static ParametersSet simFinalStep() {
        return simulationSolver(SIM_FINAL_STEP_ID, 2.0, 1.0);
    }

    private static ParametersSet simulationSolver(String id, double maximumStep, double minimalAcceptableStep) {
        ParametersSet set = new ParametersSet(id);
        set.addParameter("hMax", DOUBLE, Double.toString(maximumStep));
        set.addParameter("hMin", DOUBLE, "1.0E-6");
        set.addParameter("minimalAcceptableStep", DOUBLE, Double.toString(minimalAcceptableStep));
        set.addParameter("kReduceStep", DOUBLE, "0.5");
        set.addParameter("maxNewtonTry", INT, "10");
        set.addParameter("maximumNumberSlowStepIncrease", INT, "40");
        set.addParameter("enableSilentZ", BOOL, "true");
        set.addParameter("optimizeAlgebraicResidualsEvaluations", BOOL, "true");
        set.addParameter("optimizeReinitAlgebraicResidualsEvaluations", BOOL, "true");
        set.addParameter("skipNRIfInitialGuessOK", BOOL, "true");
        set.addParameter("order1Prediction", BOOL, "false");
        set.addParameter("minimumModeChangeTypeForAlgebraicRestoration", STRING, "ALGEBRAIC_J_UPDATE");
        set.addParameter("minimumModeChangeTypeForAlgebraicRestorationInit", STRING, "ALGEBRAIC_J_UPDATE");
        addNewtonRaphson(set, "", "1e-3", 15, 0, "100000", "1e-3");
        addNewtonRaphson(set, "Alg", "1e-3", 30, 5, "100000", "1e-3");
        addNewtonRaphson(set, "AlgInit", "1e-3", 50, 1, "100000", "1e-3");
        addNewtonRaphson(set, "AlgJ", "1e-3", 50, 1, "100000", "1e-3");
        return set;
    }

    /**
     * Variable time step solver, more robust on stiff dynamics and slower.
     */
    public static ParametersSet ida() {
        ParametersSet set = new ParametersSet(IDA_ID);
        set.addParameter("order", INT, "2");
        set.addParameter("initStep", DOUBLE, "1e-6");
        set.addParameter("minStep", DOUBLE, "1e-6");
        set.addParameter("maxStep", DOUBLE, "100");
        set.addParameter("minimalAcceptableStep", DOUBLE, "1e-6");
        set.addParameter("absAccuracy", DOUBLE, "1.0E-4");
        set.addParameter("relAccuracy", DOUBLE, "1.0E-4");
        set.addParameter("deltacj", DOUBLE, "0.25");
        set.addParameter("nlscoef", DOUBLE, "0.33");
        set.addParameter("maxcor", INT, "10");
        set.addParameter("maxncf", INT, "10");
        set.addParameter("maxnef", INT, "10");
        set.addParameter("maximumNumberSlowStepIncrease", INT, "100");
        set.addParameter("enableSilentZ", BOOL, "true");
        set.addParameter("optimizeReinitAlgebraicResidualsEvaluations", BOOL, "true");
        set.addParameter("newReinit", BOOL, "true");
        set.addParameter("useForceReinit", BOOL, "true");
        set.addParameter("restorationYPrim", BOOL, "true");
        set.addParameter("multipleStrategiesForAlgebraicRestoration", BOOL, "true");
        set.addParameter("minimumModeChangeTypeForAlgebraicRestoration", STRING, "ALGEBRAIC_J_J_UPDATE");
        set.addParameter("minimumModeChangeTypeForAlgebraicRestorationInit", STRING, "ALGEBRAIC_J_UPDATE");
        set.addParameter("uround", BOOL, "true");
        set.addParameter("uroundPrecision", DOUBLE, "1.e-8");
        set.addParameter("uroundPrecisionAlignedPrecision", BOOL, "true");
        set.addParameter("solveTask", STRING, "ONE_STEP");
        set.addParameter("activateCheckJacobian", BOOL, "false");
        set.addParameter("activateCheckJacobianAfterInit", BOOL, "false");
        set.addParameter("allLogs", BOOL, "false");
        set.addParameter("printLastNewtonY", BOOL, "false");
        set.addParameter("printNewtonSolutions", BOOL, "false");
        set.addParameter("printReinitResiduals", BOOL, "false");
        set.addParameter("printUnstableRoot", BOOL, "false");
        addNewtonRaphson(set, "Alg", "1e-4", 10, 0, "10000000", "1e-6");
        addNewtonRaphson(set, "AlgInit", "1e-4", 10, 1, "10000000", "1e-4");
        addNewtonRaphson(set, "AlgJ", "1e-4", 10, 1, "10000000", "1e-6");
        return set;
    }

    /**
     * Settings of one of the Newton Raphson resolutions a solver runs, the suffix telling which:
     * the simulation itself, the algebraic equations, their initialisation, or their jacobian.
     */
    private static void addNewtonRaphson(ParametersSet set, String suffix, String residualTolerance,
                                         int maximumIterations, int jacobianReuse, String maximumStep,
                                         String stepTolerance) {
        set.addParameter("fnormtol" + suffix, DOUBLE, residualTolerance);
        set.addParameter("initialaddtol" + suffix, DOUBLE, "1");
        set.addParameter("scsteptol" + suffix, DOUBLE, stepTolerance);
        set.addParameter("mxnewtstep" + suffix, DOUBLE, maximumStep);
        set.addParameter("msbset" + suffix, INT, Integer.toString(jacobianReuse));
        set.addParameter("mxiter" + suffix, INT, Integer.toString(maximumIterations));
        set.addParameter("printfl" + suffix, INT, "0");
    }
}
