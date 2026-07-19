/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.preassembled;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * A control wired straight to the machine: a governor, a voltage regulator.
 * <p>
 * Each one says which of its variables meets which quantity of the machine, naming the quantity
 * rather than the variable holding it, so that a machine model calling one of them differently is
 * described where it belongs and every control follows.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public class MachineControlUnit implements ControlUnit {

    /**
     * A quantity of the machine a control reads or drives.
     */
    public enum MachineQuantity {
        STATOR_VOLTAGE(MachineUnit::getStatorVoltageVarName),
        FIELD_VOLTAGE(MachineUnit::getFieldVoltageVarName),
        ROTOR_CURRENT(MachineUnit::getRotorCurrentVarName),
        TERMINAL_VOLTAGE(MachineUnit::getTerminalVoltageVarName),
        STATOR_CURRENT(MachineUnit::getStatorCurrentVarName),
        SPEED(MachineUnit::getSpeedVarName),
        MECHANICAL_POWER(MachineUnit::getMechanicalPowerVarName),
        ACTIVE_POWER(MachineUnit::getActivePowerVarName),
        REACTIVE_POWER(MachineUnit::getReactivePowerVarName),
        RUNNING(MachineUnit::getRunningVarName),
        VOLTAGE_MAGNITUDE(MachineUnit::getVoltageMagnitudeVarName),
        INIT_STATOR_VOLTAGE(MachineUnit::getInitStatorVoltageVarName),
        INIT_FIELD_VOLTAGE(MachineUnit::getInitFieldVoltageVarName),
        INIT_ROTOR_CURRENT(MachineUnit::getInitRotorCurrentVarName),
        INIT_TERMINAL_VOLTAGE(MachineUnit::getInitTerminalVoltageVarName),
        INIT_CURRENT(MachineUnit::getInitCurrentVarName),
        INIT_MECHANICAL_POWER(MachineUnit::getInitMechanicalPowerVarName),
        INIT_ACTIVE_POWER(MachineUnit::getInitActivePowerVarName),
        INIT_REACTIVE_POWER(MachineUnit::getInitReactivePowerVarName),
        INIT_VOLTAGE_MAGNITUDE(MachineUnit::getInitVoltageMagnitudeVarName);

        private final Function<MachineUnit, String> varName;

        MachineQuantity(Function<MachineUnit, String> varName) {
            this.varName = varName;
        }

        String of(MachineUnit machine) {
            return varName.apply(machine);
        }
    }

    /**
     * Something another control drives on a voltage regulator: the signal a stabiliser adds, the
     * one a limiter forces. The regulator names it, which is why the same stabiliser meets
     * {@code UPssPu} on one and {@code UpssPu} on another.
     */
    public enum RegulatorInput {
        STABILISER,
        OVER_EXCITATION,
        UNDER_EXCITATION,
        STATOR_CURRENT_OVER_EXCITATION,
        STATOR_CURRENT_UNDER_EXCITATION,
        FEEDBACK,
        INIT_VOLTAGE_REFERENCE
    }

    private record Wire(String ownVar, MachineQuantity quantity, boolean initialisation) {
    }

    private final String id;
    private final String name;
    private final String initName;
    private final List<Wire> wires = new ArrayList<>();
    private final Map<RegulatorInput, String> inputs = new EnumMap<>(RegulatorInput.class);

    public MachineControlUnit(String id, String name, String initName) {
        this.id = Objects.requireNonNull(id);
        this.name = Objects.requireNonNull(name);
        this.initName = initName;
    }

    /**
     * Wires one of its variables to a quantity of the machine while the simulation runs.
     */
    public MachineControlUnit reading(String ownVar, MachineQuantity quantity) {
        wires.add(new Wire(ownVar, quantity, false));
        return this;
    }

    /**
     * Wires one of its variables to a quantity of the machine while the initial state is worked
     * out.
     */
    public MachineControlUnit startingFrom(String ownVar, MachineQuantity quantity) {
        wires.add(new Wire(ownVar, quantity, true));
        return this;
    }

    /**
     * Names one of the inputs another control drives on this one.
     */
    public MachineControlUnit accepting(RegulatorInput input, String varName) {
        inputs.put(input, varName);
        return this;
    }

    public Optional<String> getInputVarName(RegulatorInput input) {
        return Optional.ofNullable(inputs.get(input));
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

    @Override
    public List<UnitConnection> getConnectionsWith(MachineUnit machine) {
        return connections(machine, false);
    }

    @Override
    public List<UnitConnection> getInitConnectionsWith(MachineUnit machine) {
        return connections(machine, true);
    }

    private List<UnitConnection> connections(MachineUnit machine, boolean initialisation) {
        return wires.stream()
                .filter(wire -> wire.initialisation() == initialisation)
                .map(wire -> new UnitConnection(id, wire.ownVar(), machine.getId(),
                        wire.quantity().of(machine), initialisation))
                .toList();
    }
}
