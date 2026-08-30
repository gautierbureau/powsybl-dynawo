/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings;

import com.powsybl.dynamicsimulation.DynamicModel;
import com.powsybl.dynawo.builders.ModelBuilder;
import com.powsybl.dynawo.models.automationsystems.TapChangerBlockingAutomationSystemBuilder;
import com.powsybl.dynawo.parameters.ParameterType;
import com.powsybl.dynawo.parameters.ParametersSet;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.iidm.network.extensions.ControlVoltageLevel;
import com.powsybl.iidm.network.extensions.MeasurementPoint;
import com.powsybl.iidm.network.extensions.TapChangerBlocking;
import com.powsybl.iidm.network.extensions.TapChangerBlockings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * Gives a network's tap changer blockings the automaton that stands for each.
 * <p>
 * A tap changer blocking watches the voltage at a set of measurement points and, when one falls
 * below a threshold, blocks the tap changers of a set of control voltage levels, so the network
 * does not chase a voltage it can no longer hold. The extension carries the points and the levels;
 * this reads them and resolves them against the network into what the automaton acts on: the
 * transformers of the controlled levels, and the buses and busbar sections the voltage is measured
 * at, each measurement point kept a group of its own since the model watches them one at a time.
 * <p>
 * The automaton is not one equipment's model, so it is set up by a hand of its own rather than by
 * a static id, see {@link MappedModelsSupplier}. Its thresholds and timers are given plausible
 * defaults, a study holding the real ones being expected to value them itself.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public final class TapChangerBlockingMapping {

    static final String LIB = "TapChangerBlockingAutomationSystem";

    // defaults where a study says nothing, the values the Nordic reference blocks with: a
    // threshold in kV, and no lag before or on blocking
    private static final String DEFAULT_U_MIN = "120";
    private static final String DEFAULT_T_LAG_BEFORE_BLOCKED = "0";
    private static final String DEFAULT_T_LAG_TRANS_BLOCKED_D = "0";
    private static final String DEFAULT_T_LAG_TRANS_BLOCKED_T = "0";

    private final String parameterSetPrefix;

    public TapChangerBlockingMapping(String parameterSetPrefix) {
        this.parameterSetPrefix = parameterSetPrefix;
    }

    public List<MappedModelsSupplier.MappedModel> createModelConfigs(Network network) {
        TapChangerBlockings tcbs = network.getExtension(TapChangerBlockings.class);
        if (tcbs == null) {
            return List.of();
        }
        return tcbs.getTapChangerBlockings().stream()
                .map(tcb -> new MappedModelsSupplier.MappedModel(LIB, tcb.getName(), setId(tcb),
                        configurer(network, tcb)))
                .toList();
    }

    private Consumer<ModelBuilder<DynamicModel>> configurer(Network network, TapChangerBlocking tcb) {
        Collection<String>[] measurements = measurementPoints(tcb);
        List<String> transformers = transformers(network, tcb);
        String dynamicModelId = tcb.getName();
        String setId = setId(tcb);
        return builder -> {
            TapChangerBlockingAutomationSystemBuilder tcbBuilder = (TapChangerBlockingAutomationSystemBuilder) builder;
            tcbBuilder.dynamicModelId(dynamicModelId);
            tcbBuilder.parameterSetId(setId);
            tcbBuilder.uMeasurements(measurements);
            if (!transformers.isEmpty()) {
                tcbBuilder.transformers(transformers);
            }
        };
    }

    /**
     * The ids the voltage is measured at, grouped by point: the automaton watches each point on its
     * own, so a point's buses and busbar sections stay together rather than being poured into one
     * list.
     */
    @SuppressWarnings("unchecked")
    private static Collection<String>[] measurementPoints(TapChangerBlocking tcb) {
        return tcb.getMeasurementPoints().stream()
                .map(TapChangerBlockingMapping::pointIds)
                .toArray(Collection[]::new);
    }

    private static List<String> pointIds(MeasurementPoint point) {
        List<String> ids = new ArrayList<>();
        point.getBuses().forEach(bus -> ids.add(bus.busId()));
        ids.addAll(point.getBusbarSectionIds());
        return ids;
    }

    /**
     * The transformers the automaton blocks: the two winding transformers of the levels it
     * controls, which are what a tap changer sits on.
     */
    private static List<String> transformers(Network network, TapChangerBlocking tcb) {
        List<String> transformers = new ArrayList<>();
        for (ControlVoltageLevel controlVoltageLevel : tcb.getControlVoltageLevels()) {
            VoltageLevel voltageLevel = network.getVoltageLevel(controlVoltageLevel.getId());
            if (voltageLevel != null) {
                voltageLevel.getTwoWindingsTransformerStream().forEach(t -> transformers.add(t.getId()));
            }
        }
        return transformers;
    }

    public List<ParametersSet> createParameters(Network network) {
        TapChangerBlockings tcbs = network.getExtension(TapChangerBlockings.class);
        if (tcbs == null) {
            return List.of();
        }
        return tcbs.getTapChangerBlockings().stream().map(this::parameters).toList();
    }

    private ParametersSet parameters(TapChangerBlocking tcb) {
        ParametersSet set = new ParametersSet(setId(tcb));
        // one threshold per point the automaton watches, and the timers common to them. The model
        // that watches a single point names its threshold without a number, the ones watching
        // several number them from one
        int points = tcb.getMeasurementPoints().size();
        if (points == 1) {
            set.addParameter("tapChangerBlocking_UMin", ParameterType.DOUBLE, DEFAULT_U_MIN);
        } else {
            for (int i = 1; i <= points; i++) {
                set.addParameter("tapChangerBlocking_UMin" + i, ParameterType.DOUBLE, DEFAULT_U_MIN);
            }
        }
        set.addParameter("tapChangerBlocking_tLagBeforeBlocked", ParameterType.DOUBLE, DEFAULT_T_LAG_BEFORE_BLOCKED);
        set.addParameter("tapChangerBlocking_tLagTransBlockedD", ParameterType.DOUBLE, DEFAULT_T_LAG_TRANS_BLOCKED_D);
        set.addParameter("tapChangerBlocking_tLagTransBlockedT", ParameterType.DOUBLE, DEFAULT_T_LAG_TRANS_BLOCKED_T);
        return set;
    }

    private String setId(TapChangerBlocking tcb) {
        return parameterSetPrefix + tcb.getName();
    }
}
