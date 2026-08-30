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
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * One criteria — Dynawo's {@code CRT::Criteria}: the {@link CriteriaParams} it is checked with, and the
 * components and countries it is narrowed to. With no component and no country it applies to every
 * component of its kind.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public final class Criteria {

    private final CriteriaParams params;
    private final List<ComponentRef> components;
    private final List<String> countries;

    private Criteria(Builder builder) {
        this.params = Objects.requireNonNull(builder.params, "criteria params");
        this.components = List.copyOf(builder.components);
        this.countries = List.copyOf(builder.countries);
    }

    public CriteriaParams getParams() {
        return params;
    }

    public List<ComponentRef> getComponents() {
        return Collections.unmodifiableList(components);
    }

    public List<String> getCountries() {
        return Collections.unmodifiableList(countries);
    }

    public boolean hasCountryFilter() {
        return !countries.isEmpty();
    }

    /**
     * A component a criteria is narrowed to — Dynawo's {@code Criteria::ComponentId}: its id, and the
     * voltage level it sits in for a bus component (absent otherwise).
     */
    public record ComponentRef(String id, String voltageLevelId) {

        public ComponentRef {
            Objects.requireNonNull(id, "component id");
        }

        public Optional<String> getVoltageLevelId() {
            return Optional.ofNullable(voltageLevelId);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private CriteriaParams params;
        private final List<ComponentRef> components = new ArrayList<>();
        private final List<String> countries = new ArrayList<>();

        private Builder() {
        }

        public Builder params(CriteriaParams params) {
            this.params = params;
            return this;
        }

        public Builder component(String id) {
            this.components.add(new ComponentRef(id, null));
            return this;
        }

        public Builder component(String id, String voltageLevelId) {
            this.components.add(new ComponentRef(id, voltageLevelId));
            return this;
        }

        public Builder country(String country) {
            this.countries.add(Objects.requireNonNull(country));
            return this;
        }

        public Criteria build() {
            return new Criteria(this);
        }
    }
}
