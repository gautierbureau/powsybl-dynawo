/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.extensions.impl.providers;

import com.google.auto.service.AutoService;
import com.powsybl.commons.extensions.ExtensionAdderProvider;
import com.powsybl.dynawo.extensions.api.svarc.StaticVarCompensatorProperties;
import com.powsybl.dynawo.extensions.impl.svarc.StaticVarCompensatorPropertiesAdderImpl;
import com.powsybl.iidm.network.StaticVarCompensator;

/**
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
@AutoService(ExtensionAdderProvider.class)
public class StaticVarCompensatorPropertiesAdderImplProvider implements
        ExtensionAdderProvider<StaticVarCompensator, StaticVarCompensatorProperties, StaticVarCompensatorPropertiesAdderImpl> {

    @Override
    public String getImplementationName() {
        return "Default";
    }

    @Override
    public String getExtensionName() {
        return StaticVarCompensatorProperties.NAME;
    }

    @Override
    public Class<StaticVarCompensatorPropertiesAdderImpl> getAdderClass() {
        return StaticVarCompensatorPropertiesAdderImpl.class;
    }

    @Override
    public StaticVarCompensatorPropertiesAdderImpl newAdder(StaticVarCompensator svc) {
        return new StaticVarCompensatorPropertiesAdderImpl(svc);
    }
}
