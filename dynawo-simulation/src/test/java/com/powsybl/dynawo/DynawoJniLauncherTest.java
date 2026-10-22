/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo;

import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.test.AbstractSerDeTest;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationConfig;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.dynamicsimulation.DynamicModelsSupplier;
import com.powsybl.dynamicsimulation.DynamicSimulationParameters;
import com.powsybl.dynamicsimulation.DynamicSimulationResult;
import com.powsybl.dynamicsimulation.DynamicSimulationRunParameters;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.test.FourSubstationsNodeBreakerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.powsybl.dynawo.DynawoSimulationConstants.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Dynawo run through JNI on the network itself, with a launcher that writes into the network as Dynawo would
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
class DynawoJniLauncherTest extends AbstractSerDeTest {

    private static final DynamicModelsSupplier EMPTY_DYNAMIC_MODELS_SUPPLIER = (n, r) -> Collections.emptyList();
    private static final Path DYNAWO_HOME = Path.of("/home/dynawo");

    private ComputationManager computationManager;

    /**
     * Records what Dynawo is run on, and writes into the network as it would
     */
    private static final class DynawoJniLauncherMock implements DynawoJniLauncher {

        private final String version;
        private final Consumer<Network> dynawo;
        private Path jobsFile;
        private List<String> workingDirFiles;
        private List<String> inputFileNames;
        private String jobs;
        private String variantId;
        private String workingVariantId;

        private DynawoJniLauncherMock(Consumer<Network> dynawo) {
            this("1.8.0 (rev:master-0123456)", dynawo);
        }

        private DynawoJniLauncherMock(String version, Consumer<Network> dynawo) {
            this.version = version;
            this.dynawo = dynawo;
        }

        @Override
        public String getVersion(Path dynawoHome) {
            assertEquals(DYNAWO_HOME, dynawoHome);
            return version;
        }

        @Override
        public void run(Path dynawoHome, Path jobsFile, Map<Path, byte[]> inputFiles, Network network, String variantId) {
            assertEquals(DYNAWO_HOME, dynawoHome);
            this.jobsFile = jobsFile;
            workingDirFiles = listFileNames(jobsFile.getParent());
            inputFileNames = inputFiles.keySet().stream()
                    .peek(inputFile -> assertEquals(jobsFile.getParent(), inputFile.getParent()))
                    .map(inputFile -> inputFile.getFileName().toString())
                    .sorted()
                    .toList();
            jobs = new String(inputFiles.get(jobsFile), StandardCharsets.UTF_8);
            this.variantId = variantId;
            workingVariantId = network.getVariantManager().getWorkingVariantId();
            dynawo.accept(network);
        }
    }

    @BeforeEach
    @Override
    public void setUp() throws IOException {
        super.setUp();
        // no command is run: Dynawo runs in this JVM
        computationManager = new LocalComputationManager(new LocalComputationConfig(tmpDir, 1), new AbstractLocalCommandExecutor() {
            @Override
            public int execute(String program, List<String> args, Path outFile, Path errFile, Path workingDir, Map<String, String> env) {
                throw new AssertionError("No command expected: " + program);
            }
        }, ForkJoinPool.commonPool());
    }

    @AfterEach
    @Override
    public void tearDown() throws IOException {
        computationManager.close();
        super.tearDown();
    }

    private static List<String> listFileNames(Path dir) {
        try (Stream<Path> files = Files.list(dir)) {
            return files.map(file -> file.getFileName().toString()).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private DynamicSimulationResult run(Network network, String variantId, DynawoJniLauncher launcher, DynawoSimulationParameters dynawoParameters) {
        return run(network, variantId, launcher, dynawoParameters, false);
    }

    private DynamicSimulationResult run(Network network, String variantId, DynawoJniLauncher launcher, DynawoSimulationParameters dynawoParameters,
                                        boolean debug) {
        DynawoSimulationProvider provider = new DynawoSimulationProvider(new DynawoSimulationConfig(DYNAWO_HOME, debug, DynawoSimulationConfig.Launcher.JNI), launcher);
        DynamicSimulationParameters parameters = new DynamicSimulationParameters().setStartTime(0).setStopTime(10);
        parameters.addExtension(DynawoSimulationParameters.class, dynawoParameters);
        DynamicSimulationRunParameters runParameters = new DynamicSimulationRunParameters()
                .setComputationManager(computationManager)
                .setParameters(parameters);
        return provider.run(network, variantId, EMPTY_DYNAMIC_MODELS_SUPPLIER, runParameters).join();
    }

    private DynamicSimulationResult run(Network network, DynawoJniLauncher launcher) {
        return run(network, VariantManagerConstants.INITIAL_VARIANT_ID, launcher, DynawoSimulationParameters.load());
    }

    @Test
    void testNoIidmFile() {
        Network network = FourSubstationsNodeBreakerFactory.create();
        DynawoJniLauncherMock launcher = new DynawoJniLauncherMock(n -> {
        });

        assertEquals(DynamicSimulationResult.Status.SUCCESS, run(network, launcher).getStatus());

        // Dynawo reads the network itself, and its other input files from memory: no file in the working directory,
        // and no IIDM file of the final state asked for
        assertEquals(JOBS_FILENAME, launcher.jobsFile.getFileName().toString());
        assertEquals(List.of(), launcher.workingDirFiles);
        assertEquals(List.of(network.getId() + ".par", MODELS_PARAMETERS_FILENAME, NETWORK_PARAMETERS_FILENAME, DYD_FILENAME, JOBS_FILENAME,
                SOLVER_PARAMETERS_FILENAME), launcher.inputFileNames);
        assertTrue(launcher.jobs.contains("exportIIDMFile=\"false\""), launcher.jobs);
    }

    @Test
    void testDebug() {
        Network network = FourSubstationsNodeBreakerFactory.create();
        DynawoJniLauncherMock launcher = new DynawoJniLauncherMock(n -> {
        });

        assertEquals(DynamicSimulationResult.Status.SUCCESS,
                run(network, VariantManagerConstants.INITIAL_VARIANT_ID, launcher, DynawoSimulationParameters.load(), true).getStatus());

        // the input files given in memory are written in the working directory after the run, for inspection
        assertEquals(List.of(), launcher.workingDirFiles);
        assertEquals(launcher.inputFileNames, listFileNames(launcher.jobsFile.getParent()));
    }

    @Test
    void testFinalStateWrittenIntoNetwork() {
        Network network = FourSubstationsNodeBreakerFactory.create();
        TwoWindingsTransformer twt = network.getTwoWindingsTransformer("TWT");
        ShuntCompensator shunt = network.getShuntCompensator("SHUNT");
        DynawoJniLauncherMock launcher = new DynawoJniLauncherMock(n -> {
            n.getGenerator("GH1").getTerminal().setP(-90.5).setQ(-10.2);
            n.getTwoWindingsTransformer("TWT").getRatioTapChanger().setTapPosition(2);
            n.getTwoWindingsTransformer("TWT").getPhaseTapChanger().setTapPosition(12);
            n.getShuntCompensator("SHUNT").setSectionCount(0);
            n.getLoad("LD1").getTerminal().disconnect();
        });

        DynamicSimulationResult result = run(network, launcher);

        assertEquals(DynamicSimulationResult.Status.SUCCESS, result.getStatus());
        assertEquals("", result.getStatusText());
        assertEquals(VariantManagerConstants.INITIAL_VARIANT_ID, launcher.variantId);
        assertEquals(VariantManagerConstants.INITIAL_VARIANT_ID, launcher.workingVariantId);
        // the final state, written by Dynawo into the network itself
        assertEquals(-90.5, network.getGenerator("GH1").getTerminal().getP());
        assertEquals(-10.2, network.getGenerator("GH1").getTerminal().getQ());
        assertFalse(network.getLoad("LD1").getTerminal().isConnected());
        // the tap positions and section counts it wrote are the solved ones, as from the IIDM file of the final state
        assertEquals(2, twt.getRatioTapChanger().getSolvedTapPosition());
        assertEquals(1, twt.getRatioTapChanger().getTapPosition());
        assertEquals(12, twt.getPhaseTapChanger().getSolvedTapPosition());
        assertEquals(15, twt.getPhaseTapChanger().getTapPosition());
        assertEquals(0, shunt.getSolvedSectionCount());
        assertEquals(1, shunt.getSectionCount());
    }

    @Test
    void testFailure() {
        Network network = FourSubstationsNodeBreakerFactory.create();
        TwoWindingsTransformer twt = network.getTwoWindingsTransformer("TWT");
        DynawoJniLauncherMock launcher = new DynawoJniLauncherMock(n -> {
            throw new PowsyblException("DYN Error: Simulation failed at t=1.5");
        });

        DynamicSimulationResult result = run(network, launcher);

        assertEquals(DynamicSimulationResult.Status.FAILURE, result.getStatus());
        assertEquals("Simulation failed at t=1.5", result.getStatusText());
        // no solved positions: the network as it was
        assertNull(twt.getRatioTapChanger().getSolvedTapPosition());
        assertEquals(1, twt.getRatioTapChanger().getTapPosition());
        assertNull(network.getShuntCompensator("SHUNT").getSolvedSectionCount());
    }

    @Test
    void testVariant() {
        Network network = FourSubstationsNodeBreakerFactory.create();
        network.getVariantManager().cloneVariant(VariantManagerConstants.INITIAL_VARIANT_ID, "dynawo");
        double initialP = network.getGenerator("GH1").getTerminal().getP();
        DynawoJniLauncherMock launcher = new DynawoJniLauncherMock(n -> n.getGenerator("GH1").getTerminal().setP(-42.0));

        DynamicSimulationResult result = run(network, "dynawo", launcher, DynawoSimulationParameters.load());

        assertEquals(DynamicSimulationResult.Status.SUCCESS, result.getStatus());
        assertEquals("dynawo", launcher.variantId);
        assertEquals("dynawo", launcher.workingVariantId);
        network.getVariantManager().setWorkingVariant("dynawo");
        assertEquals(-42.0, network.getGenerator("GH1").getTerminal().getP());
        network.getVariantManager().setWorkingVariant(VariantManagerConstants.INITIAL_VARIANT_ID);
        assertEquals(initialP, network.getGenerator("GH1").getTerminal().getP());
    }

    @Test
    void testMergeLoads() {
        Network network = FourSubstationsNodeBreakerFactory.create();
        DynawoJniLauncherMock launcher = new DynawoJniLauncherMock(n -> fail("Dynawo should not be run"));
        DynawoSimulationParameters dynawoParameters = DynawoSimulationParameters.load().setMergeLoads(true);

        PowsyblException e = assertThrows(PowsyblException.class,
                () -> run(network, VariantManagerConstants.INITIAL_VARIANT_ID, launcher, dynawoParameters));
        assertEquals("Merged loads need a copy of the network: the jni launcher of Dynawo cannot merge loads", e.getMessage());
    }

    @Test
    void testBadVersion() {
        Network network = FourSubstationsNodeBreakerFactory.create();
        DynawoJniLauncherMock launcher = new DynawoJniLauncherMock("1.2.0 (rev:master-0123456)", n -> fail("Dynawo should not be run"));

        PowsyblException e = assertThrows(PowsyblException.class, () -> run(network, launcher));
        assertTrue(e.getMessage().startsWith("dynawo version not supported. Must be >= "), e.getMessage());
    }

    @Test
    void testMissingJniModule() {
        Network network = FourSubstationsNodeBreakerFactory.create();
        DynawoSimulationProvider provider = new DynawoSimulationProvider(new DynawoSimulationConfig(DYNAWO_HOME, false, DynawoSimulationConfig.Launcher.JNI));
        DynamicSimulationRunParameters runParameters = new DynamicSimulationRunParameters().setComputationManager(computationManager);

        PowsyblException e = assertThrows(PowsyblException.class,
                () -> provider.run(network, VariantManagerConstants.INITIAL_VARIANT_ID, EMPTY_DYNAMIC_MODELS_SUPPLIER, runParameters));
        assertEquals("The jni launcher of Dynawo needs the powsybl-dynawo-simulation-jni module", e.getMessage());
    }
}
