/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.commons;

import com.powsybl.iidm.network.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalInt;

/**
 * The tap positions and section counts that Dynawo, run on a network itself, writes into it become its solved ones,
 * as {@link NetworkResultsUpdater} sets them from the IIDM file of the final state, and the network gets its own back.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public final class SolvedPositions {

    private final Map<RatioTapChanger, OptionalInt> ratioTapPositions = new LinkedHashMap<>();
    private final Map<PhaseTapChanger, OptionalInt> phaseTapPositions = new LinkedHashMap<>();
    private final Map<ShuntCompensator, OptionalInt> sectionCounts = new LinkedHashMap<>();

    private SolvedPositions() {
    }

    /**
     * The tap positions and section counts of the working variant of a network, before Dynawo runs on it
     */
    public static SolvedPositions save(Network network) {
        SolvedPositions positions = new SolvedPositions();
        network.getTwoWindingsTransformerStream().forEach(positions::saveTapPositions);
        network.getThreeWindingsTransformerStream().flatMap(ThreeWindingsTransformer::getLegStream).forEach(positions::saveTapPositions);
        network.getShuntCompensatorStream().forEach(shunt -> positions.sectionCounts.put(shunt, shunt.findSectionCount()));
        return positions;
    }

    private <H extends RatioTapChangerHolder & PhaseTapChangerHolder> void saveTapPositions(H holder) {
        holder.getOptionalRatioTapChanger().ifPresent(tapChanger -> ratioTapPositions.put(tapChanger, tapChanger.findTapPosition()));
        holder.getOptionalPhaseTapChanger().ifPresent(tapChanger -> phaseTapPositions.put(tapChanger, tapChanger.findTapPosition()));
    }

    /**
     * After Dynawo wrote the final state into the network: the tap positions and section counts it wrote become the
     * solved ones, and the network gets its own back
     */
    public void applySolved() {
        ratioTapPositions.forEach(SolvedPositions::applySolved);
        phaseTapPositions.forEach(SolvedPositions::applySolved);
        sectionCounts.forEach((shunt, sectionCount) -> {
            shunt.findSectionCount().ifPresent(shunt::setSolvedSectionCount);
            if (sectionCount.isPresent()) {
                shunt.setSectionCount(sectionCount.getAsInt());
            } else {
                shunt.unsetSectionCount();
            }
        });
    }

    private static void applySolved(TapChanger<?, ?, ?, ?> tapChanger, OptionalInt tapPosition) {
        tapChanger.findTapPosition().ifPresent(tapChanger::setSolvedTapPosition);
        if (tapPosition.isPresent()) {
            tapChanger.setTapPosition(tapPosition.getAsInt());
        } else {
            tapChanger.unsetTapPosition();
        }
    }
}
