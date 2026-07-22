/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.tools;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dynawo.mappings.preassembled.PreassembledModel;
import com.powsybl.dynawo.mappings.preassembled.PreassembledModelXml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Turns a model we have described into one Dynawo can run.
 * <p>
 * Compiling is what Dynawo does to the models it ships, so a model of ours goes through the same
 * tool and comes out the same kind of thing: a library, which is all a simulation reads. It is
 * left in a directory of our own, which a simulation is told to look in besides the ones Dynawo
 * ships, so nothing has to be installed into the distribution.
 * <p>
 * Compiling a model takes the better part of a minute, so a library already there is left alone.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public class PreassembledModelCompiler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreassembledModelCompiler.class);

    private static final long TIMEOUT_SECONDS = 1800;
    private static final String LIBRARY_EXTENSION = ".so";

    private final DynawoLauncher launcher;

    public PreassembledModelCompiler(Path homeDir) {
        this(new DynawoLauncher(homeDir));
    }

    public PreassembledModelCompiler(DynawoLauncher launcher) {
        this.launcher = launcher;
    }

    /**
     * Compiles a model into a directory, and answers the library that came out.
     */
    public Path compile(PreassembledModel model, Path modelsDir) {
        Path library = modelsDir.resolve(model.getId() + LIBRARY_EXTENSION);
        if (Files.exists(library)) {
            return library;
        }
        Path definition = modelsDir.resolve(model.getId() + ".xml");
        try {
            Files.createDirectories(modelsDir);
            // the tool is told what to build by a definition on disk, so the model is written out
            // where the library will land, which is also what says afterwards what was built
            Files.writeString(definition, PreassembledModelXml.toXml(model));
            return compile(definition, modelsDir);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (PowsyblException e) {
            // a definition describing a model that was never built says nothing true, so what was
            // written to ask for it goes with the attempt
            deleteQuietly(definition);
            throw e;
        }
    }

    /**
     * Compiles a definition already on disk, for a model described elsewhere.
     */
    public Path compile(Path definition, Path modelsDir) {
        String name = definition.getFileName().toString().replaceFirst("\\.xml$", "");
        Path library = modelsDir.resolve(name + LIBRARY_EXTENSION);
        if (Files.exists(library)) {
            return library;
        }
        // the tool leaves what it makes in the directory it is run from as well as the one it is
        // given, so it is run somewhere of its own, which is also where it says what it did
        Path work = modelsDir.resolve("." + name + ".work");
        try {
            Files.createDirectories(work);
            launcher.run(work, TIMEOUT_SECONDS, DynawoLauncher.GENERATE_PREASSEMBLED,
                    "--model-list", definition.toAbsolutePath().toString(),
                    "--output-dir", work.toAbsolutePath().toString());
            Path built = work.resolve(name + LIBRARY_EXTENSION);
            if (!Files.exists(built)) {
                throw new PowsyblException("generate-preassembled reported success but built no library for " + name);
            }
            Files.move(built, library);
            // swept only where it built: what the tool wrote is the whole account of why it did
            // not, and a directory cleared in a finally takes that away with it
            deleteRecursively(work);
            return library;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void deleteQuietly(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            LOGGER.warn("Left {} behind, which describes a model that was not built: {}", file, e.getMessage());
        }
    }

    private static void deleteRecursively(Path dir) {
        if (!Files.exists(dir)) {
            return;
        }
        try (var walk = Files.walk(dir)) {
            walk.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
