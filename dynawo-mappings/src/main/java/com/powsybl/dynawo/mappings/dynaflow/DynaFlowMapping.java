/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.dynaflow;

import com.powsybl.dynawo.DynawoSimulationParameters;
import com.powsybl.dynawo.mappings.DynamicModelsMapping;
import com.powsybl.dynawo.mappings.MappedModelsSupplier;
import com.powsybl.dynawo.mappings.parameters.ModelDescriptionLookup;
import com.powsybl.dynawo.parameters.ParametersSet;
import com.powsybl.iidm.network.Network;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A whole network described for a DynaFlow steady-state study, each kind of equipment by its own part.
 * <p>
 * This is the Java replacement for the DynaFlow Launcher's model creation (see {@code
 * DYNAFLOW_MAPPING_PLAN.md}): given a network, it decides the DynaFlow model each equipment runs, so a
 * study reads them from {@code get_models} and, later, feeds them to DynaFlow instead of shelling out
 * to the C++ launcher. Unlike a transient study, DynaFlow deduces a machine's model from the network
 * itself rather than from an extension a study describes beforehand, so it reads no extensions.
 * <p>
 * <strong>Scaffold.</strong> Only the generators are wired, and only their plain PV model; the loads,
 * static var compensators, HVDC links and the global scaffolding (slack, the SignalN frequency signal,
 * {@code Network.par} / {@code solver.par}) are the phases the plan lays out.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public class DynaFlowMapping implements DynamicModelsMapping {

    public static final String NAME = "DynaFlow";

    private final String name;
    private final DynaFlowGeneratorMapping generators;
    private final DynaFlowLoadMapping loads;
    private final DynaFlowSvarcMapping staticVarCompensators;
    private final DynaFlowSvcMapping secondaryVoltageControls;

    public DynaFlowMapping(String name) {
        this(name, DynaFlowConfig.defaults());
    }

    public DynaFlowMapping(String name, DynaFlowConfig config) {
        this.name = Objects.requireNonNull(name);
        this.generators = new DynaFlowGeneratorMapping(config);
        this.loads = new DynaFlowLoadMapping(config);
        this.staticVarCompensators = new DynaFlowSvarcMapping(config);
        this.secondaryVoltageControls = new DynaFlowSvcMapping();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public DynawoSimulationParameters.SolverType getSolverType() {
        // DynaFlow runs the simplified (SIM) solver
        return DynawoSimulationParameters.SolverType.SIM;
    }

    @Override
    public void createExtensions(Network network) {
        // DynaFlow reads a machine's model from the network, not from an extension a study sets up
    }

    @Override
    public List<MappedModelsSupplier.MappedModel> createModelConfigs(Network network) {
        List<MappedModelsSupplier.MappedModel> models = new ArrayList<>(generators.createModelConfigs(network));
        models.addAll(loads.createModelConfigs(network));
        models.addAll(staticVarCompensators.createModelConfigs(network));
        models.addAll(secondaryVoltageControls.createModelConfigs(network));
        return models;
    }

    @Override
    public List<ParametersSet> createParameters(Network network, ModelDescriptionLookup descriptions) {
        // each generator's set of fixed values + IIDM references (ParGenerator), plus the loads' shared set
        List<ParametersSet> sets = new ArrayList<>(generators.createParameters(network));
        sets.addAll(loads.createParameters(network));
        sets.addAll(staticVarCompensators.createParameters(network));
        return sets;
    }
}
