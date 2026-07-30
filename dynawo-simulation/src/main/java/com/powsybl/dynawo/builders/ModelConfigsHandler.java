/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com/)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.builders;

import com.google.common.collect.Lists;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.dynamicsimulation.DynamicModel;
import com.powsybl.dynamicsimulation.EventModel;
import com.powsybl.dynawo.commons.DynawoVersion;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
 */
public final class ModelConfigsHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModelConfigsHandler.class);
    private static final ModelConfigsHandler INSTANCE = new ModelConfigsHandler();

    private final Map<String, ModelConfigs> modelConfigsCat = new HashMap<>();
    private final List<BuilderConfig> builderConfigs;
    private final Map<String, BuilderConfig.ModelBuilderConstructor> builderConstructorByName = new HashMap<>();
    private final List<EventBuilderConfig> eventBuilderConfigs;
    private final Map<String, EventBuilderConfig.EventModelBuilderConstructor> eventBuilderConstructorByName;
    private final CatalogSnapshot baseCatalog;

    private ModelConfigsHandler() {
        List<ModelConfigLoader> modelConfigLoaders = Lists.newArrayList(ServiceLoader.load(ModelConfigLoader.class));
        modelConfigLoaders.forEach(l -> l.loadModelConfigs().forEach(
                (cat, modelsMap) -> modelConfigsCat.merge(cat, modelsMap, (configs1, configs2) -> {
                    configs1.addModelConfigs(configs2);
                    return configs1;
                })
        ));
        builderConfigs = modelConfigLoaders.stream()
                .flatMap(ModelConfigLoader::loadBuilderConfigs)
                .sorted(Comparator.comparing(BuilderConfig::getCategory))
                .toList();
        builderConfigs.forEach(bc -> modelConfigsCat.get(bc.getCategory()).getModelsName()
                .forEach(lib -> builderConstructorByName.put(lib, bc.getBuilderConstructor())));
        eventBuilderConfigs = modelConfigLoaders.stream()
                .flatMap(ModelConfigLoader::loadEventBuilderConfigs)
                .sorted(Comparator.comparing(e -> e.getEventModelInfo().name()))
                .toList();
        eventBuilderConstructorByName = eventBuilderConfigs.stream()
                .collect(Collectors.toMap(e -> e.getEventModelInfo().name(), EventBuilderConfig::getBuilderConstructor));
        // the catalog as the loaders leave it, before any run adds to it, to reset to per study
        baseCatalog = capture();
    }

    public static ModelConfigsHandler getInstance() {
        return INSTANCE;
    }

    public ModelConfigs getModelConfigs(String categoryName) {
        return modelConfigsCat.get(categoryName);
    }

    public List<BuilderConfig> getBuilderConfigs() {
        return builderConfigs;
    }

    public List<EventBuilderConfig> getEventBuilderConfigs() {
        return eventBuilderConfigs;
    }

    public ModelBuilder<DynamicModel> getModelBuilder(Network network, String modelName, ReportNode reportNode) {
        BuilderConfig.ModelBuilderConstructor constructor = builderConstructorByName.get(modelName);
        if (constructor == null) {
            BuilderReports.reportBuilderNotFound(reportNode, modelName);
            return null;
        }
        return constructor.createBuilder(network, modelName, reportNode);
    }

    public ModelBuilder<EventModel> getEventModelBuilder(Network network, String modelName, ReportNode reportNode) {
        EventBuilderConfig.EventModelBuilderConstructor constructor = eventBuilderConstructorByName.get(modelName);
        if (constructor == null) {
            BuilderReports.reportBuilderNotFound(reportNode, modelName);
            return null;
        }
        return constructor.createBuilder(network, reportNode);
    }

    public void addModels(AdditionalModelConfigLoader additionalModelsLoader) {
        additionalModelsLoader.loadModelConfigs().forEach((cat, modelsMap) -> mergeModelConfigs(cat, modelsMap, false));
    }

    /**
     * Registers additional model configurations programmatically, without going through a JSON file.
     * The models are provided by category name (the same categories used in the models.json catalog);
     * models declared in an unknown category are skipped, and models overwriting an existing one are ignored.
     *
     * @param modelConfigsByCategory the additional model configurations grouped by category name
     */
    public void addModels(Map<String, List<ModelConfig>> modelConfigsByCategory) {
        modelConfigsByCategory.forEach((cat, modelConfigList) ->
                mergeModelConfigs(cat, toModelConfigs(modelConfigList), false));
    }

    /**
     * Registers configurations that stand in for ones already there, correcting them rather than
     * adding beside them. Where {@link #addModels} keeps a name already present, this replaces it,
     * for a caller that means to change a model's configuration, its version among the reasons.
     */
    public void overrideModels(Map<String, List<ModelConfig>> modelConfigsByCategory) {
        modelConfigsByCategory.forEach((cat, modelConfigList) ->
                mergeModelConfigs(cat, toModelConfigs(modelConfigList), true));
    }

    private static ModelConfigs toModelConfigs(List<ModelConfig> modelConfigList) {
        SortedMap<String, ModelConfig> modelConfigMap = new TreeMap<>();
        modelConfigList.forEach(modelConfig -> modelConfigMap.put(modelConfig.name(), modelConfig));
        return new ModelConfigs(modelConfigMap, null);
    }

    private void mergeModelConfigs(String cat, ModelConfigs modelsMap, boolean override) {
        ModelConfigs currentModelConfigs = modelConfigsCat.get(cat);
        if (currentModelConfigs != null) {
            if (override) {
                currentModelConfigs.overrideModelConfigs(modelsMap);
            } else {
                currentModelConfigs.addModelConfigs(modelsMap);
            }
            BuilderConfig.ModelBuilderConstructor constructor = builderConfigs.stream()
                        .filter(bc -> bc.getCategory().equals(cat))
                        .map(BuilderConfig::getBuilderConstructor)
                        .findFirst()
                        .orElse(null);
            modelsMap.getModelsName().forEach(lib -> builderConstructorByName.put(lib, constructor));
        } else {
            LOGGER.warn("Category {} not found, the additional models under this category will be skipped", cat);
        }
    }

    /**
     * Opens a scope over the runtime model registrations. The catalog as it stands is captured now;
     * closing the scope puts it back, dropping every model {@link #addModels} or {@link #overrideModels}
     * registered while it was open. A run registers the models a mapping built inside such a scope,
     * so a later run in the same long-lived process does not read them as installed — the handler is
     * a JVM-wide singleton that nothing else takes models back out of.
     */
    public Scope openScope() {
        return new Scope();
    }

    /**
     * Drops every runtime registration, putting the catalog back as the loaders left it. Where a
     * {@link Scope} restores to a point in time, this restores to the base catalog outright, for a
     * study that means to start from what Dynawo ships rather than from whatever an earlier study in
     * the same process left behind.
     */
    public void resetToBase() {
        restore(baseCatalog);
    }

    private CatalogSnapshot capture() {
        Map<String, ModelConfigs.Snapshot> categorySnapshots = new HashMap<>();
        modelConfigsCat.forEach((cat, configs) -> categorySnapshots.put(cat, configs.snapshot()));
        return new CatalogSnapshot(categorySnapshots, new HashMap<>(builderConstructorByName));
    }

    private void restore(CatalogSnapshot snapshot) {
        snapshot.categorySnapshots().forEach((cat, s) -> modelConfigsCat.get(cat).restore(s));
        builderConstructorByName.clear();
        builderConstructorByName.putAll(snapshot.builderConstructorByName());
    }

    private record CatalogSnapshot(Map<String, ModelConfigs.Snapshot> categorySnapshots,
                                   Map<String, BuilderConfig.ModelBuilderConstructor> builderConstructorByName) {
    }

    /**
     * A span across which runtime registrations are undone on {@link #close}. Not reentrant and not
     * thread-safe, like the handler it scopes; use it in a try-with-resources around a run's
     * registration and the reads that depend on it.
     */
    public final class Scope implements AutoCloseable {

        private final CatalogSnapshot snapshot = capture();

        private Scope() {
        }

        @Override
        public void close() {
            restore(snapshot);
        }
    }

    public Set<String> getCategories() {
        return Collections.unmodifiableSet(modelConfigsCat.keySet());
    }

    /**
     * Returns every model configuration of every category, whichever {@link ModelConfigLoader}
     * contributed it. Used to select a model from its name and capabilities without having to
     * know in which category it was declared.
     */
    public Stream<ModelConfig> getModelConfigStream() {
        return modelConfigsCat.values().stream().flatMap(mc -> mc.getModelConfigs().stream());
    }

    public Optional<ModelConfig> findModelConfig(String modelName) {
        return modelConfigsCat.values().stream()
                .map(mc -> mc.getModelConfig(modelName))
                .filter(Objects::nonNull)
                .findFirst();
    }

    public List<String> getSupportedLibs(DynawoVersion dynawoVersion) {
        return modelConfigsCat.values().stream()
                .flatMap(mc -> mc.getModelInfos().stream())
                .filter(mi -> mi.version().includes(dynawoVersion))
                .map(ModelInfo::lib)
                .toList();
    }
}
