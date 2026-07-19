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

    public static final String PROPORTIONAL = "Proportional";
    public static final String PROPORTIONAL_INTEGRAL = "ProportionalIntegral";

    @Override
    public Map<String, String> getGovernorTranslations() {
        // the detailed turbine governors all reduce to a proportional speed regulation, the
        // dynamics they differ by being faster than a voltage stability study resolves
        return Map.of("GovCt2", PROPORTIONAL,
                "GovSteam1", PROPORTIONAL,
                "GovSteamEu", PROPORTIONAL,
                "GovHydro4", PROPORTIONAL,
                "HyGov", PROPORTIONAL,
                "IEEEG1", PROPORTIONAL,
                "TGov1", PROPORTIONAL,
                "TGov3", PROPORTIONAL);
    }

    @Override
    public Map<String, String> getVoltageRegulatorTranslations() {
        // the detailed exciters reduce to a proportional voltage regulation, which is what the
        // IEEE test systems are described with once simplified. A fleet whose exciters are known
        // to hold no steady state voltage error is better served by mapping them to
        // {@link #PROPORTIONAL_INTEGRAL}, which a contribution can do by overriding these entries
        return Map.of("St4b", PROPORTIONAL,
                "St5b", PROPORTIONAL,
                "St6b", PROPORTIONAL,
                "St7b", PROPORTIONAL,
                "IEEX2A", PROPORTIONAL,
                "SCRX", PROPORTIONAL,
                "SEXS", PROPORTIONAL);
    }

    @Override
    public Map<SimplifiedControls, String> getSimplifiedControlsFragments() {
        return Map.of(new SimplifiedControls(PROPORTIONAL, PROPORTIONAL), "ProportionalRegulations",
                new SimplifiedControls(PROPORTIONAL, PROPORTIONAL_INTEGRAL), "GoverPropVRPropInt");
    }
}
