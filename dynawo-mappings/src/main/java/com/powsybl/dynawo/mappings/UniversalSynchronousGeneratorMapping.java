/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings;

import com.powsybl.dynawo.extensions.api.generator.SynchronousGeneratorProperties;
import com.powsybl.dynawo.mappings.generators.GeneratorLibResolver;
import com.powsybl.dynawo.mappings.generators.IidmSynchronousGeneratorPropertiesProvider;
import com.powsybl.dynawo.mappings.generators.SynchronousGeneratorPropertiesProvider;
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

    public UniversalSynchronousGeneratorMapping(String name, boolean simplified, double tsoVoltageMin,
                                                SynchronousGeneratorPropertiesProvider propertiesProvider,
                                                GeneratorLibResolver libResolver) {
        this.name = Objects.requireNonNull(name);
        this.simplified = simplified;
        this.tsoVoltageMin = tsoVoltageMin;
        this.propertiesProvider = Objects.requireNonNull(propertiesProvider);
        this.libResolver = Objects.requireNonNull(libResolver);
    }

    public static UniversalSynchronousGeneratorMapping dynaWaltz() {
        return dynaWaltz(IidmSynchronousGeneratorPropertiesProvider.DEFAULT_TSO_VOLTAGE_MIN);
    }

    public static UniversalSynchronousGeneratorMapping dynaWaltz(double tsoVoltageMin) {
        return new UniversalSynchronousGeneratorMapping(DYNAWALTZ_NAME, true, tsoVoltageMin,
                new IidmSynchronousGeneratorPropertiesProvider(tsoVoltageMin), new GeneratorLibResolver());
    }

    public static UniversalSynchronousGeneratorMapping dynaSwing() {
        return dynaSwing(IidmSynchronousGeneratorPropertiesProvider.DEFAULT_TSO_VOLTAGE_MIN);
    }

    public static UniversalSynchronousGeneratorMapping dynaSwing(double tsoVoltageMin) {
        return new UniversalSynchronousGeneratorMapping(DYNASWING_NAME, false, tsoVoltageMin,
                new IidmSynchronousGeneratorPropertiesProvider(tsoVoltageMin), new GeneratorLibResolver());
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
        List<DynamicModelConfig> configs = new ArrayList<>();
        for (Generator generator : network.getGenerators()) {
            SynchronousGeneratorProperties properties = generator.getExtension(SynchronousGeneratorProperties.class);
            if (properties == null) {
                continue;
            }
            boolean transformer = generator.getTerminal().getVoltageLevel().getNominalV() >= tsoVoltageMin;
            libResolver.resolve(properties, simplified, transformer)
                    .ifPresentOrElse(lib -> configs.add(createModelConfig(generator, lib)),
                            () -> LOGGER.warn("No model found for generator {}, it will not be mapped", generator.getId()));
        }
        return configs;
    }

    private DynamicModelConfig createModelConfig(Generator generator, String lib) {
        return new DynamicModelConfig(lib, getParameterSetId(generator),
                List.of(new Property(STATIC_ID, generator.getId(), PropertyType.STRING.getPropertyClass())));
    }

    /**
     * One parameter set per generator, since the sets are generated from the characteristics of
     * each machine.
     */
    private String getParameterSetId(Generator generator) {
        return (simplified ? "DynaWaltz_" : "DynaSwing_") + generator.getId();
    }
}
