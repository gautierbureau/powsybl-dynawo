/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.models.hvdc;

import com.powsybl.iidm.network.HvdcConverterStation;
import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.LccConverterStation;
import com.powsybl.iidm.network.MinMaxReactiveLimits;
import com.powsybl.iidm.network.ReactiveCapabilityCurve;
import com.powsybl.iidm.network.ReactiveLimits;
import com.powsybl.iidm.network.ReactiveLimitsKind;
import com.powsybl.iidm.network.VscConverterStation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The reactive-capability-diagram table file an {@code Hvdc...DiagramPQ} model reads for one converter,
 * and the names a parameter set points at it by — the way the DynaFlow Launcher's {@code Diagram} writer
 * builds them for HVDC converters.
 * <p>
 * One file per converter, named after it, holding the converter's Q(P) capability as two Modelica tables
 * — a {@code qmin} and a {@code qmax} table, each {@code (n,2)} of {@code p q} rows, every value divided
 * by 100, the file starting with the {@code #1} Modelica requires. A VSC with a reactive-capability curve
 * tabulates its points; a VSC with min/max limits, and every LCC (whose reactive bound is {@code pMax ·
 * √(1/cosφ² − 1)}), tabulates a rectangular box between {@code ∓pMax}. A mapping keeps the {@code
 * TableFile} / {@code TableName} parameters and the file in step through these shared names.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public final class HvdcDiagram {

    private static final double DIVISOR = 100.0;

    private HvdcDiagram() {
    }

    public static String fileName(String converterId) {
        return base(converterId) + "_Diagram.txt";
    }

    public static String qMaxTableName(String converterId) {
        return base(converterId) + "_tableqmax";
    }

    public static String qMinTableName(String converterId) {
        return base(converterId) + "_tableqmin";
    }

    /** The reactive bound of an LCC converter — the launcher's {@code computeQmax}. */
    public static double computeQmax(double powerFactor, double pMax) {
        return pMax * Math.sqrt(1.0 / (powerFactor * powerFactor) - 1.0);
    }

    /**
     * Writes the converter's diagram file to the working directory: a VSC's reactive-capability curve as
     * its points, a VSC's min/max limits or an LCC's power-factor bound as a rectangular box between the
     * line's {@code ∓pMax}.
     */
    public static void write(HvdcConverterStation<?> station, HvdcLine line, Path workingDir) throws IOException {
        String id = station.getId();
        List<double[]> qMinRows = new ArrayList<>();
        List<double[]> qMaxRows = new ArrayList<>();
        double pMax = line.getMaxP();

        if (station instanceof VscConverterStation vsc) {
            ReactiveLimits limits = vsc.getReactiveLimits();
            if (limits.getKind() == ReactiveLimitsKind.CURVE) {
                List<ReactiveCapabilityCurve.Point> points = new ArrayList<>(((ReactiveCapabilityCurve) limits).getPoints());
                points.sort(Comparator.comparingDouble(ReactiveCapabilityCurve.Point::getP));
                for (ReactiveCapabilityCurve.Point point : points) {
                    qMinRows.add(new double[] {point.getP(), point.getMinQ()});
                    qMaxRows.add(new double[] {point.getP(), point.getMaxQ()});
                }
            } else {
                MinMaxReactiveLimits minMax = (MinMaxReactiveLimits) limits;
                box(qMinRows, qMaxRows, pMax, minMax.getMinQ(), minMax.getMaxQ());
            }
        } else {
            double qMax = computeQmax(((LccConverterStation) station).getPowerFactor(), pMax);
            box(qMinRows, qMaxRows, pMax, -qMax, qMax);
        }

        StringBuilder buffer = new StringBuilder("#1");
        appendTable(buffer, qMinTableName(id), qMinRows);
        appendTable(buffer, qMaxTableName(id), qMaxRows);
        Files.writeString(workingDir.resolve(fileName(id)), buffer.toString(), StandardCharsets.UTF_8);
    }

    /** A flat capability box: the reactive bound held at both {@code ∓pMax}. */
    private static void box(List<double[]> qMinRows, List<double[]> qMaxRows, double pMax, double qMin, double qMax) {
        qMinRows.add(new double[] {-pMax, qMin});
        qMinRows.add(new double[] {pMax, qMin});
        qMaxRows.add(new double[] {-pMax, qMax});
        qMaxRows.add(new double[] {pMax, qMax});
    }

    private static void appendTable(StringBuilder buffer, String tableName, List<double[]> rows) {
        buffer.append("\ndouble ").append(tableName).append('(').append(rows.size()).append(",2)");
        for (double[] row : rows) {
            buffer.append('\n').append(row[0] / DIVISOR).append(' ').append(row[1] / DIVISOR);
        }
    }

    private static String base(String converterId) {
        return converterId.replaceAll("[^A-Za-z0-9_]", "_");
    }
}
