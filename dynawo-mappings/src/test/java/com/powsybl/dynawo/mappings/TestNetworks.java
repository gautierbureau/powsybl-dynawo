/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings;

import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.StaticVarCompensator;
import com.powsybl.iidm.network.TopologyKind;
import com.powsybl.iidm.network.VoltageLevel;

/**
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public final class TestNetworks {

    private TestNetworks() {
    }

    /**
     * A network holding a single generator, enough to exercise the model resolution.
     */
    public static Generator singleGenerator(double nominalV) {
        Network network = Network.create("test", "test");
        VoltageLevel voltageLevel = network.newSubstation().setId("s").add()
                .newVoltageLevel().setId("vl").setNominalV(nominalV).setTopologyKind(TopologyKind.BUS_BREAKER).add();
        voltageLevel.getBusBreakerView().newBus().setId("bus").add();
        return voltageLevel.newGenerator().setId("g")
                .setBus("bus").setConnectableBus("bus")
                .setTargetP(100).setMinP(0).setMaxP(200).setTargetV(nominalV).setVoltageRegulatorOn(true)
                .add();
    }

    /**
     * A network holding a single static var compensator, enough to exercise its mapping.
     */
    public static StaticVarCompensator singleStaticVarCompensator(double nominalV) {
        Network network = Network.create("test", "test");
        VoltageLevel voltageLevel = network.newSubstation().setId("s").add()
                .newVoltageLevel().setId("vl").setNominalV(nominalV).setTopologyKind(TopologyKind.BUS_BREAKER).add();
        voltageLevel.getBusBreakerView().newBus().setId("bus").add();
        return voltageLevel.newStaticVarCompensator().setId("svc")
                .setBus("bus").setConnectableBus("bus")
                .setBmin(-0.01).setBmax(0.01)
                .setRegulating(true).setRegulationMode(StaticVarCompensator.RegulationMode.VOLTAGE)
                .setVoltageSetpoint(nominalV)
                .add();
    }
}
