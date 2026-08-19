/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.dynaflow;

import com.powsybl.dynawo.extensions.api.generator.SynchronizedGeneratorProperties;
import com.powsybl.dynawo.mappings.MappedModelsSupplier.MappedModel;
import com.powsybl.dynawo.parameters.ParametersSet;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.EnergySource;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.MinMaxReactiveLimits;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.ReactiveCapabilityCurve;
import com.powsybl.iidm.network.ReactiveLimits;
import com.powsybl.iidm.network.ReactiveLimitsKind;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.iidm.network.extensions.ControlUnit;
import com.powsybl.iidm.network.extensions.ControlZone;
import com.powsybl.iidm.network.extensions.PilotPoint;
import com.powsybl.iidm.network.extensions.SecondaryVoltageControl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Gives every generator its DynaFlow model, the way the DynaFlow Launcher's
 * {@code GeneratorDefinitionAlgorithm} does.
 * <p>
 * A generator runs a dynamic model only where it holds a voltage on a valid operating point — the top
 * gate ({@code isTargetPValid && isVoltageRegulatorOn && isDiagramValid}); everything else keeps the
 * static {@code NETWORK} model. Which model it runs is read from the network alone: whether it regulates
 * its own bus or a remote one, whether one machine or several regulate that bus, whether its transformer
 * is described in the static model (a voltage threshold), and the shape of its reactive diagram
 * (infinite / rectangular / a genuine PQ curve). The result is one of the {@code GeneratorPV*SignalN} /
 * {@code GeneratorPQProp*SignalN} libraries, all sharing the single {@code SignalN} frequency signal.
 * <p>
 * A generator in a secondary voltage control zone — a control unit of the {@code secondaryVoltageControl}
 * extension — instead runs a reactive-power-control-loop model ({@code GeneratorPV*Rpcl*SignalN}), and a
 * second loop where a study marks it {@code Rpcl2} through the {@code synchronizedGeneratorProperties}
 * extension. So, unlike the rest of the tree, these two branches read the network's extensions rather
 * than deduce from it. Parameter sets (IIDM references) and the {@code VRRemote} wiring the remote models
 * need are the next step of the phase.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public final class DynaFlowGeneratorMapping {

    // Local generators (regulate their own bus). INFINITE and RECTANGULAR share a library; a genuine PQ
    // curve gets the DiagramPQ one. The Tfo variants apply when the transformer is not in the static model.
    static final String PV_SIGNALN = "GeneratorPVSignalN";
    static final String PV_DIAGRAM_PQ_SIGNALN = "GeneratorPVDiagramPQSignalN";
    static final String PV_TFO_SIGNALN = "GeneratorPVTfoSignalN";
    static final String PV_TFO_DIAGRAM_PQ_SIGNALN = "GeneratorPVTfoDiagramPQSignalN";

    // A generator regulating a remote bus, alone on that bus.
    static final String PV_REMOTE_SIGNALN = "GeneratorPVRemoteSignalN";
    static final String PV_REMOTE_DIAGRAM_PQ_SIGNALN = "GeneratorPVRemoteDiagramPQSignalN";

    // Several generators regulating one bus share its reactive power proportionally.
    static final String PQ_PROP_SIGNALN = "GeneratorPQPropSignalN";
    static final String PQ_PROP_DIAGRAM_PQ_SIGNALN = "GeneratorPQPropDiagramPQSignalN";

    // A generator in a secondary voltage control zone, on a reactive power control loop (Rpcl) or a
    // second one (Rpcl2). Same four transformer/diagram flavours as the plain local generators.
    static final String PV_RPCL_SIGNALN = "GeneratorPVRpclSignalN";
    static final String PV_RPCL2_SIGNALN = "GeneratorPVRpcl2SignalN";
    static final String PV_DIAGRAM_PQ_RPCL_SIGNALN = "GeneratorPVDiagramPQRpclSignalN";
    static final String PV_DIAGRAM_PQ_RPCL2_SIGNALN = "GeneratorPVDiagramPQRpcl2SignalN";
    static final String PV_TFO_RPCL_SIGNALN = "GeneratorPVTfoRpclSignalN";
    static final String PV_TFO_RPCL2_SIGNALN = "GeneratorPVTfoRpcl2SignalN";
    static final String PV_TFO_DIAGRAM_PQ_RPCL_SIGNALN = "GeneratorPVTfoDiagramPQRpclSignalN";
    static final String PV_TFO_DIAGRAM_PQ_RPCL2_SIGNALN = "GeneratorPVTfoDiagramPQRpcl2SignalN";

    // the shared, value-keyed parameter set ids the launcher gives its infinite generators (ParCommon.h /
    // OutputsConstants.h): several machines of a kind share one set, a nuclear one its own; a diagram
    // machine takes a set of its own, named after it.
    private static final String SIGNALN_SET = "signalNGenerator";
    private static final String SIGNALN_FIXED_P_SET = "signalNGeneratorFixedP";
    private static final String SIGNALN_TFO_SET = "signalNTfoGenerator";
    private static final String PROP_SET = "propSignalNGenerator";
    private static final String PROP_FIXED_P_SET = "propSignalNGeneratorFixedP";
    private static final String REMOTE_SET = "remoteVControl";
    private static final String REMOTE_FIXED_P_SET = "remoteSignalNFixedP";
    private static final String NUCLEAR_SUFFIX = "_Nuc";

    private static final double EPSILON = 1e-6;

    private final DynaFlowConfig config;

    DynaFlowGeneratorMapping(DynaFlowConfig config) {
        this.config = config;
    }

    public List<MappedModel> createModelConfigs(Network network) {
        Map<String, Integer> regulationCount = countRegulationsPerBus(network);
        Set<String> svcMembers = secondaryVoltageControlMembers(network);
        List<MappedModel> models = new ArrayList<>();
        for (Generator generator : network.getGenerators()) {
            String lib = selectLib(generator, regulationCount, svcMembers);
            if (lib != null) {
                models.add(new MappedModel(lib, generator.getId(), parameterSetId(generator, lib)));
            }
        }
        return models;
    }

    public List<ParametersSet> createParameters(Network network) {
        Map<String, Integer> regulationCount = countRegulationsPerBus(network);
        Set<String> svcMembers = secondaryVoltageControlMembers(network);
        List<ParametersSet> sets = new ArrayList<>();
        Set<String> built = new HashSet<>();
        for (Generator generator : network.getGenerators()) {
            String lib = selectLib(generator, regulationCount, svcMembers);
            if (lib != null) {
                String parId = parameterSetId(generator, lib);
                // a shared set is built once, from the first machine that reaches it, as the launcher does
                if (built.add(parId)) {
                    sets.add(DynaFlowGeneratorParameters.build(generator, lib, parId, config));
                }
            }
        }
        return sets;
    }

    /**
     * The parameter set a generator reads: on infinite reactive limits, several machines of a kind share
     * one value-keyed set (nuclear apart) as the launcher's {@code getGeneratorParameterSetId} gives; every
     * other machine — a diagram machine, or an Rpcl one — takes a set of its own, named after it.
     */
    private String parameterSetId(Generator generator, String lib) {
        if (config.infiniteReactiveLimits() && sharesParameterSet(lib)) {
            boolean fixedP = generator.getTargetP() == 0;
            String base = switch (lib) {
                case PV_SIGNALN -> fixedP ? SIGNALN_FIXED_P_SET : SIGNALN_SET;
                case PV_TFO_SIGNALN -> SIGNALN_TFO_SET;
                case PQ_PROP_SIGNALN -> fixedP ? PROP_FIXED_P_SET : PROP_SET;
                case PV_REMOTE_SIGNALN -> fixedP ? REMOTE_FIXED_P_SET : REMOTE_SET;
                default -> generator.getId();
            };
            return generator.getEnergySource() == EnergySource.NUCLEAR ? base + NUCLEAR_SUFFIX : base;
        }
        return generator.getId();
    }

    /** Whether an infinite machine on this library shares its value-keyed parameter set (the non-Rpcl ones). */
    private static boolean sharesParameterSet(String lib) {
        return lib.equals(PV_SIGNALN) || lib.equals(PV_TFO_SIGNALN)
                || lib.equals(PQ_PROP_SIGNALN) || lib.equals(PV_REMOTE_SIGNALN);
    }

    /**
     * The DynaFlow model a generator runs, or {@code null} to keep the static {@code NETWORK} model —
     * the launcher's {@code GeneratorDefinitionAlgorithm::operator()} decision tree. A generator in a
     * secondary voltage control zone ({@code isInSVC}) takes a reactive-power-control-loop model, a second
     * loop ({@code isRPCL2}) where a study marks it so through the synchronized generator properties.
     */
    private String selectLib(Generator generator, Map<String, Integer> regulationCount, Set<String> svcMembers) {
        if (!generator.isVoltageRegulatorOn() || !isTargetPValid(generator) || !isDiagramValid(generator)) {
            return null;
        }
        Bus connectedBus = busViewBus(generator.getTerminal());
        Bus regulatedBus = busViewBus(generator.getRegulatingTerminal());
        if (connectedBus == null || regulatedBus == null) {
            return null;
        }
        boolean local = regulatedBus.getId().equals(connectedBus.getId());
        boolean inSvc = svcMembers.contains(generator.getId());
        boolean rpcl2 = isRpcl2(generator);
        double nominalV = generator.getTerminal().getVoltageLevel().getNominalV();

        // Branch A — the transformer is assumed absent from the static model: local (or SVC) regulation at
        // or above the threshold voltage runs a Tfo model.
        if ((inSvc || local) && (nominalV > config.tfoVoltageLevel() || doubleEquals(nominalV, config.tfoVoltageLevel()))) {
            return selectDiagramGenerator(generator, true, inSvc, rpcl2);
        }

        // Branch B — the transformer is in the static model, or the regulation is remote.
        boolean multipleRegulators = regulationCount.getOrDefault(regulatedBus.getId(), 1) >= 2;
        if (!multipleRegulators) {
            if (inSvc || local) {
                return selectDiagramGenerator(generator, false, inSvc, rpcl2);
            }
            if (config.infiniteReactiveLimits()) {
                return PV_REMOTE_SIGNALN;
            }
            return isDiagramRectangular(generator) ? PV_REMOTE_SIGNALN : PV_REMOTE_DIAGRAM_PQ_SIGNALN;
        }
        // Several generators regulate this bus. An SVC machine keeps its control loop; on infinite limits,
        // or where a single machine regulates its own bus, it runs the diagram model, else it shares the
        // reactive power proportionally.
        if (config.infiniteReactiveLimits()) {
            return inSvc ? selectDiagramGenerator(generator, false, true, rpcl2) : PQ_PROP_SIGNALN;
        }
        boolean oneRegulatorOnConnectedBus = regulationCount.getOrDefault(connectedBus.getId(), 1) < 2;
        if (inSvc && oneRegulatorOnConnectedBus) {
            return selectDiagramGenerator(generator, false, true, rpcl2);
        }
        return isDiagramRectangular(generator) ? PQ_PROP_SIGNALN : PQ_PROP_DIAGRAM_PQ_SIGNALN;
    }

    /**
     * The launcher's {@code selectDiagramGenerators} leaf: a PV model whose flavour is the transformer
     * presence, the reactive power control loop (none / Rpcl / Rpcl2) and the diagram shape (infinite and
     * rectangular share a library; a genuine PQ curve gets its own).
     */
    private String selectDiagramGenerator(Generator generator, boolean withTransformer, boolean inSvc, boolean rpcl2) {
        if (config.infiniteReactiveLimits()) {
            if (withTransformer) {
                return inSvc ? (rpcl2 ? PV_TFO_RPCL2_SIGNALN : PV_TFO_RPCL_SIGNALN) : PV_TFO_SIGNALN;
            }
            return inSvc ? (rpcl2 ? PV_RPCL2_SIGNALN : PV_RPCL_SIGNALN) : PV_SIGNALN;
        }
        boolean rectangular = isDiagramRectangular(generator);
        if (inSvc) {
            return withTransformer
                    ? rpcl2 ? (rectangular ? PV_TFO_RPCL2_SIGNALN : PV_TFO_DIAGRAM_PQ_RPCL2_SIGNALN)
                            : (rectangular ? PV_TFO_RPCL_SIGNALN : PV_TFO_DIAGRAM_PQ_RPCL_SIGNALN)
                    : rpcl2 ? (rectangular ? PV_RPCL2_SIGNALN : PV_DIAGRAM_PQ_RPCL2_SIGNALN)
                            : (rectangular ? PV_RPCL_SIGNALN : PV_DIAGRAM_PQ_RPCL_SIGNALN);
        }
        if (withTransformer) {
            return rectangular ? PV_TFO_SIGNALN : PV_TFO_DIAGRAM_PQ_SIGNALN;
        }
        return rectangular ? PV_SIGNALN : PV_DIAGRAM_PQ_SIGNALN;
    }

    /**
     * How many voltage-regulating generators bear on each bus, keyed by the regulated bus and, when it
     * differs, the connected bus — the launcher's {@code busesToNumberOfRegulationMap}. A bus reached
     * twice or more is regulated by several machines. Every machine in voltage regulation counts, even one
     * the top gate later leaves on the static model.
     */
    private static Map<String, Integer> countRegulationsPerBus(Network network) {
        Map<String, Integer> counts = new HashMap<>();
        for (Generator generator : network.getGenerators()) {
            if (!generator.isVoltageRegulatorOn()) {
                continue;
            }
            Bus regulatedBus = busViewBus(generator.getRegulatingTerminal());
            if (regulatedBus == null) {
                continue;
            }
            counts.merge(regulatedBus.getId(), 1, Integer::sum);
            Bus connectedBus = busViewBus(generator.getTerminal());
            if (connectedBus != null && !connectedBus.getId().equals(regulatedBus.getId())) {
                counts.merge(connectedBus.getId(), 1, Integer::sum);
            }
        }
        return counts;
    }

    private static Bus busViewBus(Terminal terminal) {
        return terminal == null ? null : terminal.getBusView().getBus();
    }

    /** Whether a study marks this generator's second reactive power control loop on the synchronized properties. */
    private static boolean isRpcl2(Generator generator) {
        SynchronizedGeneratorProperties properties = generator.getExtension(SynchronizedGeneratorProperties.class);
        return properties != null && properties.isRpcl2();
    }

    /**
     * The generators the secondary voltage control puts on a reactive-power-control-loop model — the
     * control units of a zone whose pilot point the control can reach. A zone whose pilot resolves to no
     * bus is dropped, so its generators stay on the plain model: the launcher's {@code removeRpclFromModel}
     * post-filter, an SVC with no connection to a bus removed and its machines losing their control loop.
     */
    private static Set<String> secondaryVoltageControlMembers(Network network) {
        SecondaryVoltageControl svc = network.getExtension(SecondaryVoltageControl.class);
        if (svc == null) {
            return Set.of();
        }
        Set<String> members = new HashSet<>();
        for (ControlZone zone : svc.getControlZones()) {
            if (pilotPointResolves(network, zone)) {
                for (ControlUnit unit : zone.getControlUnits()) {
                    members.add(unit.getId());
                }
            }
        }
        return members;
    }

    /** Whether a zone's pilot point — a busbar section or a bus — is one the network holds. */
    private static boolean pilotPointResolves(Network network, ControlZone zone) {
        PilotPoint pilotPoint = zone.getPilotPoint();
        boolean busbarResolves = pilotPoint.getBusbarSectionIds().stream()
                .anyMatch(id -> network.getBusbarSection(id) != null);
        boolean busResolves = pilotPoint.getBuses().stream().anyMatch(busRef -> {
            VoltageLevel voltageLevel = network.getVoltageLevel(busRef.voltageLevelId());
            return voltageLevel != null && voltageLevel.getBusBreakerView().getBus(busRef.busId()) != null;
        });
        return busbarResolves || busResolves;
    }

    /** The operating point {@code -targetP} lies within the active power limits (bounds included). */
    private static boolean isTargetPValid(Generator generator) {
        double operatingPoint = -generator.getTargetP();
        return (operatingPoint > generator.getMinP() || doubleEquals(operatingPoint, generator.getMinP()))
                && (operatingPoint < generator.getMaxP() || doubleEquals(operatingPoint, generator.getMaxP()));
    }

    /**
     * Whether the generator's reactive diagram is usable — the launcher's {@code isDiagramValid}. A
     * min/max diagram is rejected when its active or reactive band is degenerate; a curve is rejected when
     * it has a single point, all points share one active power, or all points have {@code qMin == qMax}.
     */
    private boolean isDiagramValid(Generator generator) {
        if (config.infiniteReactiveLimits()) {
            return true;
        }
        ReactiveLimits limits = generator.getReactiveLimits();
        if (limits.getKind() == ReactiveLimitsKind.MIN_MAX) {
            MinMaxReactiveLimits minMax = (MinMaxReactiveLimits) limits;
            return !doubleEquals(generator.getMinP(), generator.getMaxP())
                    && !doubleEquals(minMax.getMinQ(), minMax.getMaxQ());
        }
        List<ReactiveCapabilityCurve.Point> points =
                new ArrayList<>(((ReactiveCapabilityCurve) limits).getPoints());
        if (points.size() == 1) {
            return false;
        }
        double firstP = points.get(0).getP();
        boolean allQminEqualQmax = true;
        boolean allPEqual = true;
        for (ReactiveCapabilityCurve.Point point : points) {
            allQminEqualQmax = allQminEqualQmax && doubleEquals(point.getMinQ(), point.getMaxQ());
            allPEqual = allPEqual && doubleEquals(point.getP(), firstP);
        }
        return !allQminEqualQmax && !allPEqual;
    }

    /**
     * Whether the reactive diagram is a rectangle — every curve point shares one {@code (qMin, qMax)} — so
     * the plain (rectangular) model fits and the PQ-curve one is not needed. A min/max diagram is
     * rectangular by definition.
     */
    private static boolean isDiagramRectangular(Generator generator) {
        ReactiveLimits limits = generator.getReactiveLimits();
        if (limits.getKind() == ReactiveLimitsKind.MIN_MAX) {
            return true;
        }
        List<ReactiveCapabilityCurve.Point> points =
                new ArrayList<>(((ReactiveCapabilityCurve) limits).getPoints());
        for (int i = 1; i < points.size(); i++) {
            ReactiveCapabilityCurve.Point previous = points.get(i - 1);
            ReactiveCapabilityCurve.Point current = points.get(i);
            if (!doubleEquals(previous.getMinQ(), current.getMinQ())
                    || !doubleEquals(previous.getMaxQ(), current.getMaxQ())) {
                return false;
            }
        }
        return true;
    }

    private static boolean doubleEquals(double a, double b) {
        return Math.abs(a - b) <= EPSILON * Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
    }
}
