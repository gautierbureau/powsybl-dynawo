/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.extensions.impl.svarc;

import com.powsybl.commons.extensions.AbstractExtensionAdder;
import com.powsybl.dynawo.extensions.api.svarc.StaticVarCompensatorProperties;
import com.powsybl.dynawo.extensions.api.svarc.StaticVarCompensatorPropertiesAdder;
import com.powsybl.iidm.network.StaticVarCompensator;

/**
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public class StaticVarCompensatorPropertiesAdderImpl
        extends AbstractExtensionAdder<StaticVarCompensator, StaticVarCompensatorProperties>
        implements StaticVarCompensatorPropertiesAdder {

    private String constructor;

    public StaticVarCompensatorPropertiesAdderImpl(StaticVarCompensator svc) {
        super(svc);
    }

    @Override
    protected StaticVarCompensatorProperties createExtension(StaticVarCompensator extendable) {
        return new StaticVarCompensatorPropertiesImpl(extendable, constructor);
    }

    @Override
    public StaticVarCompensatorPropertiesAdderImpl withConstructor(String constructor) {
        this.constructor = constructor;
        return this;
    }
}
