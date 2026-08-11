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

    /**
     * The name the catalog keys this control under, and a built model carries in its library, which
     * is the last part of its Modelica model unless the control stands for that model under a name
     * of its own.
     * <p>
     * A control is usually named after the model that implements it, so the two are the same. Some
     * are not: a machine may name a regulator no distinct model implements, the fictitious regulator
     * built on the {@code VRP320} model for one, and the catalog answers that name with the model
     * standing in for it. Decoupling the two is what lets one model answer several such names.
     */
    default String getCatalogName() {
        String model = getName();
        return model.substring(model.lastIndexOf('.') + 1);
    }

    List<UnitConnection> getConnectionsWith(MachineUnit machine);

    List<UnitConnection> getInitConnectionsWith(MachineUnit machine);
}
