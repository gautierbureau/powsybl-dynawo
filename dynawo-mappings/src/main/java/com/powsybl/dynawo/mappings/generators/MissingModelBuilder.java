/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.generators;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dynawo.extensions.api.generator.SynchronousGeneratorProperties;
import com.powsybl.dynawo.mappings.preassembled.GeneratorModelDesigner;
import com.powsybl.dynawo.mappings.preassembled.ModelNaming;
import com.powsybl.dynawo.mappings.preassembled.PreassembledModel;
import com.powsybl.dynawo.mappings.tools.PreassembledModelCompiler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Optional;

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

    private final GeneratorModelDesigner designer;
    private final PreassembledModelCompiler compiler;
    private final Path modelsDir;

    public MissingModelBuilder(Path dynawoHomeDir, Path modelsDir, ModelNaming naming) {
        this(new GeneratorModelDesigner(naming), new PreassembledModelCompiler(dynawoHomeDir), modelsDir);
    }

    public MissingModelBuilder(GeneratorModelDesigner designer, PreassembledModelCompiler compiler, Path modelsDir) {
        this.designer = designer;
        this.compiler = compiler;
        this.modelsDir = modelsDir;
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
            return Optional.of(model.getId());
        } catch (PowsyblException | UncheckedIOException e) {
            LOGGER.warn("Could not build {}, falling back on an installed model: {}",
                    model.getId(), e.getMessage());
            return Optional.empty();
        }
    }
}
