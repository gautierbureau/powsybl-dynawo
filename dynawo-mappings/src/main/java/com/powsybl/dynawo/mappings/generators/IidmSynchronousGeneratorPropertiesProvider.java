/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.generators;

import com.google.auto.service.AutoService;
import com.powsybl.dynawo.extensions.api.generator.RpclType;
import com.powsybl.dynawo.extensions.api.generator.SynchronousGeneratorProperties;
import com.powsybl.dynawo.extensions.api.generator.SynchronousGeneratorPropertiesAdder;
import com.powsybl.dynawo.mappings.parameters.GeneratorSizing;
import com.powsybl.iidm.network.EnergySource;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Derives the generator controls from the IIDM characteristics, mainly the energy source, the way
 * the historical groovy and python mappings did.
 * <p>
 * The controls are the detailed (DynaSwing) ones, the simplified (DynaWaltz) ones being deduced
 * from them by {@link com.powsybl.dynawo.mappings.controls.ControlTranslations}, so that a single
 * set of extensions feeds both mappings.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
@AutoService(SynchronousGeneratorPropertiesProvider.class)
public class IidmSynchronousGeneratorPropertiesProvider implements SynchronousGeneratorPropertiesProvider {

    public static final String NAME = "IidmRules";
    private static final Logger LOGGER = LoggerFactory.getLogger(IidmSynchronousGeneratorPropertiesProvider.class);

    /**
     * Voltage level nominal voltage above which a generator is connected through a transformer and
     * carries its auxiliaries.
     */
    public static final double DEFAULT_TSO_VOLTAGE_MIN = 30.0;

    private static final Controls NUCLEAR_CONTROLS = new Controls(SynchronousGeneratorProperties.Windings.FOUR_WINDINGS, "GovCt2", "St4b");
    private static final Controls THERMAL_CONTROLS = new Controls(SynchronousGeneratorProperties.Windings.FOUR_WINDINGS, "GovSteam1", "St4b");
    private static final Controls HYDRO_CONTROLS = new Controls(SynchronousGeneratorProperties.Windings.THREE_WINDINGS, "GovHydro4", "St4b");
    private static final Map<EnergySource, Controls> CONTROLS_BY_ENERGY_SOURCE = Map.of(
            EnergySource.NUCLEAR, NUCLEAR_CONTROLS,
            EnergySource.THERMAL, THERMAL_CONTROLS,
            EnergySource.HYDRO, HYDRO_CONTROLS);

    /**
     * Nominal active power above which an unqualified machine is treated as a large steam unit,
     * in MW.
     */
    public static final double LARGE_UNIT_POWER = 600.0;

    /**
     * Nominal active power above which an unqualified machine is treated as a steam unit rather
     * than a hydro one, in MW.
     */
    public static final double MEDIUM_UNIT_POWER = 100.0;

    private record Controls(SynchronousGeneratorProperties.Windings windings, String governor, String voltageRegulator) {
    }

    private final double tsoVoltageMin;
    private final Predicate<Generator> filter;

    public IidmSynchronousGeneratorPropertiesProvider() {
        this(DEFAULT_TSO_VOLTAGE_MIN);
    }

    public IidmSynchronousGeneratorPropertiesProvider(double tsoVoltageMin) {
        this(tsoVoltageMin, GeneratorFilters.connected());
    }

    /**
     * @param filter which machines get an extension, see {@link GeneratorFilters}
     */
    public IidmSynchronousGeneratorPropertiesProvider(double tsoVoltageMin, Predicate<Generator> filter) {
        this.tsoVoltageMin = tsoVoltageMin;
        this.filter = Objects.requireNonNull(filter);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean isEligible(Generator generator) {
        return filter.test(generator);
    }

    /**
     * The controls of a machine, from its energy source when the network gives one.
     * <p>
     * Many networks do not: the IEEE and PEGASE test systems declare every machine as
     * {@link EnergySource#OTHER}. Rather than refuse to describe them, the size of the machine
     * stands in for the missing information, a large unit behaving like a steam set and a small one
     * like a hydro set. It is a guess, and a study that knows better should either set the energy
     * sources or write the extensions itself, which the mapping then leaves untouched.
     */
    private static Controls controlsOf(Generator generator) {
        Controls controls = CONTROLS_BY_ENERGY_SOURCE.get(generator.getEnergySource());
        if (controls != null) {
            return controls;
        }
        double nominalP = GeneratorSizing.nominalActivePower(generator);
        controls = nominalP >= LARGE_UNIT_POWER ? NUCLEAR_CONTROLS
                : nominalP >= MEDIUM_UNIT_POWER ? THERMAL_CONTROLS : HYDRO_CONTROLS;
        LOGGER.debug("No controls defined for energy source {} of generator {}, {} deduced from its {} MW",
                generator.getEnergySource(), generator.getId(), controls.governor(), nominalP);
        return controls;
    }

    @Override
    public void createExtensions(Network network) {
        network.getGeneratorStream()
                .filter(this::isEligible)
                .filter(g -> g.getExtension(SynchronousGeneratorProperties.class) == null)
                .forEach(this::createExtension);
    }

    private void createExtension(Generator generator) {
        Controls controls = controlsOf(generator);
        boolean tso = generator.getTerminal().getVoltageLevel().getNominalV() >= tsoVoltageMin;
        generator.newExtension(SynchronousGeneratorPropertiesAdder.class)
                .withNumberOfWindings(controls.windings())
                .withGovernor(controls.governor())
                .withVoltageRegulator(controls.voltageRegulator())
                .withPss("")
                .withAuxiliaries(tso)
                .withInternalTransformer(false)
                .withRpcl(RpclType.NONE)
                .withUva(tso ? SynchronousGeneratorProperties.Uva.LOCAL : SynchronousGeneratorProperties.Uva.DISTANT)
                .withAggregated(false)
                .withQlim(false)
                .add();
    }
}
