/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com/)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.models.generators;

import com.powsybl.dynawo.builders.ModelConfig;
import com.powsybl.dynawo.models.frequencysynchronizers.SignalNModel;
import com.powsybl.dynawo.models.svc.RpclGeneratorModel;
import com.powsybl.dynawo.models.utils.BusUtils;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Generator;

import java.io.IOException;
import java.nio.file.Path;

/**
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
 */
public class SignalNGenerator extends BaseGenerator implements SignalNModel, RpclGeneratorModel {

    protected SignalNGenerator(Generator generator, String parameterSetId, ModelConfig modelConfig) {
        super(generator, parameterSetId, modelConfig);
    }

    @Override
    public String getNVarName() {
        return "generator_N";
    }

    @Override
    public Bus getConnectableBus() {
        return BusUtils.getConnectableBus(equipment);
    }

    // A machine is only ever connected to the secondary voltage control on an Rpcl library, where these
    // variables exist; the control leaves any other machine out (see SecondaryVoltageControlSimplified).

    @Override
    public String getQStatorVarName() {
        return "generator_QStator";
    }

    @Override
    public String getBlockerVarName() {
        return "generator_blocker";
    }

    @Override
    public String getLevelVarName() {
        return "reactivePowerControlLoop_level";
    }

    @Override
    public void writeAuxiliaryFiles(Path workingDir) throws IOException {
        // a DiagramPQ model reads its reactive capability curve from a table file of its own
        if (getLib().contains("DiagramPQ")) {
            GeneratorDiagram.write(equipment, workingDir);
        }
    }
}
