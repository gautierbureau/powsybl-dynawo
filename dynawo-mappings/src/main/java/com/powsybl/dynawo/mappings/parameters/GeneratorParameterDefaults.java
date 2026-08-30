/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.parameters;

import java.util.Collections;
import java.util.Map;

/**
 * Default values used to fill the parameter sets a mapping generates.
 * <p>
 * Contributions are discovered with a {@link java.util.ServiceLoader} and merged by
 * {@link GeneratorParameterDefaultsRegistry}, so that a jar providing additional models also
 * provides the values its controls expect, the way
 * {@link com.powsybl.dynawo.mappings.controls.ControlTranslation} contributes translations.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public interface GeneratorParameterDefaults {

    /**
     * Parameters read from the network rather than valued, whichever voltage level the generator
     * sits on. Maps the Dynawo parameter name to the IIDM name it refers to.
     */
    default Map<String, String> getReferences() {
        return Collections.emptyMap();
    }

    /**
     * Additional references used when the generator is directly connected to a low voltage level.
     */
    default Map<String, String> getLowVoltageReferences() {
        return Collections.emptyMap();
    }

    /**
     * Additional references used when the generator is connected through a transformer.
     */
    default Map<String, String> getHighVoltageReferences() {
        return Collections.emptyMap();
    }

    /**
     * Values shared by every model, keyed by parameter name.
     */
    default Map<String, String> getValues() {
        return Collections.emptyMap();
    }

    /**
     * Values specific to a control, keyed by the control name as it appears in the model name
     * ({@code GovCt2}, {@code St4b}, ...) then by parameter name.
     */
    default Map<String, Map<String, String>> getControlValues() {
        return Collections.emptyMap();
    }
}
