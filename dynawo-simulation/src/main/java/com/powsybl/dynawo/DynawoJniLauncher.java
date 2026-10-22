/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo;

import com.powsybl.iidm.network.Network;

import java.nio.file.Path;

/**
 * Runs Dynawo in this JVM, through JNI, on a network of this JVM: Dynawo reads the network itself and writes the
 * results of the simulation into it, so that no IIDM file is written or read. Used by the {@code jni} launcher of
 * {@link DynawoSimulationConfig}; its implementation, in the powsybl-dynawo-simulation-jni module, is found by the
 * {@link java.util.ServiceLoader}.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public interface DynawoJniLauncher {

    /**
     * The version of the Dynawo installed in a directory, as {@code dynawo.sh version} prints it
     */
    String getVersion(Path dynawoHome);

    /**
     * Runs the jobs of a jobs file on a network: Dynawo reads the given variant of the network and writes the final
     * values of the simulation into it. A failure of Dynawo throws a {@link com.powsybl.commons.PowsyblException}
     * with the message of Dynawo, and leaves the network as it was.
     */
    void run(Path dynawoHome, Path jobsFile, Network network, String variantId);
}
