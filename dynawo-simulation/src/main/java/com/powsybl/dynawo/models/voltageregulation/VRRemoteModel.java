/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.models.voltageregulation;

import com.powsybl.dynawo.models.Model;
import com.powsybl.iidm.network.Bus;

/**
 * A generator (or other equipment) that regulates a <em>remote</em> bus's voltage and therefore needs a
 * {@link VRRemote} coordinating it with the other machines regulating the same bus.
 * <p>
 * A user never instantiates a {@link VRRemote}: it exists only because one or more of these models call
 * for it. The framework gathers every {@code VRRemoteModel} regulating the same bus and, in
 * {@code AbstractContextBuilder}, adds one {@link VRRemote} per regulated bus — the way the DynaFlow
 * Launcher does. This mirrors how {@code SignalNModel}s summon a single {@code SignalN} frequency signal.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public interface VRRemoteModel extends Model {

    /** The bus this model regulates remotely — the grouping key: one {@link VRRemote} per regulated bus. */
    Bus getRegulatedBus();

    /** The voltage setpoint at the regulated bus, in per-unit — shared by every model regulating it. */
    double getURef0Pu();

    /** This model's reactive-power injection variable, summed by the {@link VRRemote}. */
    String getNQVarName();

    /** This model's upper reactive-limit flag, read by the {@link VRRemote} at this model's index. */
    String getLimUQUpVarName();

    /** This model's lower reactive-limit flag, read by the {@link VRRemote} at this model's index. */
    String getLimUQDownVarName();
}
