/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.dynamicsimulation.DynamicModel;
import com.powsybl.dynamicsimulation.DynamicModelsSupplier;
import com.powsybl.dynawo.models.automationsystems.TapChangerBlockingAutomationSystem;
import com.powsybl.dynawo.parameters.ParametersSet;
import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.iidm.network.extensions.MeasurementPoint;
import com.powsybl.iidm.network.extensions.TapChangerBlockingsAdder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks that a network's tap changer blocking becomes the automaton that stands for it: the model
 * built for it watching the points it names and blocking the transformers of the levels it
 * controls, and a set holding a threshold per point.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
class TapChangerBlockingMappingTest {

    private static Network ieee14WithTcb() {
        Network network = IeeeCdfNetworkFactory.create14();
        TwoWindingsTransformer transformer = network.getTwoWindingsTransformerStream().findFirst().orElseThrow();
        String controlVoltageLevelId = transformer.getTerminal1().getVoltageLevel().getId();
        VoltageLevel measuredVoltageLevel = network.getVoltageLevelStream()
                .filter(vl -> vl.getBusBreakerView().getBusStream().findAny().isPresent())
                .findFirst().orElseThrow();
        Bus measuredBus = measuredVoltageLevel.getBusBreakerView().getBusStream().findFirst().orElseThrow();
        // the automaton only watches a point that carries a voltage, so the bus is energized as a
        // load flow would leave it
        measuredBus.setV(measuredVoltageLevel.getNominalV());

        network.newExtension(TapChangerBlockingsAdder.class)
                .newTapChangerBlocking()
                    .withName("tcb1")
                    .newMeasurementPoint()
                        .withId("mp1")
                        .withBuses(List.of(new MeasurementPoint.BusRef(measuredVoltageLevel.getId(), measuredBus.getId())))
                        .add()
                    .newControlVoltageLevel()
                        .withId(controlVoltageLevelId)
                        .add()
                    .add()
                .add();
        return network;
    }

    @Test
    void shouldMapATapChangerBlockingToItsAutomaton() {
        Network network = ieee14WithTcb();
        TapChangerBlockingMapping mapping = new TapChangerBlockingMapping("DynaWaltz_");

        List<MappedModelsSupplier.MappedModel> models = mapping.createModelConfigs(network);
        assertThat(models).singleElement().satisfies(model -> {
            assertThat(model.lib()).isEqualTo(TapChangerBlockingMapping.LIB);
            assertThat(model.staticId()).isEqualTo("tcb1");
            // it is not one equipment's model, so it sets itself up
            assertThat(model.configurer()).isNotNull();
        });

        // and the model actually builds into the automation system, watching its point and holding
        // the transformer of its controlled level
        DynamicModelsSupplier supplier = new MappedModelsSupplier(models);
        List<DynamicModel> built = supplier.get(network, ReportNode.NO_OP);
        assertThat(built).singleElement().isInstanceOf(TapChangerBlockingAutomationSystem.class);
    }

    @Test
    void shouldHoldAThresholdPerMeasurementPoint() {
        Network network = ieee14WithTcb();
        List<ParametersSet> sets = new TapChangerBlockingMapping("DynaWaltz_").createParameters(network);
        assertThat(sets).singleElement().satisfies(set -> {
            assertThat(set.getId()).isEqualTo("DynaWaltz_tcb1");
            // one measurement point, so the threshold is named without a number
            assertThat(set.hasParameter("tapChangerBlocking_UMin")).isTrue();
            assertThat(set.hasParameter("tapChangerBlocking_UMin1")).isFalse();
            assertThat(set.hasParameter("tapChangerBlocking_tLagBeforeBlocked")).isTrue();
        });
    }

    @Test
    void shouldMapNothingWhereTheNetworkHoldsNoBlocking() {
        Network network = IeeeCdfNetworkFactory.create14();
        TapChangerBlockingMapping mapping = new TapChangerBlockingMapping("DynaWaltz_");
        assertThat(mapping.createModelConfigs(network)).isEmpty();
        assertThat(mapping.createParameters(network)).isEmpty();
    }
}
