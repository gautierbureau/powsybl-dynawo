/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com/)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.criteria;

import java.util.OptionalDouble;

/**
 * A voltage band a bus criteria checks — Dynawo's {@code CriteriaParamsVoltageLevel}. It bounds the
 * per-unit voltage ({@code uMinPu} / {@code uMaxPu}) over a nominal-voltage range ({@code uNomMin} /
 * {@code uNomMax}); every bound is optional, absent where not set.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public final class CriteriaParamsVoltageLevel {

    private final Double uMinPu;
    private final Double uMaxPu;
    private final Double uNomMin;
    private final Double uNomMax;

    private CriteriaParamsVoltageLevel(Builder builder) {
        this.uMinPu = builder.uMinPu;
        this.uMaxPu = builder.uMaxPu;
        this.uNomMin = builder.uNomMin;
        this.uNomMax = builder.uNomMax;
    }

    public OptionalDouble getUMinPu() {
        return uMinPu == null ? OptionalDouble.empty() : OptionalDouble.of(uMinPu);
    }

    public OptionalDouble getUMaxPu() {
        return uMaxPu == null ? OptionalDouble.empty() : OptionalDouble.of(uMaxPu);
    }

    public OptionalDouble getUNomMin() {
        return uNomMin == null ? OptionalDouble.empty() : OptionalDouble.of(uNomMin);
    }

    public OptionalDouble getUNomMax() {
        return uNomMax == null ? OptionalDouble.empty() : OptionalDouble.of(uNomMax);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private Double uMinPu;
        private Double uMaxPu;
        private Double uNomMin;
        private Double uNomMax;

        private Builder() {
        }

        public Builder uMinPu(double uMinPu) {
            this.uMinPu = uMinPu;
            return this;
        }

        public Builder uMaxPu(double uMaxPu) {
            this.uMaxPu = uMaxPu;
            return this;
        }

        public Builder uNomMin(double uNomMin) {
            this.uNomMin = uNomMin;
            return this;
        }

        public Builder uNomMax(double uNomMax) {
            this.uNomMax = uNomMax;
            return this;
        }

        public CriteriaParamsVoltageLevel build() {
            return new CriteriaParamsVoltageLevel(this);
        }
    }
}
