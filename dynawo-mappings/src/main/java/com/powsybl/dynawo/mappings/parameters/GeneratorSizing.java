/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.parameters;

import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Terminal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sizes a machine from what the network says about it.
 * <p>
 * The rated apparent power drives most of the generated parameters, and it is often missing:
 * {@code ratedS} is undefined on many networks and {@code maxP} is frequently a placeholder rather
 * than a real limit, the IEEE test systems declaring 9999 MW for every machine. Sizing a 232 MW
 * unit as a 11 GW one would make the simulation meaningless, so the value is picked from the first
 * trustworthy source:
 * <ol>
 *     <li>{@code ratedS} when defined,</li>
 *     <li>{@code maxP} when it is consistent with the operating point,</li>
 *     <li>the operating point itself, a machine being assumed to run at {@value #LOAD_FACTOR} of
 *     its rated power.</li>
 * </ol>
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public final class GeneratorSizing {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeneratorSizing.class);

    /**
     * Load factor assumed when the machine size has to be deduced from its operating point.
     */
    public static final double LOAD_FACTOR = 0.8;

    /**
     * {@code maxP} is considered a placeholder beyond this many times the operating point.
     */
    public static final double MAX_PLAUSIBLE_RATIO = 10.0;

    /**
     * Size given to a machine nothing is known about.
     */
    public static final double DEFAULT_APPARENT_POWER = 100.0;

    private GeneratorSizing() {
    }

    /**
     * Rated apparent power in MVA.
     */
    public static double apparentPower(Generator generator) {
        double ratedS = generator.getRatedS();
        if (isDefined(ratedS) && ratedS > 0) {
            return ratedS;
        }
        double operatingPoint = operatingPoint(generator);
        double maxP = generator.getMaxP();
        if (isDefined(maxP) && maxP > 0 && maxP <= MAX_PLAUSIBLE_RATIO * Math.max(operatingPoint, 1)) {
            return 1.1 * maxP;
        }
        if (operatingPoint > 0) {
            LOGGER.debug("Generator {} has no rated apparent power and an implausible maximum active power {}, sized from its operating point",
                    generator.getId(), maxP);
            return operatingPoint / LOAD_FACTOR;
        }
        LOGGER.warn("Generator {} has neither a rated apparent power nor a usable operating point, sized at {} MVA",
                generator.getId(), DEFAULT_APPARENT_POWER);
        return DEFAULT_APPARENT_POWER;
    }

    /**
     * Nominal active power in MW, the machine being assumed to run at {@value #LOAD_FACTOR} of its
     * rated apparent power when {@code maxP} cannot be trusted.
     */
    public static double nominalActivePower(Generator generator) {
        double maxP = generator.getMaxP();
        double operatingPoint = operatingPoint(generator);
        if (isDefined(maxP) && maxP > 0 && maxP <= MAX_PLAUSIBLE_RATIO * Math.max(operatingPoint, 1)) {
            return maxP;
        }
        return LOAD_FACTOR * apparentPower(generator);
    }

    /**
     * Apparent power actually flowing, taken from the load flow results when available since
     * running one is a prerequisite of a dynamic simulation, and from the targets otherwise.
     */
    private static double operatingPoint(Generator generator) {
        Terminal terminal = generator.getTerminal();
        double p = terminal.getP();
        double q = terminal.getQ();
        if (isDefined(p)) {
            return Math.hypot(p, isDefined(q) ? q : 0);
        }
        double targetP = generator.getTargetP();
        double targetQ = generator.getTargetQ();
        return Math.hypot(isDefined(targetP) ? targetP : 0, isDefined(targetQ) ? targetQ : 0);
    }

    private static boolean isDefined(double value) {
        return !Double.isNaN(value);
    }
}
