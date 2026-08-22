/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.controls;

import com.google.auto.service.AutoService;

import java.util.Map;

/**
 * Open source translation tables.
 * <p>
 * Only the detailed regulations have a simplified counterpart to translate to. A control that is
 * already simple enough for a voltage stability study is left alone: the Nordic system runs its
 * {@code GoverNordic} governor and {@code VRNordic} regulator in both kinds of study, and a
 * machine whose mechanical power is held constant keeps {@code PmConst}. Hence no wildcard here,
 * an unlisted control translating to itself.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
@AutoService(ControlTranslation.class)
public class DefaultControlTranslation implements ControlTranslation {

    public static final String GOVER_PROPORTIONAL = "GoverProportional";
    public static final String VR_PROPORTIONAL = "VRProportional";
    public static final String VR_PROPORTIONAL_INTEGRAL = "VRProportionalIntegral";

    @Override
    public Map<String, String> getGovernorTranslations() {
        // the detailed turbine governors all reduce to a proportional speed regulation, the
        // dynamics they differ by being faster than a voltage stability study resolves
        return Map.of("GovCt2", GOVER_PROPORTIONAL,
                "GovSteam1", GOVER_PROPORTIONAL,
                "GovSteamEu", GOVER_PROPORTIONAL,
                "GovHydro4", GOVER_PROPORTIONAL,
                "HyGov", GOVER_PROPORTIONAL,
                "IEEEG1", GOVER_PROPORTIONAL,
                "TGov1", GOVER_PROPORTIONAL,
                "TGov3", GOVER_PROPORTIONAL);
    }

    @Override
    public Map<String, String> getVoltageRegulatorTranslations() {
        // a machine carrying one of these exciters holds no steady state voltage error, which a
        // proportional regulation would not reproduce, hence the integral term. The simple
        // exciters, which do leave an error, reduce to a proportional regulation
        return Map.of("St4b", VR_PROPORTIONAL_INTEGRAL,
                "St5b", VR_PROPORTIONAL_INTEGRAL,
                "St6b", VR_PROPORTIONAL_INTEGRAL,
                "St7b", VR_PROPORTIONAL_INTEGRAL,
                "IEEX2A", VR_PROPORTIONAL_INTEGRAL,
                "SCRX", VR_PROPORTIONAL,
                "SEXS", VR_PROPORTIONAL);
    }

}
