/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com/)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.models.hvdc;

import com.powsybl.dynawo.models.macroconnections.MacroConnectAttribute;
import com.powsybl.dynawo.models.voltageregulation.VRRemote;
import com.powsybl.dynawo.models.voltageregulation.VRRemoteModel;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.VscConverterStation;

import java.util.List;

/**
 * One side of an HVDC line, seen by the {@link VRRemote} that coordinates its VSC converter's voltage with
 * the other machines regulating the same bus — the launcher's {@code HVDC_VRREMOTE_CONNECTOR_SIDE1/2}.
 * <p>
 * An HVDC line is two-sided, so it presents one of these per in-component converter: the model's own {@code
 * hvdc_NQ1} / {@code hvdc_NQ2} reactive injection and limit flags are summed at the converter's bus, and a
 * distinct name gives each side its own macro connector. It is a connection view, not a model of its own —
 * its macro connection targets the HVDC black box model by id.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public class HvdcVRRemoteSide implements VRRemoteModel {

    private final String hvdcModelId;
    private final VscConverterStation converter;
    private final String side;

    /**
     * @param hvdcModelId the HVDC black box model's dynamic id, the target of the macro connection
     * @param converter the VSC converter this side regulates, giving the bus and voltage set point
     * @param side {@code "1"} or {@code "2"}, the model's side these variables belong to
     */
    public HvdcVRRemoteSide(String hvdcModelId, VscConverterStation converter, String side) {
        this.hvdcModelId = hvdcModelId;
        this.converter = converter;
        this.side = side;
    }

    @Override
    public String getName() {
        return "HvdcVRRemoteSide" + side;
    }

    @Override
    public List<MacroConnectAttribute> getMacroConnectToAttributes() {
        return List.of(MacroConnectAttribute.of("id2", hvdcModelId));
    }

    @Override
    public Bus getRegulatedBus() {
        // the launcher keys a VRRemote by the converter's bus-breaker (topology) node, so two nodes a closed
        // switch merges into one calculated bus stay distinct here (e.g. HERAP7_S_VL7_TN1 vs …_TN2)
        return converter.getTerminal().getBusBreakerView().getBus();
    }

    @Override
    public double getURef0Pu() {
        return converter.getVoltageSetpoint() / converter.getTerminal().getVoltageLevel().getNominalV();
    }

    @Override
    public String getNQVarName() {
        return "hvdc_NQ" + side;
    }

    @Override
    public String getLimUQUpVarName() {
        return "hvdc_limUQUp" + side;
    }

    @Override
    public String getLimUQDownVarName() {
        return "hvdc_limUQDown" + side;
    }
}
