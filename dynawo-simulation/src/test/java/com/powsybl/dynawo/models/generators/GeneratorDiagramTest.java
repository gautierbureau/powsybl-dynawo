/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.models.generators;

import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TopologyKind;
import com.powsybl.iidm.network.VoltageLevel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The reactive-capability-diagram file a DiagramPQ generator writes: a Modelica {@code #1} table file
 * holding its Q(P) curve as a {@code qmin} and a {@code qmax} table, sorted by active power, every value
 * divided by 100, the way the DynaFlow Launcher's {@code Diagram} writer builds it.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
class GeneratorDiagramTest {

    @Test
    void itWritesTheCurveAsTwoTables(@TempDir Path workingDir) throws IOException {
        Generator generator = generatorWithCurve();
        GeneratorDiagram.write(generator, workingDir);

        String content = Files.readString(workingDir.resolve("GEN_Diagram.txt"), StandardCharsets.UTF_8);
        assertEquals("""
                #1
                double GEN_tableqmin(2,2)
                0.0 -1.0
                2.0 -0.5
                double GEN_tableqmax(2,2)
                0.0 1.0
                2.0 0.5""", content);
    }

    @Test
    void aMinMaxGeneratorHasNoDiagramToWrite(@TempDir Path workingDir) throws IOException {
        Network network = Network.create("t", "t");
        VoltageLevel vl = network.newSubstation().setId("S").add()
                .newVoltageLevel().setId("VL").setNominalV(20).setTopologyKind(TopologyKind.BUS_BREAKER).add();
        vl.getBusBreakerView().newBus().setId("B").add();
        Generator generator = vl.newGenerator().setId("GEN").setBus("B").setConnectableBus("B")
                .setMinP(0).setMaxP(100).setTargetP(50).setTargetV(20).setVoltageRegulatorOn(true).add();
        generator.newMinMaxReactiveLimits().setMinQ(-10).setMaxQ(10).add();

        GeneratorDiagram.write(generator, workingDir);
        assertTrue(Files.list(workingDir).findAny().isEmpty(), "a min/max generator writes no diagram file");
    }

    private static Generator generatorWithCurve() {
        Network network = Network.create("t", "t");
        VoltageLevel vl = network.newSubstation().setId("S").add()
                .newVoltageLevel().setId("VL").setNominalV(20).setTopologyKind(TopologyKind.BUS_BREAKER).add();
        vl.getBusBreakerView().newBus().setId("B").add();
        Generator generator = vl.newGenerator().setId("GEN").setBus("B").setConnectableBus("B")
                .setMinP(0).setMaxP(200).setTargetP(50).setTargetV(20).setVoltageRegulatorOn(true).add();
        // points given out of order, to check they are sorted by active power
        generator.newReactiveCapabilityCurve()
                .beginPoint().setP(200).setMinQ(-50).setMaxQ(50).endPoint()
                .beginPoint().setP(0).setMinQ(-100).setMaxQ(100).endPoint()
                .add();
        return generator;
    }
}
