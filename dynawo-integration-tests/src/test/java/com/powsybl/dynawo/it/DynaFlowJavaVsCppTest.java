/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.it;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.dynaflow.DynaFlowConfig;
import com.powsybl.dynaflow.DynaFlowJavaProvider;
import com.powsybl.dynaflow.DynaFlowParameters;
import com.powsybl.dynaflow.DynaFlowProvider;
import com.powsybl.dynawo.DynawoSimulationConfig;
import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManagerConstants;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.loadflow.LoadFlowRunParameters;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Runs the same network through the two DynaFlow load flows against a local DynaFlow Launcher install: the
 * C++ path ({@code DynaFlowProvider}, which shells out to {@code dynaflow-launcher}) and the Java path
 * ({@code DynaFlowJavaProvider}, which runs the {@code "DynaFlow"} mapping through Dynawo). Both should
 * converge to the same steady state; this pins the Java replacement against the launcher it replaces.
 * <p>
 * The test needs the real binaries, so it is skipped unless the install is present at {@link #INSTALL}
 * (override with {@code -Ddynaflow.install=/path/to/dynaflow-launcher}). It uses a local computation
 * manager, not the Docker one the other integration tests use.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
class DynaFlowJavaVsCppTest {

    private static final Path INSTALL = Path.of(System.getProperty("dynaflow.install",
            System.getProperty("user.home") + "/Projects/powsybl/powsybl-dynawo-mapping/DynaFlowLauncher_Linux_v1.7.0/dynaflow-launcher"));

    // voltages match the C++ launcher exactly on IEEE14; angles differ only by the small phase-reference
    // datum (the SignalN thetaRef / slack anchor), so these guard against a real divergence of the port
    private static final double V_TOLERANCE = 0.05;
    private static final double ANGLE_TOLERANCE = 0.5;

    @Test
    void theJavaPathConvergesLikeTheCppPath() throws Exception {
        assumeTrue(Files.exists(INSTALL.resolve("dynaflow-launcher.sh")) && Files.exists(INSTALL.resolve("dynawo.sh")),
                "local DynaFlow Launcher install required at " + INSTALL);

        try (LocalComputationManager computationManager = new LocalComputationManager()) {
            Network cppNetwork = IeeeCdfNetworkFactory.create14Solved();
            LoadFlowResult cppResult = runCpp(cppNetwork, computationManager);

            Network javaNetwork = IeeeCdfNetworkFactory.create14Solved();
            LoadFlowResult javaResult = runJava(javaNetwork, computationManager);

            assertEquals(LoadFlowResult.ComponentResult.Status.CONVERGED,
                    cppResult.getComponentResults().getFirst().getStatus(), "C++ DynaFlow should converge");
            assertEquals(LoadFlowResult.ComponentResult.Status.CONVERGED,
                    javaResult.getComponentResults().getFirst().getStatus(), "Java DynaFlow should converge");

            compareBusStates(cppNetwork, javaNetwork);
        }
    }

    @Test
    void theGeneratedDydAndParMatchTheLauncher() throws Exception {
        assumeTrue(Files.exists(INSTALL.resolve("dynaflow-launcher.sh")) && Files.exists(INSTALL.resolve("dynawo.sh")),
                "local DynaFlow Launcher install required at " + INSTALL);
        Path cppDir = Files.createTempDirectory("cpp_capture");
        Path javaDir = Files.createTempDirectory("java_capture");
        try (LocalComputationManager cm = new LocalComputationManager()) {
            Network cppNet = IeeeCdfNetworkFactory.create14Solved();
            LoadFlowParameters cppParams = new LoadFlowParameters()
                    .setVoltageInitMode(LoadFlowParameters.VoltageInitMode.PREVIOUS_VALUES).setReadSlackBus(false);
            cppParams.addExtension(DynaFlowParameters.class, new DynaFlowParameters());
            cppParams.setDebugDir(cppDir.toString());
            new DynaFlowProvider(() -> new DynaFlowConfig(INSTALL, false)).run(cppNet, VariantManagerConstants.INITIAL_VARIANT_ID,
                    runParameters(cm, cppParams)).join();

            Network javaNet = IeeeCdfNetworkFactory.create14Solved();
            LoadFlowParameters javaParams = new LoadFlowParameters()
                    .setVoltageInitMode(LoadFlowParameters.VoltageInitMode.PREVIOUS_VALUES).setReadSlackBus(false);
            javaParams.setDebugDir(javaDir.toString());
            new DynaFlowJavaProvider(() -> new DynawoSimulationConfig(INSTALL, false)).run(javaNet, VariantManagerConstants.INITIAL_VARIANT_ID,
                    runParameters(cm, javaParams)).join();
        }

        Path cppDyd = findFile(cppDir, "powsybl_dynawo.dyd");
        Path javaDyd = findFile(javaDir, "powsybl_dynawo.dyd");
        // the dyd: the Java mapping gives every equipment the same model as the launcher
        assertEquals(modelMap(cppDyd), modelMap(javaDyd), "the model selected per equipment must match the launcher");

        // the par: each model's numeric parameter values match (sets aligned by the model's parId, since the
        // launcher keys sets by a uuid and names diagram tables differently)
        compareParValues(cppDyd, findFile(cppDir, "powsybl_dynawo.par"), javaDyd, findFile(javaDir, "models.par"));
    }

    private static Path findFile(Path dir, String name) throws Exception {
        try (var s = Files.walk(dir)) {
            return s.filter(p -> p.getFileName().toString().equals(name)).findFirst().orElseThrow();
        }
    }

    /** Compares the numeric parameter each model reads on both paths, aligning parameter sets by parId. */
    private static void compareParValues(Path cppDyd, Path cppPar, Path javaDyd, Path javaPar) throws Exception {
        java.util.Map<String, String> cppParId = parIdByStaticId(cppDyd);
        java.util.Map<String, String> javaParId = parIdByStaticId(javaDyd);
        java.util.Map<String, java.util.Map<String, Double>> cppSets = numericParSets(cppPar);
        java.util.Map<String, java.util.Map<String, Double>> javaSets = numericParSets(javaPar);

        double maxDiff = 0;
        String worst = "";
        int compared = 0;
        for (String staticId : modelMap(cppDyd).keySet()) {
            java.util.Map<String, Double> cppValues = cppSets.getOrDefault(cppParId.get(staticId), java.util.Map.of());
            java.util.Map<String, Double> javaValues = javaSets.getOrDefault(javaParId.get(staticId), java.util.Map.of());
            for (String name : cppValues.keySet()) {
                if (javaValues.containsKey(name)) {
                    double diff = Math.abs(cppValues.get(name) - javaValues.get(name));
                    compared++;
                    if (diff > maxDiff) {
                        maxDiff = diff;
                        worst = staticId + "." + name;
                    }
                }
            }
        }
        System.out.printf("DynaFlow C++ vs Java par: %d numeric parameters compared, max |Δ| = %.6g (%s)%n", compared, maxDiff, worst);
        assertTrue(compared > 0, "no numeric parameters were aligned between the two par files");
        assertTrue(maxDiff < 1e-6, "parameter " + worst + " differs by " + maxDiff);
    }

    private static java.util.Map<String, String> parIdByStaticId(Path dyd) throws Exception {
        java.util.Map<String, String> parIds = new java.util.TreeMap<>();
        java.util.regex.Matcher tag = java.util.regex.Pattern
                .compile("<dyn:blackBoxModel\\b([^>]*)>").matcher(Files.readString(dyd));
        while (tag.find()) {
            String staticId = attribute(tag.group(1), "staticId");
            String parId = attribute(tag.group(1), "parId");
            if (staticId != null && parId != null) {
                parIds.put(staticId, parId);
            }
        }
        return parIds;
    }

    /**
     * setId -> {parameter name -> numeric value}, references (IIDM-sourced) and strings skipped. The
     * launcher factors shared values into {@code <macroParameterSet>}s a per-model {@code <set>} pulls in
     * through {@code <macroParSet>}; those are resolved so the set carries the values the model reads.
     */
    private static java.util.Map<String, java.util.Map<String, Double>> numericParSets(Path par) throws Exception {
        String content = Files.readString(par);
        java.util.Map<String, java.util.Map<String, Double>> macros = new java.util.HashMap<>();
        java.util.regex.Matcher macroMatcher = java.util.regex.Pattern
                .compile("<macroParameterSet\\s+id=\"([^\"]*)\">(.*?)</macroParameterSet>", java.util.regex.Pattern.DOTALL)
                .matcher(content);
        while (macroMatcher.find()) {
            macros.put(macroMatcher.group(1), numericPars(macroMatcher.group(2)));
        }
        java.util.Map<String, java.util.Map<String, Double>> sets = new java.util.TreeMap<>();
        java.util.regex.Matcher setMatcher = java.util.regex.Pattern
                .compile("<set\\s+id=\"([^\"]*)\">(.*?)</set>", java.util.regex.Pattern.DOTALL).matcher(content);
        while (setMatcher.find()) {
            java.util.Map<String, Double> values = new java.util.TreeMap<>(numericPars(setMatcher.group(2)));
            java.util.regex.Matcher macroRef = java.util.regex.Pattern
                    .compile("<macroParSet\\s+id=\"([^\"]*)\"").matcher(setMatcher.group(2));
            while (macroRef.find()) {
                values.putAll(macros.getOrDefault(macroRef.group(1), java.util.Map.of()));
            }
            sets.put(setMatcher.group(1), values);
        }
        return sets;
    }

    private static java.util.Map<String, Double> numericPars(String body) {
        java.util.Map<String, Double> values = new java.util.TreeMap<>();
        java.util.regex.Matcher parMatcher = java.util.regex.Pattern.compile("<par\\b([^>]*)/>").matcher(body);
        while (parMatcher.find()) {
            String name = attribute(parMatcher.group(1), "name");
            String value = attribute(parMatcher.group(1), "value");
            if (name != null && value != null) {
                try {
                    values.put(name, Double.parseDouble(value));
                } catch (NumberFormatException e) {
                    // a non-numeric (string) parameter, e.g. a diagram table name — skip
                }
            }
        }
        return values;
    }

    private static java.util.Map<String, String> modelMap(Path dyd) throws Exception {
        java.util.Map<String, String> models = new java.util.TreeMap<>();
        java.util.regex.Matcher tag = java.util.regex.Pattern
                .compile("<dyn:blackBoxModel\\b([^>]*)>").matcher(Files.readString(dyd));
        while (tag.find()) {
            String attrs = tag.group(1);
            String staticId = attribute(attrs, "staticId");
            String lib = attribute(attrs, "lib");
            if (staticId != null && lib != null) {   // skip the pure-dynamic SignalN (no staticId)
                models.put(staticId, lib);
            }
        }
        return models;
    }

    private static String attribute(String attrs, String name) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\b" + name + "=\"([^\"]*)\"").matcher(attrs);
        return m.find() ? m.group(1) : null;
    }

    private static LoadFlowResult runCpp(Network network, LocalComputationManager computationManager) {
        DynaFlowConfig config = new DynaFlowConfig(INSTALL, false);
        DynaFlowProvider provider = new DynaFlowProvider(() -> config);
        LoadFlowParameters parameters = new LoadFlowParameters()
                .setVoltageInitMode(LoadFlowParameters.VoltageInitMode.PREVIOUS_VALUES)
                .setReadSlackBus(false);
        parameters.addExtension(DynaFlowParameters.class, new DynaFlowParameters());
        return provider.run(network, VariantManagerConstants.INITIAL_VARIANT_ID, runParameters(computationManager, parameters)).join();
    }

    private static LoadFlowResult runJava(Network network, LocalComputationManager computationManager) {
        DynawoSimulationConfig config = new DynawoSimulationConfig(INSTALL, false);
        DynaFlowJavaProvider provider = new DynaFlowJavaProvider(() -> config);
        LoadFlowParameters parameters = new LoadFlowParameters()
                .setVoltageInitMode(LoadFlowParameters.VoltageInitMode.PREVIOUS_VALUES)
                .setReadSlackBus(false);
        return provider.run(network, VariantManagerConstants.INITIAL_VARIANT_ID, runParameters(computationManager, parameters)).join();
    }

    private static LoadFlowRunParameters runParameters(LocalComputationManager computationManager, LoadFlowParameters parameters) {
        return new LoadFlowRunParameters()
                .setComputationManager(computationManager)
                .setParameters(parameters)
                .setReportNode(ReportNode.NO_OP);
    }

    /** Every bus in the main component must reach the same voltage on both paths, within tolerance. */
    private static void compareBusStates(Network cppNetwork, Network javaNetwork) {
        double maxV = 0;
        double maxAngle = 0;
        String worstBus = "";
        for (Bus cppBus : cppNetwork.getBusView().getBuses()) {
            if (!cppBus.isInMainConnectedComponent()) {
                continue;
            }
            Bus javaBus = javaNetwork.getBusView().getBus(cppBus.getId());
            double vDiff = Math.abs(cppBus.getV() - javaBus.getV());
            double angleDiff = Math.abs(cppBus.getAngle() - javaBus.getAngle());
            if (vDiff > maxV) {
                maxV = vDiff;
                worstBus = cppBus.getId();
            }
            maxAngle = Math.max(maxAngle, angleDiff);
        }
        System.out.printf("DynaFlow C++ vs Java: max |ΔV| = %.4f kV (bus %s), max |Δangle| = %.4f deg%n", maxV, worstBus, maxAngle);
        assertTrue(maxV < V_TOLERANCE, "max bus voltage difference " + maxV + " kV exceeds " + V_TOLERANCE);
        assertTrue(maxAngle < ANGLE_TOLERANCE, "max bus angle difference " + maxAngle + " deg exceeds " + ANGLE_TOLERANCE);
    }
}
