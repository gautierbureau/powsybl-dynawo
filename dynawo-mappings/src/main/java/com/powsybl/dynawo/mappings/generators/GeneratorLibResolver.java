/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.generators;

import com.powsybl.dynawo.builders.ModelConfig;
import com.powsybl.dynawo.builders.ModelConfigsHandler;
import com.powsybl.dynawo.extensions.api.generator.SynchronousGeneratorProperties;
import com.powsybl.dynawo.mappings.controls.ControlTranslations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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

    public GeneratorLibResolver() {
        this(ControlTranslations.getInstance());
    }

    public GeneratorLibResolver(ControlTranslations controlTranslations) {
        this.controlTranslations = controlTranslations;
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
        return controlCore(properties, simplified)
                .flatMap(core -> selectLib(core, capabilities(properties, simplified, transformer)));
    }

    /**
     * Builds the control part of the library name, common to every capability variant.
     */
    private Optional<String> controlCore(SynchronousGeneratorProperties properties, boolean simplified) {
        String windings = properties.getNumberOfWindings() == SynchronousGeneratorProperties.Windings.THREE_WINDINGS
                ? THREE_WINDINGS : FOUR_WINDINGS;
        if (simplified) {
            Optional<String> fragment = controlTranslations.getSimplifiedFragment(properties.getGovernor(), properties.getVoltageRegulator());
            if (fragment.isEmpty()) {
                LOGGER.warn("No simplified model for governor {} and voltage regulator {}",
                        properties.getGovernor(), properties.getVoltageRegulator());
            }
            return fragment.map(f -> LIB_PREFIX + windings + f);
        }
        String pss = properties.getPss() != null ? properties.getPss() : "";
        return Optional.of(LIB_PREFIX + windings + properties.getGovernor() + properties.getVoltageRegulator() + pss);
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
            // the local/distant distinction is carried by getUva() and will select between the
            // regular and the external uva point categories once those models are available
            capabilities.add(GeneratorCapability.UVA);
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
