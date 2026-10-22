/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.jni;

import com.powsybl.commons.datasource.ResourceDataSource;
import com.powsybl.commons.datasource.ResourceSet;
import com.powsybl.computation.local.LocalComputationConfig;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.dynamicsimulation.*;
import com.powsybl.dynawo.DynawoSimulationConfig;
import com.powsybl.dynawo.DynawoSimulationParameters;
import com.powsybl.dynawo.DynawoSimulationProvider;
import com.powsybl.dynawo.models.events.EventDisconnectionBuilder;
import com.powsybl.dynawo.models.generators.SignalNGeneratorBuilder;
import com.powsybl.dynawo.models.loads.BaseLoadBuilder;
import com.powsybl.iidm.network.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static com.powsybl.dynawo.DynawoSimulationConstants.JOBS_FILENAME;
import static com.powsybl.dynawo.commons.DynawoConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Dynawo, installed in the directory of the {@code DYNAWO_INSTALL_DIR} environment variable, run in this JVM on the
 * network itself. The libraries of Dynawo and of its third parties must be on the {@code LD_LIBRARY_PATH}; the dynamic
 * tests need the GeneratorPVSignalN, LoadAlphaBetaRestorative, SignalN and EventSetPointBoolean models.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
@EnabledIfEnvironmentVariable(named = "DYNAWO_INSTALL_DIR", matches = ".+")
class JniDynawoSimulationTest {

    private static final String BUS_ID = "_BUS___10_TN";
    private static final String DISCONNECTED_LOAD_ID = "_LOAD__10_EC";
    private static final String[] GENERATOR_IDS = {"_GEN____1_SM", "_GEN____2_SM", "_GEN____3_SM", "_GEN____6_SM", "_GEN____8_SM"};
    private static final String[] GENERATOR_PARAMETER_SET_IDS = {"Generator1", "Generator2", "Generator3", "Generator6", "Generator8"};

    @TempDir
    Path localDir;

    private Path dynawoHome;
    private LocalComputationManager computationManager;

    @BeforeEach
    void setUp() throws IOException {
        dynawoHome = Path.of(System.getenv("DYNAWO_INSTALL_DIR"));
        computationManager = new LocalComputationManager(new LocalComputationConfig(localDir, 1));
    }

    @AfterEach
    void tearDown() {
        computationManager.close();
    }

    /**
     * IEEE 14, a solved load flow: the initial state of Dynawo
     */
    private static Network createIeee14() {
        return Network.read(new ResourceDataSource("IEEE14", new ResourceSet("/ieee14", "IEEE14.iidm")));
    }

    private static InputStream getResourceAsStream(String name) {
        return Objects.requireNonNull(JniDynawoSimulationTest.class.getResourceAsStream(name));
    }

    /**
     * Generators and loads with their dynamic models, and the load of the bus 10 disconnected at 1 s
     */
    private DynamicSimulationResult runSignalN(Network network, DynawoSimulationConfig.Launcher launcher) {
        return runSignalN(network, launcher, createSignalNParameters());
    }

    private static DynawoSimulationParameters createSignalNParameters() {
        return new DynawoSimulationParameters()
                .setModelsParameters(getResourceAsStream("/ieee14/signal_n/IEEE14.par"))
                .setNetworkParameters(getResourceAsStream("/ieee14/signal_n/IEEE14.par"), "Network")
                .setSolverParameters(getResourceAsStream("/ieee14/signal_n/IEEE14.par"), "SimplifiedSolver")
                .setSolverType(DynawoSimulationParameters.SolverType.SIM);
    }

    private DynamicSimulationResult runSignalN(Network network, DynawoSimulationConfig.Launcher launcher, DynawoSimulationParameters dynawoParameters) {
        DynamicModelsSupplier dynamicModels = (n, r) -> {
            List<DynamicModel> models = new ArrayList<>();
            for (int i = 0; i < GENERATOR_IDS.length; i++) {
                models.add(SignalNGeneratorBuilder.of(n, "GeneratorPVSignalN")
                        .staticId(GENERATOR_IDS[i])
                        .parameterSetId(GENERATOR_PARAMETER_SET_IDS[i])
                        .build());
            }
            n.getLoadStream().forEach(load -> models.add(BaseLoadBuilder.of(n, "LoadAlphaBetaRestorative")
                    .staticId(load.getId())
                    .parameterSetId("GenericRestorativeLoad")
                    .build()));
            return models;
        };
        EventModelsSupplier events = (n, r) -> List.of(EventDisconnectionBuilder.of(n)
                .staticId(DISCONNECTED_LOAD_ID)
                .startTime(1)
                .build());
        return run(network, launcher, dynawoParameters, dynamicModels, events);
    }

    private DynamicSimulationResult run(Network network, DynawoSimulationConfig.Launcher launcher, DynawoSimulationParameters dynawoParameters,
                                        DynamicModelsSupplier dynamicModels, EventModelsSupplier events) {
        DynawoSimulationProvider provider = new DynawoSimulationProvider(new DynawoSimulationConfig(dynawoHome, true, launcher));
        DynamicSimulationParameters parameters = new DynamicSimulationParameters().setStartTime(0).setStopTime(10);
        parameters.addExtension(DynawoSimulationParameters.class, dynawoParameters);
        DynamicSimulationRunParameters runParameters = new DynamicSimulationRunParameters()
                .setComputationManager(computationManager)
                .setParameters(parameters)
                .setEventModelsSupplier(events);
        return provider.run(network, VariantManagerConstants.INITIAL_VARIANT_ID, dynamicModels, runParameters).join();
    }

    /**
     * The working directory of the only run, kept in debug
     */
    private Path getWorkingDir() {
        try (Stream<Path> dirs = Files.list(localDir)) {
            List<Path> workingDirs = dirs.filter(dir -> dir.getFileName().toString().startsWith("dynawo_")).toList();
            assertEquals(1, workingDirs.size(), workingDirs::toString);
            return workingDirs.getFirst();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * No IIDM file: Dynawo has read the network itself, and written the final state into it
     */
    private void assertNoIidmFile() {
        Path workingDir = getWorkingDir();
        assertTrue(Files.exists(workingDir.resolve(JOBS_FILENAME)));
        assertFalse(Files.exists(workingDir.resolve(NETWORK_FILENAME)));
        assertFalse(Files.exists(workingDir.resolve(FINAL_STATE_FOLDER_PATH).resolve(OUTPUT_IIDM_FILENAME)));
    }

    private static void assertSuccess(DynamicSimulationResult result) {
        assertEquals(DynamicSimulationResult.Status.SUCCESS, result.getStatus(), result.getStatusText());
        assertEquals("", result.getStatusText());
    }

    @Test
    void testIeee14SignalN() {
        Network network = createIeee14();
        double v = network.getBusBreakerView().getBus(BUS_ID).getV();

        DynamicSimulationResult result = runSignalN(network, DynawoSimulationConfig.Launcher.JNI);

        assertSuccess(result);
        assertNoIidmFile();
        assertThat(result.getTimeLine()).isNotEmpty();
        // the load is off, and the voltage of its bus has risen
        assertEquals(0, network.getLoad(DISCONNECTED_LOAD_ID).getTerminal().getP(), 1e-6);
        assertThat(network.getBusBreakerView().getBus(BUS_ID).getV()).isGreaterThan(v + 1e-3);
    }

    @Test
    void testIeee14SignalNAsProcess() {
        Network processNetwork = createIeee14();
        Network jniNetwork = createIeee14();

        DynamicSimulationResult processResult = runSignalN(processNetwork, DynawoSimulationConfig.Launcher.PROCESS);
        DynamicSimulationResult jniResult = runSignalN(jniNetwork, DynawoSimulationConfig.Launcher.JNI);

        assertSuccess(processResult);
        assertSuccess(jniResult);
        assertEquals(processResult.getTimeLine(), jniResult.getTimeLine());
        assertSameState(processNetwork, jniNetwork);
    }

    /**
     * The final state written into the network is the one of the IIDM file of the final state
     */
    private static void assertSameState(Network processNetwork, Network jniNetwork) {
        for (Bus processBus : processNetwork.getBusBreakerView().getBuses()) {
            Bus jniBus = jniNetwork.getBusBreakerView().getBus(processBus.getId());
            assertEquals(processBus.getV(), jniBus.getV(), 1e-9, processBus::getId);
            assertEquals(processBus.getAngle(), jniBus.getAngle(), 1e-9, processBus::getId);
        }
        processNetwork.getLoadStream().forEach(load -> assertSameFlow(load.getTerminal(), jniNetwork.getLoad(load.getId()).getTerminal(), load.getId()));
        processNetwork.getGeneratorStream().forEach(generator -> assertSameFlow(generator.getTerminal(), jniNetwork.getGenerator(generator.getId()).getTerminal(), generator.getId()));
        processNetwork.getShuntCompensatorStream().forEach(shunt -> {
            ShuntCompensator jniShunt = jniNetwork.getShuntCompensator(shunt.getId());
            assertSameFlow(shunt.getTerminal(), jniShunt.getTerminal(), shunt.getId());
            assertEquals(shunt.getSolvedSectionCount(), jniShunt.getSolvedSectionCount(), shunt::getId);
            assertEquals(shunt.getSectionCount(), jniShunt.getSectionCount(), shunt::getId);
        });
        processNetwork.getLineStream().forEach(line -> {
            assertSameFlow(line.getTerminal1(), jniNetwork.getLine(line.getId()).getTerminal1(), line.getId());
            assertSameFlow(line.getTerminal2(), jniNetwork.getLine(line.getId()).getTerminal2(), line.getId());
        });
        processNetwork.getTwoWindingsTransformerStream().forEach(twt -> {
            TwoWindingsTransformer jniTwt = jniNetwork.getTwoWindingsTransformer(twt.getId());
            assertSameFlow(twt.getTerminal1(), jniTwt.getTerminal1(), twt.getId());
            assertSameFlow(twt.getTerminal2(), jniTwt.getTerminal2(), twt.getId());
            twt.getOptionalRatioTapChanger().ifPresent(rtc -> {
                assertEquals(rtc.getSolvedTapPosition(), jniTwt.getRatioTapChanger().getSolvedTapPosition(), twt::getId);
                assertEquals(rtc.getTapPosition(), jniTwt.getRatioTapChanger().getTapPosition(), twt::getId);
            });
        });
    }

    private static void assertSameFlow(Terminal expected, Terminal actual, String id) {
        assertEquals(expected.getP(), actual.getP(), 1e-9, id);
        assertEquals(expected.getQ(), actual.getQ(), 1e-9, id);
        assertEquals(expected.isConnected(), actual.isConnected(), id);
    }

    @Test
    void testFailure() throws URISyntaxException {
        Network network = createIeee14();
        Terminal load = network.getLoad(DISCONNECTED_LOAD_ID).getTerminal();
        double v = network.getBusBreakerView().getBus(BUS_ID).getV();
        double p = load.getP();
        // a criteria checked at the end of the simulation, after the disconnection of the load, never respected
        DynawoSimulationParameters dynawoParameters = createSignalNParameters()
                .setCriteriaFilePath(Path.of(Objects.requireNonNull(getClass().getResource("/ieee14/final_criteria.crt")).toURI()));

        DynamicSimulationResult result = runSignalN(network, DynawoSimulationConfig.Launcher.JNI, dynawoParameters);

        assertEquals(DynamicSimulationResult.Status.FAILURE, result.getStatus());
        assertThat(result.getStatusText()).containsIgnoringCase("criteria");
        // the network as it was, without the final state of the failed simulation
        assertTrue(load.isConnected());
        assertEquals(p, load.getP());
        assertEquals(v, network.getBusBreakerView().getBus(BUS_ID).getV());
    }

    @Test
    void testVersion() {
        assertThat(new JniDynawoLauncher().getVersion(dynawoHome)).matches("\\d+\\.\\d+\\.\\d+ \\(rev:.*\\)");
    }
}
