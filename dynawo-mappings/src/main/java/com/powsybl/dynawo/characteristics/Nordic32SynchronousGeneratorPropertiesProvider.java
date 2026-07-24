/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.characteristics;

import com.google.auto.service.AutoService;
import com.powsybl.dynawo.extensions.api.generator.RpclType;
import com.powsybl.dynawo.extensions.api.generator.SynchronousGeneratorProperties;
import com.powsybl.dynawo.extensions.api.generator.SynchronousGeneratorProperties.Windings;
import com.powsybl.dynawo.extensions.api.generator.SynchronousGeneratorPropertiesAdder;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Holds the controls of the Nordic 32 test system, machine by machine.
 * <p>
 * Its machines are known individually rather than deduced from a rule: twelve run a Nordic
 * governor with the Nordic regulator, eight hold their mechanical power constant, and which is
 * which is a fact of the system, taken here from its reference description. So the controls are
 * named for each generator, and a machine the system does not name is left for another provider
 * to describe.
 * <p>
 * The system is a voltage stability study: no detailed counterpart of these controls exists, so
 * they are run in DynaWaltz and stand for themselves. Whether a machine is taken to sit behind a
 * transformer is not fixed here but left to the study, through the voltage it maps below, so the
 * plain models or their transformer variants are chosen without this having to know the grid.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
@AutoService(SynchronousGeneratorPropertiesProvider.class)
public class Nordic32SynchronousGeneratorPropertiesProvider implements SynchronousGeneratorPropertiesProvider {

    public static final String NAME = "Nordic32";

    private static final String VOLTAGE_REGULATOR = "VRNordic";
    private static final String GOVER_NORDIC = "GoverNordic";
    private static final String PM_CONST = "PmConst";

    private record Controls(Windings windings, String governor) {
    }

    private static final Controls GOVER_NORDIC_THREE = new Controls(Windings.THREE_WINDINGS, GOVER_NORDIC);
    private static final Controls PM_CONST_FOUR = new Controls(Windings.FOUR_WINDINGS, PM_CONST);
    private static final Controls PM_CONST_THREE = new Controls(Windings.THREE_WINDINGS, PM_CONST);

    // the machine by machine controls of the system, from its reference DynaWaltz description
    private static final Map<String, Controls> CONTROLS_BY_GENERATOR = controlsByGenerator();

    private static Map<String, Controls> controlsByGenerator() {
        Map<String, Controls> controls = new LinkedHashMap<>();
        for (String id : new String[] {"g01", "g02", "g03", "g04", "g05", "g08", "g09", "g10", "g11", "g12", "g19", "g20"}) {
            controls.put(id, GOVER_NORDIC_THREE);
        }
        for (String id : new String[] {"g06", "g07", "g14", "g15", "g16", "g17", "g18"}) {
            controls.put(id, PM_CONST_FOUR);
        }
        controls.put("g13", PM_CONST_THREE);
        return controls;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "Controls of the Nordic 32 test system, machine by machine, for a voltage stability "
                + "study (DynaWaltz).";
    }

    @Override
    public boolean isEligible(Generator generator) {
        return CONTROLS_BY_GENERATOR.containsKey(generator.getId());
    }

    @Override
    public void createExtensions(Network network) {
        network.getGeneratorStream()
                .filter(this::isEligible)
                .filter(g -> g.getExtension(SynchronousGeneratorProperties.class) == null)
                .forEach(this::createExtension);
    }

    private void createExtension(Generator generator) {
        Controls controls = CONTROLS_BY_GENERATOR.get(generator.getId());
        generator.newExtension(SynchronousGeneratorPropertiesAdder.class)
                .withNumberOfWindings(controls.windings())
                .withGovernor(controls.governor())
                .withVoltageRegulator(VOLTAGE_REGULATOR)
                .withPss("")
                .withAuxiliaries(false)
                .withInternalTransformer(false)
                .withRpcl(RpclType.NONE)
                .withUva(SynchronousGeneratorProperties.Uva.LOCAL)
                .withAggregated(false)
                .withQlim(false)
                .add();
    }
}
