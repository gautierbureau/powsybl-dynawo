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
import com.powsybl.iidm.network.EnergySource;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.extensions.ActivePowerControl;

/**
 * Builds a generator's parameter set, the way the DynaFlow Launcher's {@code ParGenerator} does — the
 * IIDM references and fixed values a {@code GeneratorPV*SignalN} model reads.
 * <p>
 * Two shapes, as the launcher has two ({@code writeConstantGeneratorsSets} vs the diagram branch of
 * {@code buildGeneratorMacroParameterSet}, which §9 inlines instead of factoring into a
 * macro-parameter-set): on infinite reactive limits a machine reads the constant set — infinite Q and P
 * limits, warm/flat start, {@code PNom}, {@code URef0}/{@code URef0Pu} by its shared set id; otherwise it
 * reads its diagram set — {@code pMin}/{@code pMax}, the initial point, the Q limits and voltage set
 * point. Either way a transformer adds {@code QNomAlt}/{@code SNom} + {@code XTfoPu} (0.1426 nuclear), and
 * a remote machine its regulated bus's voltage {@code URegulated0}.
 * <p>
 * The reactive-power-control-loop tunings the launcher reads from its setting database, and a PQ-curve
 * machine's {@code QMax/QMinTableFile} (the Diagram files), are not network data and are left for later.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
final class DynaFlowGeneratorParameters {

    private static final ParameterType DOUBLE = ParameterType.DOUBLE;

    private static final String DEAD_BAND = "0.0001";
    private static final String K_GOVER_OFF = "0";
    private static final String K_GOVER_DEFAULT = "1";
    private static final String X_TFO = "0.1228";
    private static final String X_TFO_NUCLEAR = "0.1426";
    // the launcher's powerValueMax = the largest double, meaning an infinite limit
    private static final String PLUS_INFINITE = Double.toString(Double.MAX_VALUE);
    private static final String MINUS_INFINITE = Double.toString(-Double.MAX_VALUE);

    // the two shared set ids whose voltage reference the launcher wires differently (updateSignalNGenerator)
    private static final String REMOTE_SET = "remoteVControl";
    private static final String REMOTE_NUCLEAR_SET = "remoteVControl_Nuc";
    private static final String PROP_SET = "propSignalNGenerator";

    private DynaFlowGeneratorParameters() {
    }

    static ParametersSet build(Generator generator, String lib, String parameterSetId, DynaFlowConfig config) {
        boolean rectangular = !lib.contains("DiagramPQ");
        boolean transformer = lib.contains("Tfo");
        boolean rpcl = lib.contains("Rpcl");
        boolean remote = lib.contains("Remote");
        boolean prop = lib.contains("Prop");
        boolean nuclear = generator.getEnergySource() == EnergySource.NUCLEAR;
        boolean fixedP = generator.getTargetP() == 0;
        boolean activePowerControl = generator.getExtension(ActivePowerControl.class) != null;

        ParametersSet set = new ParametersSet(parameterSetId);
        if (config.infiniteReactiveLimits()) {
            buildInfinite(set, parameterSetId, prop, config, activePowerControl, fixedP);
        } else {
            buildDiagram(set, rectangular, prop, remote, config, activePowerControl, fixedP);
        }

        // the launcher's per-generator additions: a transformer's reactance and off-set references, and a
        // remote machine's regulated bus voltage
        if (transformer || rpcl) {
            addIfAbsent(set, "generator_QNomAlt", "qNom");
            addIfAbsent(set, "generator_SNom", "sNom");
        }
        if (transformer) {
            set.addParameter("generator_XTfoPu", DOUBLE, nuclear ? X_TFO_NUCLEAR : X_TFO);
        }
        if (remote) {
            set.addReference("generator_URegulated0", DOUBLE, "U", regulatedBusId(generator));
        }
        return set;
    }

    /** The launcher's {@code buildGeneratorMacroParameterSet} diagram branch, inlined. */
    private static void buildDiagram(ParametersSet set, boolean rectangular, boolean prop, boolean remote,
                                     DynaFlowConfig config, boolean activePowerControl, boolean fixedP) {
        set.addReference("generator_PMin", DOUBLE, "pMin");
        set.addReference("generator_PMax", DOUBLE, "pMax");
        addStartingPoint(set, config.startingPointMode());
        set.addReference("generator_PRef0Pu", DOUBLE, "targetP_pu");
        setKGover(set, activePowerControl, fixedP);
        switch (config.activePowerCompensation()) {
            case P -> set.addReference("generator_PNom", DOUBLE, "p_pu");
            case TARGET_P -> set.addReference("generator_PNom", DOUBLE, "targetP_pu");
            case PMAX -> set.addReference("generator_PNom", DOUBLE, "pMax_pu");
        }
        if (!rectangular) {
            set.addReference("generator_QMin0", DOUBLE, "qMin");
            set.addReference("generator_QMax0", DOUBLE, "qMax");
        }
        set.addParameter("generator_QDeadBandPu", DOUBLE, DEAD_BAND);
        set.addParameter("generator_UDeadBandPu", DOUBLE, DEAD_BAND);
        if (prop) {
            set.addReference("generator_QRef0Pu", DOUBLE, "targetQ_pu");
            set.addReference("generator_QPercent", DOUBLE, "qMax_pu");
        } else if (remote) {
            set.addReference("generator_URef0", DOUBLE, "targetV");
        }
        if (rectangular) {
            set.addReference("generator_QMin", DOUBLE, "qMin");
            set.addReference("generator_QMax", DOUBLE, "qMax");
            set.addReference("generator_URef0Pu", DOUBLE, "targetV_pu");
        } else if (!prop && !remote) {
            set.addReference("generator_URef0Pu", DOUBLE, "targetV_pu");
        }
    }

    /** The launcher's {@code updateSignalNGenerator} — the constant set an infinite machine reads. */
    private static void buildInfinite(ParametersSet set, String parameterSetId, boolean prop,
                                      DynaFlowConfig config, boolean activePowerControl, boolean fixedP) {
        setKGover(set, activePowerControl, fixedP);
        set.addParameter("generator_QMin", DOUBLE, MINUS_INFINITE);
        set.addParameter("generator_QMax", DOUBLE, PLUS_INFINITE);
        set.addParameter("generator_PMin", DOUBLE, MINUS_INFINITE);
        set.addParameter("generator_PMax", DOUBLE, PLUS_INFINITE);
        set.addParameter("generator_QDeadBandPu", DOUBLE, DEAD_BAND);
        set.addParameter("generator_UDeadBandPu", DOUBLE, DEAD_BAND);
        switch (config.activePowerCompensation()) {
            case P, PMAX -> set.addReference("generator_PNom", DOUBLE, "p_pu");
            case TARGET_P -> set.addReference("generator_PNom", DOUBLE, "targetP_pu");
        }
        addStartingPoint(set, config.startingPointMode());
        set.addReference("generator_PRef0Pu", DOUBLE, "targetP_pu");
        if (parameterSetId.equals(REMOTE_SET) || parameterSetId.equals(REMOTE_NUCLEAR_SET)) {
            set.addReference("generator_URef0", DOUBLE, "targetV");
        } else if (!parameterSetId.equals(PROP_SET)) {
            set.addReference("generator_URef0Pu", DOUBLE, "targetV_pu");
        }
        if (prop) {
            set.addReference("generator_QRef0Pu", DOUBLE, "targetQ_pu");
            set.addReference("generator_QPercent", DOUBLE, "qMax_pu");
        }
    }

    private static void addStartingPoint(ParametersSet set, StartingPointMode startingPointMode) {
        if (startingPointMode == StartingPointMode.WARM) {
            set.addReference("generator_P0Pu", DOUBLE, "p_pu");
            set.addReference("generator_Q0Pu", DOUBLE, "q_pu");
            set.addReference("generator_U0Pu", DOUBLE, "v_pu");
            set.addReference("generator_UPhase0", DOUBLE, "angle_pu");
        } else {
            set.addReference("generator_P0Pu", DOUBLE, "targetP_pu");
            set.addReference("generator_Q0Pu", DOUBLE, "targetQ_pu");
            set.addParameter("generator_U0Pu", DOUBLE, "1.0");
            set.addParameter("generator_UPhase0", DOUBLE, "0");
        }
    }

    /** The governor gain: off when the machine holds no active power, referenced under active power control, else on. */
    private static void setKGover(ParametersSet set, boolean activePowerControl, boolean fixedP) {
        if (fixedP) {
            set.addParameter("generator_KGover", DOUBLE, K_GOVER_OFF);
        } else if (activePowerControl) {
            set.addReference("generator_KGover", DOUBLE, "kGover");
        } else {
            set.addParameter("generator_KGover", DOUBLE, K_GOVER_DEFAULT);
        }
    }

    private static void addIfAbsent(ParametersSet set, String name, String origName) {
        if (!set.getReferences().containsKey(name)) {
            set.addReference(name, DOUBLE, origName);
        }
    }

    private static String regulatedBusId(Generator generator) {
        return generator.getRegulatingTerminal().getBusView().getBus().getId();
    }
}
