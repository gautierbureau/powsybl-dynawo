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
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.StaticVarCompensator;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Gives every static var compensator a dynamic model.
 * <p>
 * A compensator holds a voltage the way a machine does, faster and within a band, so a transient
 * study needs one behind each. They are all given the same model, watching the voltage and moving
 * its susceptance between the limits to hold it, which is what a network solves around where a
 * compensator, not a machine, is what steadies a point.
 * <p>
 * Where its working point starts is read from the load flow, the rest of what the model expects
 * being values the reference systems use where nothing more is known of a compensator.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public final class SvarcMapping {

    static final String LIB = "StaticVarCompensator";

    // what the model expects beyond its working point, the values the reference systems ship where
    // nothing singles out a compensator; the susceptance limits, the regulation gains and the mode
    // thresholds a compensator holds its band by
    private static final Map<String, String> DEFAULT_PARAMETERS = new java.util.LinkedHashMap<>();

    static {
        DEFAULT_PARAMETERS.put("SVarC_BMaxPu", "1.07");
        DEFAULT_PARAMETERS.put("SVarC_BMinPu", "-0.83");
        DEFAULT_PARAMETERS.put("SVarC_BShuntPu", "0.1");
        DEFAULT_PARAMETERS.put("SVarC_IMaxPu", "29");
        DEFAULT_PARAMETERS.put("SVarC_IMinPu", "-29");
        DEFAULT_PARAMETERS.put("SVarC_KCurrentLimiter", "6.0");
        DEFAULT_PARAMETERS.put("SVarC_Kp", "2.7");
        DEFAULT_PARAMETERS.put("SVarC_Lambda", "0.016");
        DEFAULT_PARAMETERS.put("SVarC_SNom", "215");
        DEFAULT_PARAMETERS.put("SVarC_Ti", "0.002");
        DEFAULT_PARAMETERS.put("SVarC_UBlock", "72");
        DEFAULT_PARAMETERS.put("SVarC_UNom", "225");
        DEFAULT_PARAMETERS.put("SVarC_URefDown", "235");
        DEFAULT_PARAMETERS.put("SVarC_URefUp", "242");
        DEFAULT_PARAMETERS.put("SVarC_UThresholdDown", "230");
        DEFAULT_PARAMETERS.put("SVarC_UThresholdUp", "245");
        DEFAULT_PARAMETERS.put("SVarC_UUnblockDown", "160");
        DEFAULT_PARAMETERS.put("SVarC_UUnblockUp", "3050");
        DEFAULT_PARAMETERS.put("SVarC_tThresholdDown", "0");
        DEFAULT_PARAMETERS.put("SVarC_tThresholdUp", "34.3");
    }

    // the starting mode the reference compensator ships; the network's own regulation mode is not
    // carried in, so every compensator begins the same and the run settles it
    private static final String DEFAULT_MODE = "3";

    private final String parameterSetPrefix;

    public SvarcMapping(String parameterSetPrefix) {
        this.parameterSetPrefix = parameterSetPrefix;
    }

    public List<MappedModelsSupplier.MappedModel> createModelConfigs(Network network) {
        return svarcs(network)
                .map(svarc -> new MappedModelsSupplier.MappedModel(LIB, svarc.getId(), setId(svarc)))
                .toList();
    }

    public List<ParametersSet> createParameters(Network network) {
        return svarcs(network).map(this::parameters).toList();
    }

    private ParametersSet parameters(StaticVarCompensator svarc) {
        ParametersSet set = new ParametersSet(setId(svarc));
        // where the working point starts, read from the load flow of the compensator the model
        // stands for
        set.addReference("SVarC_P0Pu", ParameterType.DOUBLE, "p_pu");
        set.addReference("SVarC_Q0Pu", ParameterType.DOUBLE, "q_pu");
        set.addReference("SVarC_U0Pu", ParameterType.DOUBLE, "v_pu");
        set.addReference("SVarC_UPhase0", ParameterType.DOUBLE, "angle_pu");
        set.addParameter("SVarC_Mode0", ParameterType.INT, DEFAULT_MODE);
        DEFAULT_PARAMETERS.forEach((name, value) -> set.addParameter(name, ParameterType.DOUBLE, value));
        return set;
    }

    private String setId(StaticVarCompensator svarc) {
        return parameterSetPrefix + svarc.getId();
    }

    /**
     * The compensators a mapping covers, which is all of them: a compensator steadies a voltage the
     * study solves around, so every one is behind it.
     */
    private static Stream<StaticVarCompensator> svarcs(Network network) {
        return network.getStaticVarCompensatorStream();
    }
}
