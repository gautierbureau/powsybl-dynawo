/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.models.voltageregulation;

import com.powsybl.dynawo.models.defaultmodels.AbstractDefaultModel;

/**
 * The remotely regulated bus seen on the {@code NETWORK} model, giving the {@link VRRemote} the bus's
 * per-unit voltage. Unlike the frequency synchronizers — which reach the bus through the equipment they
 * synchronize — the regulated bus is a distinct bus, so it is keyed on its own static id.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public final class DefaultBusOfVRRemote extends AbstractDefaultModel implements BusOfVRRemoteModel {

    public static DefaultBusOfVRRemote of(VRRemoteModel model) {
        return new DefaultBusOfVRRemote(model.getRegulatedBus().getId());
    }

    private DefaultBusOfVRRemote(String regulatedBusStaticId) {
        super(regulatedBusStaticId);
    }

    @Override
    public String getName() {
        return "DefaultBusOfVRRemote";
    }

    @Override
    public String getUpuVarName() {
        return "@@NAME@@@NODE@_Upu_value";
    }
}
