/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dynamicsimulation.DynamicModelsSupplier;
import com.powsybl.dynawo.DynawoSimulationParameters;
import com.powsybl.dynawo.mappings.parameters.DefaultNetworkParameters;
import com.powsybl.dynawo.mappings.parameters.DefaultSolverParameters;
import com.powsybl.dynawo.mappings.parameters.ModelDescriptionLookup;
import com.powsybl.iidm.network.Network;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * Registry of the {@link DynamicModelsMapping} found on the classpath.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public final class DynamicModelsMappings {

    private static final DynamicModelsMappings INSTANCE = new DynamicModelsMappings(ServiceLoader.load(DynamicModelsMapping.class));

    /**
     * A mapping describes every equipment it recognises, including the ones a simulation cannot
     * start from: an equipment left out of the main connected component, or one whose terminals
     * are not all energised, has no operating point to initialise from and would fail the
     * simulation. Dropping their models is what makes a mapping usable on a network as it comes.
     */
    private static final Set<String> DEFAULT_SIMPLIFIERS = Set.of("energizedEquipment", "mainComponentEquipment");

    private final Map<String, DynamicModelsMapping> mappings = new LinkedHashMap<>();

    DynamicModelsMappings(Iterable<DynamicModelsMapping> mappings) {
        mappings.forEach(m -> this.mappings.put(m.getName(), m));
    }

    public static DynamicModelsMappings getInstance() {
        return INSTANCE;
    }

    public Set<String> getMappingNames() {
        return mappings.keySet();
    }

    public DynamicModelsMapping getMapping(String name) {
        DynamicModelsMapping mapping = mappings.get(name);
        if (mapping == null) {
            throw new PowsyblException("Mapping " + name + " not found, available mappings are " + getMappingNames());
        }
        return mapping;
    }

    /**
     * Applies the named mapping to the network and returns the models supplier a simulation
     * expects, creating the missing extensions on the way.
     * <p>
     * The models are left without their parameters, use
     * {@link #apply(String, Network, DynawoSimulationParameters)} to get a runnable simulation.
     */
    public DynamicModelsSupplier apply(String name, Network network) {
        DynamicModelsMapping mapping = getMapping(name);
        mapping.createExtensions(network);
        return new MappedModelsSupplier(mapping.createModelConfigs(network));
    }

    /**
     * Applies the named mapping to the network and feeds the parameter sets it generates to the
     * simulation parameters, so that running the returned supplier needs nothing else.
     *
     * @param descriptions where the parameters each model declares are read from, see
     *                     {@link ModelDescriptionLookup#fromDynawo}. The Dynawo version is needed
     *                     to know which models the installation supports, and it is only known
     *                     once Dynawo has been asked for it, so the caller provides the lookup.
     */
    public DynamicModelsSupplier apply(String name, Network network, DynawoSimulationParameters parameters,
                                       ModelDescriptionLookup descriptions) {
        DynamicModelsMapping mapping = getMapping(name);
        mapping.createExtensions(network);
        DynamicModelsSupplier supplier = new MappedModelsSupplier(mapping.createModelConfigs(network));
        mapping.createParameters(network, descriptions).forEach(parameters::addModelParameters);
        addDefaultParameters(mapping, parameters);
        return supplier;
    }

    /**
     * Gives the simulation solver and network settings when it has none, so that a mapped network
     * is runnable as is. What the user configured is left untouched.
     * <p>
     * The solver comes from the mapping rather than from the simulation defaults, since it follows
     * from the models: the simplified ones run with the fixed time step solver, the detailed ones
     * with the variable time step one.
     */
    private static void addDefaultParameters(DynamicModelsMapping mapping, DynawoSimulationParameters parameters) {
        if (parameters.getSolverParameters() == null) {
            DynawoSimulationParameters.SolverType solverType = mapping.getSolverType();
            parameters.setSolverType(solverType);
            parameters.setSolverParameters(solverType == DynawoSimulationParameters.SolverType.IDA
                    ? DefaultSolverParameters.ida() : DefaultSolverParameters.sim());
        }
        if (parameters.getNetworkParameters() == null) {
            parameters.setNetworkParameters(DefaultNetworkParameters.network());
        }
        if (parameters.getModelSimplifiers().isEmpty()) {
            parameters.setModelSimplifiers(DEFAULT_SIMPLIFIERS);
        }
    }
}
