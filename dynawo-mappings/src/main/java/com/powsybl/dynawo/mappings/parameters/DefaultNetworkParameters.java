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

/**
 * Behaviour of the equipments the network model represents itself, rather than through a dynamic
 * model: how loads respond to voltage, how long a tap changer waits, when a protection operates.
 * <p>
 * Held in Java for the same reason as the solver settings: a mapping should run without a
 * parameter file to point at.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public final class DefaultNetworkParameters {

    public static final String NETWORK_ID = "Network";
    public static final String NETWORK_FINAL_STEP_ID = "Network_FINAL_STEP";

    private DefaultNetworkParameters() {
    }

    /**
     * The detailed (DynaSwing) network: the machines and the loads it does not model in detail
     * follow the voltage, the tap changers are held for the length of a transient run, and the
     * foreign nodes of an HVDC link are kept so the link has something to sit against.
     */
    public static ParametersSet dynaSwing() {
        ParametersSet set = common(NETWORK_ID);
        set.addParameter("generator_alpha", DOUBLE, "1.5");
        set.addParameter("generator_beta", DOUBLE, "2.5");
        set.addParameter("generator_isVoltageDependant", BOOL, "true");
        set.addParameter("load_alpha", DOUBLE, "1.5");
        set.addParameter("load_beta", DOUBLE, "2.5");
        set.addParameter("load_isRestorative", BOOL, "false");
        set.addParameter("deactivateRootFunctions", BOOL, "true");
        set.addParameter("keepHvdcForeignNodes", BOOL, "true");
        addTapChangerDelays(set, 6000, 6000, 6000, 6000);
        return set;
    }

    /**
     * The simplified (DynaWaltz) network: the generators are held fixed while the loads restore to
     * their set point, which is what a long term voltage stability study asks of the part of the
     * network it does not model in detail, and the tap changers act on their real time constants.
     */
    public static ParametersSet dynaWaltz() {
        ParametersSet set = common(NETWORK_ID);
        set.addParameter("generator_alpha", DOUBLE, "0");
        set.addParameter("generator_beta", DOUBLE, "0");
        set.addParameter("generator_isVoltageDependant", BOOL, "false");
        set.addParameter("load_alpha", DOUBLE, "1");
        set.addParameter("load_beta", DOUBLE, "2");
        set.addParameter("load_isRestorative", BOOL, "true");
        addTapChangerDelays(set, 30, 60, 10, 10);
        return set;
    }

    /**
     * Settings of the final step of a two step simulation, where the tap changers act on their
     * real time constants instead of being held.
     */
    public static ParametersSet networkFinalStep() {
        ParametersSet set = common(NETWORK_FINAL_STEP_ID);
        set.addParameter("generator_alpha", DOUBLE, "0");
        set.addParameter("generator_beta", DOUBLE, "0");
        set.addParameter("generator_isVoltageDependant", BOOL, "false");
        set.addParameter("load_alpha", DOUBLE, "0");
        set.addParameter("load_beta", DOUBLE, "0");
        set.addParameter("load_isRestorative", BOOL, "false");
        addTapChangerDelays(set, 30, 60, 10, 10);
        return set;
    }

    private static ParametersSet common(String id) {
        ParametersSet set = new ParametersSet(id);
        set.addParameter("load_Tp", DOUBLE, "90");
        set.addParameter("load_Tq", DOUBLE, "90");
        set.addParameter("load_alphaLong", DOUBLE, "0");
        set.addParameter("load_betaLong", DOUBLE, "0");
        set.addParameter("load_isControllable", BOOL, "false");
        set.addParameter("load_zPMax", DOUBLE, "100");
        set.addParameter("load_zQMax", DOUBLE, "100");
        set.addParameter("capacitor_no_reclosing_delay", DOUBLE, "300");
        set.addParameter("reactance_no_reclosing_delay", DOUBLE, "0");
        set.addParameter("line_currentLimit_maxTimeOperation", DOUBLE, "240");
        set.addParameter("dangling_line_currentLimit_maxTimeOperation", DOUBLE, "240");
        set.addParameter("transformer_currentLimit_maxTimeOperation", DOUBLE, "240");
        set.addParameter("transformer_tolV", DOUBLE, "0.015");
        return set;
    }

    /**
     * How long a tap changer waits before its first move and between the next ones, on the
     * transmission and the sub transmission levels.
     */
    private static void addTapChangerDelays(ParametersSet set, double firstTransmission, double firstSubTransmission,
                                            double nextTransmission, double nextSubTransmission) {
        set.addParameter("transformer_t1st_THT", DOUBLE, Double.toString(firstTransmission));
        set.addParameter("transformer_t1st_HT", DOUBLE, Double.toString(firstSubTransmission));
        set.addParameter("transformer_tNext_THT", DOUBLE, Double.toString(nextTransmission));
        set.addParameter("transformer_tNext_HT", DOUBLE, Double.toString(nextSubTransmission));
    }
}
