/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.tools;

import com.powsybl.commons.PowsyblException;
import org.apache.commons.lang3.SystemUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Runs the tools of a Dynawo installation the way the installation says to.
 * <p>
 * They are reached through the launcher an installation ships, which is what {@code dynawo.sh
 * jobs-help} calls its launcher options, and which is how a simulation is run too. Going through
 * it means the environment a Dynawo binary needs is the installation's business rather than ours:
 * the script sets it before handing over, so nothing here has to know about shared libraries,
 * dictionaries or where the Modelica compiler lives.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public class DynawoLauncher {

    /**
     * The launcher option that reaches the dynamic library generator.
     */
    public static final String GENERATE_PREASSEMBLED = "--generate-preassembled";

    /**
     * The launcher option that reaches the tool reading a description out of a library.
     */
    public static final String DUMP_MODEL = "--dump-model";

    private static final String LAUNCHER_ARGUMENT = "jobs";

    private final Path homeDir;

    public DynawoLauncher(Path homeDir) {
        // a tool runs from a directory of its own choosing, so a home given relative to us would
        // be read relative to it
        this.homeDir = homeDir.toAbsolutePath().normalize();
    }

    /**
     * Whether this installation offers to build a preassembled model at all.
     * <p>
     * Building is not something a deployment turns on: a machine asking for controls no installed
     * model implements is one we can compile a model for, so it gets one. What can stand in the
     * way is the installation, which is asked here rather than assumed: the launcher has to be
     * there and to carry the option that builds a model.
     * <p>
     * This says the option is offered, not that it works: an installation carrying it can still
     * fail to compile, for want of a toolchain it needs, and only trying tells. That is settled by
     * {@link com.powsybl.dynawo.mappings.generators.MissingModelBuilder}, which stops asking an
     * installation that has failed once.
     */
    public boolean canGeneratePreassembled() {
        Path script = homeDir.resolve("dynawo" + (SystemUtils.IS_OS_WINDOWS ? ".cmd" : ".sh"));
        if (!Files.exists(script)) {
            return false;
        }
        try {
            return Files.readString(script).contains(GENERATE_PREASSEMBLED);
        } catch (IOException e) {
            return false;
        }
    }

    public Path getHomeDir() {
        return homeDir;
    }

    private Path script() {
        Path script = homeDir.resolve("dynawo" + (SystemUtils.IS_OS_WINDOWS ? ".cmd" : ".sh"));
        if (!Files.exists(script)) {
            throw new PowsyblException("No Dynawo installation at " + homeDir);
        }
        return script;
    }

    /**
     * Runs a launcher option and answers what it said, failing on anything but a clean exit.
     *
     * @param workingDir where to run it, for the tools that leave their workings beside themselves
     *                   rather than only where they are told to put what they made
     */
    /**
     * Whether the distribution ships Boost of its own, which the released one does and a debug
     * build against the system's does not. Told by the Boost libraries beside its own: a bundled
     * distribution keeps {@code libboost_*} in its {@code lib}, one linking the system's keeps
     * none there.
     */
    private boolean bundlesBoost() {
        Path lib = homeDir.resolve("lib");
        if (!Files.isDirectory(lib)) {
            return false;
        }
        try (var files = Files.newDirectoryStream(lib, "libboost_*")) {
            return files.iterator().hasNext();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public String run(Path workingDir, long timeoutSeconds, String option, String... arguments) {
        List<String> command = new ArrayList<>(List.of(script().toString(), LAUNCHER_ARGUMENT, option));
        command.addAll(List.of(arguments));
        ProcessBuilder builder = new ProcessBuilder(command);
        if (workingDir != null) {
            builder.directory(workingDir.toFile());
        }
        // The script sets the rest of what its tools need; only what a distribution cannot be
        // assumed to carry is filled in, and only where nobody has chosen otherwise, so an
        // environment that knows better still wins. The script calls Python as "python", which a
        // system holding only "python3" does not have. And a distribution that ships no Boost of
        // its own was built against the system's, which lives under /usr: forcing that on one that
        // bundles Boost would send it to the wrong headers, so it is set only for the one without.
        builder.environment().putIfAbsent("DYNAWO_PYTHON_COMMAND", "python3");
        if (!bundlesBoost()) {
            builder.environment().putIfAbsent("DYNAWO_BOOST_HOME", "/usr");
        }
        builder.redirectErrorStream(true);
        try {
            Process process = builder.start();
            String output = new String(process.getInputStream().readAllBytes());
            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new PowsyblException(option + " did not answer within " + timeoutSeconds + "s");
            }
            if (process.exitValue() != 0) {
                throw new PowsyblException(option + " failed: " + output.strip());
            }
            return output;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PowsyblException("Interrupted while running " + option, e);
        }
    }
}
