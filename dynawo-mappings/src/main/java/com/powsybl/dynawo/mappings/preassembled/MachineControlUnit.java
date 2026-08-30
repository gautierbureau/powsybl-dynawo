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
        STATOR_CURRENT_MAGNITUDE(MachineUnit::getStatorCurrentMagnitudeVarName),
        SPEED(MachineUnit::getSpeedVarName),
        MECHANICAL_POWER(MachineUnit::getMechanicalPowerVarName),
        ACTIVE_POWER(MachineUnit::getActivePowerVarName),
        REACTIVE_POWER(MachineUnit::getReactivePowerVarName),
        STATOR_REACTIVE_POWER(MachineUnit::getStatorReactivePowerVarName),
        NOMINAL_STATOR_REACTIVE_POWER(MachineUnit::getNominalStatorReactivePowerVarName),
        ROTOR_ANGLE(MachineUnit::getRotorAngleVarName),
        DIRECT_AXIS_CURRENT(MachineUnit::getDirectAxisCurrentVarName),
        RUNNING(MachineUnit::getRunningVarName),
        VOLTAGE_MAGNITUDE(MachineUnit::getVoltageMagnitudeVarName),
        INIT_STATOR_VOLTAGE(MachineUnit::getInitStatorVoltageVarName),
        INIT_FIELD_VOLTAGE(MachineUnit::getInitFieldVoltageVarName),
        INIT_ROTOR_CURRENT(MachineUnit::getInitRotorCurrentVarName),
        INIT_TERMINAL_VOLTAGE(MachineUnit::getInitTerminalVoltageVarName),
        INIT_CURRENT(MachineUnit::getInitCurrentVarName),
        INIT_STATOR_CURRENT_MAGNITUDE(MachineUnit::getInitStatorCurrentMagnitudeVarName),
        INIT_MECHANICAL_POWER(MachineUnit::getInitMechanicalPowerVarName),
        INIT_ACTIVE_POWER(MachineUnit::getInitActivePowerVarName),
        INIT_REACTIVE_POWER(MachineUnit::getInitReactivePowerVarName),
        INIT_STATOR_REACTIVE_POWER(MachineUnit::getInitStatorReactivePowerVarName),
        INIT_NOMINAL_STATOR_REACTIVE_POWER(MachineUnit::getInitNominalStatorReactivePowerVarName),
        INIT_ROTOR_ANGLE(MachineUnit::getInitRotorAngleVarName),
        INIT_DIRECT_AXIS_CURRENT(MachineUnit::getInitDirectAxisCurrentVarName),
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
        VOLTAGE_REFERENCE,
        LIMITATION_UP,
        LIMITATION_DOWN,
        INIT_VOLTAGE_REFERENCE
    }

    private record Wire(String ownVar, MachineQuantity quantity, boolean initialisation) {
    }

    private record RegulatorLink(String var, boolean initialisation) {
    }

    private final String id;
    private final String name;
    private final String initName;
    private String catalogName;
    private MachineControlUnit exciter;
    private final List<Wire> wires = new ArrayList<>();
    private final List<RegulatorLink> regulatorLinks = new ArrayList<>();
    private final List<RegulatorLink> governorLinks = new ArrayList<>();
    private final Map<RegulatorInput, String> inputs = new EnumMap<>(RegulatorInput.class);

    public MachineControlUnit(String id, String name, String initName) {
        this.id = Objects.requireNonNull(id);
        this.name = Objects.requireNonNull(name);
        this.initName = initName;
    }

    /**
     * Gives this regulator the exciter it drives, a downstream control it brings with it though the
     * model is not named for it: {@code VRRance} drives {@code ExHydrH}. The exciter watches the
     * machine on its own account and states, itself, how it is {@link #linkedToRegulator wired to}
     * the regulator, so this only has to name it.
     */
    public MachineControlUnit driving(MachineControlUnit exciter) {
        this.exciter = Objects.requireNonNull(exciter);
        return this;
    }

    /**
     * The exciter this regulator drives, or null where it drives none.
     */
    public MachineControlUnit getExciter() {
        return exciter;
    }

    /**
     * A variable this exciter shares with the regulator driving it while the simulation runs, its
     * output {@code VROutPu} or the current {@code IExPu} it sends back, the same on both sides.
     */
    public MachineControlUnit linkedToRegulator(String var) {
        regulatorLinks.add(new RegulatorLink(var, false));
        return this;
    }

    /**
     * A variable this exciter shares with the regulator while the initial state is worked out.
     */
    public MachineControlUnit linkedToRegulatorAtInit(String var) {
        regulatorLinks.add(new RegulatorLink(var, true));
        return this;
    }

    /**
     * The connections between this exciter and the regulator driving it, each variable meeting its
     * namesake across the two.
     */
    List<UnitConnection> regulatorConnections(MachineControlUnit regulator) {
        return regulatorLinks.stream()
                .map(link -> new UnitConnection(regulator.getId(), link.var(), id, link.var(), link.initialisation()))
                .toList();
    }

    /**
     * A variable this regulator shares with the governor beside it, the mechanical power {@code
     * PmTurHpPu} a combined regulator takes from its turbine, the same on both sides. Only some
     * regulators reach into the governor this way; most stand apart from it.
     */
    public MachineControlUnit linkedToGovernor(String var) {
        governorLinks.add(new RegulatorLink(var, false));
        return this;
    }

    boolean hasGovernorLinks() {
        return !governorLinks.isEmpty();
    }

    /**
     * The connections between this regulator and the governor beside it, each variable meeting its
     * namesake across the two.
     */
    List<UnitConnection> governorConnections(MachineControlUnit governor) {
        return governorLinks.stream()
                .map(link -> new UnitConnection(governor.getId(), link.var(), id, link.var(), link.initialisation()))
                .toList();
    }

    /**
     * Names this control in the catalog, and in a built model's library, apart from its Modelica
     * model, where it stands for that model under a name of its own: the fictitious regulator names
     * itself so though it is the {@code VRP320} model. Left unset, it goes by the last part of its
     * Modelica model, as a control that is its own model does.
     */
    public MachineControlUnit named(String catalogName) {
        this.catalogName = Objects.requireNonNull(catalogName);
        return this;
    }

    @Override
    public String getCatalogName() {
        return catalogName != null ? catalogName : ControlUnit.super.getCatalogName();
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
                // a machine with nothing to say about a quantity is not wired for it, which is how
                // a release that never carried one leaves the wire unmade rather than dangling
                .filter(wire -> wire.quantity().of(machine) != null)
                .map(wire -> new UnitConnection(id, wire.ownVar(), machine.getId(),
                        wire.quantity().of(machine), initialisation))
                .toList();
    }
}
