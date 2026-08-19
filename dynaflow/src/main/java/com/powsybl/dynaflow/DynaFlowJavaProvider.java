/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynaflow;

import com.google.auto.service.AutoService;
import com.powsybl.commons.config.ModuleConfig;
import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.commons.extensions.Extension;
import com.powsybl.commons.extensions.ExtensionJsonSerializer;
import com.powsybl.commons.parameters.Parameter;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.ExecutionEnvironment;
import com.powsybl.dynamicsimulation.DynamicModelsSupplier;
import com.powsybl.dynamicsimulation.DynamicSimulationParameters;
import com.powsybl.dynamicsimulation.DynamicSimulationResult;
import com.powsybl.dynawo.DynawoSimulationConfig;
import com.powsybl.dynawo.DynawoSimulationContext;
import com.powsybl.dynawo.DynawoSimulationHandler;
import com.powsybl.dynawo.DynawoSimulationParameters;
import com.powsybl.dynawo.DynawoSimulationProvider;
import com.powsybl.dynawo.commons.DynawoUtil;
import com.powsybl.dynawo.commons.DynawoVersion;
import com.powsybl.dynawo.commons.ExecutionEnvironmentUtils;
import com.powsybl.dynawo.commons.PowsyblDynawoVersion;
import com.powsybl.dynawo.mappings.DynamicModelsMapping;
import com.powsybl.dynawo.mappings.DynamicModelsMappings;
import com.powsybl.dynawo.mappings.MappedModelsSupplier;
import com.powsybl.dynawo.mappings.MappedModelsSupplier.MappedModel;
import com.powsybl.dynawo.mappings.MappingParameters;
import com.powsybl.dynawo.mappings.dynaflow.DynaFlowGlobalParameters;
import com.powsybl.dynawo.mappings.dynaflow.DynaFlowMapping;
import com.powsybl.dynawo.models.BlackBoxModel;
import com.powsybl.dynawo.models.utils.BlackBoxSupplierUtils;
import com.powsybl.dynawo.parameters.ParametersSet;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowProvider;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.loadflow.LoadFlowResultImpl;
import com.powsybl.loadflow.LoadFlowRunParameters;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Runs a DynaFlow steady-state study as a load flow, in pure Java, without the C++ DynaFlow Launcher.
 * <p>
 * This is a thin hardcode of the {@code "DynaFlow"} dynamic-models mapping: it asks the same registry the
 * dynamic-simulation API uses for that mapping, turns its models and parameter sets into a Dynawo
 * simulation context (with the DynaFlow {@code Network} / {@code SimplifiedSolver} parameter sets and the
 * simplified solver), runs Dynawo to a steady state, and reports the final network state as a load flow
 * result. The C++-launcher {@link DynaFlowProvider} (load flow name {@code "DynaFlow"}) is untouched; this
 * provider registers under a distinct name so the two run side by side.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
@AutoService(LoadFlowProvider.class)
public class DynaFlowJavaProvider implements LoadFlowProvider {

    public static final String NAME = "DynaFlowJava";

    private static final String WORKING_DIR_PREFIX = "dynaflow_java_";
    // the launcher's steady-state simulation window (Configuration defaults)
    private static final double START_TIME = 0.0;
    private static final double STOP_TIME = 100.0;

    private final Supplier<DynawoSimulationConfig> configSupplier;

    public DynaFlowJavaProvider() {
        this(DynawoSimulationConfig::load);
    }

    public DynaFlowJavaProvider(Supplier<DynawoSimulationConfig> configSupplier) {
        this.configSupplier = Objects.requireNonNull(configSupplier);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getVersion() {
        return new PowsyblDynawoVersion().getMavenProjectVersion();
    }

    @Override
    public CompletableFuture<LoadFlowResult> run(Network network, String workingStateId, LoadFlowRunParameters runParameters) {
        Objects.requireNonNull(network);
        Objects.requireNonNull(workingStateId);
        Objects.requireNonNull(runParameters);
        ComputationManager computationManager = Objects.requireNonNull(runParameters.getComputationManager());
        LoadFlowParameters loadFlowParameters = Objects.requireNonNull(runParameters.getLoadFlowParameters());
        ReportNode reportNode = Objects.requireNonNull(runParameters.getReportNode());

        DynawoSimulationConfig config = configSupplier.get();
        String dumpDir = loadFlowParameters.getDebugDir();
        ExecutionEnvironment versionEnv = ExecutionEnvironmentUtils.createVersionEnv(config, WORKING_DIR_PREFIX, dumpDir);
        DynawoVersion version = DynawoUtil.requireDynaMinVersion(versionEnv, computationManager,
                DynawoSimulationProvider.getVersionCommand(config), DynawoSimulationConfig.DYNAWO_LAUNCHER_PROGRAM_NAME, false);

        network.getVariantManager().setWorkingVariant(workingStateId);
        DynawoSimulationContext context = buildContext(network, workingStateId, version, reportNode);

        ExecutionEnvironment simulationEnv = ExecutionEnvironmentUtils.createSimulationEnv(config, WORKING_DIR_PREFIX, dumpDir);
        return computationManager
                .execute(simulationEnv, new DynawoSimulationHandler(context, DynawoSimulationProvider.getCommand(config), reportNode))
                .thenApply(DynaFlowJavaProvider::toLoadFlowResult);
    }

    /** Assembles the Dynawo context from the {@code DynaFlow} mapping, exactly as the dynamic-simulation API would. */
    static DynawoSimulationContext buildContext(Network network, String workingStateId, DynawoVersion version, ReportNode reportNode) {
        // the DynaFlow flavour reads no configuration knobs here; a study configures it through MappingParameters
        MappingParameters mappingParameters = MappingParameters.empty();
        DynamicModelsMapping mapping = DynamicModelsMappings.getInstance().create(DynaFlowMapping.NAME, mappingParameters);

        mapping.createExtensions(network);
        List<MappedModel> models = mapping.createModelConfigs(network, reportNode);
        DynamicModelsSupplier modelsSupplier = new MappedModelsSupplier(models);
        List<BlackBoxModel> blackBoxModels = BlackBoxSupplierUtils.getBlackBoxModelList(modelsSupplier, network, reportNode);
        List<ParametersSet> modelsParameters = mapping.createParameters(network, null);

        DynawoSimulationParameters dynawoParameters = new DynawoSimulationParameters()
                .setNetworkParameters(DynaFlowGlobalParameters.networkParameters(mappingParameters))
                .setSolverParameters(DynaFlowGlobalParameters.solverParameters(mappingParameters))
                .setSolverType(mapping.getSolverType())
                .setModelsParameters(modelsParameters);
        DynamicSimulationParameters simulationParameters = new DynamicSimulationParameters()
                .setStartTime(START_TIME)
                .setStopTime(STOP_TIME);

        return new DynawoSimulationContext.Builder(network, blackBoxModels)
                .workingVariantId(workingStateId)
                .dynamicSimulationParameters(simulationParameters)
                .dynawoParameters(dynawoParameters)
                .currentVersion(version)
                .reportNode(reportNode)
                .build();
    }

    /**
     * The load flow result mirrors {@link DynaFlowHandler}: the Dynawo handler has already merged the
     * final-state IIDM back into the network, so this only carries the convergence status.
     */
    private static LoadFlowResult toLoadFlowResult(DynamicSimulationResult simulationResult) {
        boolean ok = simulationResult.getStatus() == DynamicSimulationResult.Status.SUCCESS;
        List<LoadFlowResult.ComponentResult> componentResults = List.of(new LoadFlowResultImpl.ComponentResultImpl(
                0, 0,
                ok ? LoadFlowResult.ComponentResult.Status.CONVERGED : LoadFlowResult.ComponentResult.Status.FAILED,
                0, "", 0.0, Double.NaN));
        return new LoadFlowResultImpl(ok, Collections.emptyMap(), null, componentResults);
    }

    // No load-flow-specific parameters: the DynaFlow study is configured through the mapping, not a LoadFlowParameters extension.

    @Override
    public Optional<Class<? extends Extension<LoadFlowParameters>>> getSpecificParametersClass() {
        return Optional.empty();
    }

    @Override
    public Optional<Extension<LoadFlowParameters>> loadSpecificParameters(PlatformConfig platformConfig) {
        return Optional.empty();
    }

    @Override
    public Optional<Extension<LoadFlowParameters>> loadSpecificParameters(Map<String, String> properties) {
        return Optional.empty();
    }

    @Override
    public void updateSpecificParameters(Extension<LoadFlowParameters> extension, PlatformConfig platformConfig) {
        // no specific parameters to update
    }

    @Override
    public void updateSpecificParameters(Extension<LoadFlowParameters> extension, Map<String, String> properties) {
        // no specific parameters to update
    }

    @Override
    public Map<String, String> createMapFromSpecificParameters(Extension<LoadFlowParameters> extension) {
        return Collections.emptyMap();
    }

    @Override
    public List<Parameter> getRawSpecificParameters() {
        return Collections.emptyList();
    }

    @Override
    public Optional<ModuleConfig> getModuleConfig(PlatformConfig platformConfig) {
        return Optional.empty();
    }

    @Override
    public Optional<ExtensionJsonSerializer> getSpecificParametersSerializer() {
        return Optional.empty();
    }
}
