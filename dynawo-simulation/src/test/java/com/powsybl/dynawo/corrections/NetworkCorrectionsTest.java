/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.corrections;

import com.google.auto.service.AutoService;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
class NetworkCorrectionsTest {

    // records the order the corrections were applied in, across a single test
    static final List<String> APPLIED = new ArrayList<>();

    private NetworkCorrections corrections;
    private Network network;

    @BeforeEach
    void setUp() {
        APPLIED.clear();
        corrections = new NetworkCorrections();
        network = Network.create("test", "test");
    }

    @Test
    void loadsRegisteredCorrections() {
        assertTrue(corrections.getNetworkCorrectionNames().containsAll(List.of("correctionA", "correctionB")));
    }

    @Test
    void appliesActiveInGivenOrder() {
        corrections.applyActive(network, new LinkedHashSet<>(List.of("correctionB", "correctionA")), ReportNode.NO_OP);
        assertEquals(List.of("correctionB", "correctionA"), APPLIED);
    }

    @Test
    void ignoresUnknownNames() {
        corrections.applyActive(network, new LinkedHashSet<>(List.of("nope", "correctionA")), ReportNode.NO_OP);
        assertEquals(List.of("correctionA"), APPLIED);
    }

    @Test
    void appliesEachAtMostOncePerNetwork() {
        Set<String> active = new LinkedHashSet<>(List.of("correctionA"));
        corrections.applyActive(network, active, ReportNode.NO_OP);
        // a second pass (e.g. the run after get_models) sees the mark and skips it
        corrections.applyActive(network, active, ReportNode.NO_OP);
        assertEquals(List.of("correctionA"), APPLIED);
        assertTrue(network.hasProperty("dynawo.appliedCorrection.correctionA"));
    }

    @AutoService(NetworkCorrection.class)
    public static final class CorrectionA implements NetworkCorrection {
        @Override
        public NetworkCorrectionInfo getCorrectionInfo() {
            return new NetworkCorrectionInfo("correctionA", "A test correction");
        }

        @Override
        public void apply(Network network, ReportNode reportNode) {
            APPLIED.add("correctionA");
        }
    }

    @AutoService(NetworkCorrection.class)
    public static final class CorrectionB implements NetworkCorrection {
        @Override
        public NetworkCorrectionInfo getCorrectionInfo() {
            return new NetworkCorrectionInfo("correctionB", "B test correction");
        }

        @Override
        public void apply(Network network, ReportNode reportNode) {
            APPLIED.add("correctionB");
        }
    }
}
