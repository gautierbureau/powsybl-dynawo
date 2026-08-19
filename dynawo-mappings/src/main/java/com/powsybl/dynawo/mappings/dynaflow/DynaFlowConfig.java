/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.dynaflow;

import com.powsybl.dynawo.mappings.MappingParameters;

/**
 * The knobs a DynaFlow study sets, the DynaFlow Launcher's {@code Configuration} defaults among them: the
 * reactive limits (a machine's diagram, or infinite), where its transformer is assumed to be, how the
 * simulation starts, which power the active-power compensation follows, the distribution (DSO) voltage
 * below which a load stays static, whether a fictitious load is modelled, and the simplified solver's
 * maximum and minimum time steps.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
record DynaFlowConfig(boolean infiniteReactiveLimits,
                      StartingPointMode startingPointMode,
                      ActivePowerCompensation activePowerCompensation,
                      double tfoVoltageLevel,
                      double dsoVoltageLevel,
                      boolean restorativeFictitiousLoads,
                      double timeStep,
                      double minTimeStep) {

    /** How a machine's initial voltage and power are taken — from the load flow (warm) or the set points (flat). */
    enum StartingPointMode {
        WARM,
        FLAT
    }

    /** Which power the active-power compensation is referenced to. */
    enum ActivePowerCompensation {
        P,
        TARGET_P,
        PMAX
    }

    static final String INFINITE_REACTIVE_LIMITS = "dynaflow_infinite_reactive_limits";
    static final String STARTING_POINT_MODE = "dynaflow_starting_point_mode";
    static final String ACTIVE_POWER_COMPENSATION = "dynaflow_active_power_compensation";
    static final String TFO_VOLTAGE_LEVEL = "dynaflow_tfo_voltage_level";
    static final String DSO_VOLTAGE_LEVEL = "dynaflow_dso_voltage_level";
    static final String RESTORATIVE_FICTITIOUS_LOADS = "dynaflow_restorative_fictitious_loads";
    static final String TIME_STEP = "dynaflow_time_step";
    static final String MIN_TIME_STEP = "dynaflow_min_time_step";

    /** The launcher's defaults: honour each diagram, a 100 kV transformer threshold, warm start, PMax compensation. */
    static DynaFlowConfig defaults() {
        return from(MappingParameters.empty());
    }

    static DynaFlowConfig from(MappingParameters parameters) {
        return new DynaFlowConfig(
                parameters.getBoolean(INFINITE_REACTIVE_LIMITS, false),
                StartingPointMode.valueOf(parameters.getString(STARTING_POINT_MODE).orElse("WARM").toUpperCase()),
                ActivePowerCompensation.valueOf(parameters.getString(ACTIVE_POWER_COMPENSATION).orElse("PMAX").toUpperCase()),
                parameters.getDouble(TFO_VOLTAGE_LEVEL, 100.0),
                parameters.getDouble(DSO_VOLTAGE_LEVEL, 45.0),
                parameters.getBoolean(RESTORATIVE_FICTITIOUS_LOADS, false),
                parameters.getDouble(TIME_STEP, 10.0),
                parameters.getDouble(MIN_TIME_STEP, 1.0));
    }
}
