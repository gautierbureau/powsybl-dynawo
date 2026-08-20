/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com/)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.criteria;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
class CriteriaModelTest {

    @Test
    void aBusVoltageCriteriaHoldsItsBandAndComponents() {
        Criteria busCriteria = Criteria.builder()
                .params(CriteriaParams.builder()
                        .id("hv_voltage")
                        .scope(CriteriaScope.FINAL)
                        .type(CriteriaType.LOCAL_VALUE)
                        .voltageLevel(CriteriaParamsVoltageLevel.builder()
                                .uNomMin(225).uNomMax(400).uMinPu(0.85).uMaxPu(1.15).build())
                        .build())
                .component("B1", "VL1")
                .country("FR")
                .build();

        CriteriaParams params = busCriteria.getParams();
        assertEquals("hv_voltage", params.getId());
        assertEquals(CriteriaScope.FINAL, params.getScope());
        assertEquals(CriteriaType.LOCAL_VALUE, params.getType());
        assertEquals(OptionalDouble.empty(), params.getPMin());
        assertEquals(1, params.getVoltageLevels().size());
        CriteriaParamsVoltageLevel band = params.getVoltageLevels().get(0);
        assertEquals(OptionalDouble.of(0.85), band.getUMinPu());
        assertEquals(OptionalDouble.of(1.15), band.getUMaxPu());
        assertEquals(OptionalDouble.of(225), band.getUNomMin());
        assertEquals(OptionalDouble.of(400), band.getUNomMax());

        assertEquals(1, busCriteria.getComponents().size());
        Criteria.ComponentRef component = busCriteria.getComponents().get(0);
        assertEquals("B1", component.id());
        assertEquals("VL1", component.getVoltageLevelId().orElseThrow());
        assertTrue(busCriteria.hasCountryFilter());
        assertEquals(List.of("FR"), busCriteria.getCountries());
    }

    @Test
    void aGeneratorPowerCriteriaHoldsItsBoundsWithoutVoltageOrComponents() {
        Criteria generatorCriteria = Criteria.builder()
                .params(CriteriaParams.builder()
                        .id("gen_p")
                        .scope(CriteriaScope.DYNAMIC)
                        .type(CriteriaType.SUM)
                        .pMin(-100).pMax(1000)
                        .build())
                .build();

        CriteriaParams params = generatorCriteria.getParams();
        assertEquals(CriteriaScope.DYNAMIC, params.getScope());
        assertEquals(CriteriaType.SUM, params.getType());
        assertEquals(OptionalDouble.of(-100), params.getPMin());
        assertEquals(OptionalDouble.of(1000), params.getPMax());
        assertTrue(params.getVoltageLevels().isEmpty());
        assertTrue(generatorCriteria.getComponents().isEmpty());
        assertFalse(generatorCriteria.hasCountryFilter());
    }

    @Test
    void aCollectionGroupsCriteriaByComponentKind() {
        Criteria bus = Criteria.builder()
                .params(CriteriaParams.builder().id("v").scope(CriteriaScope.FINAL).type(CriteriaType.LOCAL_VALUE).build())
                .build();
        Criteria load = Criteria.builder()
                .params(CriteriaParams.builder().id("load_p").scope(CriteriaScope.FINAL).type(CriteriaType.SUM).pMax(500).build())
                .build();

        CriteriaCollection collection = new CriteriaCollection()
                .add(CriteriaCollection.Type.BUS, bus)
                .add(CriteriaCollection.Type.LOAD, load);

        assertFalse(collection.isEmpty());
        assertEquals(List.of(bus), collection.getCriteria(CriteriaCollection.Type.BUS));
        assertEquals(List.of(load), collection.getCriteria(CriteriaCollection.Type.LOAD));
        assertTrue(collection.getCriteria(CriteriaCollection.Type.GENERATOR).isEmpty());
        assertTrue(new CriteriaCollection().isEmpty());
    }
}
