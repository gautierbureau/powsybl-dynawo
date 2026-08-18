/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.models.svc;

import com.powsybl.dynawo.models.Model;

/**
 * A generator whose reactive power a {@link SecondaryVoltageControlSimplified} coordinates — the
 * simplified secondary voltage control DynaFlow runs.
 * <p>
 * It is far simpler than the detailed RTE reactive-power-control loop: the control reads the machine's
 * stator reactive power and its reactive-limit blocker, and writes back one shared level. Only the
 * {@code GeneratorPV*Rpcl*SignalN} libraries carry these variables, so only a generator the mapping put
 * on such a library — one in a control zone — is ever connected to the control.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public interface RpclGeneratorModel extends Model {

    /** The machine's stator reactive power the control reads, per machine. */
    String getQStatorVarName();

    /** The machine's reactive-limit blocker the control reads, per machine. */
    String getBlockerVarName();

    /** The control level the machine reads back — one shared signal, written to every machine. */
    String getLevelVarName();
}
