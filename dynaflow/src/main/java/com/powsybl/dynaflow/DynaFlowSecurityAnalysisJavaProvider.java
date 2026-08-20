/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynaflow;

import com.google.auto.service.AutoService;
import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.computation.ExecutionEnvironment;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.dynamicsimulation.DynamicModelsSupplier;
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
import com.powsybl.security.SecurityAnalysisReport;
import com.powsybl.security.dynamic.DynamicSecurityAnalysisParameters;
import com.powsybl.security.dynamic.DynamicSecurityAnalysisProvider;
import com.powsybl.security.dynamic.DynamicSecurityAnalysisRunParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        DynamicSecurityAnalysisParameters parameters = runParameters.getDynamicSecurityAnalysisParameters();

        String dumpDir = parameters.getDebugDir();
        ExecutionEnvironment versionEnv = ExecutionEnvironmentUtils.createVersionEnv(config, WORKING_DIR_PREFIX, dumpDir);
        DynawoVersion version = DynawoUtil.requireDynaMinVersion(versionEnv, runParameters.getComputationManager(),
                getVersionCommand(config), DYNAWO_LAUNCHER_PROGRAM_NAME, false);

        // TODO(dynaflow-sa) source the DynaFlow knobs (dso/tfo voltage, time step, svc regulation) for the
        //  base models from a DynaFlow security-analysis parameters bridge instead of the defaults — see
        //  DYNAFLOW_SA_PLAN.md §4. The contingency start time (event_tEvent) already flows from
        //  DynamicSecurityAnalysisParameters.getDynamicContingenciesParameters().getContingenciesStartTime().
        DynaFlowParameters dynaFlowParameters = new DynaFlowParameters();
        DynaFlowJavaProvider.MappedInputs inputs = DynaFlowJavaProvider.buildMappedInputs(network, saReportNode, dynaFlowParameters);

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

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getVersion() {
        return new PowsyblDynawoVersion().getMavenProjectVersion();
    }
}
