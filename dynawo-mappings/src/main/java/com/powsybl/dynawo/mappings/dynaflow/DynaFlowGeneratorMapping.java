/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.dynaflow;

import com.powsybl.dynawo.mappings.MappedModelsSupplier.MappedModel;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.MinMaxReactiveLimits;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.ReactiveCapabilityCurve;
import com.powsybl.iidm.network.ReactiveLimits;
import com.powsybl.iidm.network.ReactiveLimitsKind;
import com.powsybl.iidm.network.Terminal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 * <strong>Scope.</strong> The RPCL / RPCL2 and secondary-voltage-control (SVC) branches of the launcher's
 * tree are left out on purpose: they are driven by the assembling SVC automatons this mapping does not
 * read (see {@code DYNAFLOW_MAPPING_PLAN.md} §3.7 / §9), and their libraries are not in the catalogue.
 * With no SVC, {@code isInSVC} and {@code isRPCL2} are always false, so the reachable models are exactly
 * the eight non-RPCL libraries. Parameter sets (IIDM references) and the {@code VRRemote} wiring the
 * remote models need are the next step of the phase.
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

    /**
     * The nominal voltage (kV) at or above which a generator's transformer is assumed <em>absent</em> from
     * the static model, so DynaFlow models it (the Tfo variants). Below it, the transformer is taken to be
     * in the static description. The DynaFlow Launcher's {@code TfoVoltageLevel} default.
     */
    private static final double TFO_VOLTAGE_LEVEL = 100.0;

    /** The DynaFlow Launcher's {@code InfiniteReactiveLimits} default: honour each generator's diagram. */
    private static final boolean USE_INFINITE_REACTIVE_LIMITS = false;

    private static final double EPSILON = 1e-6;

    public List<MappedModel> createModelConfigs(Network network) {
        Map<String, Integer> regulationCount = countRegulationsPerBus(network);
        List<MappedModel> models = new ArrayList<>();
        for (Generator generator : network.getGenerators()) {
            String lib = selectLib(generator, regulationCount);
            if (lib != null) {
                models.add(new MappedModel(lib, generator.getId(), generator.getId()));
            }
        }
        return models;
    }

    /**
     * The DynaFlow model a generator runs, or {@code null} to keep the static {@code NETWORK} model —
     * the launcher's {@code GeneratorDefinitionAlgorithm::operator()} decision tree, without its SVC /
     * RPCL branches.
     */
    private static String selectLib(Generator generator, Map<String, Integer> regulationCount) {
        if (!generator.isVoltageRegulatorOn() || !isTargetPValid(generator) || !isDiagramValid(generator)) {
            return null;
        }
        Bus connectedBus = busViewBus(generator.getTerminal());
        Bus regulatedBus = busViewBus(generator.getRegulatingTerminal());
        if (connectedBus == null || regulatedBus == null) {
            return null;
        }
        boolean local = regulatedBus.getId().equals(connectedBus.getId());
        double nominalV = generator.getTerminal().getVoltageLevel().getNominalV();

        // Branch A — the transformer is assumed absent from the static model: local regulation at or above
        // the threshold voltage runs a Tfo model.
        if (local && (nominalV > TFO_VOLTAGE_LEVEL || doubleEquals(nominalV, TFO_VOLTAGE_LEVEL))) {
            return selectDiagramGenerator(generator, true);
        }

        // Branch B — the transformer is in the static model, or the regulation is remote.
        boolean multipleRegulators = regulationCount.getOrDefault(regulatedBus.getId(), 1) >= 2;
        if (!multipleRegulators) {
            if (local) {
                return selectDiagramGenerator(generator, false);
            }
            if (USE_INFINITE_REACTIVE_LIMITS) {
                return PV_REMOTE_SIGNALN;
            }
            return isDiagramRectangular(generator) ? PV_REMOTE_SIGNALN : PV_REMOTE_DIAGRAM_PQ_SIGNALN;
        }
        // Several generators regulate this bus (SVC absent): proportional reactive sharing.
        if (USE_INFINITE_REACTIVE_LIMITS) {
            return PQ_PROP_SIGNALN;
        }
        return isDiagramRectangular(generator) ? PQ_PROP_SIGNALN : PQ_PROP_DIAGRAM_PQ_SIGNALN;
    }

    /**
     * The launcher's {@code selectDiagramGenerators} leaf, with SVC / RPCL off: a plain PV model whose
     * flavour is the transformer presence and the diagram shape (infinite and rectangular share a
     * library; a genuine PQ curve gets its own).
     */
    private static String selectDiagramGenerator(Generator generator, boolean withTransformer) {
        if (USE_INFINITE_REACTIVE_LIMITS) {
            return withTransformer ? PV_TFO_SIGNALN : PV_SIGNALN;
        }
        boolean rectangular = isDiagramRectangular(generator);
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
    private static boolean isDiagramValid(Generator generator) {
        if (USE_INFINITE_REACTIVE_LIMITS) {
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
