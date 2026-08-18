/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.models.svc;

import com.powsybl.dynawo.models.defaultmodels.AbstractDefaultModel;

/**
 * The bus whose voltage a {@link SecondaryVoltageControlSimplified} holds, read straight from the static
 * {@code NETWORK} model by the bus id.
 * <p>
 * The simplified control always reads the pilot voltage from the network — the DynaFlow Launcher wires it
 * that way whether or not the bus carries a dynamic model (its {@code launch_svc} and {@code
 * launch_svc_network} references connect the same {@code UpPu_value <-> @NAME@_Upu_value} to
 * {@code NETWORK}). This is unlike the detailed control, which reaches the pilot through the generic
 * {@code ActionConnectionPoint}: that too falls back to the network for a static bus, but on a different
 * variable ({@code @NAME@_Upu}), and it can instead bind a dynamic bus's own variable. Because the
 * network variable the simplified control needs — {@code @NAME@_Upu_value} — is not the one the generic
 * connection point gives, it has this pilot point of its own.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public final class SvcPilotPoint extends AbstractDefaultModel {

    public SvcPilotPoint(String pilotBusStaticId) {
        super(pilotBusStaticId);
    }

    @Override
    public String getName() {
        return "SvcPilotPoint";
    }

    public String getUpuVarName() {
        return "@NAME@_Upu_value";
    }
}
