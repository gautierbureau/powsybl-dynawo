/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.generators;

import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.dynawo.DynawoSimulationConfig;
import com.powsybl.dynawo.builders.ModelConfig;
import com.powsybl.dynawo.builders.ModelConfigsHandler;
import com.powsybl.dynawo.extensions.api.generator.SynchronousGeneratorProperties;
import com.powsybl.dynawo.mappings.MappingConfig;
import com.powsybl.dynawo.mappings.controls.ControlTranslations;
import com.powsybl.dynawo.mappings.preassembled.GeneratorControls;
import com.powsybl.dynawo.mappings.preassembled.GeneratorModelDesigner;
import com.powsybl.dynawo.mappings.preassembled.ModelNaming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Resolves the Dynawo library implementing the controls and capabilities carried by a
 * {@link SynchronousGeneratorProperties} extension.
 * <p>
 * The controls stored in the extension are Dynawo model name fragments, so the control part of
 * the library name is a plain concatenation and needs no per model knowledge. The capabilities
 * (transformer, auxiliaries, rpcl, ...) are <em>not</em> composed by appending suffixes: the
 * catalog is queried instead, and the best matching model is selected among the ones actually
 * available. This keeps a single mapping working
 * <ul>
 *     <li>today, where open source has no auxiliary nor uva model: they are simply dropped,</li>
 *     <li>tomorrow, when those models reach open source: they are picked up with no code change,</li>
 *     <li>and with a jar providing more models, whose catalog is merged by
 *     {@link ModelConfigsHandler}.</li>
 * </ul>
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public class GeneratorLibResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeneratorLibResolver.class);
    private static final String LIB_PREFIX = "GeneratorSynchronous";
    private static final String THREE_WINDINGS = "ThreeWindings";
    private static final String FOUR_WINDINGS = "FourWindings";

    private final ControlTranslations controlTranslations;
    private final Supplier<MissingModelBuilder> builderSupplier;
    private MissingModelBuilder missingModelBuilder;
    private boolean builderResolved;

    /**
     * A resolver going by what is installed, and building what is not where the deployment has
     * said where to keep such a model.
     */
    public GeneratorLibResolver() {
        this(ControlTranslations.getInstance(), GeneratorLibResolver::configuredModelBuilder);
    }

    public GeneratorLibResolver(ControlTranslations controlTranslations) {
        this(controlTranslations, (MissingModelBuilder) null);
    }

    /**
     * @param missingModelBuilder builds what no installed model provides, or null to make do with
     *                            what is installed
     */
    public GeneratorLibResolver(ControlTranslations controlTranslations, MissingModelBuilder missingModelBuilder) {
        this(controlTranslations, () -> missingModelBuilder);
    }

    /**
     * @param builderSupplier what to build missing models with, asked for the first time one is
     *                        wanted rather than here
     */
    public GeneratorLibResolver(ControlTranslations controlTranslations,
                                Supplier<MissingModelBuilder> builderSupplier) {
        this.controlTranslations = controlTranslations;
        this.builderSupplier = Objects.requireNonNull(builderSupplier);
    }

    /**
     * What builds the missing models, worked out the first time one is wanted.
     * <p>
     * Deliberately not resolved in the constructor. A registered mapping is constructed by the
     * {@link java.util.ServiceLoader} held in a static field, and a native image may evaluate that
     * static initialisation when the image is built rather than when it runs. Reading the
     * deployment's configuration there would bake in the configuration of the machine that built
     * the image, which is how a mapping running under pypowsybl came to have no builder whatever
     * the deployment configured. Asking for it on the way past reads the configuration of the
     * machine actually running.
     */
    private MissingModelBuilder builder() {
        if (!builderResolved) {
            missingModelBuilder = builderSupplier.get();
            builderResolved = true;
        }
        return missingModelBuilder;
    }

    /**
     * What the deployment has configured to build missing models, or null where it has configured
     * nothing, which is the default.
     * <p>
     * Read once here rather than asked of every caller: whether a model happens to be installed is
     * not something a mapping's caller should have to know about, so neither is what to do when it
     * is not.
     */
    private static MissingModelBuilder configuredModelBuilder() {
        return configuredHomeDir()
                .flatMap(homeDir -> MissingModelBuilder.fromConfig(MappingConfig.load(), () -> homeDir,
                        ModelNaming.DYNAWO_1_7_0))
                .orElse(null);
    }

    /**
     * The installation to build with, or nothing where the deployment configured none: a mapping
     * is usable without Dynawo, to see which models a network would be given, and asking for an
     * installation that is not there to answer that would refuse the question.
     */
    private static Optional<Path> configuredHomeDir() {
        try {
            return Optional.of(DynawoSimulationConfig.load().getHomeDir());
        } catch (PowsyblException e) {
            LOGGER.debug("No Dynawo installation configured, no model will be built: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * What builds the models nothing installed provides, where anything does.
     */
    public Optional<MissingModelBuilder> getMissingModelBuilder() {
        return Optional.ofNullable(builder());
    }

    /**
     * Resolves the library for the given generator properties.
     *
     * @param properties  the controls and capabilities of the generator
     * @param simplified  when {@code true} the detailed controls are translated to their
     *                    simplified counterpart (DynaWaltz), otherwise they are used as is
     *                    (DynaSwing)
     * @param transformer whether a generator transformer is wanted, which the properties alone
     *                    cannot tell since it depends on the voltage level the generator sits on
     */
    public Optional<String> resolve(SynchronousGeneratorProperties properties, boolean simplified, boolean transformer) {
        return resolve(properties, simplified, transformer, ReportNode.NO_OP, null);
    }

    /**
     * @param reportNode  where the model settled on is said, and what the machine asked for and did
     *                    not get, which is the only place a study is read from afterwards
     * @param generatorId the machine being resolved, to name it in the report
     */
    public Optional<String> resolve(SynchronousGeneratorProperties properties, boolean simplified,
                                    boolean transformer, ReportNode reportNode, String generatorId) {
        GeneratorControls controls = controls(properties, simplified);
        String core = controlCore(controls, properties);
        Set<GeneratorCapability> wanted = capabilities(properties, simplified, transformer);
        Optional<String> installed = selectLib(core, wanted);
        // an installed model providing everything asked for is the model asked for, and nothing
        // is built for a generator the catalog already answers
        if (installed.isPresent() && nothingDropped(core, wanted)) {
            installed.ifPresent(this::useInstalled);
            installed.ifPresent(lib -> GeneratorMappingReports.reportModelSelected(reportNode, generatorId, lib));
            return installed;
        }
        MissingModelBuilder modelBuilder = builder();
        if (modelBuilder != null) {
            Optional<String> built = modelBuilder.build(controls, properties.getNumberOfWindings(),
                    transformer && !properties.isInternalTransformer(), properties.isAuxiliaries());
            if (built.isPresent()) {
                built.ifPresent(lib -> GeneratorMappingReports.reportModelBuilt(reportNode, generatorId, lib));
                return built;
            }
        }
        installed.ifPresent(this::useInstalled);
        installed.ifPresentOrElse(
                lib -> GeneratorMappingReports.reportCapabilitiesDropped(reportNode, generatorId, lib,
                        dropped(core, wanted), modelBuilder != null),
                () -> GeneratorMappingReports.reportNoModel(reportNode, generatorId, core));
        return installed;
    }

    /**
     * What the machine asked for that the chosen model does not provide, which is what the report
     * has to name for the difference to be actionable.
     */
    private static Set<GeneratorCapability> dropped(String controlCore, Set<GeneratorCapability> wanted) {
        Set<GeneratorCapability> dropped = EnumSet.noneOf(GeneratorCapability.class);
        dropped.addAll(wanted);
        ModelConfigsHandler.getInstance().getModelConfigStream()
                .filter(mc -> mc.lib().startsWith(controlCore))
                .filter(mc -> providedCapabilities(mc).stream().allMatch(wanted::contains))
                .map(GeneratorLibResolver::providedCapabilities)
                .max(Comparator.comparingInt(Set::size))
                .ifPresent(dropped::removeAll);
        return dropped;
    }

    /**
     * Where a model comes from the catalog rather than being built, lets the builder note it, in
     * case the catalog dates it later than the installation can run it.
     */
    private void useInstalled(String lib) {
        MissingModelBuilder modelBuilder = builder();
        if (modelBuilder != null) {
            modelBuilder.useInstalled(lib);
        }
    }

    /**
     * Whether the catalog answers the wanted capabilities in full, the model it offers otherwise
     * being a near miss rather than the model asked for.
     */
    private static boolean nothingDropped(String controlCore, Set<GeneratorCapability> wanted) {
        return ModelConfigsHandler.getInstance().getModelConfigStream()
                .filter(mc -> mc.lib().startsWith(controlCore))
                .anyMatch(mc -> providedCapabilities(mc).equals(wanted));
    }

    /**
     * The controls the study is to run, which is where a simplified study turns the detailed ones
     * a machine carries into the simplified regulations standing for them. Everything downstream,
     * the name looked up and any model built, works from these and no longer from what the
     * extension happens to hold.
     */
    private GeneratorControls controls(SynchronousGeneratorProperties properties, boolean simplified) {
        GeneratorControls carried = new GeneratorControls(properties.getGovernor(),
                properties.getVoltageRegulator(), properties.getPss());
        return simplified ? controlTranslations.simplify(carried) : carried;
    }

    /**
     * Builds the control part of the library name, common to every capability variant, naming the
     * controls the way the model carrying them is named.
     */
    private static String controlCore(GeneratorControls controls, SynchronousGeneratorProperties properties) {
        return GeneratorModelDesigner.name(controls, properties.getNumberOfWindings(), false, false);
    }

    private static Set<GeneratorCapability> capabilities(SynchronousGeneratorProperties properties, boolean simplified, boolean transformer) {
        Set<GeneratorCapability> capabilities = EnumSet.noneOf(GeneratorCapability.class);
        if (transformer && !properties.isInternalTransformer()) {
            capabilities.add(GeneratorCapability.TRANSFORMER);
        }
        if (properties.isAuxiliaries()) {
            capabilities.add(GeneratorCapability.AUXILIARY);
        }
        // rpcl, qlim and uva only exist on the simplified models
        if (simplified) {
            if (properties.isRpcl1()) {
                capabilities.add(GeneratorCapability.RPCL);
            }
            if (properties.isRpcl2()) {
                capabilities.add(GeneratorCapability.RPCL2);
            }
            if (properties.isQlim()) {
                capabilities.add(GeneratorCapability.QLIM);
            }
            // uva is not asked for here. No open source model carries one, so asking would drop
            // it again for every machine, and a mapping that means to have them is the one to ask:
            // the local and distant distinction the extension carries selects between the regular
            // and the external uva point categories, which is a matter for whoever holds them.
        }
        return capabilities;
    }

    /**
     * Selects, among the models implementing the given controls, the one providing as many of the
     * wanted capabilities as possible without providing any unwanted one. Falls back on the model
     * without capability when none matches better.
     */
    private static Optional<String> selectLib(String controlCore, Set<GeneratorCapability> wanted) {
        List<ModelConfig> candidates = ModelConfigsHandler.getInstance().getModelConfigStream()
                .filter(mc -> mc.lib().startsWith(controlCore))
                .filter(mc -> providedCapabilities(mc).stream().allMatch(wanted::contains))
                .toList();
        if (candidates.isEmpty()) {
            LOGGER.warn("No model found for controls {}", controlCore);
            return Optional.empty();
        }
        ModelConfig best = candidates.stream()
                .max(Comparator.comparingInt((ModelConfig mc) -> providedCapabilities(mc).size())
                        // shortest name first so that a model does not win on an unrelated suffix
                        .thenComparing(mc -> mc.lib().length(), Comparator.reverseOrder()))
                .orElseThrow();
        Set<GeneratorCapability> dropped = EnumSet.noneOf(GeneratorCapability.class);
        dropped.addAll(wanted);
        dropped.removeAll(providedCapabilities(best));
        if (!dropped.isEmpty()) {
            LOGGER.info("No model providing {} for controls {}, {} used instead", dropped, controlCore, best.lib());
        }
        return Optional.of(best.name());
    }

    private static Set<GeneratorCapability> providedCapabilities(ModelConfig modelConfig) {
        Set<GeneratorCapability> provided = EnumSet.noneOf(GeneratorCapability.class);
        for (GeneratorCapability capability : GeneratorCapability.values()) {
            if (capability.isProvidedBy(modelConfig)) {
                provided.add(capability);
            }
        }
        return provided;
    }
}
