/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.cgmesdy;

import com.powsybl.dynawo.cgmesdy.load.LoadAggregate;
import com.powsybl.dynawo.cgmesdy.parser.CgmesDyModelLoader;
import com.powsybl.dynawo.cgmesdy.wind.WindGenTurbineType3aIEC;
import com.powsybl.dynawo.cgmesdy.wind.WindPlantIEC;
import com.powsybl.triplestore.api.TripleStore;
import com.powsybl.triplestore.api.TripleStoreFactory;
import org.junit.jupiter.api.*;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for the standard CGMES association role names.
 *
 * <p>The Load and Wind IEC queries additionally accept the standard, declaring-
 * class-qualified PascalCase role names (e.g. {@code LoadDynamics.EnergyConsumer},
 * {@code WindTurbineType3or4IEC.WindProtectionIEC}) alongside the parser's private
 * {@code xxxId} convention. This fixture uses only the standard names, so these
 * tests fail if the tolerant OPTIONAL siblings are removed from the queries.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>
 */
@DisplayName("Standard CGMES association role names (Load / Wind IEC)")
class StandardAssociationRoleNamesTest {

    private static final String FIXTURE = "/com/powsybl/dynawo/cgmesdy/standard_assoc_cim16.xml";
    private static CgmesDyModel model;

    @BeforeAll
    static void loadFixture() throws Exception {
        try (InputStream is = StandardAssociationRoleNamesTest.class.getResourceAsStream(FIXTURE)) {
            assertNotNull(is, "Fixture not found: " + FIXTURE);
            TripleStore ts = TripleStoreFactory.create("rdf4j");
            ts.read(is, CgmesDyConstants.RDF_NS, "urn:test:standard-assoc");
            model = new CgmesDyModelLoader(ts, CgmesDyConstants.CIM16_NS).load();
        }
    }

    @Test
    @DisplayName("LoadAggregate resolves EnergyConsumer (via LoadDynamics), LoadMotor and LoadStatic")
    void loadAggregateStandardRoles() {
        assertEquals(1, model.loadAggregateList().size(), "LoadAggregate count");
        LoadAggregate a = model.loadAggregateList().get(0);
        assertFalse(a.energyConsumerId().isBlank(), "energyConsumerId (LoadDynamics.EnergyConsumer)");
        assertFalse(a.loadMotorId().isBlank(), "loadMotorId (LoadAggregate.LoadMotor)");
        assertFalse(a.loadStaticId().isBlank(), "loadStaticId (LoadAggregate.LoadStatic)");
    }

    @Test
    @DisplayName("WindPlantIEC resolves the freq and reactive control links")
    void windPlantStandardRoles() {
        assertEquals(1, model.windPlantList().size(), "WindPlantIEC count");
        WindPlantIEC w = model.windPlantList().get(0);
        assertFalse(w.windPlantFreqPcontrolIECId().isBlank(), "WindPlantFreqPcontrolIEC");
        assertFalse(w.windPlantReactiveControlIECId().isBlank(), "WindPlantReactiveControlIEC");
    }

    @Test
    @DisplayName("WindGenTurbineType3aIEC resolves inherited roles incl. the WIndContQIEC typo")
    void windType3aStandardRoles() {
        assertEquals(1, model.windType3aList().size(), "WindGenTurbineType3aIEC count");
        WindGenTurbineType3aIEC w = model.windType3aList().get(0);
        assertFalse(w.windContPType3IECId().isBlank(), "WindContPType3IEC");
        assertFalse(w.windContQIECId().isBlank(), "WIndContQIEC (standard typo)");
        assertFalse(w.windMechIECId().isBlank(), "WindMechIEC");
        assertFalse(w.windContCurrLimIECId().isBlank(), "WindContCurrLimIEC");
        assertFalse(w.windProtectionIECId().isBlank(), "WindProtectionIEC");
        // numeric fields still parse alongside the standard-named associations
        assertEquals(0.05, w.kpc(), 1e-9);
        assertEquals(0.2, w.xs(), 1e-9);
    }
}
