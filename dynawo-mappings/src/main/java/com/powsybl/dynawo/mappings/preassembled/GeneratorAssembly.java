/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.preassembled;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dynawo.extensions.api.generator.SynchronousGeneratorProperties.Windings;

import java.util.ArrayList;
import java.util.List;

/**
 * A machine, its controls, and what stands between it and the grid.
 * <p>
 * The elements between the machine and the grid form a chain: the machine, then its transformer if
 * it has one, then the switch coupling it to the network if its auxiliaries need one. The first
 * link initialises the machine, which is why giving a unit its auxiliaries changes how it is
 * initialised even though nothing was said about a transformer, and why the two cannot be added
 * one after the other as if they were independent.
 * <p>
 * Its auxiliaries hang on the two ends of that chain, the machine side ones on the node the
 * machine sits on and the grid side ones on the far end of the transformer.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public class GeneratorAssembly {

    private final GeneratorSynchronousUnit machine;
    private final GeneratorTransformerUnit transformer;
    private final CouplingUnit coupling;
    private final AuxiliaryUnit machineSideAuxiliaries;
    private final AuxiliaryUnit gridSideAuxiliaries;
    private final List<ControlUnit> controls = new ArrayList<>();
    private final List<RegulatorControlUnit> regulatorControls = new ArrayList<>();

    public GeneratorAssembly(Windings windings, boolean withTransformer, boolean withAuxiliaries) {
        this(windings, withTransformer, withAuxiliaries, ModelNaming.CURRENT);
    }

    /**
     * @param naming what the Dynawo release this is built for calls the models and the quantities
     *               they exchange
     */
    public GeneratorAssembly(Windings windings, boolean withTransformer, boolean withAuxiliaries,
                             ModelNaming naming) {
        // a machine initialised through something reads its operating point from that element
        this.machine = new GeneratorSynchronousUnit(windings, withTransformer || withAuxiliaries, naming);
        this.transformer = withTransformer ? new GeneratorTransformerUnit(withAuxiliaries, naming) : null;
        // the switch is there for the auxiliaries; next to the machine it initialises it, behind a
        // transformer it only carries the grid side ones
        this.coupling = withAuxiliaries ? new CouplingUnit(!withTransformer) : null;
        this.machineSideAuxiliaries = withAuxiliaries ? new AuxiliaryUnit(false) : null;
        this.gridSideAuxiliaries = withAuxiliaries && withTransformer ? new AuxiliaryUnit(true) : null;
    }

    public GeneratorAssembly add(ControlUnit control) {
        controls.add(control);
        return this;
    }

    /**
     * Adds a control acting on the machine through its voltage regulator: a stabiliser, a limiter.
     * <p>
     * It is the regulator that names the input such a control drives, so one can only be placed on
     * an assembly that has a regulator to place it on, and a regulator offering no such input
     * simply does not get that wire.
     */
    public GeneratorAssembly add(RegulatorControlUnit control) {
        regulatorControls.add(control);
        return this;
    }

    public PreassembledModel build(String id) {
        PreassembledModel model = new PreassembledModel(id, machine);
        controls.forEach(model::add);
        drivenExciters(model);
        governorLinks(model, id);
        units().forEach(model::addUnit);
        model.addConnections(connections());
        regulatorControls.forEach(control -> {
            model.addUnit(control);
            model.addConnections(control.getConnectionsWith(machine, voltageRegulator(id)));
        });
        return model;
    }

    /**
     * The exciter a regulator drives, added beside it and wired to it: it watches the machine on its
     * own account, its connections following from its being a control of the model, and takes the
     * regulator's output through the one variable that stands between them.
     */
    private void drivenExciters(PreassembledModel model) {
        controls.stream()
                .filter(MachineControlUnit.class::isInstance)
                .map(MachineControlUnit.class::cast)
                .filter(regulator -> regulator.getExciter() != null)
                .forEach(regulator -> {
                    MachineControlUnit exciter = regulator.getExciter();
                    model.add(exciter);
                    model.addConnections(exciter.regulatorConnections(regulator));
                });
    }

    /**
     * The regulator a stabiliser or a limiter acts through, which the assembly must have been
     * given before it can be told what acts on it.
     */
    private MachineControlUnit voltageRegulator(String id) {
        return controls.stream()
                .filter(MachineControlUnit.class::isInstance)
                .map(MachineControlUnit.class::cast)
                .filter(control -> "voltageRegulator".equals(control.getId()))
                .findFirst()
                .orElseThrow(() -> new PowsyblException(
                        "No voltage regulator on " + id + " for its stabilisers and limiters to act through"));
    }

    /**
     * Wires a regulator that reaches into the governor beside it, a combined regulator taking its
     * turbine's mechanical power, to that governor.
     */
    private void governorLinks(PreassembledModel model, String id) {
        controls.stream()
                .filter(MachineControlUnit.class::isInstance)
                .map(MachineControlUnit.class::cast)
                .filter(MachineControlUnit::hasGovernorLinks)
                .forEach(regulator -> model.addConnections(regulator.governorConnections(governor(id))));
    }

    private MachineControlUnit governor(String id) {
        return controls.stream()
                .filter(MachineControlUnit.class::isInstance)
                .map(MachineControlUnit.class::cast)
                .filter(control -> "governor".equals(control.getId()))
                .findFirst()
                .orElseThrow(() -> new PowsyblException(
                        "No governor on " + id + " for its regulator to take mechanical power from"));
    }

    private List<UnitModel> units() {
        List<UnitModel> units = new ArrayList<>();
        addIfPresent(units, transformer);
        addIfPresent(units, coupling);
        addIfPresent(units, machineSideAuxiliaries);
        addIfPresent(units, gridSideAuxiliaries);
        return units;
    }

    private static void addIfPresent(List<UnitModel> units, UnitModel unit) {
        if (unit != null) {
            units.add(unit);
        }
    }

    private List<UnitConnection> connections() {
        List<UnitConnection> connections = new ArrayList<>();
        SeriesUnit machineSide = transformer != null ? transformer : coupling;
        if (machineSide == null) {
            return connections;
        }
        connections.addAll(initialiseMachineThrough(machineSide));
        connections.add(UnitConnection.of(machine, "terminal", machineSide, machineSide.getMachineSideTerminalVarName()));
        connections.add(UnitConnection.of(machine, "switchOffSignal1", machineSide, machineSide.getSwitchOffSignalVarName()));
        if (transformer != null && coupling != null) {
            connections.add(UnitConnection.of(transformer, transformer.getGridSideTerminalVarName(),
                    coupling, coupling.getMachineSideTerminalVarName()));
        }
        addIfPresent(connections, machineSideAuxiliaries, machineSide);
        addIfPresent(connections, gridSideAuxiliaries, machineSide);
        return connections;
    }

    /**
     * The machine reads its operating point from the machine side of the first link.
     */
    private List<UnitConnection> initialiseMachineThrough(SeriesUnit machineSide) {
        List<UnitConnection> connections = new ArrayList<>();
        connections.add(UnitConnection.ofInitialisation(machine, "U0Pu", machineSide, machineSide.getInitMachineSideVoltageVarName()));
        connections.add(UnitConnection.ofInitialisation(machine, "UPhase0", machineSide, machineSide.getInitMachineSideAngleVarName()));
        if (machineSide.initialisesMachineWithCurrent()) {
            // the RTE external init reads the machine's complex terminal current in one connect, where
            // the open init reads its active and reactive power apart
            connections.add(UnitConnection.ofInitialisation(machine, "i0Pu", machineSide, machineSide.getInitMachineSideCurrentVarName()));
        } else {
            connections.add(UnitConnection.ofInitialisation(machine, "P0Pu", machineSide, machineSide.getInitMachineSideActivePowerVarName()));
            connections.add(UnitConnection.ofInitialisation(machine, "Q0Pu", machineSide, machineSide.getInitMachineSideReactivePowerVarName()));
        }
        return connections;
    }

    private void addIfPresent(List<UnitConnection> connections, AuxiliaryUnit auxiliaries, SeriesUnit machineSide) {
        if (auxiliaries == null) {
            return;
        }
        boolean gridSide = auxiliaries.isGridSide();
        connections.add(UnitConnection.ofInitialisation(auxiliaries, auxiliaries.getInitVoltageVarName(),
                machineSide, machineSide.getInitAuxiliaryVoltageVarName(gridSide)));
        connections.add(UnitConnection.ofInitialisation(machineSide, machineSide.getInitAuxiliaryActivePowerVarName(gridSide),
                auxiliaries, auxiliaries.getInitActivePowerVarName()));
        connections.add(UnitConnection.ofInitialisation(machineSide, machineSide.getInitAuxiliaryReactivePowerVarName(gridSide),
                auxiliaries, auxiliaries.getInitReactivePowerVarName()));
        connections.add(UnitConnection.of(auxiliaries, auxiliaries.getTerminalVarName(), node(gridSide), terminalOf(gridSide)));
        UnitModel switchedBy = gridSide ? machineSide : machine;
        connections.add(UnitConnection.of(switchedBy, "switchOffSignal1", auxiliaries, auxiliaries.getSwitchOffSignalVarName()));
    }

    /**
     * Which model names the node the auxiliaries hang on. The grid side ones hang on the coupling,
     * the machine side ones on the machine when a transformer stands beside it, on the coupling
     * otherwise, following the definitions Dynawo ships.
     */
    private UnitModel node(boolean gridSide) {
        if (gridSide) {
            return coupling;
        }
        return transformer != null ? machine : coupling;
    }

    private String terminalOf(boolean gridSide) {
        if (gridSide) {
            return coupling.getMachineSideTerminalVarName();
        }
        return transformer != null ? "terminal" : coupling.getMachineSideTerminalVarName();
    }
}
