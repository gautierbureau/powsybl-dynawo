/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.parameters;

import com.powsybl.dynawo.desc.FilteredDescriptionXml;
import com.powsybl.dynawo.desc.ModelDescription;
import com.powsybl.dynawo.parameters.ParametersSet;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TopologyKind;
import com.powsybl.iidm.network.VoltageLevel;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks that the default values cover the models the mapping selects, against the model database
 * of an installed Dynawo.
 * <p>
 * Skipped when no Dynawo is available, the models being too large to keep as test resources.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
class DynawoModelCoverageTest {

    /**
     * Location of the Dynawo distribution the tests read the model descriptions from.
     */
    private static final String DYNAWO_HOME_PROPERTY = "dynawo.home";
    private static final Path DEFAULT_DYNAWO_HOME = Path.of("..", "..", "dynawo");

    @ParameterizedTest
    @ValueSource(strings = {
        "GeneratorSynchronousFourWindingsGovCt2St4b",
        "GeneratorSynchronousFourWindingsGovSteam1St4b",
        "GeneratorSynchronousThreeWindingsGovHydro4St4b",
        "GeneratorSynchronousFourWindingsGoverPropVRPropInt",
        "GeneratorSynchronousThreeWindingsGoverPropVRPropInt",
        "GeneratorSynchronousThreeWindingsProportionalRegulations"
    })
    void shouldValueEveryParameterOfTheMappedModels(String lib, @TempDir Path tablesDirectory) {
        Path descFile = ddb().resolve(lib + ".desc.xml");
        Assumptions.assumeTrue(Files.exists(descFile), "no Dynawo model database available");

        ModelDescription description = FilteredDescriptionXml.load(descFile);
        ParametersSet set = new SynchronousGeneratorParametersGenerator(GeneratorParameterDefaultsRegistry.getInstance(), tablesDirectory)
                .generate("set", description, generator(), false);

        List<String> unresolved = new ArrayList<>(description.parameters().stream().map(p -> p.name()).toList());
        unresolved.removeAll(set.getParameters().keySet());
        unresolved.removeAll(set.getReferences().keySet());
        assertThat(unresolved).as("parameters of %s left without a value", lib).isEmpty();
    }

    private static Path ddb() {
        String home = System.getProperty(DYNAWO_HOME_PROPERTY);
        return (home != null ? Path.of(home) : DEFAULT_DYNAWO_HOME).resolve("ddb");
    }

    private static Generator generator() {
        Network network = Network.create("test", "test");
        VoltageLevel voltageLevel = network.newSubstation().setId("s").add()
                .newVoltageLevel().setId("vl").setNominalV(400.0).setTopologyKind(TopologyKind.BUS_BREAKER).add();
        voltageLevel.getBusBreakerView().newBus().setId("bus").add();
        Generator generator = voltageLevel.newGenerator().setId("g")
                .setBus("bus").setConnectableBus("bus")
                .setTargetP(220).setMinP(0).setMaxP(250).setTargetV(400).setVoltageRegulatorOn(true)
                .add();
        generator.getTerminal().setP(-220.0).setQ(0.0);
        return generator;
    }
}
