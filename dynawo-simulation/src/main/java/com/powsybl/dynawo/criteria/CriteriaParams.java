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
import java.util.OptionalDouble;

/**
 * The parameters a criteria is checked with — Dynawo's {@code CriteriaParams}: an id, when and how it is
 * checked ({@link CriteriaScope} / {@link CriteriaType}), an optional active-power band ({@code pMin} /
 * {@code pMax}) and, for a bus criteria, the voltage bands ({@link CriteriaParamsVoltageLevel}) it holds.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public final class CriteriaParams {

    private final String id;
    private final CriteriaScope scope;
    private final CriteriaType type;
    private final Double pMin;
    private final Double pMax;
    private final List<CriteriaParamsVoltageLevel> voltageLevels;

    private CriteriaParams(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "criteria params id");
        this.scope = Objects.requireNonNull(builder.scope, "criteria params scope");
        this.type = Objects.requireNonNull(builder.type, "criteria params type");
        this.pMin = builder.pMin;
        this.pMax = builder.pMax;
        this.voltageLevels = List.copyOf(builder.voltageLevels);
    }

    public String getId() {
        return id;
    }

    public CriteriaScope getScope() {
        return scope;
    }

    public CriteriaType getType() {
        return type;
    }

    public OptionalDouble getPMin() {
        return pMin == null ? OptionalDouble.empty() : OptionalDouble.of(pMin);
    }

    public OptionalDouble getPMax() {
        return pMax == null ? OptionalDouble.empty() : OptionalDouble.of(pMax);
    }

    public List<CriteriaParamsVoltageLevel> getVoltageLevels() {
        return Collections.unmodifiableList(voltageLevels);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String id;
        private CriteriaScope scope;
        private CriteriaType type;
        private Double pMin;
        private Double pMax;
        private final List<CriteriaParamsVoltageLevel> voltageLevels = new ArrayList<>();

        private Builder() {
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder scope(CriteriaScope scope) {
            this.scope = scope;
            return this;
        }

        public Builder type(CriteriaType type) {
            this.type = type;
            return this;
        }

        public Builder pMin(double pMin) {
            this.pMin = pMin;
            return this;
        }

        public Builder pMax(double pMax) {
            this.pMax = pMax;
            return this;
        }

        public Builder voltageLevel(CriteriaParamsVoltageLevel voltageLevel) {
            this.voltageLevels.add(Objects.requireNonNull(voltageLevel));
            return this;
        }

        public CriteriaParams build() {
            return new CriteriaParams(this);
        }
    }
}
