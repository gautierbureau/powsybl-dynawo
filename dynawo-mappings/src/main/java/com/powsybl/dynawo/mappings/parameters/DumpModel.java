/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.parameters;

import com.powsybl.commons.PowsyblException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Asks Dynawo what a compiled model expects.
 * <p>
 * A model database holds a description beside every library, but a model we have just compiled has
 * none: the description is not something the library carries, it is something the tool reads out
 * of it. This is the same step the Dynawo build takes after compiling a model, so a model we
 * generate ends up described exactly like one that was shipped.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public final class DumpModel {

    private static final long TIMEOUT_SECONDS = 60;

    private final Path homeDir;

    public DumpModel(Path homeDir) {
        this.homeDir = homeDir;
    }

    /**
     * Writes the description of a compiled model beside it, and answers where it went.
     * <p>
     * A description already there is left alone: reading a library is the slow part, and the
     * description of a library that has not changed cannot have.
     */
    public Path describe(Path library) {
        Path description = library.resolveSibling(libName(library) + ".desc.xml");
        if (Files.exists(description)) {
            return description;
        }
        if (!Files.exists(library)) {
            throw new PowsyblException("No model library at " + library);
        }
        run(library, description);
        if (!Files.exists(description)) {
            throw new PowsyblException("dumpModel reported success but wrote no description for " + library);
        }
        return description;
    }

    private void run(Path library, Path description) {
        Path dumpModel = homeDir.resolve("sbin").resolve("dumpModel");
        if (!Files.exists(dumpModel)) {
            throw new PowsyblException("No dumpModel in the Dynawo installation at " + homeDir);
        }
        ProcessBuilder builder = new ProcessBuilder(dumpModel.toString(),
                "-m", library.toString(), "-o", description.toString());
        // the tool is a Dynawo binary like any other, so it wants the libraries of its own
        // installation ahead of whatever the caller happens to have
        Map<String, String> env = builder.environment();
        String libDir = homeDir.resolve("lib").toString();
        env.merge("LD_LIBRARY_PATH", libDir, (existing, added) -> added + ":" + existing);
        env.putIfAbsent("DYNAWO_INSTALL_DIR", homeDir.toString());
        env.putIfAbsent("DYNAWO_RESOURCES_DIR", homeDir.resolve("share").toString());
        env.putIfAbsent("DYNAWO_DICTIONARIES", "dictionaries_mapping");
        env.putIfAbsent("DYNAWO_LOCALE", "en_GB");
        builder.redirectErrorStream(true);

        try {
            Process process = builder.start();
            String output = new String(process.getInputStream().readAllBytes());
            if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new PowsyblException("dumpModel did not answer within " + TIMEOUT_SECONDS
                        + "s for " + library);
            }
            if (process.exitValue() != 0) {
                throw new PowsyblException("dumpModel failed on " + library + ": " + output.strip());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PowsyblException("Interrupted while describing " + library, e);
        }
    }

    /**
     * The name a library goes by, which is the name of the model it holds.
     */
    static String libName(Path library) {
        String fileName = library.getFileName().toString();
        int extension = fileName.lastIndexOf('.');
        return extension < 0 ? fileName : fileName.substring(0, extension);
    }
}
