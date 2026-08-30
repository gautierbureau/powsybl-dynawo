/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.generators;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dynawo.builders.ModelConfig;
import com.powsybl.dynawo.builders.ModelConfigsHandler;
import com.powsybl.dynawo.builders.VersionInterval;
import com.powsybl.dynawo.extensions.api.generator.RpclType;
import com.powsybl.dynawo.extensions.api.generator.SynchronousGeneratorProperties.Windings;
import com.powsybl.dynawo.mappings.MappingConfig;
import com.powsybl.dynawo.mappings.parameters.ModelDescriptionLookup;
import com.powsybl.dynawo.mappings.preassembled.GeneratorControls;
import com.powsybl.dynawo.mappings.preassembled.GeneratorModelDesigner;
import com.powsybl.dynawo.mappings.preassembled.ModelNaming;
import com.powsybl.dynawo.mappings.preassembled.PreassembledModel;
import com.powsybl.dynawo.mappings.tools.DynawoLauncher;
import com.powsybl.dynawo.mappings.tools.PreassembledModelCompiler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Builds the model a generator asks for when nothing holds one.
 * <p>
 * Nobody asks for this: a mapping says which controls a generator has, and whether a model for
 * them happens to exist is not something the person running a simulation should have to know.
 * So it happens on the way past, and what it did is said in the log rather than asked about.
 * <p>
 * A model that will not build is not an error either. The mapping carries on with whichever
 * catalogued model came closest, which is what it did before anything could be built, so a
 * failure here costs what was already being paid rather than the whole run.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public class MissingModelBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(MissingModelBuilder.class);

    /**
     * The category a built generator is registered under, the one its builder is declared for.
     */
    static final String GENERATOR_CATEGORY = "SYNCHRONOUS_GENERATOR";
    private static final String LIBRARY_EXTENSION = ".so";
    private static final String CONTROLLABLE = "CONTROLLABLE";
    private static final String TRANSFORMER = "TRANSFORMER";
    private static final String AUXILIARY = "AUXILIARY";

    private final GeneratorModelDesigner designer;
    private final PreassembledModelCompiler compiler;
    private final Path modelsDir;
    private final Path dynawoHomeDir;
    // what was built, so the simulation can be told the model exists and how to connect it
    private final Map<String, ModelConfig> builtModelConfigs = new LinkedHashMap<>();
    // installed models the catalog dates later than they run, corrected to their true availability
    private final Map<String, ModelConfig> versionOverrides = new LinkedHashMap<>();
    // set once an installation has shown it cannot build, so the rest of the network does not pay
    // half a minute each to be told the same thing
    private boolean installationCannotBuild;
    // why the last model asked for was not built, to be said where anyone will read it
    private String lastFailure;

    public MissingModelBuilder(Path dynawoHomeDir, Path modelsDir, ModelNaming naming) {
        this(new GeneratorModelDesigner(naming), new PreassembledModelCompiler(dynawoHomeDir), modelsDir,
                dynawoHomeDir);
    }

    public MissingModelBuilder(GeneratorModelDesigner designer, PreassembledModelCompiler compiler, Path modelsDir) {
        this(designer, compiler, modelsDir, null);
    }

    public MissingModelBuilder(GeneratorModelDesigner designer, PreassembledModelCompiler compiler, Path modelsDir,
                               Path dynawoHomeDir) {
        this.designer = designer;
        this.compiler = compiler;
        this.modelsDir = modelsDir;
        this.dynawoHomeDir = dynawoHomeDir;
    }

    /**
     * Where the parameters a model built here expects are read from, which is out of the library
     * itself since nothing else describes it, falling back on what Dynawo ships for the rest.
     */
    public ModelDescriptionLookup describe(ModelDescriptionLookup installed) {
        return dynawoHomeDir == null ? installed
                : ModelDescriptionLookup.fromCompiledModels(modelsDir, dynawoHomeDir).orElse(installed);
    }

    /**
     * The builder for this deployment, which every deployment has: a model a machine asks for and
     * nothing installed provides is one we can compile, so the only question a deployment answers
     * is where such a model is kept, see {@link MappingConfig#getOrCreateBuiltModelsDir()}.
     */
    public static Optional<MissingModelBuilder> fromConfig(MappingConfig mappingConfig,
                                                           Supplier<Path> dynawoHomeDir, ModelNaming naming) {
        Path homeDir = dynawoHomeDir.get();
        if (!new DynawoLauncher(homeDir).canGeneratePreassembled()) {
            LOGGER.info("The Dynawo installation at {} cannot generate preassembled models, "
                    + "a machine no installed model suits will get the nearest one instead", homeDir);
            return Optional.empty();
        }
        return Optional.of(new MissingModelBuilder(homeDir, mappingConfig.getOrCreateBuiltModelsDir(), naming));
    }

    /**
     * Where the models built this way are left, which is the directory a simulation is told to
     * look in besides the ones Dynawo ships.
     */
    public Path getModelsDir() {
        return modelsDir;
    }

    /**
     * The model for those properties, built if it is not there already, or nothing if it cannot
     * be.
     */
    public Optional<String> build(GeneratorControls controls, Windings windings, boolean transformer,
                                  boolean auxiliaries) {
        return build(controls, windings, transformer, auxiliaries, false, RpclType.NONE);
    }

    /**
     * The model for those properties, with the reactive limits and reactive power control loop a
     * voltage stability study asks for, built if it is not there already.
     */
    public Optional<String> build(GeneratorControls controls, Windings windings, boolean transformer,
                                  boolean auxiliaries, boolean qlim, RpclType rpcl) {
        if (installationCannotBuild) {
            return Optional.empty();
        }
        lastFailure = null;
        Optional<PreassembledModel> designed = designer.design(controls, windings, transformer, auxiliaries, qlim, rpcl);
        if (designed.isEmpty()) {
            lastFailure = "nothing describes a machine with governor " + controls.governor()
                    + " and voltage regulator " + controls.voltageRegulator();
            LOGGER.debug("Not building a model: {}", lastFailure);
            return Optional.empty();
        }
        PreassembledModel model = designed.get();
        try {
            long start = System.nanoTime();
            Files.createDirectories(modelsDir);
            compiler.compile(model, modelsDir);
            LOGGER.info("Built {} in {}, which no installed model provided ({} ms)",
                    model.getId(), modelsDir, (System.nanoTime() - start) / 1_000_000);
            builtModelConfigs.computeIfAbsent(model.getId(), lib -> config(lib, transformer, auxiliaries));
            return Optional.of(model.getId());
        } catch (PowsyblException | UncheckedIOException | IOException e) {
            // whether the option is there is one thing, whether it works is another: an
            // installation can carry generate-preassembled and still not compile, for want of a
            // toolchain it needs. Which it is only shows on trying, so the first machine to try
            // settles it for the rest, and the study carries on with the models that are there
            installationCannotBuild = true;
            lastFailure = e.getMessage();
            LOGGER.warn("Could not build {} with the Dynawo installation at {}, falling back on the "
                    + "installed models for this network: {}", model.getId(), dynawoHomeDir, lastFailure);
            return Optional.empty();
        }
    }

    /**
     * How a built model is described to the simulation: its name, and the properties that say how
     * to connect it, a transformer standing between it and the grid, auxiliaries drawing from it.
     * Without these the builder would connect it as a bare machine, which a model behind a
     * transformer is not.
     */
    private ModelConfig config(String lib, boolean transformer, boolean auxiliaries) {
        List<String> capabilities = new ArrayList<>();
        capabilities.add(CONTROLLABLE);
        if (transformer) {
            capabilities.add(TRANSFORMER);
        }
        if (auxiliaries) {
            capabilities.add(AUXILIARY);
        }
        return new ModelConfig(lib, capabilities);
    }

    /**
     * The models built here, under the category their builder is declared for, so a simulation
     * can be told they exist and stand them up like any model Dynawo ships. Empty until something
     * is built.
     */
    public Map<String, List<ModelConfig>> getBuiltModelConfigs() {
        return builtModelConfigs.isEmpty() ? Map.of()
                : Map.of(GENERATOR_CATEGORY, List.copyOf(builtModelConfigs.values()));
    }

    /**
     * Notes that an installed model is used, in case the catalog dates it later than the
     * installation can actually run it.
     * <p>
     * A model whose library sits in the database runs there whatever version the catalog first
     * shipped it in, so where the catalog holds it out for being too new its configuration is
     * corrected to say it is available from as far back as any model is. Only a model the catalog
     * dates above the floor is touched, and only if its library is there to load, so a model that
     * needs no correction gets none.
     */
    public void useInstalled(String lib) {
        if (dynawoHomeDir == null || builtModelConfigs.containsKey(lib) || versionOverrides.containsKey(lib)) {
            return;
        }
        if (!Files.exists(dynawoHomeDir.resolve("ddb").resolve(lib + LIBRARY_EXTENSION))) {
            return;
        }
        ModelConfigsHandler.getInstance().findModelConfig(lib)
                .filter(config -> config.version().min().compareTo(VersionInterval.MODEL_DEFAULT_MIN_VERSION) > 0)
                .ifPresent(config -> versionOverrides.put(lib, config.availableFromDefaultVersion()));
    }

    /**
     * Why the last model asked for was not built, where one was not, which is the only account of
     * it outside the log: a native image has none, and a study is read from its report.
     */
    public Optional<String> getLastFailure() {
        return Optional.ofNullable(lastFailure);
    }

    /**
     * The installed models whose availability had to be corrected, to be registered over the ones
     * the catalog holds rather than beside them. Empty where none needed correcting.
     */
    public Map<String, List<ModelConfig>> getModelConfigOverrides() {
        return versionOverrides.isEmpty() ? Map.of()
                : Map.of(GENERATOR_CATEGORY, List.copyOf(versionOverrides.values()));
    }
}
