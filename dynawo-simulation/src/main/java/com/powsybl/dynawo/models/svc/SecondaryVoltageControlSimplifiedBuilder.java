/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.models.svc;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.dynawo.builders.BuilderEquipmentsList;
import com.powsybl.dynawo.builders.BuilderReports;
import com.powsybl.dynawo.builders.ModelConfig;
import com.powsybl.dynawo.builders.ModelConfigs;
import com.powsybl.dynawo.builders.ModelConfigsHandler;
import com.powsybl.dynawo.builders.ModelInfo;
import com.powsybl.dynawo.commons.DynawoVersion;
import com.powsybl.dynawo.models.automationsystems.AbstractAutomationSystemModelBuilder;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.IdentifiableType;
import com.powsybl.iidm.network.Network;

import java.util.Collection;

/**
 * Builds a {@link SecondaryVoltageControlSimplified} from a control zone: the generators it coordinates
 * and the pilot bus whose voltage it holds.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public class SecondaryVoltageControlSimplifiedBuilder extends AbstractAutomationSystemModelBuilder<SecondaryVoltageControlSimplifiedBuilder> {

    public static final String CATEGORY = "SECONDARY_VOLTAGE_CONTROL";
    private static final ModelConfigs MODEL_CONFIGS = ModelConfigsHandler.getInstance().getModelConfigs(CATEGORY);

    private final BuilderEquipmentsList<Identifiable<?>> generators;
    private String pilotBusId;

    public static SecondaryVoltageControlSimplifiedBuilder of(Network network) {
        return of(network, ReportNode.NO_OP);
    }

    public static SecondaryVoltageControlSimplifiedBuilder of(Network network, ReportNode reportNode) {
        return new SecondaryVoltageControlSimplifiedBuilder(network, MODEL_CONFIGS.getDefaultModelConfig(), reportNode);
    }

    public static SecondaryVoltageControlSimplifiedBuilder of(Network network, String lib) {
        return of(network, lib, ReportNode.NO_OP);
    }

    public static SecondaryVoltageControlSimplifiedBuilder of(Network network, String lib, ReportNode reportNode) {
        ModelConfig modelConfig = MODEL_CONFIGS.getModelConfig(lib);
        if (modelConfig == null) {
            BuilderReports.reportModelNotFound(reportNode, SecondaryVoltageControlSimplifiedBuilder.class.getSimpleName(), lib);
            return null;
        }
        return new SecondaryVoltageControlSimplifiedBuilder(network, modelConfig, reportNode);
    }

    public static Collection<ModelInfo> getSupportedModelInfos() {
        return MODEL_CONFIGS.getModelInfos();
    }

    public static Collection<ModelInfo> getSupportedModelInfos(DynawoVersion dynawoVersion) {
        return MODEL_CONFIGS.getModelInfos(dynawoVersion);
    }

    protected SecondaryVoltageControlSimplifiedBuilder(Network network, ModelConfig modelConfig, ReportNode reportNode) {
        super(network, modelConfig, reportNode);
        generators = new BuilderEquipmentsList<>(IdentifiableType.GENERATOR.toString(), "generators", reportNode);
    }

    public SecondaryVoltageControlSimplifiedBuilder generators(Collection<String> generatorIds) {
        generators.addEquipments(generatorIds, network::getGenerator);
        return self();
    }

    public SecondaryVoltageControlSimplifiedBuilder pilotPoint(String pilotBusStaticId) {
        this.pilotBusId = pilotBusStaticId;
        return self();
    }

    @Override
    protected void checkData() {
        super.checkData();
        isInstantiable &= generators.checkEquipmentData();
        if (pilotBusId == null) {
            BuilderReports.reportFieldNotSet(reportNode, "pilotPoint");
            isInstantiable = false;
        }
    }

    @Override
    public SecondaryVoltageControlSimplified build() {
        return isInstantiable() ? new SecondaryVoltageControlSimplified(dynamicModelId, parameterSetId,
                generators.getEquipments(), pilotBusId, modelConfig) : null;
    }

    @Override
    protected SecondaryVoltageControlSimplifiedBuilder self() {
        return this;
    }
}
