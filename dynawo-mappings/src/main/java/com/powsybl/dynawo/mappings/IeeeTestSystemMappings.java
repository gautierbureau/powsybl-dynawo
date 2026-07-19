/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings;

import com.powsybl.dynawo.characteristics.IidmSynchronousGeneratorPropertiesProvider;
import com.powsybl.dynawo.mappings.controls.ControlTranslations;
import com.powsybl.dynawo.mappings.controls.DefaultControlTranslation;
import com.powsybl.dynawo.mappings.controls.ProportionalExciterTranslation;
import com.powsybl.dynawo.mappings.generators.GeneratorLibResolver;

/**
 * The mapping of the IEEE test systems.
 * <p>
 * They are described by the fully proportional regulations, so their exciters simplify to a
 * proportional voltage regulation rather than keeping the integral term a real machine needs. That
 * is the whole difference with {@link UniversalSynchronousGeneratorMapping}, and it is a property
 * of those systems rather than of the network they are applied to: the same mapping produces the
 * models Dynawo ships for IEEE14 and for IEEE57, what differs between the two being their
 * generation mix, which the network carries.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public final class IeeeTestSystemMappings {

    public static final String DYNAWALTZ_NAME = "IeeeDynaWaltz";
    public static final String DYNASWING_NAME = "IeeeDynaSwing";

    private IeeeTestSystemMappings() {
    }

    public static UniversalSynchronousGeneratorMapping dynaWaltz() {
        return create(DYNAWALTZ_NAME, true);
    }

    public static UniversalSynchronousGeneratorMapping dynaSwing() {
        return create(DYNASWING_NAME, false);
    }

    private static UniversalSynchronousGeneratorMapping create(String name, boolean simplified) {
        double tsoVoltageMin = IidmSynchronousGeneratorPropertiesProvider.DEFAULT_TSO_VOLTAGE_MIN;
        GeneratorLibResolver resolver = new GeneratorLibResolver(
                ControlTranslations.of(new ProportionalExciterTranslation(), new DefaultControlTranslation()));
        return new UniversalSynchronousGeneratorMapping(name, simplified, tsoVoltageMin,
                new IidmSynchronousGeneratorPropertiesProvider(tsoVoltageMin), resolver);
    }
}
