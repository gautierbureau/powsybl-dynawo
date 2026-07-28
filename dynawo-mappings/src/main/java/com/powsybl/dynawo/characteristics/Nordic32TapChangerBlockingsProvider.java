/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.characteristics;

import com.google.auto.service.AutoService;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.extensions.MeasurementPoint;
import com.powsybl.iidm.network.extensions.TapChangerBlockingAdder;
import com.powsybl.iidm.network.extensions.TapChangerBlockings;
import com.powsybl.iidm.network.extensions.TapChangerBlockingsAdder;

import java.util.List;
import java.util.Objects;

/**
 * The tap changer blocking of the Nordic 32 test system, from its reference description.
 * <p>
 * The system blocks the tap changers of its distribution transformers when the voltage at a
 * transmission bus falls, so the network stops chasing a voltage it can no longer hold. Which bus
 * and which transformers is a fact of the system, taken here from its reference: one blocking
 * watching the bus below, holding the transformers below, told by the levels they sit in so the
 * mapping resolves them the way it resolves any controlled level.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
@AutoService(TapChangerBlockingsProvider.class)
public class Nordic32TapChangerBlockingsProvider implements TapChangerBlockingsProvider {

    public static final String NAME = "Nordic32";

    private static final String BLOCKING_NAME = "TCB";
    private static final String MEASUREMENT_POINT_ID = "1042_131";

    // the transformers the system blocks, from its reference description
    private static final List<String> TRANSFORMERS = List.of(
            "Tr11-1011", "Tr12-1012", "Tr13-1013", "Tr22-1022", "Tr1-1041", "Tr2-1042", "Tr3-1043",
            "Tr4-1044", "Tr5-1045", "Tr31-2031", "Tr32-2032", "Tr41-4041", "Tr42-4042", "Tr43-4043",
            "Tr46-4046", "Tr47-4047", "Tr51-4051", "Tr61-4061", "Tr62-4062", "Tr63-4063", "Tr71-4071",
            "Tr72-4072");

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "The tap changer blocking of the Nordic 32 test system, watching a transmission bus "
                + "and holding its distribution transformers.";
    }

    @Override
    public void createExtensions(Network network) {
        if (network.getExtension(TapChangerBlockings.class) != null) {
            return;
        }
        // the levels the transformers sit in, which is what a blocking controls; found on the
        // network so a network that is not the system, holding none of them, is left alone
        List<String> controlVoltageLevels = TRANSFORMERS.stream()
                .map(network::getTwoWindingsTransformer)
                .filter(Objects::nonNull)
                .map(t -> t.getTerminal1().getVoltageLevel().getId())
                .distinct()
                .toList();
        if (controlVoltageLevels.isEmpty()) {
            return;
        }
        Bus measuredBus = network.getBusBreakerView().getBus(MEASUREMENT_POINT_ID);
        String measuredVoltageLevelId = measuredBus != null
                ? measuredBus.getVoltageLevel().getId() : MEASUREMENT_POINT_ID;

        TapChangerBlockingAdder blocking = network.newExtension(TapChangerBlockingsAdder.class)
                .newTapChangerBlocking()
                .withName(BLOCKING_NAME);
        blocking.newMeasurementPoint()
                .withId(MEASUREMENT_POINT_ID)
                .withBuses(List.of(new MeasurementPoint.BusRef(measuredVoltageLevelId, MEASUREMENT_POINT_ID)))
                .add();
        controlVoltageLevels.forEach(id -> blocking.newControlVoltageLevel().withId(id).add());
        blocking.add().add();
    }
}
