/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.dynaflow;

import com.powsybl.dynawo.mappings.MappedModelsSupplier.MappedModel;
import com.powsybl.dynawo.mappings.MappingParameters;
import com.powsybl.dynawo.parameters.ParametersSet;
import com.powsybl.iidm.network.EnergySource;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TopologyKind;
import com.powsybl.iidm.network.VoltageLevel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * A generator's parameter set, the way the DynaFlow Launcher's {@code ParGenerator} builds it: the IIDM
 * references (pMin, targetP_pu, targetV_pu, …) and the fixed values (dead bands, governor gain, the
 * transformer reactance) each machine's model reads.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
class DynaFlowGeneratorParametersTest {

    @Test
    void aPlainLocalMachineGetsTheReferencesAndFixedValues() {
        ParametersSet set = parametersFor(machine(network(), "VL", 20, "GEN", EnergySource.THERMAL), "GEN");

        assertEquals("GEN", set.getId());
        Map<String, String> refs = references(set);
        // the launcher's warm-start references, plus a rectangular diagram's Q limits and voltage set point
        assertEquals("pMin", refs.get("generator_PMin"));
        assertEquals("pMax", refs.get("generator_PMax"));
        assertEquals("p_pu", refs.get("generator_P0Pu"));
        assertEquals("angle_pu", refs.get("generator_UPhase0"));
        assertEquals("targetP_pu", refs.get("generator_PRef0Pu"));
        assertEquals("pMax_pu", refs.get("generator_PNom"));
        assertEquals("qMin", refs.get("generator_QMin"));
        assertEquals("qMax", refs.get("generator_QMax"));
        assertEquals("targetV_pu", refs.get("generator_URef0Pu"));

        Map<String, String> params = parameters(set);
        assertEquals("1", params.get("generator_KGover"));       // holds power, no active power control
        assertEquals("0.0001", params.get("generator_QDeadBandPu"));
        assertEquals("0.0001", params.get("generator_UDeadBandPu"));

        // a rectangular machine has no diagram-curve references, and no transformer
        assertFalse(refs.containsKey("generator_QMin0"));
        assertFalse(params.containsKey("generator_XTfoPu"));
    }

    @Test
    void aNuclearTransformerMachineGetsTheHigherReactanceAndTheTransformerReferences() {
        // 400 kV local nuclear -> GeneratorPVTfoSignalN
        ParametersSet set = parametersFor(machine(network(), "VL", 400, "GEN", EnergySource.NUCLEAR), "GEN");

        Map<String, String> refs = references(set);
        assertEquals("qNom", refs.get("generator_QNomAlt"));
        assertEquals("sNom", refs.get("generator_SNom"));
        assertEquals("0.1426", parameters(set).get("generator_XTfoPu"));
    }

    @Test
    void onInfiniteReactiveLimitsMachinesShareOneConstantSet() {
        Network network = network();
        machine(network, "VL1", 20, "GEN1", EnergySource.THERMAL);
        machine(network, "VL2", 20, "GEN2", EnergySource.THERMAL);
        machine(network, "VL3", 20, "GEN3", EnergySource.NUCLEAR);
        DynaFlowMapping mapping = new DynaFlowMapping(DynaFlowMapping.NAME,
                DynaFlowConfig.from(MappingParameters.of(Map.of("dynaflow_infinite_reactive_limits", "true"))));

        // the two thermal machines share the value-keyed set; the nuclear one takes its own
        Map<String, String> parIds = mapping.createModelConfigs(network).stream()
                .collect(Collectors.toMap(MappedModel::staticId, MappedModel::parameterSetId));
        assertEquals("signalNGenerator", parIds.get("GEN1"));
        assertEquals("signalNGenerator", parIds.get("GEN2"));
        assertEquals("signalNGenerator_Nuc", parIds.get("GEN3"));

        // the shared set is emitted once, and carries infinite Q and P limits
        List<ParametersSet> sets = mapping.createParameters(network, null);
        assertEquals(Set.of("signalNGenerator", "signalNGenerator_Nuc"),
                sets.stream().map(ParametersSet::getId).collect(Collectors.toSet()));
        ParametersSet shared = sets.stream().filter(s -> s.getId().equals("signalNGenerator")).findFirst().orElseThrow();
        Map<String, String> params = parameters(shared);
        assertEquals(Double.toString(Double.MAX_VALUE), params.get("generator_QMax"));
        assertEquals(Double.toString(-Double.MAX_VALUE), params.get("generator_PMin"));
        // infinite compensation references p_pu (not pMax_pu), and there are no diagram Q references
        assertEquals("p_pu", references(shared).get("generator_PNom"));
        assertFalse(references(shared).containsKey("generator_QMin0"));
    }

    private static ParametersSet parametersFor(Network network, String generatorId) {
        return new DynaFlowMapping(DynaFlowMapping.NAME).createParameters(network, null).stream()
                .filter(s -> s.getId().equals(generatorId))
                .findFirst().orElseThrow();
    }

    private static Map<String, String> references(ParametersSet set) {
        return set.getReferences().values().stream()
                .collect(Collectors.toMap(r -> r.name(), r -> r.origName()));
    }

    private static Map<String, String> parameters(ParametersSet set) {
        return set.getParameters().values().stream()
                .collect(Collectors.toMap(p -> p.name(), p -> p.value()));
    }

    private static Network network() {
        return Network.create("gen-par", "test");
    }

    private static Network machine(Network network, String vlId, double nominalV, String genId, EnergySource source) {
        VoltageLevel vl = network.newSubstation().setId("S_" + vlId).add()
                .newVoltageLevel().setId(vlId).setNominalV(nominalV).setTopologyKind(TopologyKind.BUS_BREAKER).add();
        vl.getBusBreakerView().newBus().setId("B_" + vlId).add();
        Generator generator = vl.newGenerator().setId(genId).setBus("B_" + vlId).setConnectableBus("B_" + vlId)
                .setMinP(-1000).setMaxP(1000).setTargetP(500).setTargetV(nominalV)
                .setEnergySource(source).setVoltageRegulatorOn(true).add();
        return generator.getNetwork();
    }
}
