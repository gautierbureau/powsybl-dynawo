/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.dynaflow;

import com.powsybl.dynamicsimulation.DynamicModel;
import com.powsybl.dynawo.builders.ModelBuilder;
import com.powsybl.dynawo.mappings.MappedModelsSupplier.MappedModel;
import com.powsybl.dynawo.models.svc.SecondaryVoltageControlSimplifiedBuilder;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.extensions.ControlUnit;
import com.powsybl.iidm.network.extensions.ControlZone;
import com.powsybl.iidm.network.extensions.PilotPoint;
import com.powsybl.iidm.network.extensions.SecondaryVoltageControl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Gives each secondary voltage control zone its {@code DYNModelSecondaryVoltageControlSimplified} model,
 * the way the DynaFlow Launcher's assembling wires it — the counterpart, on the mapping side, of the
 * generator tree that puts a zone's machines on an {@code Rpcl} model.
 * <p>
 * The zones are read from the {@code secondaryVoltageControl} extension: each control zone gives one SVC
 * model, coordinating the zone's control units (its generators) to hold the voltage at the zone's pilot
 * point. Like an automaton, the model is set up by a hand of its own — its generators and pilot bus
 * passed to the builder — since a bare static id cannot say what it watches and acts on.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
final class DynaFlowSvcMapping {

    /** The simplified secondary voltage control library each zone runs. */
    static final String LIB = "DYNModelSecondaryVoltageControlSimplified";

    List<MappedModel> createModelConfigs(Network network) {
        SecondaryVoltageControl secondaryVoltageControl = network.getExtension(SecondaryVoltageControl.class);
        if (secondaryVoltageControl == null) {
            return List.of();
        }
        List<MappedModel> models = new ArrayList<>();
        for (ControlZone zone : secondaryVoltageControl.getControlZones()) {
            String pilotBusId = pilotBusId(zone.getPilotPoint());
            List<String> generators = zone.getControlUnits().stream().map(ControlUnit::getId).toList();
            if (pilotBusId != null && !generators.isEmpty()) {
                models.add(new MappedModel(LIB, zone.getName(), zone.getName(),
                        configurer(zone.getName(), generators, pilotBusId)));
            }
        }
        return models;
    }

    private static Consumer<ModelBuilder<DynamicModel>> configurer(String name, List<String> generators, String pilotBusId) {
        return builder -> {
            SecondaryVoltageControlSimplifiedBuilder svcBuilder = (SecondaryVoltageControlSimplifiedBuilder) builder;
            svcBuilder.dynamicModelId(name);
            svcBuilder.parameterSetId(name);
            svcBuilder.generators(generators);
            svcBuilder.pilotPoint(pilotBusId);
        };
    }

    /** The bus whose voltage the zone holds: its first busbar section, or failing that its first bus. */
    private static String pilotBusId(PilotPoint pilotPoint) {
        if (!pilotPoint.getBusbarSectionIds().isEmpty()) {
            return pilotPoint.getBusbarSectionIds().get(0);
        }
        if (!pilotPoint.getBuses().isEmpty()) {
            return pilotPoint.getBuses().get(0).busId();
        }
        return null;
    }
}
