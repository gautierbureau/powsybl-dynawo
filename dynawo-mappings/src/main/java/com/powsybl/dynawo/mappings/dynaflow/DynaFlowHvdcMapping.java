/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.dynaflow;

import com.powsybl.dynamicsimulation.DynamicModel;
import com.powsybl.dynawo.builders.AbstractEquipmentModelBuilder;
import com.powsybl.dynawo.builders.ModelBuilder;
import com.powsybl.dynawo.mappings.MappedModelsSupplier.MappedModel;
import com.powsybl.dynawo.models.hvdc.AbstractHvdcBuilder;
import com.powsybl.dynawo.parameters.ParametersSet;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.HvdcConverterStation;
import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.iidm.network.VscConverterStation;
import com.powsybl.iidm.network.extensions.HvdcAngleDroopActivePowerControl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Gives every HVDC line its DynaFlow model, the way the DynaFlow Launcher's {@code HVDCDefinitionAlgorithm}
 * does — the heavyweight one of the four selection rules.
 * <p>
 * Which converter of a line sits in the main connected component decides its position: a line whose two
 * converters are both in reads a plain model, one whose single reachable converter is in reads a {@code
 * Dangling} model dangling on the side left out, and a line reachable from neither is dropped. On top of
 * that, an LCC line runs the {@code HvdcPTanPhi} family; a VSC line the {@code HvdcPV} family alone on its
 * bus or the {@code HvdcPQProp} family sharing it with other voltage regulators, turned into an {@code
 * EmulationSet} model where an angle-droop active power control is enabled, and into a {@code DiagramPQ}
 * model where the reactive limits are finite. Each line reads a set of its own — the launcher's {@code
 * ParHvdc} (see {@link DynaFlowHvdcParameters}).
 * <p>
 * The launcher's secondary-voltage-control {@code Rpcl2Side} HVDC variants are RTE preassembled models,
 * outside the open catalogue, so this generic DynaFlow mapping never selects them.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
final class DynaFlowHvdcMapping {

    /** Where a line's converters sit relative to the main connected component — the launcher's {@code Position}. */
    enum Position {
        FIRST_IN_MAIN,
        SECOND_IN_MAIN,
        BOTH_IN_MAIN
    }

    private final DynaFlowConfig config;

    DynaFlowHvdcMapping(DynaFlowConfig config) {
        this.config = config;
    }

    List<MappedModel> createModelConfigs(Network network) {
        Map<String, Integer> regulationCount = countVoltageRegulationsPerBus(network);
        List<MappedModel> models = new ArrayList<>();
        for (HvdcLine line : network.getHvdcLines()) {
            Position position = position(line);
            if (position != null) {
                DynaFlowHvdcModel model = selectModel(line, position, regulationCount);
                models.add(new MappedModel(model.lib(), line.getId(), line.getId(), configurer(line.getId(), danglingSide(position))));
            }
        }
        return models;
    }

    List<ParametersSet> createParameters(Network network) {
        Map<String, Integer> regulationCount = countVoltageRegulationsPerBus(network);
        List<ParametersSet> sets = new ArrayList<>();
        for (HvdcLine line : network.getHvdcLines()) {
            Position position = position(line);
            if (position != null) {
                DynaFlowHvdcModel model = selectModel(line, position, regulationCount);
                sets.add(DynaFlowHvdcParameters.build(line, model, position, config));
            }
        }
        return sets;
    }

    /**
     * The line's position, or {@code null} to drop it: a converter disconnected on either side, or neither
     * converter reaching the main component, leaves nothing to model. Membership is of the main
     * <em>synchronous</em> component — an HVDC line bridges connected components but not synchronous ones,
     * so the far side of an interconnection (a separate synchronous area) is what dangles.
     */
    private static Position position(HvdcLine line) {
        Bus bus1 = busViewBus(line.getConverterStation1().getTerminal());
        Bus bus2 = busViewBus(line.getConverterStation2().getTerminal());
        if (bus1 == null || bus2 == null) {
            return null;
        }
        boolean in1 = bus1.isInMainSynchronousComponent();
        boolean in2 = bus2.isInMainSynchronousComponent();
        if (in1 && in2) {
            return Position.BOTH_IN_MAIN;
        }
        if (in1) {
            return Position.FIRST_IN_MAIN;
        }
        if (in2) {
            return Position.SECOND_IN_MAIN;
        }
        return null;
    }

    /** The side that dangles outside the main connected component — the one to switch off, or none when both are in. */
    private static TwoSides danglingSide(Position position) {
        return switch (position) {
            case FIRST_IN_MAIN -> TwoSides.TWO;
            case SECOND_IN_MAIN -> TwoSides.ONE;
            case BOTH_IN_MAIN -> null;
        };
    }

    private DynaFlowHvdcModel selectModel(HvdcLine line, Position position, Map<String, Integer> regulationCount) {
        boolean infinite = config.infiniteReactiveLimits();
        boolean vsc = line.getConverterStation1().getHvdcType() == HvdcConverterStation.HvdcType.VSC;
        if (position == Position.BOTH_IN_MAIN) {
            if (!vsc) {
                return infinite ? DynaFlowHvdcModel.HVDC_P_TAN_PHI : DynaFlowHvdcModel.HVDC_P_TAN_PHI_DIAGRAM;
            }
            if (!emulationEnabled(line)) {
                return computeModelVsc(line, position, regulationCount, DynaFlowHvdcModel.HVDC_PQ_PROP,
                        DynaFlowHvdcModel.HVDC_PQ_PROP_DIAGRAM, DynaFlowHvdcModel.HVDC_PV, DynaFlowHvdcModel.HVDC_PV_DIAGRAM);
            }
            return computeModelVsc(line, position, regulationCount, DynaFlowHvdcModel.HVDC_PQ_PROP_EMULATION,
                    DynaFlowHvdcModel.HVDC_PQ_PROP_DIAGRAM_EMULATION, DynaFlowHvdcModel.HVDC_PV_EMULATION,
                    DynaFlowHvdcModel.HVDC_PV_DIAGRAM_EMULATION);
        }
        if (!vsc) {
            return infinite ? DynaFlowHvdcModel.HVDC_P_TAN_PHI_DANGLING : DynaFlowHvdcModel.HVDC_P_TAN_PHI_DANGLING_DIAGRAM;
        }
        return computeModelVsc(line, position, regulationCount, DynaFlowHvdcModel.HVDC_PQ_PROP_DANGLING,
                DynaFlowHvdcModel.HVDC_PQ_PROP_DANGLING_DIAGRAM, DynaFlowHvdcModel.HVDC_PV_DANGLING,
                DynaFlowHvdcModel.HVDC_PV_DANGLING_DIAGRAM);
    }

    /**
     * The VSC model, sharing its bus with several voltage regulators ({@code multiple}) or alone on it,
     * with finite or infinite reactive limits — the launcher's {@code computeModelVSC}.
     */
    private DynaFlowHvdcModel computeModelVsc(HvdcLine line, Position position, Map<String, Integer> regulationCount,
                                             DynaFlowHvdcModel multipleInfinite, DynaFlowHvdcModel multipleFinite,
                                             DynaFlowHvdcModel oneInfinite, DynaFlowHvdcModel oneFinite) {
        boolean multiple = switch (position) {
            case FIRST_IN_MAIN -> regulatedByMany(regulationCount, line.getConverterStation1().getTerminal());
            case SECOND_IN_MAIN -> regulatedByMany(regulationCount, line.getConverterStation2().getTerminal());
            case BOTH_IN_MAIN -> regulatedByMany(regulationCount, line.getConverterStation1().getTerminal())
                    || regulatedByMany(regulationCount, line.getConverterStation2().getTerminal());
        };
        boolean infinite = config.infiniteReactiveLimits();
        if (multiple) {
            return infinite ? multipleInfinite : multipleFinite;
        }
        return infinite ? oneInfinite : oneFinite;
    }

    private static boolean regulatedByMany(Map<String, Integer> regulationCount, Terminal terminal) {
        Bus bus = busViewBus(terminal);
        return bus != null && regulationCount.getOrDefault(bus.getId(), 1) >= 2;
    }

    /** Whether an enabled angle-droop active power control makes this an AC-emulation line. */
    private static boolean emulationEnabled(HvdcLine line) {
        HvdcAngleDroopActivePowerControl control = line.getExtension(HvdcAngleDroopActivePowerControl.class);
        return control != null && control.isEnabled() && control.getDroop() != 0;
    }

    /**
     * How many voltage regulators bear on each bus — the launcher's {@code busesToNumberOfRegulationMap}:
     * a generator counts at the bus it regulates and, when different, at its own bus; a VSC converter in
     * voltage regulation counts at its own bus.
     */
    private static Map<String, Integer> countVoltageRegulationsPerBus(Network network) {
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
        for (VscConverterStation converter : network.getVscConverterStations()) {
            if (!converter.isVoltageRegulatorOn()) {
                continue;
            }
            Bus bus = busViewBus(converter.getTerminal());
            if (bus != null) {
                counts.merge(bus.getId(), 1, Integer::sum);
            }
        }
        return counts;
    }

    private static Bus busViewBus(Terminal terminal) {
        return terminal == null ? null : terminal.getBusView().getBus();
    }

    private static Consumer<ModelBuilder<DynamicModel>> configurer(String staticId, TwoSides danglingSide) {
        return builder -> {
            AbstractEquipmentModelBuilder<?, ?> equipmentBuilder = (AbstractEquipmentModelBuilder<?, ?>) builder;
            equipmentBuilder.staticId(staticId);
            equipmentBuilder.parameterSetId(staticId);
            // the dangling side reaches only a dangling model, which the builder's type tells apart
            if (danglingSide != null && builder instanceof AbstractHvdcBuilder<?> hvdcBuilder) {
                hvdcBuilder.dangling(danglingSide);
            }
        };
    }
}
