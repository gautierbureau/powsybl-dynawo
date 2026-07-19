/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.preassembled;

import java.util.List;

/**
 * A control of the machine: a governor, a voltage regulator, a stabiliser.
 * <p>
 * It states what it wires to the machine, both while the simulation runs and while its initial
 * state is computed, which is what lets an assembly be composed out of controls that were never
 * assembled together.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public interface ControlUnit extends UnitModel {

    List<UnitConnection> getConnectionsWith(MachineUnit machine);

    List<UnitConnection> getInitConnectionsWith(MachineUnit machine);
}
