/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynaflow;

import com.google.auto.service.AutoService;
import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.computation.ExecutionEnvironment;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.dynamicsimulation.DynamicModelsSupplier;
import com.powsybl.dynawo.DumpFileParameters;
import com.powsybl.dynawo.DynawoSimulationConfig;
import com.powsybl.dynawo.DynawoSimulationParameters;
import com.powsybl.dynawo.algorithms.DynawoAlgorithmsConfig;
import com.powsybl.dynawo.commons.DynawoUtil;
import com.powsybl.dynawo.commons.DynawoVersion;
import com.powsybl.dynawo.commons.ExecutionEnvironmentUtils;
import com.powsybl.dynawo.commons.PowsyblDynawoVersion;
import com.powsybl.dynawo.models.utils.BlackBoxSupplierUtils;
import com.powsybl.dynawo.security.DynamicSecurityAnalysisReports;
import com.powsybl.dynawo.security.DynawoSecurityAnalysisHandler;
import com.powsybl.dynawo.security.SecurityAnalysisContext;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowRunParameters;
import com.powsybl.security.SecurityAnalysisReport;
import com.powsybl.security.dynamic.DynamicSecurityAnalysisParameters;
import com.powsybl.security.dynamic.DynamicSecurityAnalysisProvider;
import com.powsybl.security.dynamic.DynamicSecurityAnalysisRunParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static com.powsybl.dynawo.DynawoSimulationConfig.DYNAWO_LAUNCHER_PROGRAM_NAME;
import static com.powsybl.dynawo.algorithms.DynawoAlgorithmsCommandUtil.getCommand;
import static com.powsybl.dynawo.algorithms.DynawoAlgorithmsCommandUtil.getVersionCommand;

/**
 * Runs a DynaFlow security analysis in pure Java, without the C++ DynaFlow Launcher.
 * <p>
 * Like {@link DynaFlowJavaProvider} is to the load flow, this is a thin hardcode of the {@code "DynaFlow"}
 * dynamic-models mapping for security analysis: the base (pre-contingency) models and their parameter sets
 * come from {@link DynaFlowJavaProvider#buildMappedInputs}, so they are byte-identical to the load-flow
 * study, and each contingency's disconnection event model is raised by the shared Dynawo security-analysis
 * engine ({@link SecurityAnalysisContext} → {@link DynawoSecurityAnalysisHandler}), which runs the
 * multi-scenario simulation through dynawo-algorithms and aggregates the constraint results. The incoming
 * {@code dynamicModelsSupplier} is ignored — the models are the DynaFlow mapping's.
 * <p>
 * It always runs a load flow before the security analysis (N then SA, like the load-flow-based security
 * analyses): {@link DynaFlowJavaProvider} solves the base case and exports Dynawo's final state, and the
 * security analysis WARM-starts from it. So the pre-contingency result is the converged base, and each
 * contingency reports only the violations it adds — not those already present in the base case.
 * <p>
 * The C++-launcher {@link DynaFlowSecurityAnalysisProvider} (name {@code "DynaFlow"}) is untouched; this
 * provider registers under a distinct name so the two run side by side.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
@AutoService(DynamicSecurityAnalysisProvider.class)
public class DynaFlowSecurityAnalysisJavaProvider implements DynamicSecurityAnalysisProvider {

    public static final String NAME = "DynaFlowJava";

    private static final Logger LOGGER = LoggerFactory.getLogger(DynaFlowSecurityAnalysisJavaProvider.class);
    private static final String WORKING_DIR_PREFIX = "dynaflow_java_sa_";
    // the load flow exports Dynawo's final state as <workingDir>_outputState.dmp (DynawoSimulationHandler)
    private static final String OUTPUT_DUMP_SUFFIX = "outputState.dmp";
    // the DynaFlow launcher's default TimeOfEvent — the instant every contingency's disconnection fires
    // (event_tEvent). powsybl-core defaults the contingencies start time to 5s; a DynaFlow study defaults
    // to the launcher's value so it reproduces the launcher out of the box.
    static final double DYNAFLOW_DEFAULT_TIME_OF_EVENT = 10d;
    private static final double POWSYBL_DEFAULT_CONTINGENCIES_START_TIME = 5d;

    private final DynawoAlgorithmsConfig config;

    public DynaFlowSecurityAnalysisJavaProvider() {
        this(PlatformConfig.defaultConfig());
    }

    public DynaFlowSecurityAnalysisJavaProvider(PlatformConfig platformConfig) {
        this(DynawoAlgorithmsConfig.load(platformConfig));
    }

    public DynaFlowSecurityAnalysisJavaProvider(DynawoAlgorithmsConfig config) {
        this.config = Objects.requireNonNull(config);
    }

    @Override
    public CompletableFuture<SecurityAnalysisReport> run(Network network, String workingVariantId,
                                                         DynamicModelsSupplier dynamicModelsSupplier,
                                                         ContingenciesProvider contingenciesProvider,
                                                         DynamicSecurityAnalysisRunParameters runParameters) {
        Objects.requireNonNull(network);
        Objects.requireNonNull(workingVariantId);
        Objects.requireNonNull(contingenciesProvider);
        Objects.requireNonNull(runParameters);

        if (!runParameters.getMonitors().isEmpty()) {
            LOGGER.error("Monitoring is not possible with Dynawo implementation. There will not be supplementary information about monitored equipment.");
        }
        if (!runParameters.getOperatorStrategies().isEmpty()) {
            LOGGER.error("Strategies are not implemented in Dynawo");
        }
        if (!runParameters.getActions().isEmpty()) {
            LOGGER.error("Actions are not implemented in Dynawo");
        }

        ReportNode saReportNode = DynamicSecurityAnalysisReports.createDynamicSecurityAnalysisReportNode(runParameters.getReportNode(), network.getId());
        network.getVariantManager().setWorkingVariant(workingVariantId);
        DynamicSecurityAnalysisParameters parameters = withDynaFlowDefaults(runParameters.getDynamicSecurityAnalysisParameters());

        String dumpDir = parameters.getDebugDir();
        ExecutionEnvironment versionEnv = ExecutionEnvironmentUtils.createVersionEnv(config, WORKING_DIR_PREFIX, dumpDir);
        DynawoVersion version = DynawoUtil.requireDynaMinVersion(versionEnv, runParameters.getComputationManager(),
                getVersionCommand(config), DYNAWO_LAUNCHER_PROGRAM_NAME, false);

        // TODO(dynaflow-sa) source the DynaFlow knobs (dso/tfo voltage, time step, svc regulation) for the
        //  base models from a DynaFlow security-analysis parameters bridge instead of the defaults — see
        //  DYNAFLOW_SA_PLAN.md §4. The contingency start time (event_tEvent) already flows from
        //  DynamicSecurityAnalysisParameters.getDynamicContingenciesParameters().getContingenciesStartTime().
        DynaFlowParameters dynaFlowParameters = new DynaFlowParameters();

        // N then SA (mandatory, like the load-flow-based security analyses): run the DynaFlow load flow
        // first, exporting Dynawo's final state, so the security analysis WARM-starts from a converged base.
        // Its pre-contingency result is that base case, so each contingency reports only the violations it
        // adds, not those already present. Both steps build their models from the same mapping, so the base
        // is byte-identical.
        Path dumpFolder;
        try {
            dumpFolder = Files.createTempDirectory(WORKING_DIR_PREFIX + "dump_");
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
        LoadFlowParameters loadFlowParameters = new LoadFlowParameters();
        loadFlowParameters.addExtension(DynaFlowParameters.class, dynaFlowParameters);
        LoadFlowRunParameters loadFlowRunParameters = new LoadFlowRunParameters()
                .setComputationManager(runParameters.getComputationManager())
                .setParameters(loadFlowParameters)
                .setReportNode(saReportNode);
        DynaFlowJavaProvider loadFlow = new DynaFlowJavaProvider(() -> new DynawoSimulationConfig(config.getHomeDir(), config.isDebug()));

        return loadFlow.run(network, workingVariantId, loadFlowRunParameters, dumpFolder)
                .thenCompose(loadFlowResult -> runSecurityAnalysis(network, contingenciesProvider, runParameters,
                        parameters, version, saReportNode, dynaFlowParameters, dumpFolder, dumpDir))
                .whenComplete((report, throwable) -> deleteRecursivelyQuietly(dumpFolder));
    }

    /** The security-analysis half of N+SA: build the models from the solved network and WARM-start from the load flow's dump. */
    private CompletableFuture<SecurityAnalysisReport> runSecurityAnalysis(Network network, ContingenciesProvider contingenciesProvider,
                                                                          DynamicSecurityAnalysisRunParameters runParameters, DynamicSecurityAnalysisParameters parameters,
                                                                          DynawoVersion version, ReportNode saReportNode, DynaFlowParameters dynaFlowParameters,
                                                                          Path dumpFolder, String dumpDir) {
        DynaFlowJavaProvider.MappedInputs inputs = DynaFlowJavaProvider.buildMappedInputs(network, saReportNode, dynaFlowParameters);
        inputs.dynawoParameters().setDumpFileParameters(DumpFileParameters.createImportDumpFileParameters(dumpFolder, findExportedDump(dumpFolder)));
        // the mapping builds the run's Dynawo parameters fresh, so carry across a caller-supplied criteria
        // (a typed model or a file), set on the dynamic simulation parameters' Dynawo extension: the
        // security analysis then writes it and checks it, adding the criteria's violations to each result
        DynawoSimulationParameters callerDynawoParameters = parameters.getDynamicSimulationParameters()
                .getExtension(DynawoSimulationParameters.class);
        if (callerDynawoParameters != null) {
            callerDynawoParameters.getCriteria().ifPresent(inputs.dynawoParameters()::setCriteria);
            callerDynawoParameters.getCriteriaFilePath().ifPresent(inputs.dynawoParameters()::setCriteriaFilePath);
        }

        SecurityAnalysisContext context = new SecurityAnalysisContext.Builder(network,
                inputs.blackBoxModels(),
                contingenciesProvider.getContingencies(network))
                .eventModels(BlackBoxSupplierUtils.getBlackBoxModelList(runParameters.getEventModelsSupplier(), network, saReportNode))
                .dynamicSecurityAnalysisParameters(parameters)
                .dynawoParameters(inputs.dynawoParameters())
                .currentVersion(version)
                .reportNode(saReportNode)
                .build();

        ExecutionEnvironment simulationEnv = ExecutionEnvironmentUtils.createSimulationEnv(config, WORKING_DIR_PREFIX, dumpDir);
        return runParameters.getComputationManager().execute(simulationEnv,
                new DynawoSecurityAnalysisHandler(context,
                        getCommand(config, "SA", "dynawo_dynamic_sa"),
                        runParameters.getFilter(),
                        runParameters.getInterceptors(),
                        saReportNode));
    }

    /** The state dump the load flow exported into the shared folder ({@code <workingDir>_outputState.dmp}). */
    private static String findExportedDump(Path dumpFolder) {
        try (var files = Files.list(dumpFolder)) {
            return files.map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(OUTPUT_DUMP_SUFFIX))
                    .findFirst()
                    .orElseThrow(() -> new PowsyblException("The DynaFlow load flow did not export a state dump to " + dumpFolder));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void deleteRecursivelyQuietly(Path folder) {
        try (var paths = Files.walk(folder)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    LOGGER.warn("Could not delete {}", path);
                }
            });
        } catch (IOException e) {
            LOGGER.warn("Could not clean the dump folder {}", folder);
        }
    }

    /**
     * Applies the DynaFlow launcher's defaults to the security-analysis parameters: when the contingencies
     * start time is still at powsybl-core's default, it becomes the launcher's {@code TimeOfEvent}, so a
     * DynaFlow study fires its disconnections at the same instant the launcher does. An explicit value is
     * left untouched.
     */
    static DynamicSecurityAnalysisParameters withDynaFlowDefaults(DynamicSecurityAnalysisParameters parameters) {
        DynamicSecurityAnalysisParameters.ContingenciesParameters contingencies = parameters.getDynamicContingenciesParameters();
        if (contingencies.getContingenciesStartTime() == POWSYBL_DEFAULT_CONTINGENCIES_START_TIME) {
            contingencies.setContingenciesStartTime(DYNAFLOW_DEFAULT_TIME_OF_EVENT);
        }
        return parameters;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getVersion() {
        return new PowsyblDynawoVersion().getMavenProjectVersion();
    }
}
