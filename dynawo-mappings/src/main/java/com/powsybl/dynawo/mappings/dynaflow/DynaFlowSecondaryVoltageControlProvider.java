/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.dynaflow;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auto.service.AutoService;
import com.powsybl.dynawo.mappings.DynamicMappingExtensionsProvider;
import com.powsybl.dynawo.mappings.MappingParameters;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.iidm.network.extensions.ControlZoneAdder;
import com.powsybl.iidm.network.extensions.PilotPoint;
import com.powsybl.iidm.network.extensions.SecondaryVoltageControl;
import com.powsybl.iidm.network.extensions.SecondaryVoltageControlAdder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads a network's secondary voltage control zones from a JSON resource and adds them as the {@code
 * secondaryVoltageControl} extension the DynaFlow generator tree reads, so a machine in a zone runs a
 * reactive-power-control-loop model and each zone gets its simplified control.
 * <p>
 * DynaFlow deduces most of a machine's model from the network, but the zones — which the launcher's
 * assembling declared — cannot be deduced, so a study points this provider at its own JSON with the
 * {@code svc_zones_resource} parameter. Each entry is one zone: its pilot point (buses, each with its
 * voltage level, apart from busbar sections) and the generators it coordinates. Discovered with a
 * {@link java.util.ServiceLoader} and chosen by name; a network already carrying the extension is left
 * as it is.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
@AutoService(DynamicMappingExtensionsProvider.class)
public class DynaFlowSecondaryVoltageControlProvider implements DynamicMappingExtensionsProvider {

    public static final String NAME = "DynaFlowSecondaryVoltageControls";

    /** Where the JSON is read from — a study's own, there being no network-agnostic default. */
    static final String RESOURCE_PARAMETER = "svc_zones_resource";

    private static final Logger LOGGER = LoggerFactory.getLogger(DynaFlowSecondaryVoltageControlProvider.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String resource;

    public DynaFlowSecondaryVoltageControlProvider() {
        this(null);
    }

    private DynaFlowSecondaryVoltageControlProvider(String resource) {
        this.resource = resource;
    }

    /** One control zone as the JSON holds it: the pilot point it holds and the generators it coordinates. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record ZoneJson(PilotPointJson pilotPoint, List<String> generators) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PilotPointJson(List<BusJson> buses, List<String> busbarSectionIds) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        record BusJson(String voltageLevelId, String busId) {
        }
    }

    @Override
    public String getExtensionName() {
        return SecondaryVoltageControl.NAME;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "Adds the secondary voltage control zones read from the JSON resource a study points at.";
    }

    @Override
    public DynamicMappingExtensionsProvider configured(MappingParameters parameters) {
        return new DynaFlowSecondaryVoltageControlProvider(parameters.getString(RESOURCE_PARAMETER).orElse(null));
    }

    @Override
    public void createExtensions(Network network) {
        if (network.getExtension(SecondaryVoltageControl.class) != null) {
            return;
        }
        if (resource == null) {
            LOGGER.warn("No {} set, no secondary voltage control zone added", RESOURCE_PARAMETER);
            return;
        }
        Map<String, ZoneJson> zones = read();
        if (zones.isEmpty()) {
            return;
        }
        SecondaryVoltageControlAdder svcAdder = network.newExtension(SecondaryVoltageControlAdder.class);
        zones.forEach((name, zone) -> addZone(network, svcAdder, name, zone));
        svcAdder.add();
    }

    private static void addZone(Network network, SecondaryVoltageControlAdder svcAdder, String name, ZoneJson zone) {
        ControlZoneAdder zoneAdder = svcAdder.newControlZone().withName(name);
        PilotPointJson pilotPoint = zone.pilotPoint();
        List<String> busbarSectionIds = pilotPoint == null ? List.of() : orEmpty(pilotPoint.busbarSectionIds());
        List<PilotPointJson.BusJson> buses = pilotPoint == null ? List.of() : orEmpty(pilotPoint.buses());
        zoneAdder.newPilotPoint()
                .withBusbarSectionIds(busbarSectionIds)
                .withBuses(buses.stream().map(bus -> new PilotPoint.BusRef(bus.voltageLevelId(), bus.busId())).toList())
                .withTargetV(pilotTargetV(network, busbarSectionIds, buses))
                .add();
        for (String generatorId : orEmpty(zone.generators())) {
            zoneAdder.newControlUnit().withId(generatorId).withParticipate(true).add();
        }
        zoneAdder.add();
    }

    /** The pilot point's nominal voltage, from its first busbar section or, failing that, its first bus. */
    private static double pilotTargetV(Network network, List<String> busbarSectionIds, List<PilotPointJson.BusJson> buses) {
        for (String busbarSectionId : busbarSectionIds) {
            if (network.getBusbarSection(busbarSectionId) != null) {
                return network.getBusbarSection(busbarSectionId).getTerminal().getVoltageLevel().getNominalV();
            }
        }
        for (PilotPointJson.BusJson bus : buses) {
            VoltageLevel voltageLevel = network.getVoltageLevel(bus.voltageLevelId());
            if (voltageLevel != null) {
                return voltageLevel.getNominalV();
            }
        }
        return 0;
    }

    private Map<String, ZoneJson> read() {
        try (InputStream in = getClass().getResourceAsStream(resource)) {
            if (in == null) {
                LOGGER.warn("Secondary voltage control resource {} not found, no zone added", resource);
                return Map.of();
            }
            return MAPPER.readValue(in, new TypeReference<LinkedHashMap<String, ZoneJson>>() {
            });
        } catch (IOException e) {
            throw new UncheckedIOException("Reading secondary voltage control resource " + resource, e);
        }
    }

    private static <T> List<T> orEmpty(List<T> list) {
        return list == null ? List.of() : list;
    }
}
