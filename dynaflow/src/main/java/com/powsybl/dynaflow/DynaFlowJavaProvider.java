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
import java.util.HashMap;
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
    // the DynaFlow mapping's configuration keys (com.powsybl.dynawo.mappings.dynaflow.DynaFlowConfig)
    private static final String DSO_VOLTAGE_LEVEL_KEY = "dynaflow_dso_voltage_level";
    private static final String TFO_VOLTAGE_LEVEL_KEY = "dynaflow_tfo_voltage_level";
    private static final String TIME_STEP_KEY = "dynaflow_time_step";
    private static final String SVC_REGULATION_ON_KEY = "dynaflow_svc_regulation_on";

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
        DynaFlowParameters dynaFlowParameters = getParametersExt(loadFlowParameters);
        DynawoSimulationContext context = buildContext(network, workingStateId, version, reportNode, dynaFlowParameters);

        ExecutionEnvironment simulationEnv = ExecutionEnvironmentUtils.createSimulationEnv(config, WORKING_DIR_PREFIX, dumpDir);
        return computationManager
                .execute(simulationEnv, new DynawoSimulationHandler(context, DynawoSimulationProvider.getCommand(config), reportNode))
                .thenApply(DynaFlowJavaProvider::toLoadFlowResult);
    }

    /**
     * Assembles the Dynawo context from the {@code DynaFlow} mapping, exactly as the dynamic-simulation API
     * would, honouring the load flow's {@link DynaFlowParameters}: the voltage thresholds and time step
     * configure the mapping, and the simulation window, precision and load merging drive Dynawo.
     */
    static DynawoSimulationContext buildContext(Network network, String workingStateId, DynawoVersion version,
                                                ReportNode reportNode, DynaFlowParameters dynaFlowParameters) {
        MappingParameters mappingParameters = toMappingParameters(dynaFlowParameters);
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
                .setModelsParameters(modelsParameters)
                .setMergeLoads(dynaFlowParameters.isMergeLoads());
        if (dynaFlowParameters.getPrecision() != null) {
            dynawoParameters.setPrecision(dynaFlowParameters.getPrecision());
        }
        DynamicSimulationParameters simulationParameters = new DynamicSimulationParameters()
                .setStartTime(dynaFlowParameters.getStartTime())
                .setStopTime(dynaFlowParameters.getStopTime());

        return new DynawoSimulationContext.Builder(network, blackBoxModels)
                .workingVariantId(workingStateId)
                .dynamicSimulationParameters(simulationParameters)
                .dynawoParameters(dynawoParameters)
                .currentVersion(version)
                .reportNode(reportNode)
                .build();
    }

    /** Maps the load flow's DynaFlow knobs the mapping shares onto its {@link MappingParameters} keys. */
    private static MappingParameters toMappingParameters(DynaFlowParameters dynaFlowParameters) {
        Map<String, String> values = new HashMap<>();
        values.put(DSO_VOLTAGE_LEVEL_KEY, Double.toString(dynaFlowParameters.getDsoVoltageLevel()));
        values.put(TFO_VOLTAGE_LEVEL_KEY, Double.toString(dynaFlowParameters.getTfoVoltageLevel()));
        values.put(TIME_STEP_KEY, Double.toString(dynaFlowParameters.getTimeStep()));
        values.put(SVC_REGULATION_ON_KEY, Boolean.toString(dynaFlowParameters.getSvcRegulationOn()));
        return MappingParameters.of(values);
    }

    static DynaFlowParameters getParametersExt(LoadFlowParameters parameters) {
        DynaFlowParameters extension = parameters.getExtension(DynaFlowParameters.class);
        return extension != null ? extension : new DynaFlowParameters();
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

    // Reuse the same load-flow-specific parameters as the C++ DynaFlowProvider, so a study configures both
    // the same way (the DynaFlowParameters extension and the "dynaflow-default-parameters" module).

    @Override
    public Optional<Class<? extends Extension<LoadFlowParameters>>> getSpecificParametersClass() {
        return Optional.of(DynaFlowParameters.class);
    }

    @Override
    public Optional<Extension<LoadFlowParameters>> loadSpecificParameters(PlatformConfig platformConfig) {
        return Optional.of(DynaFlowParameters.load(platformConfig));
    }

    @Override
    public Optional<Extension<LoadFlowParameters>> loadSpecificParameters(Map<String, String> properties) {
        return Optional.of(DynaFlowParameters.load(properties));
    }

    @Override
    public void updateSpecificParameters(Extension<LoadFlowParameters> extension, PlatformConfig platformConfig) {
        ((DynaFlowParameters) extension).update(platformConfig);
    }

    @Override
    public void updateSpecificParameters(Extension<LoadFlowParameters> extension, Map<String, String> properties) {
        getParametersExt(extension.getExtendable()).update(properties);
    }

    @Override
    public Map<String, String> createMapFromSpecificParameters(Extension<LoadFlowParameters> extension) {
        if (extension instanceof DynaFlowParameters dynaFlowParameters) {
            return dynaFlowParameters.createMapFromParameters();
        }
        return Collections.emptyMap();
    }

    @Override
    public List<Parameter> getRawSpecificParameters() {
        return DynaFlowParameters.SPECIFIC_PARAMETERS;
    }

    @Override
    public Optional<ModuleConfig> getModuleConfig(PlatformConfig platformConfig) {
        return platformConfig.getOptionalModuleConfig(DynaFlowParameters.MODULE_SPECIFIC_PARAMETERS);
    }

    @Override
    public Optional<ExtensionJsonSerializer> getSpecificParametersSerializer() {
        // the DynaFlowParameters serializer is registered globally (@AutoService) and by the C++
        // DynaFlowProvider; returning it here too would clash on the shared extension name
        return Optional.empty();
    }
}
