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
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;

import java.util.ArrayList;
import java.util.List;

/**
 * Gives every load its DynaFlow model, the way the DynaFlow Launcher's {@code LoadDefinitionAlgorithm}
 * does — the trivial one of the four selection rules.
 * <p>
 * A load runs the restorative model {@code DYNModelLoadRestorativeWithLimits}, unless it is left on the
 * static {@code NETWORK} model: a fictitious load a study does not restore, a load below the distribution
 * (DSO) voltage, or one injecting nothing ({@code p0} and {@code q0} both zero). Every modelled load
 * reads one shared parameter set, {@code GenericRestorativeLoad}, its fixed restorative coefficients
 * inlined and its initial point taken from the network.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
final class DynaFlowLoadMapping {

    /** The restorative load model, on the shared parameter set below. */
    static final String LIB = "DYNModelLoadRestorativeWithLimits";

    /** The one parameter set every modelled load reads (the launcher's {@code loadParId}). */
    static final String PARAMETER_SET = "GenericRestorativeLoad";

    private static final ParameterType DOUBLE = ParameterType.DOUBLE;
    private static final double EPSILON = 1e-6;

    private final DynaFlowConfig config;

    DynaFlowLoadMapping(DynaFlowConfig config) {
        this.config = config;
    }

    List<MappedModel> createModelConfigs(Network network) {
        List<MappedModel> models = new ArrayList<>();
        for (Load load : network.getLoads()) {
            if (isModelled(load)) {
                models.add(new MappedModel(LIB, load.getId(), PARAMETER_SET));
            }
        }
        return models;
    }

    List<ParametersSet> createParameters(Network network) {
        return network.getLoadStream().anyMatch(this::isModelled)
                ? List.of(buildParameters())
                : List.of();
    }

    /**
     * Whether a load runs the restorative model rather than the static {@code NETWORK} one: not a
     * fictitious load left static, not below the distribution voltage, and injecting something.
     */
    private boolean isModelled(Load load) {
        boolean fictitiousLeftStatic = !config.restorativeFictitiousLoads() && load.isFictitious();
        double nominalV = load.getTerminal().getVoltageLevel().getNominalV();
        boolean belowDistributionVoltage = nominalV < config.dsoVoltageLevel()
                && Math.abs(nominalV - config.dsoVoltageLevel()) > EPSILON;
        boolean notInjecting = Math.abs(load.getP0()) < EPSILON && Math.abs(load.getQ0()) < EPSILON;
        return !fictitiousLeftStatic && !belowDistributionVoltage && !notInjecting;
    }

    /** The launcher's {@code writeConstantLoadsSet}: fixed restorative coefficients and the initial point. */
    private ParametersSet buildParameters() {
        ParametersSet set = new ParametersSet(PARAMETER_SET);
        set.addParameter("load_Alpha", DOUBLE, "1.5");
        set.addParameter("load_Beta", DOUBLE, "2.5");
        set.addParameter("load_UMax0Pu", DOUBLE, "1.15");
        set.addParameter("load_UMin0Pu", DOUBLE, "0.85");
        set.addParameter("load_UDeadBandPu", DOUBLE, "0.01");
        set.addParameter("load_tFilter", DOUBLE, "10");
        set.addReference("load_P0Pu", DOUBLE, "p0_pu");
        set.addReference("load_Q0Pu", DOUBLE, "q0_pu");
        if (config.startingPointMode() == StartingPointMode.WARM) {
            set.addReference("load_U0Pu", DOUBLE, "v_pu");
            set.addReference("load_UPhase0", DOUBLE, "angle_pu");
        } else {
            set.addParameter("load_U0Pu", DOUBLE, "1.0");
            set.addParameter("load_UPhase0", DOUBLE, "0");
        }
        return set;
    }
}
