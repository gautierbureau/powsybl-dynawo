/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.models.svc;

import com.powsybl.dynawo.builders.ModelConfig;
import com.powsybl.dynawo.models.AbstractPureDynamicBlackBoxModel;
import com.powsybl.dynawo.models.VarConnection;
import com.powsybl.dynawo.models.macroconnections.MacroConnectAttribute;
import com.powsybl.dynawo.models.macroconnections.MacroConnectionsAdder;
import com.powsybl.iidm.network.Identifiable;

import java.util.List;
import java.util.Objects;

/**
 * The simplified secondary voltage control (SVC) DynaFlow runs — {@code
 * DYNModelSecondaryVoltageControlSimplified}. It coordinates the reactive power of the machines in one
 * control zone to hold the voltage at a pilot point, the way the DynaFlow Launcher's assembling wires it.
 * <p>
 * It reads each machine's stator reactive power ({@code QStator_@INDEX@_value}) and reactive-limit
 * blocker ({@code blocker_@INDEX@_value}), writes back one shared control level ({@code level_value}),
 * and reads the pilot bus's per-unit voltage ({@code UpPu_value}) from the {@code NETWORK} model. Only a
 * machine on an {@code Rpcl} model carries these variables, so a machine that carries none is skipped.
 * This is the simplified cousin of the RTE detailed control: three variables per machine, not seven.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public class SecondaryVoltageControlSimplified extends AbstractPureDynamicBlackBoxModel {

    private final List<Identifiable<?>> generators;
    private final String pilotBusId;

    public SecondaryVoltageControlSimplified(String dynamicModelId, String parameterSetId,
                                             List<Identifiable<?>> generators, String pilotBusId,
                                             ModelConfig modelConfig) {
        super(dynamicModelId, parameterSetId, modelConfig);
        this.generators = Objects.requireNonNull(generators);
        this.pilotBusId = Objects.requireNonNull(pilotBusId);
    }

    @Override
    public void createMacroConnections(MacroConnectionsAdder adder) {
        int index = 0;
        for (Identifiable<?> generator : generators) {
            boolean skipped = adder.createMacroConnectionsOrSkip(this, generator, RpclGeneratorModel.class,
                    this::getVarConnectionsWith, MacroConnectAttribute.ofIndex1(index));
            if (!skipped) {
                index++;
            }
        }
        SvcPilotPoint pilot = new SvcPilotPoint(pilotBusId);
        adder.createMacroConnections(this, pilot, getVarConnectionsWithBus(pilot));
    }

    private List<VarConnection> getVarConnectionsWith(RpclGeneratorModel generator) {
        return List.of(
                new VarConnection("QStator_@INDEX@_value", generator.getQStatorVarName()),
                new VarConnection("blocker_@INDEX@_value", generator.getBlockerVarName()),
                new VarConnection("level_value", generator.getLevelVarName()));
    }

    private List<VarConnection> getVarConnectionsWithBus(SvcPilotPoint pilot) {
        return List.of(new VarConnection("UpPu_value", pilot.getUpuVarName()));
    }
}
