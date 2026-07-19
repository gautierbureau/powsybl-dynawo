/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings;

import com.powsybl.dynawo.builders.ModelConfigsHandler;
import com.powsybl.dynawo.extensions.api.generator.SynchronousGeneratorProperties;
import com.powsybl.dynawo.mappings.generators.GeneratorCapability;
import com.powsybl.dynawo.mappings.generators.GeneratorFilters;
import com.powsybl.dynawo.mappings.generators.GeneratorLibResolver;
import com.powsybl.dynawo.mappings.generators.IidmSynchronousGeneratorPropertiesProvider;
import com.powsybl.dynawo.mappings.generators.SynchronousGeneratorPropertiesProvider;
import com.powsybl.dynawo.mappings.parameters.ModelDescriptionLookup;
import com.powsybl.dynawo.mappings.parameters.SynchronousGeneratorParametersGenerator;
import com.powsybl.dynawo.parameters.ParametersSet;
import com.powsybl.dynawo.suppliers.Property;
import com.powsybl.dynawo.suppliers.PropertyType;
import com.powsybl.dynawo.suppliers.dynamicmodels.DynamicModelConfig;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Maps every synchronous generator of a network onto the dynamic model implementing its controls,
 * in a simplified (DynaWaltz) or detailed (DynaSwing) flavour.
 * <p>
 * Both flavours share the very same extensions and the very same algorithm: the extensions carry
 * the detailed controls, and the simplified flavour deduces its models from them. Which models are
 * actually reachable depends on the catalogs present on the classpath, see
 * {@link GeneratorLibResolver}.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public class UniversalSynchronousGeneratorMapping implements DynamicModelsMapping {

    public static final String DYNAWALTZ_NAME = "UniversalDynaWaltz";
    public static final String DYNASWING_NAME = "UniversalDynaSwing";

    private static final Logger LOGGER = LoggerFactory.getLogger(UniversalSynchronousGeneratorMapping.class);
    private static final String STATIC_ID = "staticId";

    private final String name;
    private final boolean simplified;
    private final double tsoVoltageMin;
    private final SynchronousGeneratorPropertiesProvider propertiesProvider;
    private final GeneratorLibResolver libResolver;
    private final SynchronousGeneratorParametersGenerator parametersGenerator;

    public UniversalSynchronousGeneratorMapping(String name, boolean simplified, double tsoVoltageMin,
                                                SynchronousGeneratorPropertiesProvider propertiesProvider,
                                                GeneratorLibResolver libResolver) {
        this(name, simplified, tsoVoltageMin, propertiesProvider, libResolver, new SynchronousGeneratorParametersGenerator());
    }

    public UniversalSynchronousGeneratorMapping(String name, boolean simplified, double tsoVoltageMin,
                                                SynchronousGeneratorPropertiesProvider propertiesProvider,
                                                GeneratorLibResolver libResolver,
                                                SynchronousGeneratorParametersGenerator parametersGenerator) {
        this.name = Objects.requireNonNull(name);
        this.simplified = simplified;
        this.tsoVoltageMin = tsoVoltageMin;
        this.propertiesProvider = Objects.requireNonNull(propertiesProvider);
        this.libResolver = Objects.requireNonNull(libResolver);
        this.parametersGenerator = Objects.requireNonNull(parametersGenerator);
    }

    public static UniversalSynchronousGeneratorMapping dynaWaltz() {
        return dynaWaltz(IidmSynchronousGeneratorPropertiesProvider.DEFAULT_TSO_VOLTAGE_MIN);
    }

    public static UniversalSynchronousGeneratorMapping dynaWaltz(double tsoVoltageMin) {
        return dynaWaltz(tsoVoltageMin, GeneratorFilters.connected());
    }

    /**
     * @param filter which machines the mapping describes, see {@link GeneratorFilters}
     */
    public static UniversalSynchronousGeneratorMapping dynaWaltz(double tsoVoltageMin, Predicate<Generator> filter) {
        return new UniversalSynchronousGeneratorMapping(DYNAWALTZ_NAME, true, tsoVoltageMin,
                new IidmSynchronousGeneratorPropertiesProvider(tsoVoltageMin, filter), new GeneratorLibResolver());
    }

    public static UniversalSynchronousGeneratorMapping dynaSwing() {
        return dynaSwing(IidmSynchronousGeneratorPropertiesProvider.DEFAULT_TSO_VOLTAGE_MIN);
    }

    public static UniversalSynchronousGeneratorMapping dynaSwing(double tsoVoltageMin) {
        return dynaSwing(tsoVoltageMin, GeneratorFilters.connected());
    }

    /**
     * @param filter which machines the mapping describes, see {@link GeneratorFilters}
     */
    public static UniversalSynchronousGeneratorMapping dynaSwing(double tsoVoltageMin, Predicate<Generator> filter) {
        return new UniversalSynchronousGeneratorMapping(DYNASWING_NAME, false, tsoVoltageMin,
                new IidmSynchronousGeneratorPropertiesProvider(tsoVoltageMin, filter), new GeneratorLibResolver());
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void createExtensions(Network network) {
        propertiesProvider.createExtensions(network);
    }

    @Override
    public List<DynamicModelConfig> createModelConfigs(Network network) {
        return mappedGenerators(network)
                .map(mapped -> new DynamicModelConfig(mapped.lib(), mapped.setId(),
                        List.of(new Property(STATIC_ID, mapped.generator().getId(), PropertyType.STRING.getPropertyClass()))))
                .toList();
    }

    @Override
    public List<ParametersSet> createParameters(Network network, ModelDescriptionLookup descriptions) {
        List<ParametersSet> sets = new ArrayList<>();
        mappedGenerators(network).forEach(mapped -> descriptions.find(mapped.lib())
                .ifPresentOrElse(
                        description -> sets.add(parametersGenerator.generate(mapped.setId(), description,
                                mapped.generator(), mapped.hasTransformer())),
                        () -> LOGGER.warn("No description found for model {}, no parameter set generated for generator {}",
                                mapped.lib(), mapped.generator().getId())));
        return sets;
    }

    /**
     * Resolves the model of every generator the mapping covers, so that models and parameters are
     * built from the very same resolution.
     */
    private Stream<MappedGenerator> mappedGenerators(Network network) {
        return network.getGeneratorStream()
                .map(this::resolve)
                .filter(Objects::nonNull);
    }

    private MappedGenerator resolve(Generator generator) {
        SynchronousGeneratorProperties properties = generator.getExtension(SynchronousGeneratorProperties.class);
        if (properties == null) {
            return null;
        }
        boolean transformerWanted = generator.getTerminal().getVoltageLevel().getNominalV() >= tsoVoltageMin;
        return libResolver.resolve(properties, simplified, transformerWanted)
                .map(lib -> new MappedGenerator(generator, lib, getParameterSetId(generator), modelHasTransformer(lib)))
                .orElseGet(() -> {
                    LOGGER.warn("No model found for generator {}, it will not be mapped", generator.getId());
                    return null;
                });
    }

    /**
     * Whether the selected model represents the generator transformer, which the wanted capability
     * does not tell since the catalog may not provide it.
     */
    private static boolean modelHasTransformer(String lib) {
        return ModelConfigsHandler.getInstance().findModelConfig(lib)
                .filter(GeneratorCapability.TRANSFORMER::isProvidedBy)
                .isPresent();
    }

    private record MappedGenerator(Generator generator, String lib, String setId, boolean hasTransformer) {
    }

    /**
     * One parameter set per generator, since the sets are generated from the characteristics of
     * each machine.
     */
    private String getParameterSetId(Generator generator) {
        return (simplified ? "DynaWaltz_" : "DynaSwing_") + generator.getId();
    }
}
