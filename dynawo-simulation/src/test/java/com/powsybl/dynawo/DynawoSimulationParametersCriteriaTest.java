/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com/)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo;

import com.powsybl.dynawo.criteria.Criteria;
import com.powsybl.dynawo.criteria.CriteriaCollection;
import com.powsybl.dynawo.criteria.CriteriaParams;
import com.powsybl.dynawo.criteria.CriteriaScope;
import com.powsybl.dynawo.criteria.CriteriaType;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The criteria a simulation checks can be given either as a raw file or as a typed model; a model, when
 * set, is written into the working dir under a fixed name and takes precedence over a raw file path.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
class DynawoSimulationParametersCriteriaTest {

    private static CriteriaCollection aCollection() {
        return new CriteriaCollection().add(CriteriaCollection.Type.BUS, Criteria.builder()
                .params(CriteriaParams.builder().id("v").scope(CriteriaScope.FINAL).type(CriteriaType.LOCAL_VALUE).build())
                .build());
    }

    @Test
    void aRawFilePathKeepsItsOwnName() {
        DynawoSimulationParameters parameters = new DynawoSimulationParameters()
                .setCriteriaFilePath(Path.of("some", "myCriteria.crt"));
        assertTrue(parameters.getCriteria().isEmpty());
        assertEquals(Optional.of("myCriteria.crt"), parameters.getCriteriaFileName());
    }

    @Test
    void aModelTakesTheFixedNameAndWinsOverARawFile() {
        CriteriaCollection collection = aCollection();
        DynawoSimulationParameters parameters = new DynawoSimulationParameters()
                .setCriteriaFilePath(Path.of("some", "myCriteria.crt"))
                .setCriteria(collection);

        assertEquals(Optional.of(collection), parameters.getCriteria());
        // a set model wins over a raw file, so the jobs reference the fixed name the model is written under
        assertEquals(Optional.of(DynawoSimulationConstants.CRITERIA_FILENAME), parameters.getCriteriaFileName());
    }

    @Test
    void noCriteriaMeansNoFileName() {
        DynawoSimulationParameters parameters = new DynawoSimulationParameters();
        assertTrue(parameters.getCriteria().isEmpty());
        assertTrue(parameters.getCriteriaFileName().isEmpty());
    }
}
