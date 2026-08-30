/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com/)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.models.boundarylines;

import com.powsybl.dynawo.models.defaultmodels.AbstractInjectionDefaultModel;

/**
 * The network model of a boundary line (the tcb core's name for a dangling line) — used when no dynamic
 * model represents it, e.g. to disconnect it in a security analysis. As an injection it exposes {@code
 * @NAME@_state_value} for the switch-off signal.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public class DefaultBoundaryLine extends AbstractInjectionDefaultModel {

    public DefaultBoundaryLine(String staticId) {
        super(staticId);
    }

    @Override
    public String getName() {
        return "DefaultBoundaryLine";
    }
}
