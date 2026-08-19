/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.models.generators;

import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.ReactiveCapabilityCurve;
import com.powsybl.iidm.network.ReactiveLimits;
import com.powsybl.iidm.network.ReactiveLimitsKind;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.ToDoubleFunction;

/**
 * The reactive-capability-diagram table file a {@code GeneratorPV*DiagramPQSignalN} model reads, and the
 * names a parameter set points at it by — the way the DynaFlow Launcher's {@code Diagram} writer builds
 * them.
 * <p>
 * One file per generator, named after it, holding the machine's Q(P) curve as two Modelica tables — a
 * {@code qmin} and a {@code qmax} table, each {@code (n,2)} of {@code p q} rows sorted by active power,
 * every value divided by 100. The file starts with {@code #1}, which Modelica requires of a table file. A
 * mapping keeps the {@code TableFile} / {@code TableName} parameters and the file itself in step through
 * these shared names; the id is reduced to a bare word so both the file name and the Modelica table names
 * are valid.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public final class GeneratorDiagram {

    private static final double DIVISOR = 100.0;

    private GeneratorDiagram() {
    }

    public static String fileName(String generatorId) {
        return base(generatorId) + "_Diagram.txt";
    }

    public static String qMaxTableName(String generatorId) {
        return base(generatorId) + "_tableqmax";
    }

    public static String qMinTableName(String generatorId) {
        return base(generatorId) + "_tableqmin";
    }

    /**
     * Writes the generator's diagram file to the working directory, from its reactive capability curve;
     * a generator without a curve (min/max limits) has no diagram to tabulate and none is written.
     */
    public static void write(Generator generator, Path workingDir) throws IOException {
        ReactiveLimits limits = generator.getReactiveLimits();
        if (limits.getKind() != ReactiveLimitsKind.CURVE) {
            return;
        }
        List<ReactiveCapabilityCurve.Point> points = new ArrayList<>(((ReactiveCapabilityCurve) limits).getPoints());
        points.sort(Comparator.comparingDouble(ReactiveCapabilityCurve.Point::getP));

        StringBuilder buffer = new StringBuilder("#1");
        appendTable(buffer, qMinTableName(generator.getId()), points, ReactiveCapabilityCurve.Point::getMinQ);
        appendTable(buffer, qMaxTableName(generator.getId()), points, ReactiveCapabilityCurve.Point::getMaxQ);
        Files.writeString(workingDir.resolve(fileName(generator.getId())), buffer.toString(), StandardCharsets.UTF_8);
    }

    private static void appendTable(StringBuilder buffer, String tableName,
                                    List<ReactiveCapabilityCurve.Point> points, ToDoubleFunction<ReactiveCapabilityCurve.Point> q) {
        buffer.append("\ndouble ").append(tableName).append('(').append(points.size()).append(",2)");
        for (ReactiveCapabilityCurve.Point point : points) {
            buffer.append('\n').append(point.getP() / DIVISOR).append(' ').append(q.applyAsDouble(point) / DIVISOR);
        }
    }

    private static String base(String generatorId) {
        return generatorId.replaceAll("[^A-Za-z0-9_]", "_");
    }
}
