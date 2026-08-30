/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.models.events;

import com.powsybl.dynawo.builders.EventModelInfo;
import com.powsybl.dynawo.models.VarConnection;
import com.powsybl.dynawo.models.macroconnections.MacroConnectionsAdder;
import com.powsybl.dynawo.models.transformers.DefaultTransformer;
import com.powsybl.dynawo.models.utils.EnergizedUtils;
import com.powsybl.dynawo.parameters.ParametersSet;
import com.powsybl.iidm.network.ThreeWindingsTransformer;

import java.util.ArrayList;
import java.util.List;

import static com.powsybl.dynawo.parameters.ParameterType.BOOL;
import static com.powsybl.dynawo.parameters.ParameterType.DOUBLE;

/**
 * Disconnects one leg of a three-winding transformer. The network models a three-winding transformer as
 * three quadripoles {@code <id>_1}, {@code <id>_2}, {@code <id>_3}, so disconnecting the transformer means
 * one {@code EventQuadripoleDisconnection} per leg, each opening both ends of its leg at the network model —
 * the way the launcher does.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public class EventThreeWindingsTransformerDisconnection extends AbstractEvent {

    private static final EventModelInfo MODEL_INFO = new EventModelInfo("Disconnect", "Disconnects a three-winding transformer, one quadripole event per leg.");

    private final String legId;

    protected EventThreeWindingsTransformerDisconnection(String eventId, ThreeWindingsTransformer equipment,
                                                         double startTime, String legId) {
        super(eventId, equipment, MODEL_INFO, startTime);
        this.legId = legId;
    }

    /**
     * The three per-leg disconnection events for a three-winding transformer — legs {@code <id>_1/_2/_3} —
     * or an empty list when it is de-energized or outside the main connected component (as the other
     * disconnection events are skipped).
     */
    public static List<EventThreeWindingsTransformerDisconnection> createLegEvents(ThreeWindingsTransformer twt, double startTime) {
        boolean energized = twt.getLegStream().anyMatch(leg -> EnergizedUtils.isEnergizedAndInMainConnectedComponent(leg.getTerminal()));
        if (!energized) {
            return List.of();
        }
        List<EventThreeWindingsTransformerDisconnection> events = new ArrayList<>(3);
        for (int leg = 1; leg <= 3; leg++) {
            String legId = twt.getId() + "_" + leg;
            events.add(new EventThreeWindingsTransformerDisconnection(MODEL_INFO.name() + "_" + legId, twt, startTime, legId));
        }
        return events;
    }

    @Override
    public void createMacroConnections(MacroConnectionsAdder adder) {
        DefaultTransformer leg = new DefaultTransformer(legId);
        adder.createMacroConnections(this, leg, List.of(new VarConnection("event_state1_value", leg.getStateValueVarName())));
    }

    @Override
    protected void createEventSpecificParameters(ParametersSet paramSet) {
        paramSet.addParameter("event_tEvent", DOUBLE, Double.toString(getStartTime()));
        paramSet.addParameter("event_disconnectOrigin", BOOL, Boolean.toString(true));
        paramSet.addParameter("event_disconnectExtremity", BOOL, Boolean.toString(true));
    }

    @Override
    public String getLib() {
        return "EventQuadripoleDisconnection";
    }
}
