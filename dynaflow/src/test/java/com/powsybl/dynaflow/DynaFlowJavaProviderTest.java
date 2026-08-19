/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynaflow;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.dynawo.DynawoSimulationContext;
import com.powsybl.dynawo.DynawoSimulationParameters.SolverType;
import com.powsybl.dynawo.commons.DynawoConstants;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TopologyKind;
import com.powsybl.iidm.network.VariantManagerConstants;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowProvider;
import org.junit.jupiter.api.Test;

import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Java DynaFlow load flow: a distinct provider ({@code "DynaFlowJava"}) that hardcodes the {@code
 * "DynaFlow"} mapping and runs it through Dynawo, registered beside the untouched C++ {@code "DynaFlow"}
 * provider.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
class DynaFlowJavaProviderTest {

    @Test
    void theProviderIsRegisteredUnderItsOwnNameBesideTheCppOne() {
        boolean java = false;
        boolean cpp = false;
        for (LoadFlowProvider provider : ServiceLoader.load(LoadFlowProvider.class)) {
            java |= DynaFlowJavaProvider.NAME.equals(provider.getName());
            cpp |= DynaFlowConstants.DYNAFLOW_NAME.equals(provider.getName());
        }
        assertTrue(java, "DynaFlowJava load flow provider should be registered");
        assertTrue(cpp, "the C++ DynaFlow provider should still be registered beside it");

        assertEquals(DynaFlowJavaProvider.NAME, LoadFlow.find(DynaFlowJavaProvider.NAME).getName());
    }

    @Test
    void theProviderReusesTheCppDynaFlowParameters() {
        DynaFlowJavaProvider provider = new DynaFlowJavaProvider();
        assertEquals(DynaFlowParameters.class, provider.getSpecificParametersClass().orElseThrow());
        assertEquals(DynaFlowParameters.SPECIFIC_PARAMETERS, provider.getRawSpecificParameters());
        assertTrue(provider.loadSpecificParameters(java.util.Map.of()).orElseThrow() instanceof DynaFlowParameters);
        // the DynaFlowParameters serializer is registered globally, so this provider must not re-declare it
        assertTrue(provider.getSpecificParametersSerializer().isEmpty());
    }

    @Test
    void theContextHardcodesTheDynaFlowMappingWithItsGlobalParametersAndSolver() {
        Network network = twoBusNetworkWithGenerator();
        DynawoSimulationContext context = DynaFlowJavaProvider.buildContext(
                network, VariantManagerConstants.INITIAL_VARIANT_ID, DynawoConstants.VERSION_MIN, ReportNode.NO_OP,
                new DynaFlowParameters());

        // the DynaFlow global sets and the simplified solver
        assertEquals(SolverType.SIM, context.getDynawoSimulationParameters().getSolverType());
        assertEquals("Network", context.getDynawoSimulationParameters().getNetworkParameters().getId());
        assertEquals("SimplifiedSolver", context.getDynawoSimulationParameters().getSolverParameters().getId());
        // the mapping placed the generator on a SignalN model, so the framework added the shared SignalN signal
        assertTrue(context.getBlackBoxDynamicModels().stream().anyMatch(m -> "SignalN".equals(m.getLib())),
                "the DynaFlow generator mapping should have produced SignalN models and their frequency signal");
    }

    private static Network twoBusNetworkWithGenerator() {
        Network network = Network.create("dynaflow-java", "test");
        var substation = network.newSubstation().setId("S").add();
        VoltageLevel vl1 = substation.newVoltageLevel().setId("VL1").setNominalV(400).setTopologyKind(TopologyKind.BUS_BREAKER).add();
        Bus b1 = vl1.getBusBreakerView().newBus().setId("B1").add();
        VoltageLevel vl2 = substation.newVoltageLevel().setId("VL2").setNominalV(400).setTopologyKind(TopologyKind.BUS_BREAKER).add();
        Bus b2 = vl2.getBusBreakerView().newBus().setId("B2").add();
        network.newLine().setId("L").setVoltageLevel1("VL1").setBus1("B1").setConnectableBus1("B1")
                .setVoltageLevel2("VL2").setBus2("B2").setConnectableBus2("B2")
                .setR(1).setX(10).setG1(0).setB1(0).setG2(0).setB2(0).add();
        // the DynaFlow gate checks -targetP is within [minP, maxP] (the launcher's sign convention)
        vl1.newGenerator().setId("G").setBus("B1").setConnectableBus("B1")
                .setTargetP(500).setTargetV(400).setMinP(-1000).setMaxP(1000).setVoltageRegulatorOn(true).add()
                .newMinMaxReactiveLimits().setMinQ(-100).setMaxQ(100).add();
        vl2.newLoad().setId("LD").setBus("B2").setConnectableBus("B2").setP0(90).setQ0(30).add();
        return network;
    }
}
