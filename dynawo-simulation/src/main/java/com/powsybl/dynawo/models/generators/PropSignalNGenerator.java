/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com/)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.models.generators;

import com.powsybl.dynawo.builders.ModelConfig;
import com.powsybl.dynawo.models.voltageregulation.VRRemoteModel;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Generator;

/**
 * A SignalN generator that shares a bus's reactive power proportionally with the other machines regulating
 * it — a {@code GeneratorPQProp*SignalN} library.
 * <p>
 * These are the machines a {@link VRRemoteModel} coordinates: unlike a machine alone on its bus (a plain or
 * remote model) or one led by a secondary voltage control (an {@code Rpcl} model), a proportional machine
 * needs the {@code VRRemote} the framework raises per regulated bus to split the reactive power — the
 * launcher's {@code isRegulatingLocallyWithOthers} generators.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public class PropSignalNGenerator extends SignalNGenerator implements VRRemoteModel {

    protected PropSignalNGenerator(Generator generator, String parameterSetId, ModelConfig modelConfig) {
        super(generator, parameterSetId, modelConfig);
    }

    @Override
    public Bus getRegulatedBus() {
        return equipment.getRegulatingTerminal().getBusView().getBus();
    }

    @Override
    public double getURef0Pu() {
        return equipment.getTargetV() / equipment.getRegulatingTerminal().getVoltageLevel().getNominalV();
    }

    @Override
    public String getNQVarName() {
        return "generator_NQ";
    }

    @Override
    public String getLimUQUpVarName() {
        return "generator_limUQUp";
    }

    @Override
    public String getLimUQDownVarName() {
        return "generator_limUQDown";
    }
}
