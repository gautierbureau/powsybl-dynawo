/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.jni;

import com.powsybl.commons.PowsyblException;

/**
 * A failure of Dynawo, thrown by its native methods with the message that {@code dynawo.sh} prints for it
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public class DynawoNativeException extends PowsyblException {

    /**
     * Called by the native methods of Dynawo (DYNJNILauncher.cpp)
     */
    public DynawoNativeException(String message) {
        super(message);
    }
}
