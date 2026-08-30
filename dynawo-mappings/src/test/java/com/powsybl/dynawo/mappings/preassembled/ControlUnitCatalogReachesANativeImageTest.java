/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.preassembled;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks that the classes the catalog reads its controls off are kept in a native image.
 * <p>
 * {@link ControlUnitCatalog} asks each declaring class for its methods, which a native image
 * answers with nothing unless the class is registered for reflection. Nothing fails when it does:
 * the catalog comes up empty, no model is ever designed, and every machine is quietly answered
 * with the nearest installed model. A pypowsybl study ran a whole IEEE14 that way, its machines
 * mapped to models without the transformer and auxiliaries their extension asked for, and the
 * reason showed nowhere.
 * <p>
 * This is what the ordinary run cannot see, since reflection works everywhere but there. So the
 * declaration is checked instead: a class the catalog reads has to be named in the configuration
 * the module ships, and one added later without it is caught here rather than in a study.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
class ControlUnitCatalogReachesANativeImageTest {

    private static final String CONFIG =
            "/META-INF/native-image/com.powsybl/powsybl-dynawo-mappings/reflect-config.json";

    @Test
    void shouldRegisterEveryClassTheCatalogReadsControlsFrom() throws Exception {
        String config = read(CONFIG);
        assertThat(config).as("the reflection configuration the module ships").isNotBlank();

        for (Class<?> declaring : new Class<?>[] {GovernorUnits.class, VoltageRegulatorUnits.class,
            RegulatorControlUnits.class}) {
            assertThat(config).as(declaring.getSimpleName() + " is read by reflection")
                    .contains(declaring.getName());
        }
    }

    @Test
    void shouldReadEveryKindOfControl() {
        // and here they are read the ordinary way, so an empty catalog is a failure whichever
        // reason emptied it
        ControlUnitCatalog catalog = ControlUnitCatalog.getInstance();
        assertThat(catalog.getGovernor("GoverProportional")).isPresent();
        assertThat(catalog.getVoltageRegulator("VRProportionalIntegral")).isPresent();
        assertThat(catalog.getRegulatorControl("Pss2b")).isPresent();
    }

    private static String read(String resource) throws Exception {
        try (InputStream is = ControlUnitCatalog.class.getResourceAsStream(resource)) {
            return is == null ? "" : new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
