/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.models.voltageregulation;

import com.powsybl.dynawo.models.Model;

/**
 * View of the remotely regulated bus from the {@link VRRemote}, used to reach the bus's per-unit voltage
 * on the {@code NETWORK} model.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public interface BusOfVRRemoteModel extends Model {

    String getUpuVarName();
}
