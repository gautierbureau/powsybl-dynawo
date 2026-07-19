/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.parameters;

import com.powsybl.dynawo.desc.ModelDescription;
import com.powsybl.dynawo.desc.ModifiableParameter;
import com.powsybl.dynawo.parameters.ParametersSet;
import com.powsybl.iidm.network.Generator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Builds the parameter set a synchronous generator model needs, from the parameters the model
 * declares in its Dynawo description.
 * <p>
 * Each parameter is valued from the first source able to provide it: the network itself through a
 * reference, a value computed from the characteristics of the machine, or a default value. A
 * parameter no source can provide is left out and reported, rather than written empty, so that a
 * missing value surfaces here instead of failing inside Dynawo.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public class SynchronousGeneratorParametersGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(SynchronousGeneratorParametersGenerator.class);

    /**
     * Voltage of the machine side of a generator transformer, in kV.
     */
    private static final double GENERATION_VOLTAGE = 20.0;

    /**
     * Share of the generation consumed by the auxiliaries.
     */
    private static final double AUXILIARY_RATIO = 0.005;

    /**
     * Reactive over active power of the auxiliaries.
     */
    private static final double AUXILIARY_POWER_FACTOR = 0.5;

    /**
     * Leakage reactance standing in for a generator transformer the model does not represent.
     */
    private static final double LEAKAGE_REACTANCE = 0.01;

    private final GeneratorParameterDefaultsRegistry defaults;
    private final Path tablesDirectory;

    public SynchronousGeneratorParametersGenerator() {
        this(GeneratorParameterDefaultsRegistry.getInstance(), GovernorTables.defaultDirectory());
    }

    /**
     * @param tablesDirectory directory the governor tables are written in, or {@code null} to leave
     *                        the models needing them without their table file
     */
    public SynchronousGeneratorParametersGenerator(GeneratorParameterDefaultsRegistry defaults, Path tablesDirectory) {
        this.defaults = defaults;
        this.tablesDirectory = tablesDirectory;
    }

    /**
     * @param setId       identifier of the generated set, referenced by the dynamic model
     * @param description description of the model the set is built for
     * @param generator   the machine the parameters describe
     * @param transformer whether the model includes a generator transformer
     */
    public ParametersSet generate(String setId, ModelDescription description, Generator generator, boolean transformer) {
        ParametersSet set = new ParametersSet(setId);
        double nominalV = generator.getTerminal().getVoltageLevel().getNominalV();
        List<String> unresolved = new ArrayList<>();

        for (ModifiableParameter parameter : description.parameters()) {
            String name = parameter.name();
            Optional<String> reference = defaults.getReference(name, transformer);
            if (reference.isPresent()) {
                set.addReference(name, parameter.valueType(), reference.get());
                continue;
            }
            Optional<String> value = computeValue(name, generator, nominalV, transformer)
                    .or(() -> tablesFile(name))
                    .or(() -> defaults.getValue(name, description.name()));
            if (value.isPresent()) {
                set.addParameter(name, parameter.valueType(), value.get());
            } else {
                unresolved.add(name);
            }
        }

        if (!unresolved.isEmpty()) {
            LOGGER.warn("No value found for parameters {} of model {}, they are left out of set {}",
                    unresolved, description.name(), setId);
        }
        return set;
    }

    /**
     * Values the parameters depending on the machine itself, which no table can provide.
     */
    private static Optional<String> computeValue(String name, Generator generator, double nominalV, boolean transformer) {
        double nominalP = GeneratorSizing.nominalActivePower(generator);
        return switch (name) {
            case "generator_SNom", "generator_SnTfo", "transformer_SNom" -> Optional.of(format(GeneratorSizing.apparentPower(generator)));
            case "generator_PNomAlt" -> Optional.of(format(0.9 * nominalP));
            case "generator_PNomTurb", "governor_PBaseMw" -> Optional.of(format(0.8 * nominalP));
            case "generator_UBaseHV", "generator_UNomHV", "transformer_UBaseHV", "transformer_UNomHV" -> Optional.of(format(nominalV));
            // the machine side voltage of a unit connected through its transformer is not
            // described by the network, the usual generation voltage is used instead
            case "generator_UNom", "generator_UNomLV", "generator_UBaseLV", "transformer_UNomLV", "transformer_UBaseLV" -> optional(transformer, GENERATION_VOLTAGE);
            case "auxHV_P0Pu", "auxLV_P0Pu" -> optional(transformer, AUXILIARY_RATIO * nominalP);
            case "auxHV_Q0Pu", "auxLV_Q0Pu" -> optional(transformer, AUXILIARY_RATIO * nominalP * AUXILIARY_POWER_FACTOR);
            // a unit connected to a transmission voltage level but modelled without its
            // transformer keeps a small leakage reactance standing in for it
            case "generator_XTfPu" -> optional(!transformer && nominalV > GENERATION_VOLTAGE, LEAKAGE_REACTANCE);
            default -> Optional.empty();
        };
    }

    /**
     * Points the governors reading their valve characteristic from a file at the shipped tables.
     */
    private Optional<String> tablesFile(String name) {
        if (!name.endsWith("TablesFile") || tablesDirectory == null) {
            return Optional.empty();
        }
        return Optional.of(GovernorTables.writeTo(tablesDirectory).toString());
    }

    private static Optional<String> optional(boolean condition, double value) {
        return condition ? Optional.of(format(value)) : Optional.empty();
    }

    private static String format(double value) {
        return Double.toString(value);
    }
}
