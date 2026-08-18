/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.dynaflow;

import com.powsybl.dynawo.mappings.MappedModelsSupplier.MappedModel;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;

import java.util.ArrayList;
import java.util.List;

/**
 * Gives every generator its DynaFlow model, the way the DynaFlow Launcher's
 * {@code GeneratorDefinitionAlgorithm} does.
 * <p>
 * <strong>Scaffold — the start of the DynaFlow generator rule.</strong> A generator is modelled only
 * where it holds a voltage on a valid operating point, and — for now — on the plain
 * {@code GeneratorPVSignalN}, the infinite-reactive-limits PV model on the shared frequency signal.
 * The full decision tree (the diagram, transformer, remote, proportional and reactive-power-control
 * variants of {@code GeneratorPV*SignalN} / {@code GeneratorPQProp*SignalN}) and its config knobs are
 * Phase 1; the remote variants additionally instantiate a {@code VRRemote} model per regulated bus,
 * which is the Phase 0a prerequisite. A generator the rule leaves out keeps the static network model.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public final class DynaFlowGeneratorMapping {

    /** The plain PV model on the SignalN frequency signal, the infinite-reactive-limits case. */
    static final String SIGNALN_INFINITE = "GeneratorPVSignalN";

    /** The set the plain infinite model reads, shared by every generator the rule gives it. */
    static final String SIGNALN_GENERATOR_SET = "signalNGenerator";

    public List<MappedModel> createModelConfigs(Network network) {
        List<MappedModel> models = new ArrayList<>();
        for (Generator generator : network.getGenerators()) {
            if (isModelled(generator)) {
                models.add(new MappedModel(SIGNALN_INFINITE, generator.getId(), SIGNALN_GENERATOR_SET));
            }
        }
        return models;
    }

    /**
     * Whether a generator holds a voltage on a valid operating point, so it runs a dynamic model
     * rather than the static network one — the DynaFlow Launcher's top gate (target P valid, voltage
     * regulation on, diagram valid; the diagram is always valid on infinite reactive limits).
     */
    private static boolean isModelled(Generator generator) {
        return generator.isVoltageRegulatorOn() && isTargetPValid(generator);
    }

    /** The operating point {@code -targetP} lies strictly inside the active power limits. */
    private static boolean isTargetPValid(Generator generator) {
        double operatingPoint = -generator.getTargetP();
        return operatingPoint > generator.getMinP() && operatingPoint < generator.getMaxP();
    }
}
