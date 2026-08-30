/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.dynaflow;

import com.google.auto.service.AutoService;
import com.powsybl.dynawo.extensions.api.generator.SynchronizedGeneratorProperties;
import com.powsybl.dynawo.extensions.api.generator.SynchronizedGeneratorPropertiesAdder;
import com.powsybl.dynawo.mappings.DynamicMappingExtensionsProvider;
import com.powsybl.dynawo.mappings.MappingParameters;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Names, from a CSV resource, the machines carrying a second reactive power control loop, writing the
 * {@code synchronizedGeneratorProperties} extension the DynaFlow generator tree reads to tell an {@code
 * Rpcl2} machine apart from a plain {@code Rpcl} one.
 * <p>
 * A machine in a secondary voltage control zone runs a control-loop model; one named here runs the
 * second-loop variant. The database is a plain list of ids, every machine it names being {@code Rpcl2}; a
 * machine it does not name is left at the plain loop. DynaFlow reads only the loop, so the extension's
 * {@code type}, which it needs, is set to {@code PV}. A study points at its own CSV with the {@code
 * rpcl2_generators_resource} parameter, there being no network-agnostic default.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
@AutoService(DynamicMappingExtensionsProvider.class)
public class DynaFlowRpcl2Provider implements DynamicMappingExtensionsProvider {

    public static final String NAME = "DynaFlowRpcl2Generators";

    static final String RESOURCE_PARAMETER = "rpcl2_generators_resource";
    private static final String PV_TYPE = "PV";

    private static final Logger LOGGER = LoggerFactory.getLogger(DynaFlowRpcl2Provider.class);

    private final String resource;

    public DynaFlowRpcl2Provider() {
        this(null);
    }

    private DynaFlowRpcl2Provider(String resource) {
        this.resource = resource;
    }

    @Override
    public String getExtensionName() {
        return SynchronizedGeneratorProperties.NAME;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "Names the Rpcl2 machines read from the CSV resource a study points at.";
    }

    @Override
    public DynamicMappingExtensionsProvider configured(MappingParameters parameters) {
        return new DynaFlowRpcl2Provider(parameters.getString(RESOURCE_PARAMETER).orElse(null));
    }

    @Override
    public void createExtensions(Network network) {
        if (resource == null) {
            LOGGER.warn("No {} set, no Rpcl2 machine named", RESOURCE_PARAMETER);
            return;
        }
        try (InputStream in = getClass().getResourceAsStream(resource)) {
            if (in == null) {
                LOGGER.warn("Rpcl2 generators resource {} not found, none named", resource);
                return;
            }
            read(network, in);
        } catch (IOException e) {
            throw new UncheckedIOException("Reading Rpcl2 generators resource " + resource, e);
        }
    }

    private static void read(Network network, InputStream in) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            reader.readLine(); // header
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    describe(network, line.trim());
                }
            }
        }
    }

    private static void describe(Network network, String id) {
        Generator generator = network.getGenerator(id);
        if (generator == null) {
            LOGGER.warn("Generator {} of the Rpcl2 database is not in the network, skipped", id);
            return;
        }
        if (generator.getExtension(SynchronizedGeneratorProperties.class) != null) {
            return;
        }
        generator.newExtension(SynchronizedGeneratorPropertiesAdder.class)
                .withType(PV_TYPE)
                .withRpcl2(true)
                .add();
    }
}
