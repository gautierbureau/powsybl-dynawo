/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.xml;

import com.powsybl.dynawo.DynawoSimulationConstants;
import com.powsybl.dynawo.DynawoSimulationContext;
import com.powsybl.dynawo.commons.DynawoVersion;
import com.powsybl.dynawo.models.events.EventDisconnectionBuilder;
import com.powsybl.dynawo.models.generators.SignalNGeneratorBuilder;
import com.powsybl.dynawo.models.loads.BaseLoadBuilder;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import java.io.IOException;

/**
 * The variables of the models for Dynawo 2.0.0: without their {@code _value} suffix
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
class Dynawo2SignalNXmlTest extends AbstractDynamicModelXmlTest {

    @Override
    protected void setupNetwork() {
        network = EurostagTutorialExample1Factory.createWithLFResults();
    }

    @Override
    protected void addDynamicModels() {
        dynamicModels.add(SignalNGeneratorBuilder.of(network, "GeneratorPVSignalN")
                .staticId("GEN")
                .parameterSetId("n")
                .build());
        dynamicModels.add(BaseLoadBuilder.of(network, "LoadAlphaBetaRestorative")
                .staticId("LOAD")
                .parameterSetId("load")
                .build());
        eventModels.add(EventDisconnectionBuilder.of(network)
                .staticId("NHV1_NHV2_1")
                .startTime(1)
                .disconnectOnly(TwoSides.TWO)
                .build());
        eventModels.add(EventDisconnectionBuilder.of(network)
                .staticId("LOAD")
                .startTime(1)
                .build());
    }

    @Override
    protected DynawoSimulationContext.Builder setupDynawoContextBuilder() {
        return super.setupDynawoContextBuilder().currentVersion(new DynawoVersion(2, 0, 0));
    }

    @Test
    void writeModel() throws SAXException, IOException {
        DydXml.write(tmpDir, context.getSimulationDydData(), context.getCurrentDynawoVersion());
        validate("dyd.xsd", "signal_n_dynawo_2_dyd.xml", tmpDir.resolve(DynawoSimulationConstants.DYD_FILENAME));
    }
}
