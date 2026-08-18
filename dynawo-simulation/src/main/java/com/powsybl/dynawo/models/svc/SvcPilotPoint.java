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
 * The bus whose voltage a {@link SecondaryVoltageControlSimplified} holds, seen on the {@code NETWORK}
 * model: the control reads the bus's per-unit voltage straight from the static network, keyed on the
 * bus id, no dynamic model of the bus required.
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
