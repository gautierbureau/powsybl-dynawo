/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.models.voltageregulation;

import com.powsybl.dynawo.builders.ModelConfig;
import com.powsybl.dynawo.models.AbstractPureDynamicBlackBoxModel;
import com.powsybl.dynawo.models.VarConnection;
import com.powsybl.dynawo.models.macroconnections.MacroConnectAttribute;
import com.powsybl.dynawo.models.macroconnections.MacroConnectionsAdder;
import com.powsybl.dynawo.parameters.ParametersSet;

import java.util.List;
import java.util.function.Consumer;

import static com.powsybl.dynawo.parameters.ParameterType.BOOL;
import static com.powsybl.dynawo.parameters.ParameterType.DOUBLE;

/**
 * Coordinates the machines that remotely regulate one bus's voltage. Like {@code SignalN} and
 * {@code OmegaRef}, this is a pure-dynamic model no user maps: it exists only because one or more
 * {@link VRRemoteModel}s call for it, and the framework adds one {@code VRRemote} per regulated bus (see
 * {@code AbstractContextBuilder}), the way the DynaFlow Launcher does.
 * <p>
 * Its inputs are wired the way the launcher wires them: every regulating machine's reactive injection is
 * summed into {@code vrremote_NQ} (an un-indexed flow point), while its two reactive-limit flags are read
 * at the machine's index through {@code vrremote_limUQUp_@INDEX@_} / {@code vrremote_limUQDown_@INDEX@_};
 * the regulated bus's per-unit voltage feeds {@code vrremote_URegulatedPu} from the {@code NETWORK} model.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public class VRRemote extends AbstractPureDynamicBlackBoxModel {

    private static final String ID_PREFIX = "Model_Signal_NQ_";
    private static final ModelConfig MODEL_CONFIG = new ModelConfig("VRRemote");

    private final List<VRRemoteModel> regulatingModels;
    private final String defaultParFile;

    /**
     * @param regulatedBusId the bus every model in {@code regulatingModels} regulates — its id names both
     *                       the model and its parameter set
     * @param regulatingModels the machines regulating that bus, in the order they read their reactive limits
     * @param defaultParFile the study's default parameter file
     */
    public VRRemote(String regulatedBusId, List<VRRemoteModel> regulatingModels, String defaultParFile) {
        super(ID_PREFIX + regulatedBusId, ID_PREFIX + regulatedBusId, MODEL_CONFIG);
        this.regulatingModels = regulatingModels;
        this.defaultParFile = defaultParFile;
    }

    @Override
    public void createMacroConnections(MacroConnectionsAdder adder) {
        int index = 0;
        for (VRRemoteModel model : regulatingModels) {
            adder.createMacroConnections(this, model, getVarConnectionsWith(model), MacroConnectAttribute.ofIndex1(index));
            index++;
        }
        BusOfVRRemoteModel busOf = DefaultBusOfVRRemote.of(regulatingModels.getFirst());
        adder.createMacroConnections(this, busOf, getVarConnectionsWithBus(busOf));
    }

    private List<VarConnection> getVarConnectionsWith(VRRemoteModel model) {
        return List.of(
                new VarConnection("vrremote_NQ", model.getNQVarName()),
                new VarConnection("vrremote_limUQUp_@INDEX@_", model.getLimUQUpVarName()),
                new VarConnection("vrremote_limUQDown_@INDEX@_", model.getLimUQDownVarName()));
    }

    private List<VarConnection> getVarConnectionsWithBus(BusOfVRRemoteModel busOf) {
        return List.of(new VarConnection("vrremote_URegulatedPu", busOf.getUpuVarName()));
    }

    @Override
    public void createDynamicModelParameters(Consumer<ParametersSet> parametersAdder) {
        double uRef0Pu = regulatingModels.getFirst().getURef0Pu();
        ParametersSet set = new ParametersSet(getParameterSetId());
        set.addParameter("vrremote_U0Pu", DOUBLE, Double.toString(uRef0Pu));
        set.addParameter("vrremote_URef0Pu", DOUBLE, Double.toString(uRef0Pu));
        set.addParameter("vrremote_Frozen0", BOOL, "true");
        parametersAdder.accept(set);
    }

    @Override
    public String getDefaultParFile() {
        return defaultParFile;
    }
}
