/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.models;

import com.powsybl.dynawo.commons.DynawoVersion;

import java.util.function.UnaryOperator;

/**
 * The names of the variables of the models, as the dyd file gives them to a version of Dynawo. Since Dynawo 2.0.0, the
 * variables of its C++ models and the connectors of its Modelica models are no more structures with a value: they lost
 * their {@code _value} suffix, and the indexed variables of the C++ models end with an underscore.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public final class VariableNames {

    private static final DynawoVersion WITHOUT_VALUE_MIN_VERSION = new DynawoVersion(2, 0, 0);
    private static final String VALUE = "value";
    private static final String VALUE_SUFFIX = "_" + VALUE;
    private static final String INDEX = "@INDEX@";

    private VariableNames() {
    }

    /**
     * The names of the variables for a version of Dynawo: the ones of the models before Dynawo 2.0.0, or if the version
     * is unknown
     */
    public static UnaryOperator<String> of(DynawoVersion dynawoVersion) {
        return dynawoVersion != null && dynawoVersion.compareTo(WITHOUT_VALUE_MIN_VERSION) >= 0
                ? VariableNames::withoutValue
                : UnaryOperator.identity();
    }

    /**
     * The name of a variable for Dynawo 2.0.0 and later
     */
    static String withoutValue(String name) {
        if (name.endsWith(INDEX + VALUE_SUFFIX)) {
            return name.substring(0, name.length() - VALUE.length());
        }
        if (name.endsWith(INDEX)) {
            return name + "_";
        }
        if (name.endsWith(VALUE_SUFFIX)) {
            return name.substring(0, name.length() - VALUE_SUFFIX.length());
        }
        return name;
    }
}
