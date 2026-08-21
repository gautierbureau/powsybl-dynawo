/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings;
import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.dynamicsimulation.DynamicModelsSupplier;
import com.powsybl.dynawo.DynawoSimulationParameters;
import com.powsybl.dynawo.mappings.parameters.DefaultNetworkParameters;
import com.powsybl.dynawo.mappings.parameters.DefaultSolverParameters;
import com.powsybl.dynawo.mappings.parameters.ModelDescriptionLookup;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Registry of the {@link DynamicMappingProvider} found on the classpath, each making one of the
 * named mappings a study is selected by.
 * <p>
 * A mapping is not held ready made: it is configured, and a caller chooses it by name and creates
 * it with the settings it takes, see {@link #create(String, MappingParameters)}. What is held here
 * is the provider that makes it, which is what carries the name and the description the mappings
 * are listed by.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public final class DynamicModelsMappings {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicModelsMappings.class);

    private static final DynamicModelsMappings INSTANCE = new DynamicModelsMappings(ServiceLoader.load(DynamicMappingProvider.class));

    /**
     * A mapping describes every equipment it recognises, including the ones a simulation cannot
     * start from: an equipment left out of the main connected component, or one whose terminals
     * are not all energised, has no operating point to initialise from and would fail the
     * simulation. Dropping their models is what makes a mapping usable on a network as it comes.
     */
    private static final Set<String> DEFAULT_SIMPLIFIERS = Set.of("energizedEquipment", "mainComponentEquipment");

    // the transient study (DynaSwing) resolves a finer step than the voltage stability one, which keeps
    // the DynawoSimulationParameters default (1e-6); the detailed flavour tightens the precision to 1e-8
    private static final double DYNA_SWING_PRECISION = 1e-8;

    private final Map<String, DynamicMappingProvider> providers = new LinkedHashMap<>();

    DynamicModelsMappings(Iterable<DynamicMappingProvider> providers) {
        providers.forEach(p -> this.providers.put(p.getName(), p));
    }

    public static DynamicModelsMappings getInstance() {
        return INSTANCE;
    }

    public Set<String> getMappingNames() {
        return providers.keySet();
    }

    /**
     * What each registered mapping is: its name and the one line it is chosen by. Meant to be
     * shown to whoever is choosing one, from a listing on the python side or a help command.
     */
    public List<MappingInfo> getMappingInfos() {
        return providers.values().stream()
                .map(p -> new MappingInfo(p.getName(), p.getDescription()))
                .toList();
    }

    private DynamicMappingProvider getProvider(String name) {
        DynamicMappingProvider provider = providers.get(name);
        if (provider == null) {
            throw new PowsyblException("Mapping " + name + " not found, available mappings are " + getMappingNames());
        }
        return provider;
    }

    /**
     * The named mapping, configured with the settings given.
     */
    public DynamicModelsMapping create(String name, MappingParameters parameters) {
        return getProvider(name).create(parameters);
    }

    /**
     * The named mapping in its default configuration, for a caller with nothing to set.
     */
    public DynamicModelsMapping getMapping(String name) {
        return create(name, MappingParameters.empty());
    }

    /**
     * The name and description of one registered mapping.
     */
    public record MappingInfo(String name, String description) {
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
        return apply(getMapping(name), network, parameters, descriptions, ReportNode.NO_OP);
    }

    /**
     * The same, saying on the report node what each equipment was given and what it asked for and
     * did not get.
     */
    public DynamicModelsSupplier apply(String name, Network network, DynawoSimulationParameters parameters,
                                       ModelDescriptionLookup descriptions, ReportNode reportNode) {
        return apply(getMapping(name), network, parameters, descriptions, reportNode);
    }

    /**
     * Applies a mapping given outright rather than by name, for one not among the registered ones:
     * a mapping built to a particular deployment, or one under test.
     */
    public DynamicModelsSupplier apply(DynamicModelsMapping mapping, Network network,
                                       DynawoSimulationParameters parameters, ModelDescriptionLookup descriptions) {
        return apply(mapping, network, parameters, descriptions, ReportNode.NO_OP);
    }

    public DynamicModelsSupplier apply(DynamicModelsMapping mapping, Network network,
                                       DynawoSimulationParameters parameters, ModelDescriptionLookup descriptions,
                                       ReportNode reportNode) {
        mapping.createExtensions(network);
        // the models are resolved first, since that is what builds the ones nothing installed
        // provides, and only then is it known whether anything was built
        DynamicModelsSupplier supplier = new MappedModelsSupplier(mapping.createModelConfigs(network, reportNode));
        mapping.createParameters(network, mapping.describeBuiltModels(descriptions))
                .forEach(parameters::addModelParameters);
        addBuiltModelsDir(mapping, parameters);
        registerBuiltModels(mapping, parameters);
        addDefaultParameters(mapping, parameters);
        return supplier;
    }

    /**
     * Tells the simulation where to find the models the mapping had to build, alongside the ones
     * Dynawo ships, so that a network needing one is runnable without anything being said about it.
     * <p>
     * The directory is named only once something is in it: a run over models that all exist builds
     * nothing, and Dynawo refuses a directory that is not there.
     */
    static void addBuiltModelsDir(DynamicModelsMapping mapping, DynawoSimulationParameters parameters) {
        mapping.getBuiltModelsDir()
                .filter(Files::isDirectory)
                .filter(DynamicModelsMappings::holdsALibrary)
                .filter(dir -> !parameters.getPrecompiledModelsDirs().contains(dir))
                .ifPresent(dir -> {
                    LOGGER.info("Models built for this network are taken from {}", dir);
                    parameters.addPrecompiledModelsDir(dir);
                });
    }

    /**
     * Tells the simulation the models the mapping built exist, so it can stand them up in the dyd
     * rather than pass them over for want of a builder. Resolving the models is what builds them,
     * so this comes after.
     */
    private static void registerBuiltModels(DynamicModelsMapping mapping, DynawoSimulationParameters parameters) {
        mapping.getBuiltModelConfigs().forEach((category, configs) ->
                configs.forEach(config -> parameters.addAdditionalModel(category, config)));
        // corrections to models the catalog dates later than they run go over the catalog, so a
        // model whose library the installation holds is not held out for being too new
        mapping.getModelConfigOverrides().forEach((category, configs) ->
                configs.forEach(config -> parameters.overrideAdditionalModel(category, config)));
    }

    private static boolean holdsALibrary(Path dir) {
        try (Stream<Path> files = Files.list(dir)) {
            return files.anyMatch(f -> f.getFileName().toString().endsWith(".so"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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
        DynawoSimulationParameters.SolverType solverType = mapping.getSolverType();
        boolean detailed = solverType == DynawoSimulationParameters.SolverType.IDA;
        if (parameters.getSolverParameters() == null) {
            parameters.setSolverType(solverType);
            parameters.setSolverParameters(detailed
                    ? DefaultSolverParameters.ida() : DefaultSolverParameters.sim());
        }
        if (parameters.getNetworkParameters() == null) {
            // the network settings follow the same flavour as the solver: the detailed models
            // (DynaSwing) want the transient network, the simplified ones (DynaWaltz) the voltage
            // stability one
            parameters.setNetworkParameters(detailed
                    ? DefaultNetworkParameters.dynaSwing() : DefaultNetworkParameters.dynaWaltz());
        }
        if (parameters.getModelSimplifiers().isEmpty()) {
            parameters.setModelSimplifiers(DEFAULT_SIMPLIFIERS);
        }
        // only tighten when still at the default, so an explicitly chosen precision is left alone
        if (detailed && parameters.getPrecision() == DynawoSimulationParameters.DEFAULT_PRECISION) {
            parameters.setPrecision(DYNA_SWING_PRECISION);
        }
    }
}
