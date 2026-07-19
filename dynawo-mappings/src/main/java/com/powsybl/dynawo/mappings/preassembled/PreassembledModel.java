/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.preassembled;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A machine and the controls it carries, ready to be written as the definition Dynawo compiles.
 * <p>
 * Nothing here knows which controls go together: each one states what it wires to the machine, and
 * the assembly is their sum. That is what lets a governor and a voltage regulator that were never
 * assembled together be put on the same machine.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public class PreassembledModel {

    private final String id;
    private final MachineUnit machine;
    private final List<ControlUnit> controls = new ArrayList<>();
    private final List<UnitModel> extraUnits = new ArrayList<>();
    private final List<UnitConnection> extraConnections = new ArrayList<>();

    public PreassembledModel(String id, MachineUnit machine) {
        this.id = Objects.requireNonNull(id);
        this.machine = Objects.requireNonNull(machine);
    }

    public PreassembledModel add(ControlUnit control) {
        controls.add(Objects.requireNonNull(control));
        return this;
    }

    /**
     * Adds an element that is not a control of the machine: what stands between it and the grid,
     * what it consumes to run itself.
     */
    public PreassembledModel addUnit(UnitModel unit) {
        extraUnits.add(Objects.requireNonNull(unit));
        return this;
    }

    public PreassembledModel addConnections(List<UnitConnection> connections) {
        extraConnections.addAll(connections);
        return this;
    }

    public String getId() {
        return id;
    }

    public List<UnitModel> getUnits() {
        List<UnitModel> units = new ArrayList<>();
        units.add(machine);
        units.addAll(extraUnits);
        units.addAll(controls);
        return units;
    }

    /**
     * Every connection the assembly holds, those computing the initial state among them.
     */
    public List<UnitConnection> getConnections() {
        List<UnitConnection> connections = new ArrayList<>();
        controls.forEach(control -> {
            connections.addAll(control.getInitConnectionsWith(machine));
            connections.addAll(control.getConnectionsWith(machine));
        });
        connections.addAll(extraConnections);
        return connections;
    }
}
