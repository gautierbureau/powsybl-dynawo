/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.extensions.impl.svarc;

import com.powsybl.dynawo.extensions.api.svarc.StaticVarCompensatorProperties;
import com.powsybl.iidm.network.StaticVarCompensator;
import com.powsybl.iidm.network.impl.AbstractMultiVariantIdentifiableExtension;

import java.util.ArrayList;
import java.util.Objects;

/**
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public class StaticVarCompensatorPropertiesImpl extends AbstractMultiVariantIdentifiableExtension<StaticVarCompensator>
        implements StaticVarCompensatorProperties {

    private final ArrayList<String> constructorByVariant;

    public StaticVarCompensatorPropertiesImpl(StaticVarCompensator svc, String initialConstructor) {
        super(svc);
        Objects.requireNonNull(initialConstructor, "initialConstructor");
        int variantArraySize = getVariantManagerHolder().getVariantManager().getVariantArraySize();
        this.constructorByVariant = new ArrayList<>(variantArraySize);
        for (int i = 0; i < variantArraySize; i++) {
            this.constructorByVariant.add(initialConstructor);
        }
    }

    @Override
    public String getConstructor() {
        return constructorByVariant.get(getVariantIndex());
    }

    @Override
    public void setConstructor(String constructor) {
        this.constructorByVariant.set(getVariantIndex(), Objects.requireNonNull(constructor, "constructor"));
    }

    @Override
    public void extendVariantArraySize(int initVariantArraySize, int number, int sourceIndex) {
        constructorByVariant.ensureCapacity(constructorByVariant.size() + number);
        String source = constructorByVariant.get(sourceIndex);
        for (int i = 0; i < number; i++) {
            constructorByVariant.add(source);
        }
    }

    @Override
    public void reduceVariantArraySize(int number) {
        for (int i = 0; i < number; i++) {
            int last = this.constructorByVariant.size() - 1;
            this.constructorByVariant.remove(last);
        }
    }

    @Override
    public void deleteVariantArrayElement(int index) {
        //Nothing to do
    }

    @Override
    public void allocateVariantArrayElement(int[] indexes, int sourceIndex) {
        String source = constructorByVariant.get(sourceIndex);
        for (int idx : indexes) {
            constructorByVariant.set(idx, source);
        }
    }
}
