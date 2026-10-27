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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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

    /**
     * What part such a control plays. It is declared rather than read off the inputs it drives:
     * a limiter may drive several, an under excitation limiter its regulator's feedback beside its
     * own input for one, and nothing in the wiring tells a control of a kind nobody foresaw.
     */
    public enum Kind {
        PSS,
        OVER_EXCITATION_LIMITER,
        UNDER_EXCITATION_LIMITER,
        STATOR_CURRENT_LIMITER,
        REACTIVE_POWER_CONTROL_LOOP
    }

    /**
     * The kind of the controls the open catalog declared before the kind was, by the id they are
     * given inside a model, for a control declared without one.
     */
    private static final Map<String, Kind> KIND_BY_INSTANCE_ID = Map.of(
            "powerSystemStabilizer", Kind.PSS,
            "pss", Kind.PSS,
            "overExcitationLimiter", Kind.OVER_EXCITATION_LIMITER,
            "underExcitationLimiter", Kind.UNDER_EXCITATION_LIMITER,
            "statorCurrentLimiter", Kind.STATOR_CURRENT_LIMITER,
            "reactivePowerControlLoop", Kind.REACTIVE_POWER_CONTROL_LOOP);

    /**
     * One of its variables wired to a quantity of the machine, while the simulation runs or while
     * the initial state is computed.
     */
    public record Watch(String ownVar, MachineQuantity quantity, boolean initialisation) {
    }

    /**
     * One of its variables wired to an input of the voltage regulator, which names it.
     */
    public record Drive(String ownVar, RegulatorInput input, boolean initialisation) {
    }

    private final Kind kind;
    private final String id;
    private final String name;
    private final String initName;
    private String catalogName;
    private final List<Watch> watches = new ArrayList<>();
    private final List<Drive> drives = new ArrayList<>();

    /**
     * @param kind     what part it plays
     * @param id       how it is named inside a model, which prefixes its parameters
     * @param name     the Modelica model it stands for
     * @param initName the Modelica model computing its initial state, or null where it needs none
     */
    public RegulatorControlUnit(Kind kind, String id, String name, String initName) {
        this.kind = Objects.requireNonNull(kind);
        this.id = Objects.requireNonNull(id);
        this.name = Objects.requireNonNull(name);
        this.initName = initName;
    }

    /**
     * A control whose kind is read off the id it is given inside a model, where it is one the open
     * catalog uses, and left unknown otherwise.
     *
     * @deprecated declare the kind, with {@link #RegulatorControlUnit(Kind, String, String, String)}
     */
    @Deprecated(since = "3.4.0")
    public RegulatorControlUnit(String id, String name, String initName) {
        this.kind = KIND_BY_INSTANCE_ID.get(Objects.requireNonNull(id));
        this.id = id;
        this.name = Objects.requireNonNull(name);
        this.initName = initName;
    }

    /**
     * What part it plays, or nothing where it was declared without saying and its id does not tell.
     */
    public Optional<Kind> getKind() {
        return Optional.ofNullable(kind);
    }

    /**
     * Its variables wired to the machine, in the order they were declared.
     */
    public List<Watch> getWatches() {
        return Collections.unmodifiableList(watches);
    }

    /**
     * Its variables wired to the inputs of the voltage regulator, in the order they were declared.
     */
    public List<Drive> getDrives() {
        return Collections.unmodifiableList(drives);
    }

    /**
     * Names this control in the catalog, and in a built model's library, apart from its Modelica
     * model, as {@link MachineControlUnit#named} does for a governor or a regulator. Left unset, it
     * goes by the last part of its Modelica model.
     */
    public RegulatorControlUnit named(String catalogName) {
        this.catalogName = Objects.requireNonNull(catalogName);
        return this;
    }

    /**
     * The name the catalog keys this control under, and a built model carries in its library.
     */
    public String getCatalogName() {
        return catalogName != null ? catalogName : name.substring(name.lastIndexOf('.') + 1);
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
        watches.stream()
                .filter(watch -> watch.quantity().of(machine) != null)
                .forEach(watch -> connections.add(new UnitConnection(id, watch.ownVar(), machine.getId(),
                        watch.quantity().of(machine), watch.initialisation())));
        drives.forEach(drive -> regulator.getInputVarName(drive.input())
                .ifPresent(varName -> connections.add(new UnitConnection(id, drive.ownVar(), regulator.getId(),
                        varName, drive.initialisation()))));
        return connections;
    }
}
