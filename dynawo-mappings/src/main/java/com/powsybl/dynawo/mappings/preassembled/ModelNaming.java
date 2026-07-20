/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.preassembled;

/**
 * What a Dynawo release calls the things a preassembled model is made of.
 * <p>
 * The models themselves are the same from one release to the next far more often than their names
 * are, so a definition that will not build is usually a definition naming things the way another
 * release does. Everything that differs between the releases we target is gathered here, so that
 * describing a model says what it is made of and this says what to call it.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public enum ModelNaming {

    /**
     * What Dynawo calls things today.
     */
    CURRENT("Dynawo.Electrical.Transformers.TransformersFixedTap.",
            "", "i0Pu", "U0PuVar", "running.value"),

    /**
     * What Dynawo 1.7.0 called them.
     * <p>
     * The machine exchanges its quantities through connectors there, which meet the plain input of
     * a control through their value. #3895 unwrapped them, leaving the bare names. The
     * transformers were still one flat package, #3585 having split them afterwards without
     * touching the models. And a regulator had no running input at all, so the machine has no such
     * quantity to offer and the wire that would carry it is never made: a regulator behaves as
     * always running either way.
     */
    DYNAWO_1_7_0("Dynawo.Electrical.Transformers.",
            ".value", "iStator0Pu", "U0Pu", null);

    private final String transformerPackage;
    private final String connectorSuffix;
    private final String initCurrent;
    private final String initVoltageMagnitude;
    private final String running;

    ModelNaming(String transformerPackage, String connectorSuffix, String initCurrent,
                String initVoltageMagnitude, String running) {
        this.transformerPackage = transformerPackage;
        this.connectorSuffix = connectorSuffix;
        this.initCurrent = initCurrent;
        this.initVoltageMagnitude = initVoltageMagnitude;
        this.running = running;
    }

    String getTransformerPackage() {
        return transformerPackage;
    }

    /**
     * A quantity the machine exchanges with a control, named the way this release has it.
     * <p>
     * Only the quantities a control reads while the simulation runs went through the connectors,
     * which is why the initial values and the quantities named apart are not asked of this.
     */
    String exchanged(String varName) {
        return varName + connectorSuffix;
    }

    String getInitCurrent() {
        return initCurrent;
    }

    String getInitVoltageMagnitude() {
        return initVoltageMagnitude;
    }

    /**
     * How the machine says whether it is running, or null where it has no such thing to say.
     */
    String getRunning() {
        return running;
    }
}
