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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Stream;

/**
 * Turns a model we have described into one Dynawo can run.
 * <p>
 * Compiling is what Dynawo does to the models it ships, so a model of ours goes through the same
 * tool and comes out the same kind of thing: a library, which is all a simulation reads. It is
 * left in a directory of our own, which a simulation is told to look in besides the ones Dynawo
 * ships, so nothing has to be installed into the distribution.
 * <p>
 * Compiling a model takes the better part of a minute, so a library already there is left alone,
 * as long as it was built from the same definition: a control registered again with other wiring
 * describes another model under the same name, and the library built from the first is rebuilt.
 * <p>
 * A control Dynawo does not ship is built out of Modelica sources of one's own, kept under a
 * directory the tool is told to search besides Dynawo's library. The sources go into the library
 * as much as the definition does, so what they are is written into the definition too, and a
 * library built from sources since edited is rebuilt like one built from another definition.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public class PreassembledModelCompiler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreassembledModelCompiler.class);

    private static final long TIMEOUT_SECONDS = 1800;
    private static final String LIBRARY_EXTENSION = ".so";
    private static final String DEFINITION_EXTENSION = ".xml";
    private static final String DESCRIPTION_EXTENSION = ".desc.xml";
    private static final String PROBE_MODEL = "CompilerProbe";
    private static final String PROBE_DEFINITION = """
            <?xml version="1.0" encoding="UTF-8"?>
            <dyn:dynamicModelsArchitecture xmlns:dyn="http://www.rte-france.com/dynawo">
              <dyn:modelicaModel id="CompilerProbe">
                <dyn:unitDynamicModel id="load" name="Dynawo.Electrical.Loads.LoadAlphaBeta"/>
              </dyn:modelicaModel>
            </dyn:dynamicModelsArchitecture>
            """;

    private static final String MODELICA_MODELS_DIR_OPTION = "--recursive-modelica-models-dir";

    private final DynawoLauncher launcher;
    private final Path modelicaModelsDir;
    // what the Modelica sources are, read once: they are not edited while a study builds its models
    private String sourcesStamp;

    public PreassembledModelCompiler(Path homeDir) {
        this(new DynawoLauncher(homeDir));
    }

    public PreassembledModelCompiler(DynawoLauncher launcher) {
        this(launcher, null);
    }

    public PreassembledModelCompiler(Path homeDir, Path modelicaModelsDir) {
        this(new DynawoLauncher(homeDir), modelicaModelsDir);
    }

    /**
     * @param modelicaModelsDir where the Modelica sources of controls Dynawo does not ship are kept,
     *                          searched through, or null where every control is Dynawo's own
     */
    public PreassembledModelCompiler(DynawoLauncher launcher, Path modelicaModelsDir) {
        this.launcher = launcher;
        if (modelicaModelsDir != null && !Files.isDirectory(modelicaModelsDir)) {
            // the models built out of Dynawo's library alone build as well without it, and one that
            // needed it fails on its own, the class it names not found, which is said where it is
            LOGGER.warn("No Modelica models directory at {}, the models are built out of Dynawo's library alone",
                    modelicaModelsDir);
            this.modelicaModelsDir = null;
        } else {
            this.modelicaModelsDir = modelicaModelsDir == null ? null : modelicaModelsDir.toAbsolutePath().normalize();
        }
    }

    /**
     * Compiles a model into a directory, and answers the library that came out.
     */
    public Path compile(PreassembledModel model, Path modelsDir) {
        Path library = modelsDir.resolve(model.getId() + LIBRARY_EXTENSION);
        Path definition = modelsDir.resolve(model.getId() + DEFINITION_EXTENSION);
        String xml = PreassembledModelXml.toXml(model) + sourcesStamp();
        try {
            if (Files.exists(library)) {
                // a library with no definition beside it was built some other way, and nothing says
                // what it is made of, so it is taken as it is
                if (!Files.exists(definition) || Files.readString(definition).equals(xml)) {
                    return library;
                }
                LOGGER.info("Rebuilding {}, whose definition changed since its library was built", model.getId());
                // the description read out of the old library goes with it, or the parameters
                // would be completed against variables the new one may not have
                Files.delete(library);
                Files.deleteIfExists(modelsDir.resolve(model.getId() + DESCRIPTION_EXTENSION));
            }
            Files.createDirectories(modelsDir);
            // the tool is told what to build by a definition on disk, so the model is written out
            // where the library will land, which is also what says afterwards what was built
            Files.writeString(definition, xml);
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
     * Whether this installation compiles anything at all, found out by building the plainest model
     * there is, a standard load and nothing else.
     * <p>
     * A failed build says that something is wrong, not what: the definition may name a variable a
     * model does not have, or the installation may lack the toolchain to build anything. Only the
     * second is worth giving up on the rest of a network for, and a model this plain fails only
     * that way. It is built somewhere of its own and swept, so it never stands beside the models a
     * study builds.
     */
    public boolean canCompile() {
        Path probeDir = null;
        try {
            probeDir = Files.createTempDirectory("dynawo-compiler-probe");
            Path definition = probeDir.resolve(PROBE_MODEL + ".xml");
            Files.writeString(definition, PROBE_DEFINITION);
            compile(definition, probeDir);
            return true;
        } catch (PowsyblException e) {
            LOGGER.warn("The Dynawo installation at {} compiles no model, not even the plainest: {}",
                    launcher.getHomeDir(), e.getMessage());
            return false;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (probeDir != null) {
                deleteRecursively(probeDir);
            }
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
            List<String> arguments = new ArrayList<>(List.of(
                    "--model-list", definition.toAbsolutePath().toString(),
                    "--output-dir", work.toAbsolutePath().toString()));
            if (modelicaModelsDir != null) {
                arguments.addAll(List.of(MODELICA_MODELS_DIR_OPTION, modelicaModelsDir.toString()));
            }
            launcher.run(work, TIMEOUT_SECONDS, DynawoLauncher.GENERATE_PREASSEMBLED, arguments.toArray(String[]::new));
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

    /**
     * What the Modelica sources are, as a comment closing the definition: nothing where there are
     * none of our own, a digest of every model and its external variables otherwise. The tool reads
     * past it, and two definitions differing only by it describe libraries built out of different
     * sources.
     */
    private String sourcesStamp() {
        if (modelicaModelsDir == null) {
            return "";
        }
        if (sourcesStamp == null) {
            sourcesStamp = "<!-- Modelica sources sha256 " + digest(modelicaModelsDir) + " -->" + System.lineSeparator();
        }
        return sourcesStamp;
    }

    private static String digest(Path dir) {
        try (Stream<Path> files = Files.walk(dir)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            List<Path> sources = files.filter(Files::isRegularFile)
                    .filter(f -> f.toString().endsWith(".mo") || f.toString().endsWith(".extvar"))
                    .sorted()
                    .toList();
            for (Path source : sources) {
                // the name as well as the content, so a model moved or renamed is another source
                digest.update(dir.relativize(source).toString().getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
                digest.update(Files.readAllBytes(source));
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
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
