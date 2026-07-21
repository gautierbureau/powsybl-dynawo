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
import com.powsybl.dynawo.extensions.api.generator.SynchronousGeneratorProperties;
import com.powsybl.dynawo.mappings.MappingConfig;
import com.powsybl.dynawo.mappings.parameters.ModelDescriptionLookup;
import com.powsybl.dynawo.mappings.preassembled.GeneratorModelDesigner;
import com.powsybl.dynawo.mappings.preassembled.ModelNaming;
import com.powsybl.dynawo.mappings.preassembled.PreassembledModel;
import com.powsybl.dynawo.mappings.tools.PreassembledModelCompiler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
     * The builder a deployment has configured, or nothing where it would rather make do with what
     * is installed, which is the default.
     * <p>
     * Naming a directory to keep them in is what turns building on, since a model has to be left
     * somewhere a simulation can be pointed at, and choosing where is the one thing that cannot
     * be guessed.
     */
    public static Optional<MissingModelBuilder> fromConfig(MappingConfig mappingConfig,
                                                           Supplier<Path> dynawoHomeDir, ModelNaming naming) {
        // the installation is asked for only where something is to be built with it, so a
        // deployment building nothing needs no Dynawo configured to say so
        return mappingConfig.getBuiltModelsDir()
                .map(dir -> new MissingModelBuilder(dynawoHomeDir.get(), dir, naming));
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
    public Optional<String> build(SynchronousGeneratorProperties properties, boolean transformer) {
        Optional<PreassembledModel> designed = designer.design(properties, transformer);
        if (designed.isEmpty()) {
            LOGGER.debug("Nothing describes a machine with governor {} and voltage regulator {}",
                    properties.getGovernor(), properties.getVoltageRegulator());
            return Optional.empty();
        }
        PreassembledModel model = designed.get();
        try {
            long start = System.nanoTime();
            compiler.compile(model, modelsDir);
            LOGGER.info("Built {} in {}, which no installed model provided ({} ms)",
                    model.getId(), modelsDir, (System.nanoTime() - start) / 1_000_000);
            builtModelConfigs.computeIfAbsent(model.getId(), lib -> config(lib, properties, transformer));
            return Optional.of(model.getId());
        } catch (PowsyblException | UncheckedIOException e) {
            LOGGER.warn("Could not build {}, falling back on an installed model: {}",
                    model.getId(), e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * How a built model is described to the simulation: its name, and the properties that say how
     * to connect it, a transformer standing between it and the grid, auxiliaries drawing from it.
     * Without these the builder would connect it as a bare machine, which a model behind a
     * transformer is not.
     */
    private ModelConfig config(String lib, SynchronousGeneratorProperties properties, boolean transformer) {
        List<String> capabilities = new ArrayList<>();
        capabilities.add(CONTROLLABLE);
        if (transformer && !properties.isInternalTransformer()) {
            capabilities.add(TRANSFORMER);
        }
        if (properties.isAuxiliaries()) {
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
     * The installed models whose availability had to be corrected, to be registered over the ones
     * the catalog holds rather than beside them. Empty where none needed correcting.
     */
    public Map<String, List<ModelConfig>> getModelConfigOverrides() {
        return versionOverrides.isEmpty() ? Map.of()
                : Map.of(GENERATOR_CATEGORY, List.copyOf(versionOverrides.values()));
    }
}
