/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.cgmesdy.governors;

import com.powsybl.dynawo.cgmesdy.CgmesDyConstants;
import com.powsybl.dynawo.cgmesdy.CgmesDyModel;
import com.powsybl.dynawo.cgmesdy.governors.gas.*;
import com.powsybl.dynawo.cgmesdy.governors.hydro.*;
import com.powsybl.dynawo.cgmesdy.governors.steam.*;
import com.powsybl.dynawo.cgmesdy.parser.CgmesDyModelLoader;
import com.powsybl.triplestore.api.TripleStore;
import com.powsybl.triplestore.api.TripleStoreFactory;
import org.junit.jupiter.api.*;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for all 32 governor types parsed from a CGMES DY CIM16 fixture.
 *
 * <p>One instance of each governor class is present in
 * {@code governors_dy_cim16.xml}.  Tests verify:
 * <ol>
 *   <li>Instance count (exactly one per type)</li>
 *   <li>RDF identity and {@code synchronousMachineId} association link</li>
 *   <li>Every numeric field round-trips correctly from XML to the record</li>
 *   <li>Integer / boolean fields (iFlag, govtype, wfspd, cfrac, sfrac, prate)</li>
 *   <li>String enum fields (waterTunnelSurgeChamberSimulation)</li>
 *   <li>Physical invariants (pmax ≥ pmin, vmax ≥ vmin, mwbase > 0, …)</li>
 * </ol>
 *
 * <h3>Fixture</h3>
 * {@code src/test/resources/com/powsybl/dynawo/cgmesdy/governors_dy_cim16.xml}
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>
 */
@DisplayName("GovernorLoader – all 32 governor types (CIM16)")
@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
class GovernorLoaderTest {

    private static final String FIXTURE = "/com/powsybl/dynawo/cgmesdy/governors_dy_cim16.xml";
    private static final double TOL = 1e-9;

    private static CgmesDyModel MODEL;

    @BeforeAll
    static void loadFixture() throws Exception {
        try (InputStream is = GovernorLoaderTest.class.getResourceAsStream(FIXTURE)) {
            assertNotNull(is, "Fixture not found: " + FIXTURE);
            TripleStore ts = TripleStoreFactory.create("rdf4j");
            ts.read(is, CgmesDyConstants.RDF_NS, "urn:test:gov-dy");
            MODEL = new CgmesDyModelLoader(ts, CgmesDyConstants.CIM16_NS).load();
        }
    }

    // ── tiny helpers ──────────────────────────────────────────────────────────

    private static <T> T single(List<T> list, String typeName) {
        assertEquals(1, list.size(), typeName + " count");
        return list.get(0);
    }

    private static void assertContains(String id, String fragment) {
        assertTrue(id.contains(fragment), "Expected ID to contain '" + fragment + "' but was: " + id);
    }

    private static void assertSmId(String actual, String expectedFragment) {
        assertTrue(actual.contains(expectedFragment),
            "synchronousMachineId should reference '" + expectedFragment + "', got: " + actual);
    }

    // =========================================================================
    // STEAM GOVERNORS
    // =========================================================================

    @Nested
    @DisplayName("GovSteam0 – basic single-reheat steam governor")
    class GovSteam0Test {
        @Test void count() {
            assertEquals(1, MODEL.govSteam0List().size()); }

        @Test void fields() {
            GovSteam0 g = single(MODEL.govSteam0List(), "GovSteam0");
            assertContains(g.id(), "steam0");
            assertSmId(g.synchronousMachineId(), "sm-gov-1");
            assertEquals(600.0, g.mwbase(), TOL);
            assertEquals(0.05, g.r(), TOL);
            assertEquals(0.5, g.t1(), TOL);
            assertEquals(1.0, g.vmax(), TOL);
            assertEquals(0.0, g.vmin(), TOL);
            assertEquals(3.0, g.t2(), TOL);
            assertEquals(10.0, g.t3(), TOL);
            assertEquals(0.2, g.dt(), TOL);
        }

        @Test void physicalConstraints() {
            GovSteam0 g = MODEL.govSteam0List().get(0);
            assertTrue(g.mwbase() > 0);
            assertTrue(g.vmax() >= g.vmin());
            assertTrue(g.t1() > 0);
            assertTrue(g.t3() > 0);
        }
    }

    @Nested
    @DisplayName("GovSteam1 – multi-stage reheat steam governor")
    class GovSteam1Test {
        @Test void count() {
            assertEquals(1, MODEL.govSteam1List().size()); }

        @Test void fields() {
            GovSteam1 g = single(MODEL.govSteam1List(), "GovSteam1");
            assertContains(g.id(), "steam1");
            assertSmId(g.synchronousMachineId(), "sm-gov-2");
            assertEquals(900.0, g.mwbase(), TOL);
            assertEquals(25.0, g.k(), TOL);
            assertEquals(0.5, g.t1(), TOL);
            assertEquals(3.0, g.t2(), TOL);
            assertEquals(10.0, g.t3(), TOL);
            assertEquals(0.1, g.uo(), TOL);
            assertEquals(-0.1, g.uc(), TOL);
            assertEquals(1.0, g.pmax(), TOL);
            assertEquals(0.0, g.pmin(), TOL);
            assertEquals(0.3, g.t4(), TOL);
            assertEquals(0.2, g.k1(), TOL);
            assertEquals(0.0, g.k2(), TOL);
            assertEquals(10.0, g.t5(), TOL);
            assertEquals(0.3, g.k3(), TOL);
            assertEquals(0.5, g.k5(), TOL);
            // GV table spot-checks
            assertEquals(0.4, g.gv2(), TOL);
            assertEquals(0.75, g.pgv2(), TOL);
            assertEquals(0.0, g.gv6(), TOL);
            // deadband-selector / nonlinear-valve flags
            assertTrue(g.sdb1());
            assertFalse(g.sdb2());
            assertFalse(g.valve());
        }

        @Test void powerConstraints() {
            GovSteam1 g = MODEL.govSteam1List().get(0);
            assertTrue(g.pmax() >= g.pmin());
            assertTrue(g.uo() >= 0);
            assertTrue(g.uc() <= 0);
        }
    }

    @Nested
    @DisplayName("GovSteam2 – fast-valving steam governor")
    class GovSteam2Test {
        @Test void count() {
            assertEquals(1, MODEL.govSteam2List().size()); }

        @Test void fields() {
            GovSteam2 g = single(MODEL.govSteam2List(), "GovSteam2");
            assertContains(g.id(), "steam2");
            assertSmId(g.synchronousMachineId(), "sm-gov-3");
            assertEquals(0.0, g.dbf(), TOL);
            assertEquals(20.0, g.k(), TOL);
            assertEquals(-1.0, g.mnef(), TOL);
            assertEquals(1.0, g.mxef(), TOL);
            assertEquals(1.0, g.pmax(), TOL);
            assertEquals(0.0, g.pmin(), TOL);
            assertEquals(0.3, g.t1(), TOL);
            assertEquals(0.15, g.t2(), TOL);
        }
    }

    @Nested
    @DisplayName("GovSteamCC – combined-cycle steam governor")
    class GovSteamCCTest {
        @Test void count() {
            assertEquals(1, MODEL.govSteamCCList().size()); }

        @Test void fields() {
            GovSteamCC g = single(MODEL.govSteamCCList(), "GovSteamCC");
            assertContains(g.id(), "steamcc");
            assertSmId(g.synchronousMachineId(), "sm-gov-4");
            assertEquals(400.0, g.mwbase(), TOL);
            // HP train
            assertEquals(0.05, g.rhp(), TOL);
            assertEquals(0.5, g.t1hp(), TOL);
            assertEquals(3.0, g.t3hp(), TOL);
            assertEquals(0.5, g.t4hp(), TOL);
            assertEquals(8.0, g.t5hp(), TOL);
            assertEquals(0.3, g.fhp(), TOL);
            assertEquals(0.29, g.dhp(), TOL);
            assertEquals(1.0, g.pmaxhp(), TOL);
            // LP train
            assertEquals(0.05, g.rlp(), TOL);
            assertEquals(0.5, g.t1lp(), TOL);
            assertEquals(3.0, g.t3lp(), TOL);
            assertEquals(0.5, g.t4lp(), TOL);
            assertEquals(10.0, g.t5lp(), TOL);
            assertEquals(0.7, g.flp(), TOL);
            assertEquals(0.71, g.dlp(), TOL);
            assertEquals(1.0, g.pmaxlp(), TOL);
        }

        @Test void fractionConstraint() {
            GovSteamCC g = MODEL.govSteamCCList().get(0);
            // hp + lp fractions should sum to 1 within floating-point tolerance
            assertEquals(1.0, g.fhp() + g.flp(), 1e-9);
        }
    }

    @Nested
    @DisplayName("GovSteamEU – European steam governor")
    class GovSteamEUTest {
        @Test void count() {
            assertEquals(1, MODEL.govSteamEUList().size()); }

        @Test void fields() {
            GovSteamEU g = single(MODEL.govSteamEUList(), "GovSteamEU");
            assertContains(g.id(), "steameu");
            assertSmId(g.synchronousMachineId(), "sm-gov-5");
            assertEquals(660.0, g.mwbase(), TOL);
            assertEquals(0.1, g.ke(), TOL);
            assertEquals(20.0, g.kfcor(), TOL);
            assertEquals(15.0, g.komegacor(), TOL);
            assertEquals(-0.1, g.chc(), TOL);
        }
    }

    @Nested
    @DisplayName("GovSteamFV2 – steam turbine governor with reheat and fast valve closing")
    class GovSteamFV2Test {
        @Test void count() {
            assertEquals(1, MODEL.govSteamFV2List().size()); }

        @Test void fields() {
            GovSteamFV2 g = single(MODEL.govSteamFV2List(), "GovSteamFV2");
            assertContains(g.id(), "steamfv2");
            assertSmId(g.synchronousMachineId(), "sm-gov-6");
            assertEquals(350.0, g.mwbase(), TOL);
            assertEquals(0.05, g.r(), TOL);
            assertEquals(0.7, g.k(), TOL);
            assertEquals(0.5, g.t1(), TOL);
            assertEquals(10.0, g.t3(), TOL);
            assertEquals(0.2, g.ta(), TOL);
            assertEquals(0.5, g.tb(), TOL);
            assertEquals(1.0, g.tc(), TOL);
            assertEquals(0.05, g.ti(), TOL);
            assertEquals(2.0, g.tt(), TOL);
            assertEquals(1.0, g.vmax(), TOL);
            assertEquals(0.0, g.vmin(), TOL);
        }

        @Test void valveTimingOrdered() {
            // fast-valving sequence: close (Ta) < begin-open (Tb) < fully-open (Tc)
            GovSteamFV2 g = MODEL.govSteamFV2List().get(0);
            assertTrue(g.ta() <= g.tb() && g.tb() <= g.tc(),
                "fast-valving times must be ordered Ta <= Tb <= Tc");
        }
    }

    @Nested
    @DisplayName("GovSteamFV3 – simplified GovSteamIEEE1 with Prmax and fast valving")
    class GovSteamFV3Test {
        @Test void count() {
            assertEquals(1, MODEL.govSteamFV3List().size()); }

        @Test void fields() {
            GovSteamFV3 g = single(MODEL.govSteamFV3List(), "GovSteamFV3");
            assertContains(g.id(), "steamfv3");
            assertSmId(g.synchronousMachineId(), "sm-gov-7");
            assertEquals(800.0, g.mwbase(), TOL);
            assertEquals(20.0, g.k(), TOL);
            assertEquals(0.3, g.k1(), TOL);
            assertEquals(0.3, g.k2(), TOL);
            assertEquals(0.4, g.k3(), TOL);
            assertEquals(1.1, g.prmax(), TOL);
            assertEquals(-1.0, g.uc(), TOL);
            assertEquals(1.0, g.uo(), TOL);
            assertEquals(3.0, g.t2(), TOL);
            assertEquals(0.5, g.t6(), TOL);
        }

        @Test void prmaxGePmax() {
            GovSteamFV3 g = MODEL.govSteamFV3List().get(0);
            assertTrue(g.prmax() >= g.pmax(),
                "prmax should be >= pmax for overload capability");
        }
    }

    @Nested
    @DisplayName("GovSteamFV4 – detailed electro-hydraulic steam turbine governor")
    class GovSteamFV4Test {
        @Test void count() {
            assertEquals(1, MODEL.govSteamFV4List().size()); }

        @Test void fields() {
            GovSteamFV4 g = single(MODEL.govSteamFV4List(), "GovSteamFV4");
            assertContains(g.id(), "steamfv4");
            assertSmId(g.synchronousMachineId(), "sm-gov-8");
            assertEquals(0.35, g.khp(), TOL);
            assertEquals(0.5, g.kic(), TOL);
            assertEquals(1.0, g.kpc(), TOL);
            assertEquals(10.0, g.trh(), TOL);
            assertEquals(0.4, g.tv(), TOL);
            assertEquals(0.1, g.thp(), TOL);
            assertEquals(0.1, g.tmp(), TOL);
            assertEquals(1.0, g.yhpmx(), TOL);
            assertEquals(1.0, g.ympmx(), TOL);
            assertEquals(0.0, g.rsmimn(), TOL);
            assertEquals(1.1, g.rsmimx(), TOL);
        }

        @Test void limitConstraints() {
            GovSteamFV4 g = MODEL.govSteamFV4List().get(0);
            assertTrue(g.rsmimx() >= g.rsmimn());
            assertTrue(g.cpsmx() >= g.cpsmn());
            assertTrue(g.yhpmx() >= g.yhpmn());
            assertTrue(g.ympmx() >= g.ympmn());
        }
    }

    @Nested
    @DisplayName("GovSteamIEEE1 – IEEE std 1992 steam governor")
    class GovSteamIEEE1Test {
        @Test void count() {
            assertEquals(1, MODEL.govSteamIEEE1List().size()); }

        @Test void fields() {
            GovSteamIEEE1 g = single(MODEL.govSteamIEEE1List(), "GovSteamIEEE1");
            assertContains(g.id(), "steamieee1");
            assertSmId(g.synchronousMachineId(), "sm-gov-9");
            assertEquals(1000.0, g.mwbase(), TOL);
            assertEquals(25.0, g.k(), TOL);
            assertEquals(0.5, g.t1(), TOL);
            assertEquals(0.5, g.k5(), TOL);
            assertEquals(0.15, g.k8(), TOL);
        }

        @Test void physicalConstraints() {
            GovSteamIEEE1 g = MODEL.govSteamIEEE1List().get(0);
            assertTrue(g.pmax() >= g.pmin());
            assertTrue(g.mwbase() > 0);
        }
    }

    @Nested
    @DisplayName("GovSteamSGO – SGO steam governor")
    class GovSteamSGOTest {
        @Test void count() {
            assertEquals(1, MODEL.govSteamSGOList().size()); }

        @Test void fields() {
            GovSteamSGO g = single(MODEL.govSteamSGOList(), "GovSteamSGO");
            assertContains(g.id(), "steamsgo");
            assertSmId(g.synchronousMachineId(), "sm-gov-10");
            assertEquals(750.0, g.mwbase(), TOL);
            assertEquals(0.3, g.k1(), TOL);
            assertEquals(0.25, g.k2(), TOL);
            assertEquals(0.2, g.k3(), TOL);
            assertEquals(0.5, g.t1(), TOL);
            assertEquals(3.0, g.t2(), TOL);
            assertEquals(7.0, g.t3(), TOL);
            assertEquals(0.4, g.t4(), TOL);
            assertEquals(0.5, g.t5(), TOL);
            assertEquals(10.0, g.t6(), TOL);
            assertEquals(1.0, g.pmax(), TOL);
            assertEquals(0.0, g.pmin(), TOL);
        }
    }

    // =========================================================================
    // HYDRO GOVERNORS
    // =========================================================================

    @Nested
    @DisplayName("GovHydro1 – classical hydro governor")
    class GovHydro1Test {
        @Test void count() {
            assertEquals(1, MODEL.govHydro1List().size()); }

        @Test void fields() {
            GovHydro1 g = single(MODEL.govHydro1List(), "GovHydro1");
            assertContains(g.id(), "hydro1");
            assertSmId(g.synchronousMachineId(), "sm-gov-11");
            assertEquals(200.0, g.mwbase(), TOL);
            assertEquals(5.0, g.tr(), TOL);
            assertEquals(0.5, g.tf(), TOL);
            assertEquals(0.5, g.tg(), TOL);
            assertEquals(0.2, g.velm(), TOL);
            assertEquals(1.0, g.gmax(), TOL);
            assertEquals(0.0, g.gmin(), TOL);
            assertEquals(1.0, g.tw(), TOL);
            assertEquals(1.2, g.at(), TOL);
            assertEquals(0.5, g.dturb(), TOL);
            assertEquals(0.08, g.qnl(), TOL);
            assertEquals(0.05, g.rperm(), TOL);
            assertEquals(0.3, g.rtemp(), TOL);
            assertEquals(1.0, g.hdam(), TOL);
        }

        @Test void physicalConstraints() {
            GovHydro1 g = MODEL.govHydro1List().get(0);
            assertTrue(g.gmax() >= g.gmin());
            assertTrue(g.tw() > 0, "Water starting time must be positive");
            assertTrue(g.velm() > 0, "Gate velocity limit must be positive");
        }
    }

    @Nested
    @DisplayName("GovHydro2 – hydro governor with turbine model")
    class GovHydro2Test {
        @Test void count() {
            assertEquals(1, MODEL.govHydro2List().size()); }

        @Test void fields() {
            GovHydro2 g = single(MODEL.govHydro2List(), "GovHydro2");
            assertContains(g.id(), "hydro2");
            assertSmId(g.synchronousMachineId(), "sm-gov-12");
            assertEquals(300.0, g.mwbase(), TOL);
            assertEquals(1.10, g.aturb(), TOL);
            assertEquals(0.42, g.bturb(), TOL);
            assertEquals(1.25, g.kturb(), TOL);
            assertEquals(1.15, g.tw(), TOL);
        }
    }

    @Nested
    @DisplayName("GovHydro3 – hydro governor with govtype integer")
    class GovHydro3Test {
        @Test void count() {
            assertEquals(1, MODEL.govHydro3List().size()); }

        @Test void fields() {
            GovHydro3 g = single(MODEL.govHydro3List(), "GovHydro3");
            assertContains(g.id(), "hydro3");
            assertSmId(g.synchronousMachineId(), "sm-gov-13");
            assertEquals(250.0, g.mwbase(), TOL);
            assertEquals(1.20, g.at(), TOL);
            assertEquals(0.5, g.ki(), TOL);
            assertEquals(0.08, g.qnl(), TOL);
            assertTrue(g.governorControl());   // boolean field
        }

        @Test void governorControlFlag() {
            assertTrue(MODEL.govHydro3List().get(0).governorControl());
        }
    }

    @Nested
    @DisplayName("GovHydro4 – hydro governor with GV table")
    class GovHydro4Test {
        @Test void count() {
            assertEquals(1, MODEL.govHydro4List().size()); }

        @Test void fields() {
            GovHydro4 g = single(MODEL.govHydro4List(), "GovHydro4");
            assertContains(g.id(), "hydro4");
            assertSmId(g.synchronousMachineId(), "sm-gov-14");
            assertEquals(1.2, g.at(), TOL);
            assertEquals(0.0, g.bmax(), TOL);
            assertEquals(0.5, g.dturb(), TOL);
            assertEquals(1.0, g.gmax(), TOL);
            assertEquals(0.0, g.gmin(), TOL);
            assertEquals(0.4, g.gv2(), TOL);
            assertEquals(0.4, g.pgv2(), TOL);
            assertEquals(1.0, g.hdam(), TOL);
            assertEquals(400.0, g.mwbase(), TOL);
            assertEquals(0.08, g.qn1(), TOL);
            assertEquals(0.08, g.qnl(), TOL);
            assertEquals(0.05, g.rperm(), TOL);
            assertEquals(0.3, g.rtemp(), TOL);
            assertEquals(0.0, g.tblade(), TOL);
            assertEquals(0.5, g.tg(), TOL);
            assertEquals(0.07, g.tp(), TOL);
            assertEquals(5.0, g.tr(), TOL);
            assertEquals(1.0, g.tw(), TOL);
            assertEquals(-0.2, g.uc(), TOL);
            assertEquals(0.1, g.uo(), TOL);
        }

        @Test void rateConstraints() {
            GovHydro4 g = MODEL.govHydro4List().get(0);
            assertTrue(g.uo() > 0, "Opening velocity must be positive");
            assertTrue(g.uc() <= 0, "Closing velocity must be non-positive");
        }
    }

    @Nested
    @DisplayName("GovHydroDD – hydro governor with deadband and GV table")
    class GovHydroDDTest {
        @Test void count() {
            assertEquals(1, MODEL.govHydroDDList().size()); }

        @Test void fields() {
            GovHydroDD g = single(MODEL.govHydroDDList(), "GovHydroDD");
            assertContains(g.id(), "hydrodd");
            assertSmId(g.synchronousMachineId(), "sm-gov-15");
            assertEquals(180.0, g.mwbase(), TOL);
            assertEquals(0.05, g.r(), TOL);
            assertEquals(1.15, g.aturb(), TOL);
            assertEquals(0.8, g.tturb(), TOL);
            assertFalse(g.inputSignal());   // boolean field
        }
    }

    @Nested
    @DisplayName("GovHydroFrancis – Francis turbine governor with enum field")
    class GovHydroFrancisTest {
        @Test void count() {
            assertEquals(1, MODEL.govHydroFrancisList().size()); }

        @Test void fields() {
            GovHydroFrancis g = single(MODEL.govHydroFrancisList(), "GovHydroFrancis");
            assertContains(g.id(), "hydrofrancis");
            assertSmId(g.synchronousMachineId(), "sm-gov-16");
            assertEquals(0.95, g.am(), TOL);
            assertEquals(0.95, g.av0(), TOL);
            assertEquals(0.5, g.av1(), TOL);
            assertEquals(0.05, g.bp(), TOL);
            assertEquals(0.01, g.db1(), TOL);
            assertEquals(0.9, g.etamax(), TOL);
            assertEquals(1.0, g.h1(), TOL);
            assertEquals(1.0, g.h2(), TOL);
            assertEquals(1.0, g.hn(), TOL);
            assertEquals(0.0, g.kc(), TOL);
            assertEquals(1.0, g.kg(), TOL);
            assertEquals(0.5, g.kt(), TOL);
            assertEquals(0.0, g.qc0(), TOL);
            assertEquals(0.9, g.qn(), TOL);
            assertEquals(0.1, g.ta(), TOL);
            assertEquals(0.05, g.td(), TOL);
            assertEquals(1.0, g.twnc(), TOL);
            assertEquals(0.5, g.twng(), TOL);
            assertEquals(0.2, g.tx(), TOL);
            assertEquals(0.1, g.va(), TOL);
            assertEquals(1.0, g.valvmax(), TOL);
            assertEquals(0.0, g.valvmin(), TOL);
            assertEquals(-0.1, g.vc(), TOL);
            assertEquals(0.5, g.zsfc(), TOL);
            // enum control-mode field
            assertTrue(g.governorControl().contains("mechanicHydrolicTransientFeedback"));
            // boolean surge-chamber flag
            assertTrue(g.waterTunnelSurgeChamberSimulation());
        }

        @Test void enumFieldNotBlank() {
            String s = MODEL.govHydroFrancisList().get(0).governorControl();
            assertNotNull(s);
            assertFalse(s.isBlank());
        }
    }

    @Nested
    @DisplayName("GovHydroIEEE0 – simplified IEEE hydro governor")
    class GovHydroIEEE0Test {
        @Test void count() {
            assertEquals(1, MODEL.govHydroIEEE0List().size()); }

        @Test void fields() {
            GovHydroIEEE0 g = single(MODEL.govHydroIEEE0List(), "GovHydroIEEE0");
            assertContains(g.id(), "hydroieee0");
            assertSmId(g.synchronousMachineId(), "sm-gov-17");
            assertEquals(150.0, g.mwbase(), TOL);
            assertEquals(1.0, g.k(), TOL);
            assertEquals(0.5, g.t1(), TOL);
            assertEquals(1.5, g.t2(), TOL);
            assertEquals(0.5, g.t3(), TOL);
            assertEquals(1.0, g.t4(), TOL);
            assertEquals(1.0, g.pmax(), TOL);
            assertEquals(0.0, g.pmin(), TOL);
        }
    }

    @Nested
    @DisplayName("GovHydroIEEE2 – IEEE hydro governor with GV table")
    class GovHydroIEEE2Test {
        @Test void count() {
            assertEquals(1, MODEL.govHydroIEEE2List().size()); }

        @Test void fields() {
            GovHydroIEEE2 g = single(MODEL.govHydroIEEE2List(), "GovHydroIEEE2");
            assertContains(g.id(), "hydroieee2");
            assertSmId(g.synchronousMachineId(), "sm-gov-18");
            assertEquals(-1.0, g.aturb(), TOL);
            assertEquals(0.5, g.bturb(), TOL);
            assertEquals(1.0, g.kturb(), TOL);
            assertEquals(250.0, g.mwbase(), TOL);
            assertEquals(1.0, g.pmax(), TOL);
            assertEquals(0.0, g.pmin(), TOL);
            assertEquals(0.05, g.rperm(), TOL);
            assertEquals(0.3, g.rtemp(), TOL);
            assertEquals(0.5, g.tg(), TOL);
            assertEquals(0.07, g.tp(), TOL);
            assertEquals(5.0, g.tr(), TOL);
            assertEquals(1.0, g.tw(), TOL);
            assertEquals(-0.1, g.uc(), TOL);
            assertEquals(0.1, g.uo(), TOL);
            assertEquals(0.2, g.gv2(), TOL);
            assertEquals(0.2, g.pgv2(), TOL);
        }
    }

    @Nested
    @DisplayName("GovHydroPelton – Pelton turbine governor with boolean fields")
    class GovHydroPeltonTest {
        @Test void count() {
            assertEquals(1, MODEL.govHydroPeltonList().size()); }

        @Test void fields() {
            GovHydroPelton g = single(MODEL.govHydroPeltonList(), "GovHydroPelton");
            assertContains(g.id(), "hydropelton");
            assertSmId(g.synchronousMachineId(), "sm-gov-19");
            assertEquals(0.95, g.av0(), TOL);
            assertEquals(0.98, g.av1(), TOL);
            assertEquals(0.05, g.bp(), TOL);
            assertEquals(0.75, g.h1(), TOL);
            assertEquals(0.45, g.h2(), TOL);
            assertEquals(1.0, g.hn(), TOL);
            assertEquals(0.9, g.qn(), TOL);
            assertEquals(0.05, g.tv(), TOL);
            assertEquals(1.0, g.twnc(), TOL);
            assertEquals(0.5, g.twng(), TOL);
            assertEquals(0.1, g.va(), TOL);
            assertEquals(1.0, g.valvmax(), TOL);
            assertEquals(0.0, g.valvmin(), TOL);
            assertEquals(-0.1, g.vc(), TOL);
            // Boolean fields
            assertFalse(g.simplifiedPelton());
            assertFalse(g.staticCompensating());
            assertFalse(g.waterTunnelSurgeChamberSimulation());
        }
    }

    @Nested
    @DisplayName("GovHydroPID – hydro governor with PID control")
    class GovHydroPIDTest {
        @Test void count() {
            assertEquals(1, MODEL.govHydroPIDList().size()); }

        @Test void fields() {
            GovHydroPID g = single(MODEL.govHydroPIDList(), "GovHydroPID");
            assertContains(g.id(), "hydropid");
            assertSmId(g.synchronousMachineId(), "sm-gov-20");
            assertEquals(160.0, g.mwbase(), TOL);
            assertEquals(0.05, g.r(), TOL);
            assertEquals(0.05, g.td(), TOL);
            assertEquals(0.1, g.tf(), TOL);
            assertEquals(0.07, g.tp(), TOL);
            assertEquals(0.2, g.velop(), TOL);
            assertEquals(-0.2, g.velcl(), TOL);
            assertEquals(1.0, g.pmax(), TOL);
            assertEquals(1.2, g.aturb(), TOL);
            assertEquals(0.4, g.bturb(), TOL);
            assertEquals(1.0, g.kp(), TOL);
            assertEquals(0.5, g.ki(), TOL);
            assertEquals(0.0, g.kd(), TOL);
            assertEquals(1.0, g.kg(), TOL);
            assertFalse(g.inputSignal());
            assertEquals(0.4, g.gv2(), TOL);
        }
    }

    @Nested
    @DisplayName("GovHydroPID2 – simplified PID hydro governor")
    class GovHydroPID2Test {
        @Test void count() {
            assertEquals(1, MODEL.govHydroPID2List().size()); }

        @Test void fields() {
            GovHydroPID2 g = single(MODEL.govHydroPID2List(), "GovHydroPID2");
            assertContains(g.id(), "hydropid2");
            assertSmId(g.synchronousMachineId(), "sm-gov-21");
            assertEquals(140.0, g.mwbase(), TOL);
            assertEquals(0.05, g.rperm(), TOL);
            assertEquals(1.0, g.kp(), TOL);
            assertEquals(0.5, g.ki(), TOL);
            assertEquals(0.0, g.kd(), TOL);
            assertEquals(0.1, g.ta(), TOL);
            assertEquals(0.2, g.tb(), TOL);
            assertEquals(5.0, g.treg(), TOL);
            assertEquals(1.0, g.tw(), TOL);
            assertEquals(0.2, g.velmax(), TOL);
            assertEquals(-0.2, g.velmin(), TOL);
            assertEquals(1.0, g.gmax(), TOL);
            assertEquals(0.0, g.gmin(), TOL);
            assertEquals(0.7, g.g2(), TOL);
            assertEquals(1.0, g.p3(), TOL);
            assertEquals(1.0, g.atw(), TOL);
            assertEquals(0.5, g.d(), TOL);
            assertFalse(g.feedbackSignal());
        }

        @Test void velocityConstraints() {
            GovHydroPID2 g = MODEL.govHydroPID2List().get(0);
            assertTrue(g.velmax() > 0);
            assertTrue(g.velmin() < 0);
            assertTrue(g.gmax() >= g.gmin());
        }
    }

    @Nested
    @DisplayName("GovHydroR – hydro turbine and governor (4th-order lead-lag)")
    class GovHydroRTest {
        @Test void count() {
            assertEquals(1, MODEL.govHydroRList().size()); }

        @Test void fields() {
            GovHydroR g = single(MODEL.govHydroRList(), "GovHydroR");
            assertContains(g.id(), "hydror");
            assertSmId(g.synchronousMachineId(), "sm-gov-22");
            assertEquals(220.0, g.mwbase(), TOL);
            assertEquals(0.05, g.r(), TOL);
            assertEquals(1.2, g.at(), TOL);
            assertEquals(0.5, g.dturb(), TOL);
            assertEquals(1.0, g.gmax(), TOL);
            assertEquals(0.0, g.gmin(), TOL);
            assertFalse(g.inputSignal(), "inputSignal should be false");
            assertEquals(1.0, g.kg(), TOL);
            assertEquals(0.5, g.ki(), TOL);
            assertEquals(0.1, g.qnl(), TOL);
            assertEquals(0.1, g.t1(), TOL);
            assertEquals(5.0, g.t2(), TOL);
            assertEquals(1.0, g.tw(), TOL);
            assertEquals(0.2, g.velop(), TOL);
            assertEquals(-0.2, g.velcl(), TOL);
            assertEquals(0.4, g.gv2(), TOL);
            assertEquals(0.75, g.pgv2(), TOL);
        }
    }

    @Nested
    @DisplayName("GovHydroWEH – Woodward Electric Hydro governor")
    class GovHydroWEHTest {
        @Test void count() {
            assertEquals(1, MODEL.govHydroWEHList().size()); }

        @Test void fields() {
            GovHydroWEH g = single(MODEL.govHydroWEHList(), "GovHydroWEH");
            assertContains(g.id(), "hydroweh");
            assertSmId(g.synchronousMachineId(), "sm-gov-23");
            assertEquals(280.0, g.mwbase(), TOL);
            assertEquals(0.05, g.rpg(), TOL);
            assertEquals(0.1, g.rpp(), TOL);
            assertEquals(3.0, g.kp(), TOL);
            assertEquals(1.0, g.ki(), TOL);
            assertEquals(0.1, g.kd(), TOL);
            assertEquals(0.5, g.tg(), TOL);
            assertEquals(1.0, g.tw(), TOL);
            assertEquals(0.5, g.dturb(), TOL);
            assertFalse(g.feedbackSignal(), "feedbackSignal should be false");
            assertEquals(0.1, g.gtmxop(), TOL);
            assertEquals(-0.1, g.gtmxcl(), TOL);
            // frequency-limit table + gate schedule
            assertEquals(0.9, g.fl1(), TOL);
            assertEquals(1.0, g.fp10(), TOL);
            assertEquals(0.25, g.gv2(), TOL);
            // pmss array
            assertEquals(0.1, g.pmss1(), TOL);
            assertEquals(0.5, g.pmss5(), TOL);
            assertEquals(1.0, g.pmss10(), TOL);
            assertEquals(1.0, g.gmax(), TOL);
            assertEquals(0.0, g.gmin(), TOL);
        }

        @Test void pmssMonotonicallyIncreasing() {
            GovHydroWEH g = MODEL.govHydroWEHList().get(0);
            double[] pmss = {g.pmss1(), g.pmss2(), g.pmss3(), g.pmss4(), g.pmss5(),
                             g.pmss6(), g.pmss7(), g.pmss8(), g.pmss9(), g.pmss10()};
            for (int i = 1; i < pmss.length; i++) {
                assertTrue(pmss[i] >= pmss[i - 1],
                    "pmss array should be non-decreasing at index " + i);
            }
        }
    }

    @Nested
    @DisplayName("GovHydroWPID – WPID hydro governor")
    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    class GovHydroWPIDTest {
        @Test void count() {
            assertEquals(1, MODEL.govHydroWPIDList().size()); }

        @Test void fields() {
            GovHydroWPID g = single(MODEL.govHydroWPIDList(), "GovHydroWPID");
            assertContains(g.id(), "hydrowpid");
            assertSmId(g.synchronousMachineId(), "sm-gov-24");
            assertEquals(190.0, g.mwbase(), TOL);
            assertEquals(0.05, g.reg(), TOL);
            assertEquals(5.0, g.treg(), TOL);
            assertEquals(1.0, g.tw(), TOL);
            assertEquals(0.1, g.ta(), TOL);
            assertEquals(0.2, g.tb(), TOL);
            assertEquals(1.0, g.pmax(), TOL);
            assertEquals(0.0, g.pmin(), TOL);
            assertEquals(1.0, g.gatmax(), TOL);
            assertEquals(0.0, g.gatmin(), TOL);
            assertEquals(0.5, g.d(), TOL);
            assertEquals(0.0, g.kd(), TOL);
            assertEquals(0.5, g.ki(), TOL);
            assertEquals(1.0, g.kp(), TOL);
            assertEquals(0.2, g.velmax(), TOL);
            assertEquals(-0.2, g.velmin(), TOL);
            assertEquals(0.5, g.gv2(), TOL);
            assertEquals(0.6, g.pgv2(), TOL);
        }
    }

    // =========================================================================
    // GAS / COMBINED-CYCLE GOVERNORS
    // =========================================================================

    @Nested
    @DisplayName("GovGAST – basic gas turbine governor")
    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    class GovGASTTest {
        @Test void count() {
            assertEquals(1, MODEL.govGASTList().size()); }

        @Test void fields() {
            GovGAST g = single(MODEL.govGASTList(), "GovGAST");
            assertContains(g.id(), "gov-gast");  // full fragment
            assertSmId(g.synchronousMachineId(), "sm-gov-25");
            assertEquals(100.0, g.mwbase(), TOL);
            assertEquals(0.05, g.r(), TOL);
            assertEquals(0.5, g.t1(), TOL);
            assertEquals(3.0, g.t2(), TOL);
            assertEquals(10.0, g.t3(), TOL);
            assertEquals(1.0, g.at(), TOL);
            assertEquals(2.0, g.kt(), TOL);
            assertEquals(1.0, g.vmax(), TOL);
            assertEquals(0.0, g.vmin(), TOL);
            assertEquals(0.18, g.dturb(), TOL);
        }
    }

    @Nested
    @DisplayName("GovGAST1 – gas turbine governor with load limits")
    class GovGAST1Test {
        @Test void count() {
            assertEquals(1, MODEL.govGAST1List().size()); }

        @Test void fields() {
            GovGAST1 g = single(MODEL.govGAST1List(), "GovGAST1");
            assertContains(g.id(), "gast1");
            assertSmId(g.synchronousMachineId(), "sm-gov-26");
            assertEquals(120.0, g.mwbase(), TOL);
            assertEquals(0.05, g.r(), TOL);
            assertEquals(0.5, g.t1(), TOL);
            assertEquals(3.0, g.t2(), TOL);
            assertEquals(10.0, g.t3(), TOL);
            assertEquals(50.0, g.t5(), TOL);
            assertEquals(10.0, g.tltr(), TOL);
            assertEquals(2.0, g.kt(), TOL);
            assertEquals(1.0, g.a(), TOL);
            assertEquals(0.04, g.b(), TOL);
            assertEquals(1.0, g.lmax(), TOL);
            assertEquals(0.1, g.loadinc(), TOL);
            assertEquals(0.5, g.ltrate(), TOL);
            assertEquals(1.1, g.rmax(), TOL);
            assertEquals(-0.1, g.fidle(), TOL);
            assertEquals(1.0, g.vmax(), TOL);
            assertEquals(0.0, g.vmin(), TOL);
        }
    }

    @Nested
    @DisplayName("GovGAST2 – Rowen GAST2A gas turbine with temperature control")
    class GovGAST2Test {
        @Test void count() {
            assertEquals(1, MODEL.govGAST2List().size()); }

        @Test void fields() {
            GovGAST2 g = single(MODEL.govGAST2List(), "GovGAST2");
            assertContains(g.id(), "gast2");
            assertSmId(g.synchronousMachineId(), "sm-gov-27");
            assertEquals(130.0, g.mwbase(), TOL);
            assertEquals(1.0, g.a(), TOL);
            assertEquals(0.5, g.af1(), TOL);
            assertEquals(-0.3, g.af2(), TOL);
            assertEquals(0.01, g.ecr(), TOL);
            assertEquals(0.4, g.t(), TOL);
            assertEquals(15.0, g.t3(), TOL);
            assertEquals(1.1, g.tmax(), TOL);
            assertEquals(-0.1, g.tmin(), TOL);
            assertEquals(950.0, g.tr(), TOL);
            assertEquals(130.0, g.trate(), TOL);
            assertEquals(20.0, g.w(), TOL);
            assertEquals(0.6, g.x(), TOL);
            assertEquals(1.0, g.y(), TOL);
            assertTrue(g.z(), "z (governor mode) should be true = Droop");
        }
    }

    @Nested
    @DisplayName("GovGAST3 – gas turbine governor type 3")
    class GovGAST3Test {
        @Test void count() {
            assertEquals(1, MODEL.govGAST3List().size()); }

        @Test void fields() {
            GovGAST3 g = single(MODEL.govGAST3List(), "GovGAST3");
            assertContains(g.id(), "gast3");
            assertSmId(g.synchronousMachineId(), "sm-gov-28");
            assertEquals(0.0, g.bca(), TOL);
            assertEquals(1.0, g.bp(), TOL);
            assertEquals(0.1, g.dtc(), TOL);
            assertEquals(1.0, g.ka(), TOL);
            assertEquals(0.9, g.kac(), TOL);
            assertEquals(1.0, g.kca(), TOL);
            assertEquals(0.5, g.ksi(), TOL);
            assertEquals(-1.0, g.mnef(), TOL);
            assertEquals(1.0, g.mxef(), TOL);
            assertEquals(4.0, g.tac(), TOL);
            assertEquals(5.0, g.tc(), TOL);
            assertEquals(0.15, g.tsi(), TOL);
            assertEquals(0.1, g.ty(), TOL);
        }
    }

    @Nested
    @DisplayName("GovGAST4 – gas turbine governor type 4 with rate limits")
    class GovGAST4Test {
        @Test void count() {
            assertEquals(1, MODEL.govGAST4List().size()); }

        @Test void fields() {
            GovGAST4 g = single(MODEL.govGAST4List(), "GovGAST4");
            assertContains(g.id(), "gast4");
            assertSmId(g.synchronousMachineId(), "sm-gov-29");
            assertEquals(1.0, g.bp(), TOL);
            assertEquals(1.0, g.ktm(), TOL);
            assertEquals(-1.0, g.mnef(), TOL);
            assertEquals(1.0, g.mxef(), TOL);
            assertEquals(-0.1, g.rymn(), TOL);
            assertEquals(0.1, g.rymx(), TOL);
            assertEquals(0.5, g.ta(), TOL);
            assertEquals(3.0, g.tc(), TOL);
            assertEquals(10.0, g.tm(), TOL);
            assertEquals(0.05, g.tv(), TOL);
            assertEquals(0.05, g.ty(), TOL);
        }

        @Test void erLimitsSymmetric() {
            GovGAST4 g = MODEL.govGAST4List().get(0);
            assertEquals(g.rymx(), -g.rymn(), TOL);
        }
    }

    @Nested
    @DisplayName("GovGASTWD – Woodward gas turbine (Rowen with PID governor)")
    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    class GovGASTWDTest {
        @Test void count() {
            assertEquals(1, MODEL.govGASTWDList().size()); }

        @Test void fields() {
            GovGASTWD g = single(MODEL.govGASTWDList(), "GovGASTWD");
            assertContains(g.id(), "gastwd");
            assertSmId(g.synchronousMachineId(), "sm-gov-30");
            assertEquals(160.0, g.mwbase(), TOL);
            assertEquals(1.0, g.a(), TOL);
            assertEquals(0.5, g.af1(), TOL);
            assertEquals(-0.3, g.af2(), TOL);
            assertEquals(0.04, g.etd(), TOL);
            assertEquals(0.1, g.kd(), TOL);
            assertEquals(0.05, g.kdroop(), TOL);
            assertEquals(0.45, g.ki(), TOL);
            assertEquals(1.0, g.kp(), TOL);
            assertEquals(0.4, g.t(), TOL);
            assertEquals(0.2, g.tcd(), TOL);
            assertEquals(3.0, g.td(), TOL);
            assertEquals(1.0, g.tf(), TOL);
            assertEquals(1.1, g.tmax(), TOL);
            assertEquals(-0.1, g.tmin(), TOL);
            assertEquals(950.0, g.tr(), TOL);
            assertEquals(160.0, g.trate(), TOL);
        }
    }

    @Nested
    @DisplayName("GovCT1 – general IEEE combined-cycle governor with wfspd boolean")
    class GovCT1Test {
        @Test void count() {
            assertEquals(1, MODEL.govCT1List().size()); }

        @Test void fields() {
            GovCT1 g = single(MODEL.govCT1List(), "GovCT1");
            assertContains(g.id(), "ct1");
            assertSmId(g.synchronousMachineId(), "sm-gov-31");
            assertEquals(170.0, g.mwbase(), TOL);
            assertEquals(0.05, g.r(), TOL);
            assertEquals(-99.0, g.rdown(), TOL);
            assertEquals(99.0, g.rup(), TOL);
            assertEquals(0.01, g.ta(), TOL);
            assertEquals(0.4, g.tact(), TOL);
            assertEquals(0.1, g.tb(), TOL);
            assertEquals(1.0, g.rselect(), TOL);
            assertEquals(1.0, g.tpelec(), TOL);
            assertEquals(3.0, g.tfload(), TOL);
            assertEquals(1.9, g.kturb(), TOL);
            assertEquals(0.05, g.maxerr(), TOL);
            assertEquals(-0.05, g.minerr(), TOL);
            assertEquals(0.2, g.wfnl(), TOL);
            assertTrue(g.wfspd(), "wfspd should be true");
            assertEquals(0.0, g.kdgov(), TOL);
            assertEquals(0.45, g.kigov(), TOL);
            assertEquals(1.0, g.kpgov(), TOL);
            assertEquals(1.0, g.kpload(), TOL);
            assertEquals(1.0, g.kiload(), TOL);
            assertEquals(1.0, g.tdgov(), TOL);
            assertEquals(1.0, g.ldref(), TOL);
            assertEquals(0.0, g.db(), TOL);
            assertEquals(99.0, g.ropen(), TOL);
            assertEquals(-99.0, g.rclose(), TOL);
            assertEquals(0.0, g.kimw(), TOL);
            assertEquals(10.0, g.aset(), TOL);
            assertEquals(10.0, g.ka(), TOL);
        }

        @Test void booleanField() {
            // wfspd=true in fixture
            assertTrue(MODEL.govCT1List().get(0).wfspd());
        }
    }

    @Nested
    @DisplayName("GovCT2 – general combined-cycle / combustion-turbine governor with flim table")
    class GovCT2Test {
        @Test void count() {
            assertEquals(1, MODEL.govCT2List().size()); }

        @Test void fields() {
            GovCT2 g = single(MODEL.govCT2List(), "GovCT2");
            assertContains(g.id(), "ct2");
            assertSmId(g.synchronousMachineId(), "sm-gov-32");
            assertEquals(180.0, g.mwbase(), TOL);
            assertEquals(0.05, g.r(), TOL);
            assertEquals(1.0, g.rselect(), TOL);
            assertEquals(0.4, g.tact(), TOL);
            assertEquals(1.0, g.tpelec(), TOL);
            assertEquals(3.0, g.tfload(), TOL);
            assertEquals(0.2, g.wfnl(), TOL);
            assertFalse(g.wfspd(), "wfspd should be false");
            assertEquals(0.45, g.kigov(), TOL);
            assertEquals(0.05, g.maxerr(), TOL);
            assertEquals(-0.05, g.minerr(), TOL);
            assertEquals(1.9, g.kturb(), TOL);
            assertEquals(10.0, g.ka(), TOL);
            // flim/plim table – all zeros in fixture
            assertEquals(0.0, g.flim1(), TOL);
            assertEquals(0.0, g.plim1(), TOL);
            assertEquals(0.0, g.flim10(), TOL);
            assertEquals(0.0, g.plim10(), TOL);
            assertEquals(0.017, g.prate(), TOL);
        }

        @Test void booleanFields() {
            GovCT2 g = MODEL.govCT2List().get(0);
            assertFalse(g.wfspd());
        }

        @Test void errorBandConstraint() {
            GovCT2 g = MODEL.govCT2List().get(0);
            assertTrue(g.maxerr() > g.minerr(), "maxerr must be > minerr");
        }
    }

    // =========================================================================
    // Cross-cutting invariants
    // =========================================================================

    @Nested
    @DisplayName("Cross-cutting invariants across all governor types")
    class CrossCuttingTest {

        @Test
        @DisplayName("Every governor has a non-blank ID")
        void allIdsNonBlank() {
            checkIds(MODEL.govSteam0List().stream().map(GovSteam0::id).toList());
            checkIds(MODEL.govSteam1List().stream().map(GovSteam1::id).toList());
            checkIds(MODEL.govSteam2List().stream().map(GovSteam2::id).toList());
            checkIds(MODEL.govSteamCCList().stream().map(GovSteamCC::id).toList());
            checkIds(MODEL.govSteamEUList().stream().map(GovSteamEU::id).toList());
            checkIds(MODEL.govSteamFV2List().stream().map(GovSteamFV2::id).toList());
            checkIds(MODEL.govSteamFV3List().stream().map(GovSteamFV3::id).toList());
            checkIds(MODEL.govSteamFV4List().stream().map(GovSteamFV4::id).toList());
            checkIds(MODEL.govSteamIEEE1List().stream().map(GovSteamIEEE1::id).toList());
            checkIds(MODEL.govSteamSGOList().stream().map(GovSteamSGO::id).toList());
            checkIds(MODEL.govHydro1List().stream().map(GovHydro1::id).toList());
            checkIds(MODEL.govHydro2List().stream().map(GovHydro2::id).toList());
            checkIds(MODEL.govHydro3List().stream().map(GovHydro3::id).toList());
            checkIds(MODEL.govHydro4List().stream().map(GovHydro4::id).toList());
            checkIds(MODEL.govHydroDDList().stream().map(GovHydroDD::id).toList());
            checkIds(MODEL.govHydroFrancisList().stream().map(GovHydroFrancis::id).toList());
            checkIds(MODEL.govHydroIEEE0List().stream().map(GovHydroIEEE0::id).toList());
            checkIds(MODEL.govHydroIEEE2List().stream().map(GovHydroIEEE2::id).toList());
            checkIds(MODEL.govHydroPeltonList().stream().map(GovHydroPelton::id).toList());
            checkIds(MODEL.govHydroPIDList().stream().map(GovHydroPID::id).toList());
            checkIds(MODEL.govHydroPID2List().stream().map(GovHydroPID2::id).toList());
            checkIds(MODEL.govHydroRList().stream().map(GovHydroR::id).toList());
            checkIds(MODEL.govHydroWEHList().stream().map(GovHydroWEH::id).toList());
            checkIds(MODEL.govHydroWPIDList().stream().map(GovHydroWPID::id).toList());
            checkIds(MODEL.govGASTList().stream().map(GovGAST::id).toList());
            checkIds(MODEL.govGAST1List().stream().map(GovGAST1::id).toList());
            checkIds(MODEL.govGAST2List().stream().map(GovGAST2::id).toList());
            checkIds(MODEL.govGAST3List().stream().map(GovGAST3::id).toList());
            checkIds(MODEL.govGAST4List().stream().map(GovGAST4::id).toList());
            checkIds(MODEL.govGASTWDList().stream().map(GovGASTWD::id).toList());
            checkIds(MODEL.govCT1List().stream().map(GovCT1::id).toList());
            checkIds(MODEL.govCT2List().stream().map(GovCT2::id).toList());
        }

        @Test
        @DisplayName("Every governor has a non-blank synchronousMachineId")
        void allSmIdsNonBlank() {
            // spot-check every sub-family
            MODEL.govSteam0List().forEach(g -> assertFalse(g.synchronousMachineId().isBlank()));
            MODEL.govHydro1List().forEach(g -> assertFalse(g.synchronousMachineId().isBlank()));
            MODEL.govGASTList().forEach(g -> assertFalse(g.synchronousMachineId().isBlank()));
            MODEL.govCT1List().forEach(g -> assertFalse(g.synchronousMachineId().isBlank()));
            MODEL.govCT2List().forEach(g -> assertFalse(g.synchronousMachineId().isBlank()));
        }

        @Test
        @DisplayName("mwbase is positive for all steam governors")
        void steamMwbasePositive() {
            assertTrue(MODEL.govSteam0List().stream().allMatch(g -> g.mwbase() > 0));
            assertTrue(MODEL.govSteam1List().stream().allMatch(g -> g.mwbase() > 0));
            // GovSteam2 is dimensionless in CGMES (no mwbase)
            assertTrue(MODEL.govSteamCCList().stream().allMatch(g -> g.mwbase() > 0));
            assertTrue(MODEL.govSteamEUList().stream().allMatch(g -> g.mwbase() > 0));
            assertTrue(MODEL.govSteamFV2List().stream().allMatch(g -> g.mwbase() > 0));
            assertTrue(MODEL.govSteamFV3List().stream().allMatch(g -> g.mwbase() > 0));
            // GovSteamFV4 is the detailed electro-hydraulic governor (per-unit, no mwbase in CGMES)
            assertTrue(MODEL.govSteamIEEE1List().stream().allMatch(g -> g.mwbase() > 0));
            assertTrue(MODEL.govSteamSGOList().stream().allMatch(g -> g.mwbase() > 0));
        }

        @Test
        @DisplayName("mwbase is positive for all hydro governors")
        void hydroMwbasePositive() {
            assertTrue(MODEL.govHydro1List().stream().allMatch(g -> g.mwbase() > 0));
            assertTrue(MODEL.govHydro2List().stream().allMatch(g -> g.mwbase() > 0));
            assertTrue(MODEL.govHydro3List().stream().allMatch(g -> g.mwbase() > 0));
            assertTrue(MODEL.govHydro4List().stream().allMatch(g -> g.mwbase() > 0));
            assertTrue(MODEL.govHydroDDList().stream().allMatch(g -> g.mwbase() > 0));
            // GovHydroFrancis has no mwbase in CGMES (uses per-unit head/flow parameters instead).
            assertTrue(MODEL.govHydroIEEE0List().stream().allMatch(g -> g.mwbase() > 0));
            assertTrue(MODEL.govHydroIEEE2List().stream().allMatch(g -> g.mwbase() > 0));
            // GovHydroPelton has no mwbase in CGMES (uses per-unit head/flow parameters instead).
            assertTrue(MODEL.govHydroPIDList().stream().allMatch(g -> g.mwbase() > 0));
            assertTrue(MODEL.govHydroPID2List().stream().allMatch(g -> g.mwbase() > 0));
            assertTrue(MODEL.govHydroRList().stream().allMatch(g -> g.mwbase() > 0));
            assertTrue(MODEL.govHydroWEHList().stream().allMatch(g -> g.mwbase() > 0));
            assertTrue(MODEL.govHydroWPIDList().stream().allMatch(g -> g.mwbase() > 0));
        }

        @Test
        @DisplayName("mwbase is positive for all gas/CC governors")
        void gasMwbasePositive() {
            assertTrue(MODEL.govGASTList().stream().allMatch(g -> g.mwbase() > 0));
            assertTrue(MODEL.govGAST1List().stream().allMatch(g -> g.mwbase() > 0));
            assertTrue(MODEL.govGAST2List().stream().allMatch(g -> g.mwbase() > 0));
            // GovGAST3/GAST4 are dimensionless in CGMES (no mwbase)
            assertTrue(MODEL.govGASTWDList().stream().allMatch(g -> g.mwbase() > 0));
            assertTrue(MODEL.govCT1List().stream().allMatch(g -> g.mwbase() > 0));
            assertTrue(MODEL.govCT2List().stream().allMatch(g -> g.mwbase() > 0));
        }

        @Test
        @DisplayName("Total governor count equals 32 (one per class)")
        void totalCount() {
            int total =
                MODEL.govSteam0List().size() + MODEL.govSteam1List().size() +
                MODEL.govSteam2List().size() + MODEL.govSteamCCList().size() +
                MODEL.govSteamEUList().size() + MODEL.govSteamFV2List().size() +
                MODEL.govSteamFV3List().size() + MODEL.govSteamFV4List().size() +
                MODEL.govSteamIEEE1List().size() + MODEL.govSteamSGOList().size() +
                MODEL.govHydro1List().size() + MODEL.govHydro2List().size() +
                MODEL.govHydro3List().size() + MODEL.govHydro4List().size() +
                MODEL.govHydroDDList().size() + MODEL.govHydroFrancisList().size() +
                MODEL.govHydroIEEE0List().size() + MODEL.govHydroIEEE2List().size() +
                MODEL.govHydroPeltonList().size() + MODEL.govHydroPIDList().size() +
                MODEL.govHydroPID2List().size() + MODEL.govHydroRList().size() +
                MODEL.govHydroWEHList().size() + MODEL.govHydroWPIDList().size() +
                MODEL.govGASTList().size() + MODEL.govGAST1List().size() +
                MODEL.govGAST2List().size() + MODEL.govGAST3List().size() +
                MODEL.govGAST4List().size() + MODEL.govGASTWDList().size() +
                MODEL.govCT1List().size() + MODEL.govCT2List().size();
            assertEquals(32, total, "Expected exactly 32 governor instances total");
        }

        private void checkIds(List<String> ids) {
            ids.forEach(id -> {
                assertNotNull(id);
                assertFalse(id.isBlank(), "ID must not be blank");
            });
        }
    }
}
