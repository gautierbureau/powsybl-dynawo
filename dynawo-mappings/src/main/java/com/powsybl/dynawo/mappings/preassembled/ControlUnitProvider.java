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
 * Adds machine control units to the {@link ControlUnitCatalog}, so a deployment building models of
 * its own brings the governors, voltage regulators and regulator controls the open framework does
 * not ship without touching it.
 * <p>
 * Discovered with a {@link java.util.ServiceLoader} beside the open units, and merged the same way:
 * a unit is keyed by its model name, and one the open framework already declares keeps its place, so
 * a provider adds new controls rather than redefining known ones. This is how the RTE detailed
 * governors and regulators, in the {@code DynawoRTE} package, reach the designer that assembles a
 * machine's model from its controls.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public interface ControlUnitProvider {

    default List<MachineControlUnit> getGovernors() {
        return List.of();
    }

    default List<MachineControlUnit> getVoltageRegulators() {
        return List.of();
    }

    default List<RegulatorControlUnit> getRegulatorControls() {
        return List.of();
    }
}
