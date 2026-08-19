/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.dynaflow;

import com.powsybl.dynawo.mappings.MappedModelsSupplier.MappedModel;
import com.powsybl.dynawo.mappings.dynaflow.DynaFlowConfig.StartingPointMode;
import com.powsybl.dynawo.parameters.ParameterType;
import com.powsybl.dynawo.parameters.ParametersSet;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.StaticVarCompensator;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.iidm.network.extensions.StandbyAutomaton;
import com.powsybl.iidm.network.extensions.VoltagePerReactivePowerControl;

import java.util.ArrayList;
import java.util.List;

/**
 * Gives every static var compensator its DynaFlow model, the way the DynaFlow Launcher's
 * {@code SVarCDefinitionAlgorithm} does — the light one of the four selection rules.
 * <p>
 * A compensator holding no voltage keeps the static {@code NETWORK} model. One that does runs the
 * {@code StaticVarCompensatorPV} family, its flavour read from the network: {@code Prop} where a slope
 * shares reactive power ({@code voltagePerReactivePowerControl}), {@code Remote} where it regulates a
 * distant bus, {@code ModeHandling} where a {@code standbyAutomaton} switches it between set points. Each
 * compensator reads a set of its own — the launcher's {@code ParSVarC}, with the common macro set inlined
 * (§9): the initial point and P/Q, the reference voltage, the nominal voltage and the susceptances in
 * per-unit, plus the flavour's Lambda / remote nominal / mode-handling thresholds.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
final class DynaFlowSvarcMapping {

    private static final ParameterType DOUBLE = ParameterType.DOUBLE;
    // the launcher's constants (ParSVarC.h): the base power and the mode-handling time thresholds
    private static final double SB = 100.0;
    private static final String T_THRESHOLD_DOWN = "0";
    private static final String T_THRESHOLD_UP = "60";

    private final DynaFlowConfig config;

    DynaFlowSvarcMapping(DynaFlowConfig config) {
        this.config = config;
    }

    List<MappedModel> createModelConfigs(Network network) {
        List<MappedModel> models = new ArrayList<>();
        for (StaticVarCompensator svarc : network.getStaticVarCompensators()) {
            String lib = selectLib(svarc);
            if (lib != null) {
                models.add(new MappedModel(lib, svarc.getId(), svarc.getId()));
            }
        }
        return models;
    }

    List<ParametersSet> createParameters(Network network) {
        List<ParametersSet> sets = new ArrayList<>();
        for (StaticVarCompensator svarc : network.getStaticVarCompensators()) {
            if (selectLib(svarc) != null) {
                sets.add(buildParameters(svarc));
            }
        }
        return sets;
    }

    /** The model a compensator runs, or {@code null} to keep the static network one. */
    private String selectLib(StaticVarCompensator svarc) {
        if (!isRegulatingVoltage(svarc)) {
            return null;
        }
        StringBuilder lib = new StringBuilder("StaticVarCompensatorPV");
        if (isProportional(svarc)) {
            lib.append("Prop");
        }
        if (isRemote(svarc)) {
            lib.append("Remote");
        }
        if (svarc.getExtension(StandbyAutomaton.class) != null) {
            lib.append("ModeHandling");
        }
        return lib.toString();
    }

    private ParametersSet buildParameters(StaticVarCompensator svarc) {
        boolean remote = isRemote(svarc);
        double uNom = nominalV(svarc.getTerminal());
        double uNomRemote = nominalV(svarc.getRegulatingTerminal());
        double uNomReference = remote ? uNomRemote : uNom;

        ParametersSet set = new ParametersSet(svarc.getId());
        // the common macro set, inlined
        if (config.startingPointMode() == StartingPointMode.WARM) {
            set.addReference("SVarC_U0Pu", DOUBLE, "v_pu");
            set.addReference("SVarC_UPhase0", DOUBLE, "angle_pu");
        } else {
            set.addParameter("SVarC_U0Pu", DOUBLE, "1.0");
            set.addParameter("SVarC_UPhase0", DOUBLE, "0");
        }
        set.addReference("SVarC_P0Pu", DOUBLE, "p_pu");
        set.addReference("SVarC_Q0Pu", DOUBLE, "q_pu");

        set.addParameter("SVarC_URef0Pu", DOUBLE, Double.toString(svarc.getVoltageSetpoint() / uNomReference));
        set.addParameter("SVarC_UNom", DOUBLE, Double.toString(uNom));
        set.addParameter("SVarC_BShuntPu", DOUBLE, Double.toString(bPu(b0(svarc), uNom)));
        set.addParameter("SVarC_BMaxPu", DOUBLE, Double.toString(bPu(svarc.getBmax(), uNom)));
        set.addParameter("SVarC_BMinPu", DOUBLE, Double.toString(bPu(svarc.getBmin(), uNom)));

        if (isProportional(svarc)) {
            double slope = svarc.getExtension(VoltagePerReactivePowerControl.class).getSlope();
            set.addParameter("SVarC_LambdaPu", DOUBLE, Double.toString(slope * SB / uNomReference));
        }
        if (remote) {
            set.addParameter("SVarC_UNomRemote", DOUBLE, Double.toString(uNomRemote));
        }
        StandbyAutomaton standbyAutomaton = svarc.getExtension(StandbyAutomaton.class);
        if (standbyAutomaton != null) {
            set.addReference("SVarC_Mode0", ParameterType.INT, "regulatingMode");
            set.addParameter("SVarC_URefDown", DOUBLE, Double.toString(standbyAutomaton.getLowVoltageSetpoint()));
            set.addParameter("SVarC_URefUp", DOUBLE, Double.toString(standbyAutomaton.getHighVoltageSetpoint()));
            set.addParameter("SVarC_UThresholdDown", DOUBLE, Double.toString(standbyAutomaton.getLowVoltageThreshold()));
            set.addParameter("SVarC_UThresholdUp", DOUBLE, Double.toString(standbyAutomaton.getHighVoltageThreshold()));
            set.addParameter("SVarC_tThresholdDown", DOUBLE, T_THRESHOLD_DOWN);
            set.addParameter("SVarC_tThresholdUp", DOUBLE, T_THRESHOLD_UP);
        }
        return set;
    }

    private static boolean isRegulatingVoltage(StaticVarCompensator svarc) {
        return svarc.isRegulating() && svarc.getRegulationMode() == StaticVarCompensator.RegulationMode.VOLTAGE;
    }

    private static boolean isRemote(StaticVarCompensator svarc) {
        Bus connectedBus = busViewBus(svarc.getTerminal());
        Bus regulatedBus = busViewBus(svarc.getRegulatingTerminal());
        return connectedBus != null && regulatedBus != null && !connectedBus.getId().equals(regulatedBus.getId());
    }

    /** A compensator that shares reactive power by a non-zero slope on the voltagePerReactivePowerControl. */
    private static boolean isProportional(StaticVarCompensator svarc) {
        VoltagePerReactivePowerControl control = svarc.getExtension(VoltagePerReactivePowerControl.class);
        return control != null && control.getSlope() != 0;
    }

    /** The standby susceptance, from the standby automaton, or zero without one. */
    private static double b0(StaticVarCompensator svarc) {
        StandbyAutomaton standbyAutomaton = svarc.getExtension(StandbyAutomaton.class);
        return standbyAutomaton != null ? standbyAutomaton.getB0() : 0;
    }

    /** A susceptance in per-unit — the launcher's {@code computeBPU}. */
    private static double bPu(double b, double uNom) {
        return b * uNom * uNom / SB;
    }

    private static double nominalV(Terminal terminal) {
        return terminal.getVoltageLevel().getNominalV();
    }

    private static Bus busViewBus(Terminal terminal) {
        return terminal == null ? null : terminal.getBusView().getBus();
    }
}
