/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.dynaflow;

import com.powsybl.dynawo.parameters.ParameterType;
import com.powsybl.dynawo.parameters.ParametersSet;
import com.powsybl.iidm.network.EnergySource;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.extensions.ActivePowerControl;

/**
 * Builds a generator's parameter set, the way the DynaFlow Launcher's {@code ParGenerator} does — the
 * IIDM references and fixed values a {@code GeneratorPV*SignalN} model reads.
 * <p>
 * The launcher shares value-keyed sets for the infinite models and per-generator {@code uuid} sets, with
 * macro-parameter-sets factored out; here (see {@code DYNAFLOW_MAPPING_PLAN.md} §9) every generator gets
 * its own plain set with the values inlined, named after the generator. DynaFlow honours each machine's
 * diagram (never infinite reactive limits), so every modelled machine is a diagram variant — rectangular
 * or a PQ curve — and the set is the launcher's {@code buildGeneratorMacroParameterSet} diagram branch
 * inlined, plus the transformer, remote-regulation and reactive-power-control-loop additions.
 * <p>
 * The starting point is warm and the active power compensation PMax, the launcher's defaults. The
 * per-machine reactive-power-control-loop tunings (the {@code reactivePowerControlLoop_*} values the
 * launcher reads from its setting database) are not network data and are left to a reference parameter
 * file; and a PQ-curve machine's {@code QMax/QMinTableFile} — the Diagram files — is the next step.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
final class DynaFlowGeneratorParameters {

    private static final ParameterType DOUBLE = ParameterType.DOUBLE;

    // the launcher's fixed values (OutputsConstants.h): the reactive/voltage dead bands, the governor gain
    // off / default / referenced, and the fictitious transformer reactance, higher on a nuclear unit.
    private static final String DEAD_BAND = "0.0001";
    private static final String K_GOVER_OFF = "0";
    private static final String K_GOVER_DEFAULT = "1";
    private static final String X_TFO = "0.1228";
    private static final String X_TFO_NUCLEAR = "0.1426";

    private DynaFlowGeneratorParameters() {
    }

    static ParametersSet build(Generator generator, String lib) {
        boolean rectangular = !lib.contains("DiagramPQ");
        boolean transformer = lib.contains("Tfo");
        boolean rpcl = lib.contains("Rpcl");
        boolean remote = lib.contains("Remote");
        boolean prop = lib.contains("Prop");
        boolean nuclear = generator.getEnergySource() == EnergySource.NUCLEAR;
        boolean fixedP = generator.getTargetP() == 0;
        boolean activePowerControl = generator.getExtension(ActivePowerControl.class) != null;

        ParametersSet set = new ParametersSet(generator.getId());

        // the diagram branch of buildGeneratorMacroParameterSet, inlined (warm start, PMax compensation)
        set.addReference("generator_PMin", DOUBLE, "pMin");
        set.addReference("generator_PMax", DOUBLE, "pMax");
        set.addReference("generator_P0Pu", DOUBLE, "p_pu");
        set.addReference("generator_Q0Pu", DOUBLE, "q_pu");
        set.addReference("generator_U0Pu", DOUBLE, "v_pu");
        set.addReference("generator_UPhase0", DOUBLE, "angle_pu");
        set.addReference("generator_PRef0Pu", DOUBLE, "targetP_pu");
        setKGover(set, activePowerControl, fixedP);
        set.addReference("generator_PNom", DOUBLE, "pMax_pu");
        if (!rectangular) {
            set.addReference("generator_QMin0", DOUBLE, "qMin");
            set.addReference("generator_QMax0", DOUBLE, "qMax");
        }
        set.addParameter("generator_QDeadBandPu", DOUBLE, DEAD_BAND);
        set.addParameter("generator_UDeadBandPu", DOUBLE, DEAD_BAND);

        // the model-specific block: proportional sharing, remote regulation, or the plain voltage set point
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
