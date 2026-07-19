/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.preassembled;

import java.util.List;
import java.util.Objects;

/**
 * A variable of one unit model wired to a variable of another.
 * <p>
 * The two ends are interchangeable, so two connections wiring the same pair of variables are the
 * same connection whichever way each was written. Dynawo writes some of them one way and some the
 * other, which is a difference of writing, not of meaning.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public record UnitConnection(String id1, String var1, String id2, String var2, boolean initialisation) {

    public UnitConnection {
        Objects.requireNonNull(id1);
        Objects.requireNonNull(var1);
        Objects.requireNonNull(id2);
        Objects.requireNonNull(var2);
    }

    public static UnitConnection of(UnitModel model1, String var1, UnitModel model2, String var2) {
        return new UnitConnection(model1.getId(), var1, model2.getId(), var2, false);
    }

    public static UnitConnection ofInitialisation(UnitModel model1, String var1, UnitModel model2, String var2) {
        return new UnitConnection(model1.getId(), var1, model2.getId(), var2, true);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof UnitConnection connection) || initialisation != connection.initialisation) {
            return false;
        }
        return ends().equals(connection.ends());
    }

    @Override
    public int hashCode() {
        return Objects.hash(ends(), initialisation);
    }

    /**
     * The two ends, ordered so that the way a connection was written does not tell it from an
     * identical one written the other way round.
     */
    private List<String> ends() {
        String end1 = id1 + "." + var1;
        String end2 = id2 + "." + var2;
        return end1.compareTo(end2) <= 0 ? List.of(end1, end2) : List.of(end2, end1);
    }
}
