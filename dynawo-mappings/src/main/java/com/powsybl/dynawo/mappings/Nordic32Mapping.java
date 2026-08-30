/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings;

import com.powsybl.dynawo.characteristics.Nordic32SystemProvider;
import com.powsybl.iidm.network.Network;

/**
 * The Nordic 32 voltage stability study: the universal DynaWaltz mapping over the machines the
 * system describes by their known controls, with its tap changer blocking added only when a study
 * asks for it.
 * <p>
 * It is the universal mapping in everything but how the machines come to carry their controls: not
 * deduced from an energy source but taken from the system, machine by machine, which is what the
 * Nordic 32 provider writes. The models resolved from those controls and the sets valuing them are
 * the universal mapping's own, so the reference parameters the Nordic models expect are read the
 * way they are for any study.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public class Nordic32Mapping extends UniversalMapping {

    private final boolean withTapChangerBlockings;

    protected Nordic32Mapping(double tsoVoltageMin, boolean withTapChangerBlockings) {
        super(UniversalSynchronousGeneratorMapping.DYNAWALTZ_NAME,
                UniversalSynchronousGeneratorMapping.dynaWaltz(tsoVoltageMin),
                new LoadMapping("DynaWaltz_"));
        this.withTapChangerBlockings = withTapChangerBlockings;
    }

    @Override
    public void createExtensions(Network network) {
        // the controls are the system's, machine by machine, not deduced from an energy source
        SynchronousGeneratorPropertiesProviders.getInstance()
                .createExtensions(network, Nordic32SystemProvider.NAME, MappingParameters.empty());
        // the tap changer blocking is a choice of the study, so it is added only where asked for;
        // where it is, the universal mapping maps it to its automaton as it does any it finds
        if (withTapChangerBlockings) {
            TapChangerBlockingsProviders.getInstance()
                    .createExtensions(network, Nordic32SystemProvider.NAME, MappingParameters.empty());
        }
    }
}
