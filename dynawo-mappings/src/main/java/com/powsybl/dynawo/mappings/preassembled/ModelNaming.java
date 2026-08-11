/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.preassembled;

/**
 * What a Dynawo release, or a deployment's own library, calls the things a preassembled model is
 * made of.
 * <p>
 * The models themselves are the same from one release to the next far more often than their names
 * are, so a definition that will not build is usually a definition naming things the way another
 * release does. Everything that differs between the releases we target is gathered here, so that
 * describing a model says what it is made of and this says what to call it. A deployment naming a
 * part its own way, the RTE transformer init among them, builds one of these rather than adding a
 * value to a fixed set.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public final class ModelNaming {

    private static final String GENERATOR_TRANSFORMER_AUX_INIT = "GeneratorTransformerAux_INIT";

    /**
     * What Dynawo calls things today.
     */
    public static final ModelNaming CURRENT = new ModelNaming(
            "Dynawo.Electrical.Transformers.TransformersFixedTap.", "", "i0Pu", "U0PuVar", "running.value",
            "Dynawo.Electrical.Transformers.TransformersFixedTap.", GENERATOR_TRANSFORMER_AUX_INIT);

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
    public static final ModelNaming DYNAWO_1_7_0 = new ModelNaming(
            "Dynawo.Electrical.Transformers.", ".value", "iStator0Pu", "U0Pu", null,
            "Dynawo.Electrical.Transformers.", GENERATOR_TRANSFORMER_AUX_INIT);

    private final String transformerPackage;
    private final String connectorSuffix;
    private final String initCurrent;
    private final String initVoltageMagnitude;
    private final String running;
    private final String transformerInitPackage;
    private final String generatorTransformerAuxInitName;

    /**
     * @param transformerPackage             the package the generator transformer model is in
     * @param connectorSuffix                what a running quantity gains going through a connector
     * @param initCurrent                    the machine's initial current, named this release's way
     * @param initVoltageMagnitude           the machine's initial voltage magnitude
     * @param running                        how the machine says it is running, or null where none
     * @param transformerInitPackage         the package the generator transformer init model is in,
     *                                       which a deployment may take from its own library while
     *                                       the transformer itself stays the open one
     * @param generatorTransformerAuxInitName the init model of a transformer with auxiliaries, which
     *                                       RTE names {@code GeneratorTransformerAuxExt_INIT}
     */
    public ModelNaming(String transformerPackage, String connectorSuffix, String initCurrent,
                       String initVoltageMagnitude, String running, String transformerInitPackage,
                       String generatorTransformerAuxInitName) {
        this.transformerPackage = transformerPackage;
        this.connectorSuffix = connectorSuffix;
        this.initCurrent = initCurrent;
        this.initVoltageMagnitude = initVoltageMagnitude;
        this.running = running;
        this.transformerInitPackage = transformerInitPackage;
        this.generatorTransformerAuxInitName = generatorTransformerAuxInitName;
    }

    String getTransformerPackage() {
        return transformerPackage;
    }

    /**
     * The package the transformer's init model is in, the open package unless a deployment takes it
     * from its own library.
     */
    String getTransformerInitPackage() {
        return transformerInitPackage;
    }

    /**
     * The init model of a transformer carrying auxiliaries, {@code GeneratorTransformerAux_INIT}
     * unless a deployment names it its own way.
     */
    String getGeneratorTransformerAuxInitName() {
        return generatorTransformerAuxInitName;
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
