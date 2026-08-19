/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com/)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.models.buses;

import com.powsybl.dynawo.models.defaultmodels.AbstractDefaultModel;

/**
 * The bus a {@code SignalN} anchors its phase reference to, addressed on the network by its own id — the
 * slack bus, the way the DynaFlow Launcher connects {@code signalN_thetaRef} to {@code
 * <slackBus>_phi_value}.
 * <p>
 * Unlike {@link DefaultBusOfSignalN}, which reaches a bus through an equipment sitting on it, this points
 * straight at the bus's network phase variable (the {@code @NAME@} placeholder resolving to the bus id, as
 * an {@link DefaultActionConnectionPoint} does for a voltage), so the reference can be any bus — the
 * launcher's slack node — not only one carrying a generator.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public final class SlackBusOfSignalN extends AbstractDefaultModel implements BusOfSignalNModel {

    public SlackBusOfSignalN(String slackBusStaticId) {
        super(slackBusStaticId);
    }

    @Override
    public String getName() {
        return "SlackBusOfSignalN";
    }

    @Override
    public String getPhiVarName() {
        return "@NAME@_phi_value";
    }
}
