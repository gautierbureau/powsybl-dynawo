/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.preassembled;

import com.powsybl.dynawo.mappings.preassembled.MachineControlUnit.MachineQuantity;
import com.powsybl.dynawo.mappings.preassembled.MachineControlUnit.RegulatorInput;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A control acting on the machine through its voltage regulator: a stabiliser damping the
 * oscillations of the machine, a limiter holding its excitation or its stator current within what
 * it can stand.
 * <p>
 * It watches the machine and drives an input of the regulator, which is the one naming that input.
 * The same stabiliser therefore meets {@code UPssPu} on one regulator and {@code UpssPu} on
 * another without knowing it, and a regulator offering no such input simply cannot carry it.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public class RegulatorControlUnit implements UnitModel {

    private record Watch(String ownVar, MachineQuantity quantity, boolean initialisation) {
    }

    private record Drive(String ownVar, RegulatorInput input, boolean initialisation) {
    }

    private final String id;
    private final String name;
    private final String initName;
    private final List<Watch> watches = new ArrayList<>();
    private final List<Drive> drives = new ArrayList<>();

    public RegulatorControlUnit(String id, String name, String initName) {
        this.id = Objects.requireNonNull(id);
        this.name = Objects.requireNonNull(name);
        this.initName = initName;
    }

    public RegulatorControlUnit reading(String ownVar, MachineQuantity quantity) {
        watches.add(new Watch(ownVar, quantity, false));
        return this;
    }

    public RegulatorControlUnit startingFrom(String ownVar, MachineQuantity quantity) {
        watches.add(new Watch(ownVar, quantity, true));
        return this;
    }

    public RegulatorControlUnit driving(String ownVar, RegulatorInput input) {
        drives.add(new Drive(ownVar, input, false));
        return this;
    }

    public RegulatorControlUnit startingFrom(String ownVar, RegulatorInput input) {
        drives.add(new Drive(ownVar, input, true));
        return this;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getInitName() {
        return initName;
    }

    public List<UnitConnection> getConnectionsWith(MachineUnit machine, MachineControlUnit regulator) {
        List<UnitConnection> connections = new ArrayList<>();
        watches.forEach(watch -> connections.add(new UnitConnection(id, watch.ownVar(), machine.getId(),
                watch.quantity().of(machine), watch.initialisation())));
        drives.forEach(drive -> regulator.getInputVarName(drive.input())
                .ifPresent(varName -> connections.add(new UnitConnection(id, drive.ownVar(), regulator.getId(),
                        varName, drive.initialisation()))));
        return connections;
    }
}
