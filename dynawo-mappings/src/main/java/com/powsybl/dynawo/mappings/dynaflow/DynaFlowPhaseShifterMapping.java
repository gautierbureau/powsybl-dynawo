/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.dynaflow;

import com.powsybl.dynamicsimulation.DynamicModel;
import com.powsybl.dynawo.builders.ModelBuilder;
import com.powsybl.dynawo.mappings.MappedModelsSupplier.MappedModel;
import com.powsybl.dynawo.models.automationsystems.phaseshifters.PhaseShifterIAutomationSystemBuilder;
import com.powsybl.dynawo.models.automationsystems.phaseshifters.PhaseShifterPAutomationSystemBuilder;
import com.powsybl.dynawo.parameters.ParametersSet;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.PhaseTapChanger;
import com.powsybl.iidm.network.TwoWindingsTransformer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Gives each phase-shifting transformer its phase-shifter automaton — deduced from the network, since a
 * phase shifter is a transformer with a regulating phase tap changer that {@code PhaseShifterI} drives on a
 * current or {@code PhaseShifterP} on an active power flow.
 * <p>
 * <strong>Disabled by default.</strong> The DynaFlow Launcher does not deduce phase shifters: it adds only
 * the ones its assembling database names, so deducing every regulating one over-produces against its
 * reference (a network can hold regulating phase shifters the launcher leaves alone). This mapping is kept
 * for when a study opts into it — {@code dynaflow_phase_shifter_regulation_on} — or an assembling-equivalent
 * selects which transformers to model, but stays off so the mapping matches the launcher.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
final class DynaFlowPhaseShifterMapping {

    /** The phase-shifter automaton limiting a current. */
    static final String CURRENT_LIB = "PhaseShifterI";

    /** The phase-shifter automaton controlling an active power flow. */
    static final String POWER_LIB = "PhaseShifterP";

    List<MappedModel> createModelConfigs(Network network) {
        List<MappedModel> models = new ArrayList<>();
        for (TwoWindingsTransformer transformer : network.getTwoWindingsTransformers()) {
            if (isRegulatingPhaseShifter(transformer)) {
                String lib = transformer.getPhaseTapChanger().getRegulationMode() == PhaseTapChanger.RegulationMode.CURRENT_LIMITER
                        ? CURRENT_LIB : POWER_LIB;
                String id = modelId(transformer);
                models.add(new MappedModel(lib, id, id, configurer(lib, id, transformer.getId())));
            }
        }
        return models;
    }

    /**
     * The set each phase-shifter automaton reads. The model fills it with references to its transformer at
     * build time; its fixed settings (deadband, sample time) come from a settings database, as in the launcher.
     */
    List<ParametersSet> createParameters(Network network) {
        List<ParametersSet> sets = new ArrayList<>();
        for (TwoWindingsTransformer transformer : network.getTwoWindingsTransformers()) {
            if (isRegulatingPhaseShifter(transformer)) {
                sets.add(new ParametersSet(modelId(transformer)));
            }
        }
        return sets;
    }

    private static boolean isRegulatingPhaseShifter(TwoWindingsTransformer transformer) {
        PhaseTapChanger phaseTapChanger = transformer.getPhaseTapChanger();
        return phaseTapChanger != null && phaseTapChanger.isRegulating();
    }

    private static String modelId(TwoWindingsTransformer transformer) {
        return "PhaseShifter_" + transformer.getId();
    }

    private static Consumer<ModelBuilder<DynamicModel>> configurer(String lib, String id, String transformerId) {
        return builder -> {
            if (CURRENT_LIB.equals(lib)) {
                PhaseShifterIAutomationSystemBuilder phaseShifterBuilder = (PhaseShifterIAutomationSystemBuilder) builder;
                phaseShifterBuilder.dynamicModelId(id);
                phaseShifterBuilder.parameterSetId(id);
                phaseShifterBuilder.transformer(transformerId);
            } else {
                PhaseShifterPAutomationSystemBuilder phaseShifterBuilder = (PhaseShifterPAutomationSystemBuilder) builder;
                phaseShifterBuilder.dynamicModelId(id);
                phaseShifterBuilder.parameterSetId(id);
                phaseShifterBuilder.transformer(transformerId);
            }
        };
    }
}
