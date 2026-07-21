/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings;

import com.powsybl.dynawo.parameters.ParameterType;
import com.powsybl.dynawo.parameters.ParametersSet;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;

import java.util.List;
import java.util.stream.Stream;

/**
 * Gives every load a dynamic model.
 * <p>
 * A load holds its network the way a machine holds it up, so a transient study needs one behind
 * each. They are all given the same voltage dependent model, drawing power that rises and falls
 * with the voltage rather than staying fixed, which is what a network solves around when a machine
 * is not there to hold the voltage itself.
 * <p>
 * How steeply the draw follows the voltage is the model's two parameters, and where it starts is
 * read from the load flow, so a set values a load without anything being said about it.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public final class LoadMapping {

    static final String LIB = "LoadAlphaBeta";

    // a load drawing active power with the voltage and reactive power with its square, which is
    // what the reference systems use where nothing more is known of a load
    private static final String DEFAULT_ALPHA = "1";
    private static final String DEFAULT_BETA = "2";

    private final String parameterSetPrefix;

    public LoadMapping(String parameterSetPrefix) {
        this.parameterSetPrefix = parameterSetPrefix;
    }

    public List<MappedModelsSupplier.MappedModel> createModelConfigs(Network network) {
        return loads(network)
                .map(load -> new MappedModelsSupplier.MappedModel(LIB, load.getId(), setId(load)))
                .toList();
    }

    public List<ParametersSet> createParameters(Network network) {
        return loads(network).map(this::parameters).toList();
    }

    private ParametersSet parameters(Load load) {
        ParametersSet set = new ParametersSet(setId(load));
        set.addParameter("load_alpha", ParameterType.DOUBLE, DEFAULT_ALPHA);
        set.addParameter("load_beta", ParameterType.DOUBLE, DEFAULT_BETA);
        // where the draw starts, read from the load flow of the load the model stands for
        set.addReference("load_P0Pu", ParameterType.DOUBLE, "p_pu");
        set.addReference("load_Q0Pu", ParameterType.DOUBLE, "q_pu");
        set.addReference("load_U0Pu", ParameterType.DOUBLE, "v_pu");
        set.addReference("load_UPhase0", ParameterType.DOUBLE, "angle_pu");
        return set;
    }

    private String setId(Load load) {
        return parameterSetPrefix + load.getId();
    }

    /**
     * The loads a mapping covers, which is all of them: a load is what a machine holds up, so
     * every one is behind the study whether or not anything singled it out.
     */
    private static Stream<Load> loads(Network network) {
        return network.getLoadStream();
    }
}
