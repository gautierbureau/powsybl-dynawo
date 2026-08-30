/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.dynaflow;

import com.powsybl.dynawo.mappings.dynaflow.DynaFlowConfig.StartingPointMode;
import com.powsybl.dynawo.mappings.dynaflow.DynaFlowHvdcMapping.Position;
import com.powsybl.dynawo.models.hvdc.HvdcDiagram;
import com.powsybl.dynawo.parameters.ParameterType;
import com.powsybl.dynawo.parameters.ParametersSet;
import com.powsybl.iidm.network.HvdcConverterStation;
import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.LccConverterStation;
import com.powsybl.iidm.network.VscConverterStation;
import com.powsybl.iidm.network.extensions.HvdcAngleDroopActivePowerControl;

/**
 * Builds an HVDC line's DynaFlow parameter set — the DynaFlow Launcher's {@code ParHvdc} writer, ported
 * for the models this generic mapping selects (see {@link DynaFlowHvdcMapping}).
 * <p>
 * The initial voltage/power point comes from the load flow (warm) or is flat, the active power split
 * across the DC line by {@code computeFlatP1RefSetPu}; reactive limits are infinite unless the model reads
 * a diagram, then each in-component converter contributes its {@code QInj} table file and initial bounds.
 * A VSC line adds its regulation modes and Q/V references, an LCC line its power-factor references, a
 * proportional line its {@code QPercent} shares, and an emulation line its AC-emulation gain and set point.
 * When one converter dangles, the side suffixes are swapped so the in-component side is always side one.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
final class DynaFlowHvdcParameters {

    private static final ParameterType DOUBLE = ParameterType.DOUBLE;
    private static final ParameterType STRING = ParameterType.STRING;
    private static final double FACTOR_PU = 100.0;
    private static final String PLUS_INFINITE = Double.toString(Double.MAX_VALUE);
    private static final String MINUS_INFINITE = Double.toString(-Double.MAX_VALUE);
    // the launcher's default AC-emulation filter time, used with no setting database to read one from
    private static final String AC_EMULATION_T_FILTER = "50";

    private DynaFlowHvdcParameters() {
    }

    static ParametersSet build(HvdcLine line, DynaFlowHvdcModel model, Position position, DynaFlowConfig config) {
        HvdcConverterStation<?> station1 = line.getConverterStation1();
        HvdcConverterStation<?> station2 = line.getConverterStation2();
        boolean vsc = station1.getHvdcType() == HvdcConverterStation.HvdcType.VSC;
        // the in-component converter is always side one; a dangling side two swaps the suffixes
        boolean swap = position == Position.SECOND_IN_MAIN;
        String first = swap ? "2" : "1";
        String second = swap ? "1" : "2";
        HvdcConverterStation<?> firstStation = swap ? station2 : station1;
        HvdcConverterStation<?> secondStation = swap ? station1 : station2;

        ParametersSet set = new ParametersSet(line.getId());

        if (config.startingPointMode() == StartingPointMode.WARM) {
            if (model.hasEmulation()) {
                set.addReference("hvdc_P10Pu", DOUBLE, "p" + first + "_pu");
                set.addReference("hvdc_P1RefSetPu", DOUBLE, "p" + first + "_pu");
                set.addReference("hvdc_P20Pu", DOUBLE, "p" + second + "_pu");
            } else {
                double[] powers = computeFlatP1RefSetPu(line, station1, station2, swap);
                set.addParameter("hvdc_P10Pu", DOUBLE, Double.toString(powers[0]));
                set.addParameter("hvdc_P1RefSetPu", DOUBLE, Double.toString(powers[0]));
                set.addParameter("hvdc_P20Pu", DOUBLE, Double.toString(powers[1]));
            }
            set.addReference("hvdc_Q10Pu", DOUBLE, "q" + first + "_pu");
            set.addReference("hvdc_U10Pu", DOUBLE, "v" + first + "_pu");
            set.addReference("hvdc_UPhase10", DOUBLE, "angle" + first + "_pu");
            set.addReference("hvdc_Q20Pu", DOUBLE, "q" + second + "_pu");
            set.addReference("hvdc_U20Pu", DOUBLE, "v" + second + "_pu");
            set.addReference("hvdc_UPhase20", DOUBLE, "angle" + second + "_pu");
        } else {
            set.addParameter("hvdc_U10Pu", DOUBLE, "1");
            set.addParameter("hvdc_UPhase10", DOUBLE, "0");
            set.addParameter("hvdc_U20Pu", DOUBLE, "1");
            set.addParameter("hvdc_UPhase20", DOUBLE, "0");
            double[] powers = computeFlatP1RefSetPu(line, station1, station2, swap);
            set.addParameter("hvdc_P10Pu", DOUBLE, Double.toString(powers[0]));
            set.addParameter("hvdc_P1RefSetPu", DOUBLE, Double.toString(powers[0]));
            set.addParameter("hvdc_P20Pu", DOUBLE, Double.toString(powers[1]));
            if (vsc) {
                set.addReference("hvdc_Q10Pu", DOUBLE, "targetQ_pu", firstStation.getId());
                set.addReference("hvdc_Q20Pu", DOUBLE, "targetQ_pu", secondStation.getId());
            } else {
                set.addParameter("hvdc_Q10Pu", DOUBLE, Double.toString(-Math.abs(powerFactor(firstStation) * powers[0])));
                set.addParameter("hvdc_Q20Pu", DOUBLE, Double.toString(-Math.abs(powerFactor(secondStation) * powers[1])));
            }
        }

        set.addReference("hvdc_PMaxPu", DOUBLE, "pMax_pu");
        set.addParameter("hvdc_KLosses", DOUBLE, "1.0");

        if (!model.hasDiagram()) {
            set.addParameter("hvdc_Q1MinPu", DOUBLE, MINUS_INFINITE);
            set.addParameter("hvdc_Q1MaxPu", DOUBLE, PLUS_INFINITE);
            set.addParameter("hvdc_Q2MinPu", DOUBLE, MINUS_INFINITE);
            set.addParameter("hvdc_Q2MaxPu", DOUBLE, PLUS_INFINITE);
        } else {
            addDiagramTable(set, line, firstStation, 1);
            if (position == Position.BOTH_IN_MAIN) {
                addDiagramTable(set, line, station2, 2);
            }
        }

        if (vsc) {
            set.addParameter("hvdc_modeU10", ParameterType.BOOL, Boolean.toString(voltageRegulatorOn(firstStation)));
            set.addParameter("hvdc_modeU20", ParameterType.BOOL, Boolean.toString(voltageRegulatorOn(secondStation)));
            set.addReference("hvdc_Q1Ref0Pu", DOUBLE, "targetQ_pu", firstStation.getId());
            set.addReference("hvdc_Q2Ref0Pu", DOUBLE, "targetQ_pu", secondStation.getId());
            set.addReference("hvdc_U2Ref0Pu", DOUBLE, "targetV_pu", secondStation.getId());
        }

        if (!model.hasDiagram() && vsc) {
            // no diagram and no setting database to read from: the launcher's fallback nominal values
            set.addParameter("hvdc_Q1Nom", DOUBLE, "100");
            set.addParameter("hvdc_Lambda1Pu", DOUBLE, "0");
            set.addParameter("hvdc_Q2Nom", DOUBLE, "100");
            set.addParameter("hvdc_Lambda2Pu", DOUBLE, "0");
        }

        if (!vsc) {
            set.addReference("hvdc_CosPhi1Ref0", DOUBLE, "powerFactor", firstStation.getId());
            set.addReference("hvdc_CosPhi2Ref0", DOUBLE, "powerFactor", secondStation.getId());
        }

        if (model.hasPQProp()) {
            set.addReference("hvdc_QPercent1", DOUBLE, "qMax_pu", firstStation.getId());
            if (position == Position.BOTH_IN_MAIN) {
                set.addReference("hvdc_QPercent2", DOUBLE, "qMax_pu", secondStation.getId());
            }
        }

        if (!model.hasDangling() && !model.hasEmulation()) {
            set.addReference("P1Ref_ValueIn", DOUBLE, "p1_pu");
        }

        if (model.hasEmulation()) {
            HvdcAngleDroopActivePowerControl control = line.getExtension(HvdcAngleDroopActivePowerControl.class);
            set.addParameter("acemulation_tFilter", DOUBLE, AC_EMULATION_T_FILTER);
            set.addParameter("acemulation_KACEmulation", DOUBLE, Double.toString(control.getDroop() * 1.8 / Math.PI));
            set.addParameter("acemulation_PRefSet0Pu", DOUBLE, Double.toString(control.getP0() / FACTOR_PU));
        }

        return set;
    }

    /** A converter's reactive table file and initial bounds — the launcher's {@code updateHVDCParams}. */
    private static void addDiagramTable(ParametersSet set, HvdcLine line, HvdcConverterStation<?> station, int number) {
        String id = station.getId();
        set.addParameter("hvdc_QInj" + number + "MinTableFile", STRING, HvdcDiagram.fileName(id));
        set.addParameter("hvdc_QInj" + number + "MinTableName", STRING, HvdcDiagram.qMinTableName(id));
        set.addParameter("hvdc_QInj" + number + "MaxTableFile", STRING, HvdcDiagram.fileName(id));
        set.addParameter("hvdc_QInj" + number + "MaxTableName", STRING, HvdcDiagram.qMaxTableName(id));
        if (station instanceof VscConverterStation vsc) {
            double qMin = vscQMin(vsc);
            double qMax = vscQMax(vsc);
            set.addParameter("hvdc_QInj" + number + "Min0Pu", DOUBLE, Double.toString((qMin - 1) / FACTOR_PU));
            set.addParameter("hvdc_QInj" + number + "Max0Pu", DOUBLE, Double.toString((qMax + 1) / FACTOR_PU));
            set.addParameter("hvdc_Q" + number + "Nom", DOUBLE, Double.toString(Math.max(Math.abs(qMin), Math.abs(qMax))));
            set.addParameter("hvdc_Lambda" + number + "Pu", DOUBLE, "0");
        } else {
            double qMax = HvdcDiagram.computeQmax(powerFactor(station), line.getMaxP());
            set.addParameter("hvdc_QInj" + number + "Min0Pu", DOUBLE, Double.toString((-qMax - 1) / FACTOR_PU));
            set.addParameter("hvdc_QInj" + number + "Max0Pu", DOUBLE, Double.toString((qMax + 1) / FACTOR_PU));
        }
    }

    /**
     * The initial flat active power on each side — the launcher's {@code computeFlatP1RefSetPu}. Returns
     * {@code {P10Pu, P20Pu}}: the DC set point on the rectifier side, the loss-reduced power on the other.
     */
    private static double[] computeFlatP1RefSetPu(HvdcLine line, HvdcConverterStation<?> station1, HvdcConverterStation<?> station2, boolean swap) {
        double pSetPoint = -line.getActivePowerSetpoint();
        double vdcNom = line.getNominalV();
        double loss1 = station1.getLossFactor() / FACTOR_PU;
        double loss2 = station2.getLossFactor() / FACTOR_PU;
        double pdcLoss = line.getR() * (pSetPoint / vdcNom) * (pSetPoint / vdcNom) / FACTOR_PU;
        double p0dc = pSetPoint / FACTOR_PU;
        boolean converter1Rectifier = line.getConvertersMode() == HvdcLine.ConvertersMode.SIDE_1_RECTIFIER_SIDE_2_INVERTER;
        if (!swap) {
            double factor = converter1Rectifier ? 1.0 : -1.0;
            double p01 = -factor * p0dc;
            double p02 = factor * ((p0dc * (1 - loss1)) - pdcLoss) * (1 - loss2);
            return new double[] {p01, p02};
        }
        double factor = converter1Rectifier ? -1.0 : 1.0;
        double p01 = factor * ((p0dc * (1 - loss2)) - pdcLoss) * (1 - loss1);
        double p02 = -factor * p0dc;
        return new double[] {p02, p01};
    }

    // The reactive bounds the launcher reads are the converter's capability at its operating point, not the
    // envelope over the whole curve: its data interface's getQMin/getQMax evaluate the reactive limits at
    // the converter's active power (a curve is interpolated, min/max limits are constant).
    private static double vscQMin(VscConverterStation vsc) {
        return vsc.getReactiveLimits().getMinQ(operatingP(vsc));
    }

    private static double vscQMax(VscConverterStation vsc) {
        return vsc.getReactiveLimits().getMaxQ(operatingP(vsc));
    }

    /** The converter's active power, the point its reactive capability is read at; the curve centre if unset. */
    private static double operatingP(VscConverterStation vsc) {
        double p = vsc.getTerminal().getP();
        return Double.isNaN(p) ? 0 : p;
    }

    private static double powerFactor(HvdcConverterStation<?> station) {
        return ((LccConverterStation) station).getPowerFactor();
    }

    private static boolean voltageRegulatorOn(HvdcConverterStation<?> station) {
        return ((VscConverterStation) station).isVoltageRegulatorOn();
    }
}
