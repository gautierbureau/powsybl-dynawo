/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.preassembled;

/**
 * The controls a machine is to be built with, named as the models that carry them are.
 * <p>
 * These are the controls settled on, not the ones an extension happens to hold: a study running
 * simplified regulations has already turned the detailed controls into the simplified ones by the
 * time it asks for a model. Which study it is, and what it made of the controls it was given, is
 * the mapping's business and is over by here.
 *
 * @param governor         the governor model, {@code GovCt2} or {@code GoverProportional}
 * @param voltageRegulator the voltage regulator model, {@code St4b} or {@code VRProportional}
 * @param pss              the stabiliser model, or null where the machine carries none
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public record GeneratorControls(String governor, String voltageRegulator, String pss) {

    public GeneratorControls(String governor, String voltageRegulator) {
        this(governor, voltageRegulator, null);
    }

    public boolean hasPss() {
        return pss != null && !pss.isEmpty();
    }
}
