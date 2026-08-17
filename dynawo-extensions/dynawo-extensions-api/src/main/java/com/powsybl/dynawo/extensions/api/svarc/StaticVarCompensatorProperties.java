/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.extensions.api.svarc;

import com.powsybl.commons.extensions.Extension;
import com.powsybl.iidm.network.StaticVarCompensator;

/**
 * The constructor of a static var compensator, the manufacturer whose detailed model a transient
 * study runs it on: a compensator named {@code Alstom} runs {@code StaticVarCompensatorAlstom},
 * {@code Siemens} the Siemens one, and so on, where a voltage stability study runs the one simplified
 * {@code StaticVarCompensator} whatever the constructor.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public interface StaticVarCompensatorProperties extends Extension<StaticVarCompensator> {

    String NAME = "staticVarCompensatorProperties";

    @Override
    default String getName() {
        return NAME;
    }

    String getConstructor();

    void setConstructor(String constructor);
}
