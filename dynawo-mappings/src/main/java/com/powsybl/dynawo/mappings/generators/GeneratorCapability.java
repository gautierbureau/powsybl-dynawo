/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.generators;

import com.powsybl.dynawo.builders.ModelConfig;

import java.util.function.Predicate;

/**
 * An optional feature a synchronous generator model may provide on top of its controls.
 * <p>
 * A capability is detected either from the property declared in the model catalog, which is the
 * reliable way since both the open source and the RTE catalogs use the same vocabulary, or from
 * the model name fragment when the catalog declares no property for it.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public enum GeneratorCapability {

    TRANSFORMER("Tfo", ModelConfig::hasTransformer),
    AUXILIARY("Aux", ModelConfig::hasAuxiliary),
    RPCL("Rpcl", null),
    RPCL2("Rpcl2", null),
    QLIM("Qlim", null),
    UVA("Uva", null);

    private final String nameFragment;
    private final Predicate<ModelConfig> catalogProperty;

    GeneratorCapability(String nameFragment, Predicate<ModelConfig> catalogProperty) {
        this.nameFragment = nameFragment;
        this.catalogProperty = catalogProperty;
    }

    public String getNameFragment() {
        return nameFragment;
    }

    /**
     * Tells whether the given model provides this capability.
     */
    public boolean isProvidedBy(ModelConfig modelConfig) {
        if (catalogProperty != null && catalogProperty.test(modelConfig)) {
            return true;
        }
        return matchesName(modelConfig.lib());
    }

    private boolean matchesName(String lib) {
        return switch (this) {
            // Rpcl is a prefix of Rpcl2, they must not match each other
            case RPCL -> lib.contains(nameFragment) && !lib.contains(RPCL2.nameFragment);
            default -> lib.contains(nameFragment);
        };
    }
}
