/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.corrections;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.Network;

/**
 * A correction applied to the network before the mapping turns it into dynamic models.
 * <p>
 * Where a {@link com.powsybl.dynawo.simplifiers.ModelSimplifier} acts on the models a mapping has
 * already built, a correction acts one step earlier, on the {@code Network} itself: it may add an
 * equipment, change a characteristic, force an operating point, so that what the mapping then reads
 * is the corrected network. A study chooses which corrections run by name, as it chooses simplifiers,
 * through {@link com.powsybl.dynawo.DynawoSimulationParameters#getNetworkCorrections()}.
 * <p>
 * Implementations are discovered with a {@link java.util.ServiceLoader} and must be idempotent: a
 * correction is applied at most once per network (see {@link NetworkCorrections}), but should also
 * leave a network it has nothing to change untouched.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public interface NetworkCorrection {

    /**
     * The name this correction is activated by and the description of what it changes.
     */
    NetworkCorrectionInfo getCorrectionInfo();

    /**
     * Applies the correction to the network, in place. Reports what it changed on the given node.
     */
    void apply(Network network, ReportNode reportNode);
}
