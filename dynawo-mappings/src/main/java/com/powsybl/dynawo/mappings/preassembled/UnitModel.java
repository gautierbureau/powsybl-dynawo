/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.preassembled;

/**
 * One element of a preassembled model: a machine, a governor, a voltage regulator, the transformer
 * a unit is connected through.
 * <p>
 * A preassembled model connects those the way a dyd connects the models built out of them. It is
 * the same act, wiring one variable of a model to a variable of another, and the objects here say
 * it the same way {@link com.powsybl.dynawo.models.BlackBoxModel} does: a model exposes the name of
 * each variable another may need, and states which connections it makes with whom, rather than
 * having them written for it somewhere else.
 * <p>
 * What a dyd cannot express is the initialisation, connected here through
 * {@link #getInitConnectionsWith}. That is the only reason a machine and its controls have to be
 * bundled at compile time instead of being wired at run time.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public interface UnitModel {

    /**
     * How the model is named inside the assembly, {@code generator} or {@code voltageRegulator}
     * for instance. Two models of the same assembly never share it.
     */
    String getId();

    /**
     * The Modelica model this stands for.
     */
    String getName();

    /**
     * The Modelica model computing its initial state, or {@code null} when it needs none.
     */
    String getInitName();
}
