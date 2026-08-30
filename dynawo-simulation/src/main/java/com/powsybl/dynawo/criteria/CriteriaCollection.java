/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com/)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.criteria;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A set of {@link Criteria} grouped by the kind of component they check — Dynawo's {@code
 * CRT::CriteriaCollection}. It is what a criteria file holds and what a simulation is given to check its
 * buses, loads and generators against.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public class CriteriaCollection {

    /** The kind of component a criteria checks — Dynawo's {@code CriteriaCollectionType_t}. */
    public enum Type {
        BUS,
        LOAD,
        GENERATOR
    }

    private final Map<Type, List<Criteria>> criteriaByType = new EnumMap<>(Type.class);

    public CriteriaCollection() {
        for (Type type : Type.values()) {
            criteriaByType.put(type, new ArrayList<>());
        }
    }

    /** Adds a criteria checking the given kind of component. */
    public CriteriaCollection add(Type type, Criteria criteria) {
        criteriaByType.get(Objects.requireNonNull(type)).add(Objects.requireNonNull(criteria));
        return this;
    }

    /** The criteria checking the given kind of component, in the order they were added. */
    public List<Criteria> getCriteria(Type type) {
        return Collections.unmodifiableList(criteriaByType.get(Objects.requireNonNull(type)));
    }

    /** Whether no criteria has been added, for any kind. */
    public boolean isEmpty() {
        return criteriaByType.values().stream().allMatch(List::isEmpty);
    }
}
