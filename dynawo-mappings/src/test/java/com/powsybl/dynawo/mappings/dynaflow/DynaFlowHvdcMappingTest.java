/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.dynaflow;

import com.powsybl.dynawo.mappings.MappedModelsSupplier.MappedModel;
import com.powsybl.dynawo.parameters.ParametersSet;
import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TopologyKind;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.iidm.network.extensions.HvdcAngleDroopActivePowerControlAdder;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The DynaFlow HVDC rule (the DynaFlow Launcher's {@code HVDCDefinitionAlgorithm}): an LCC line runs the
 * {@code HvdcPTanPhi} family, a VSC line the {@code HvdcPV} family alone on its bus or {@code HvdcPQProp}
 * sharing it, turned into an {@code EmulationSet} model with angle-droop control and a {@code DiagramPQ}
 * model on finite reactive limits; a line reaching the main component from one side runs a {@code Dangling}
 * model. Every modelled line reads a set of its own.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
class DynaFlowHvdcMappingTest {

    @Test
    void aLoneVscRunsPvAProportionalOneQProp() {
        Network network = twoBusMainArea();
        vsc(vl(network, "VL1"), "C1", true);
        vsc(vl(network, "VL2"), "C2", false);
        hvdc(network, "HVDC", "C1", "C2");

        assertEquals("HvdcPVDiagramPQ", libOf(network, "HVDC"));   // finite reactive limits by default

        // a second regulator on C1's bus makes it a shared, proportional model
        vl(network, "VL1").newGenerator().setId("G1").setBus("B_VL1").setConnectableBus("B_VL1")
                .setTargetP(10).setMinP(0).setMaxP(100).setTargetV(400).setVoltageRegulatorOn(true).add();
        assertEquals("HvdcPQPropDiagramPQ", libOf(network, "HVDC"));
    }

    @Test
    void anLccRunsTanPhiAndAnEmulationLineRunsEmulationSet() {
        Network lccNetwork = twoBusMainArea();
        lcc(vl(lccNetwork, "VL1"), "C1");
        lcc(vl(lccNetwork, "VL2"), "C2");
        hvdc(lccNetwork, "HVDC", "C1", "C2");
        assertEquals("HvdcPTanPhiDiagramPQ", libOf(lccNetwork, "HVDC"));

        Network emulationNetwork = twoBusMainArea();
        vsc(vl(emulationNetwork, "VL1"), "C1", true);
        vsc(vl(emulationNetwork, "VL2"), "C2", false);
        HvdcLine line = hvdc(emulationNetwork, "HVDC", "C1", "C2");
        line.newExtension(HvdcAngleDroopActivePowerControlAdder.class).withDroop(5).withP0(20).withEnabled(true).add();
        assertEquals("HvdcPVDiagramPQEmulationSet", libOf(emulationNetwork, "HVDC"));
    }

    @Test
    void aConverterOutsideTheMainComponentDangles() {
        Network network = twoBusMainArea();               // the main area is VL1 + VL2, joined by a line
        vsc(vl(network, "VL1"), "C1", true);
        VoltageLevel island = vl(network, "ISLAND");      // a lone bus, its own connected component
        vsc(island, "C2", true);
        hvdc(network, "HVDC", "C1", "C2");

        List<MappedModel> models = new DynaFlowMapping(DynaFlowMapping.NAME).createModelConfigs(network);
        assertEquals("HvdcPVDanglingDiagramPQ", byId(models).get("HVDC").lib());
    }

    @Test
    void aVscLineReadsItsDiagramSetWithBothSidesTabulated() {
        Network network = twoBusMainArea();
        vsc(vl(network, "VL1"), "C1", true);
        vsc(vl(network, "VL2"), "C2", false);
        hvdc(network, "HVDC", "C1", "C2");

        List<ParametersSet> sets = new DynaFlowMapping(DynaFlowMapping.NAME).createParameters(network, null);
        assertEquals(1, sets.size());
        ParametersSet set = sets.get(0);
        assertEquals("HVDC", set.getId());
        Map<String, String> params = set.getParameters().values().stream()
                .collect(Collectors.toMap(p -> p.name(), p -> p.value()));
        Map<String, String> refs = set.getReferences().values().stream()
                .collect(Collectors.toMap(r -> r.name(), r -> r.origName()));

        assertEquals("1.0", params.get("hvdc_KLosses"));
        // both converters tabulated (both in the main component), the table names keyed by converter id
        assertEquals("C1_tableqmin", params.get("hvdc_QInj1MinTableName"));
        assertEquals("C2_tableqmax", params.get("hvdc_QInj2MaxTableName"));
        assertEquals("C1_Diagram.txt", params.get("hvdc_QInj1MinTableFile"));
        // VSC regulation modes and references
        assertEquals("true", params.get("hvdc_modeU10"));    // C1 regulates
        assertEquals("false", params.get("hvdc_modeU20"));   // C2 does not
        assertFalse(params.containsKey("hvdc_Q1MinPu"));      // finite limits -> no infinite fallback
        assertTrue(refs.containsKey("P1Ref_ValueIn"));       // not dangling, not emulation
        assertEquals("pMax_pu", refs.get("hvdc_PMaxPu"));
        assertEquals("v1_pu", refs.get("hvdc_U10Pu"));       // warm start by default
    }

    private static String libOf(Network network, String hvdcId) {
        return byId(new DynaFlowMapping(DynaFlowMapping.NAME).createModelConfigs(network)).get(hvdcId).lib();
    }

    private static Map<String, MappedModel> byId(List<MappedModel> models) {
        return models.stream().collect(Collectors.toMap(MappedModel::staticId, m -> m));
    }

    /** A main area of two 400 kV buses joined by a line, so it is the network's main connected component. */
    private static Network twoBusMainArea() {
        Network network = Network.create("hvdc", "test");
        VoltageLevel vl1 = vl(network, "VL1");
        VoltageLevel vl2 = vl(network, "VL2");
        network.newLine().setId("L12").setVoltageLevel1("VL1").setBus1("B_VL1").setConnectableBus1("B_VL1")
                .setVoltageLevel2("VL2").setBus2("B_VL2").setConnectableBus2("B_VL2")
                .setR(1).setX(10).setG1(0).setB1(0).setG2(0).setB2(0).add();
        return network;
    }

    private static VoltageLevel vl(Network network, String id) {
        VoltageLevel existing = network.getVoltageLevel(id);
        if (existing != null) {
            return existing;
        }
        VoltageLevel vl = network.newSubstation().setId("S_" + id).add()
                .newVoltageLevel().setId(id).setNominalV(400).setTopologyKind(TopologyKind.BUS_BREAKER).add();
        vl.getBusBreakerView().newBus().setId("B_" + id).add();
        return vl;
    }

    private static void vsc(VoltageLevel vl, String id, boolean regulate) {
        vl.newVscConverterStation().setId(id).setBus("B_" + vl.getId()).setConnectableBus("B_" + vl.getId())
                .setLossFactor(1.1f).setVoltageRegulatorOn(regulate).setVoltageSetpoint(400).setReactivePowerSetpoint(0).add()
                .newMinMaxReactiveLimits().setMinQ(-50).setMaxQ(50).add();
    }

    private static void lcc(VoltageLevel vl, String id) {
        vl.newLccConverterStation().setId(id).setBus("B_" + vl.getId()).setConnectableBus("B_" + vl.getId())
                .setLossFactor(1.1f).setPowerFactor(0.9f).add();
    }

    private static HvdcLine hvdc(Network network, String id, String c1, String c2) {
        return network.newHvdcLine().setId(id).setConverterStationId1(c1).setConverterStationId2(c2)
                .setNominalV(320).setR(1).setActivePowerSetpoint(80).setMaxP(200)
                .setConvertersMode(HvdcLine.ConvertersMode.SIDE_1_RECTIFIER_SIDE_2_INVERTER).add();
    }
}
