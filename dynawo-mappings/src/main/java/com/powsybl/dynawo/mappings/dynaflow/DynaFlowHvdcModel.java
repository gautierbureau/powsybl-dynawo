/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.dynaflow;

/**
 * The DynaFlow HVDC models the DynaFlow Launcher's {@code HVDCDefinitionAlgorithm} selects, the sixteen
 * of the open catalogue's {@code HVDC_P} family reachable without a secondary voltage control (the launcher's
 * {@code Rpcl2Side} variants are RTE preassembled models, outside this generic mapping).
 * <p>
 * Each carries the flags the launcher's {@code ParHvdc} branches on: whether it reads a reactive-capability
 * {@code diagram}, whether it runs an AC-{@code emulation} active power control, whether it shares reactive
 * power proportionally ({@code pqProp}), and whether one converter is {@code dangling} outside the main
 * connected component.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
enum DynaFlowHvdcModel {

    HVDC_P_TAN_PHI("HvdcPTanPhi", false, false, false, false),
    HVDC_P_TAN_PHI_DIAGRAM("HvdcPTanPhiDiagramPQ", true, false, false, false),
    HVDC_P_TAN_PHI_DANGLING("HvdcPTanPhiDangling", false, false, false, true),
    HVDC_P_TAN_PHI_DANGLING_DIAGRAM("HvdcPTanPhiDanglingDiagramPQ", true, false, false, true),
    HVDC_PQ_PROP("HvdcPQProp", false, false, true, false),
    HVDC_PQ_PROP_DIAGRAM("HvdcPQPropDiagramPQ", true, false, true, false),
    HVDC_PQ_PROP_DANGLING("HvdcPQPropDangling", false, false, true, true),
    HVDC_PQ_PROP_DANGLING_DIAGRAM("HvdcPQPropDanglingDiagramPQ", true, false, true, true),
    HVDC_PQ_PROP_EMULATION("HvdcPQPropEmulationSet", false, true, true, false),
    HVDC_PQ_PROP_DIAGRAM_EMULATION("HvdcPQPropDiagramPQEmulationSet", true, true, true, false),
    HVDC_PV("HvdcPV", false, false, false, false),
    HVDC_PV_DIAGRAM("HvdcPVDiagramPQ", true, false, false, false),
    HVDC_PV_DANGLING("HvdcPVDangling", false, false, false, true),
    HVDC_PV_DANGLING_DIAGRAM("HvdcPVDanglingDiagramPQ", true, false, false, true),
    HVDC_PV_EMULATION("HvdcPVEmulationSet", false, true, false, false),
    HVDC_PV_DIAGRAM_EMULATION("HvdcPVDiagramPQEmulationSet", true, true, false, false);

    private final String lib;
    private final boolean diagram;
    private final boolean emulation;
    private final boolean pqProp;
    private final boolean dangling;

    DynaFlowHvdcModel(String lib, boolean diagram, boolean emulation, boolean pqProp, boolean dangling) {
        this.lib = lib;
        this.diagram = diagram;
        this.emulation = emulation;
        this.pqProp = pqProp;
        this.dangling = dangling;
    }

    String lib() {
        return lib;
    }

    boolean hasDiagram() {
        return diagram;
    }

    boolean hasEmulation() {
        return emulation;
    }

    boolean hasPQProp() {
        return pqProp;
    }

    boolean hasDangling() {
        return dangling;
    }
}
