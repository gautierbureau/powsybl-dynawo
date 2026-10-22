/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.jni;

import com.powsybl.commons.PowsyblException;

import java.nio.file.Path;

/**
 * The native methods of Dynawo, in the dynawo_JNILauncher library of its installation (DYNJNILauncher.cpp)
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
final class DynawoNative {

    private static final String LIBRARY_NAME = "dynawo_JNILauncher";

    private static Path loadedLibrary;

    private DynawoNative() {
    }

    /**
     * Loads the library of the Dynawo installed in a directory, once: a JVM can load only one of them. The libraries
     * it needs, those of Dynawo and of its third parties, are found on the {@code LD_LIBRARY_PATH}, as dynawo.sh sets it.
     */
    static synchronized void load(Path dynawoHome) {
        Path library = dynawoHome.resolve("lib").resolve(System.mapLibraryName(LIBRARY_NAME)).toAbsolutePath().normalize();
        if (loadedLibrary == null) {
            try {
                System.load(library.toString());
            } catch (UnsatisfiedLinkError e) {
                throw new PowsyblException("Dynawo cannot be loaded from " + library + " (the libraries of Dynawo and of its third parties must be on the library path): " + e.getMessage(), e);
            }
            loadedLibrary = library;
        } else if (!loadedLibrary.equals(library)) {
            throw new PowsyblException("Dynawo is already loaded from " + loadedLibrary + ": it cannot be loaded from " + library);
        }
    }

    /**
     * The version of Dynawo, as {@code dynawo.sh version} prints it
     */
    static native String getVersion();

    /**
     * Runs the jobs of a jobs file on the network registered in {@link com.powsybl.iidmbridge.jni.IidmBridgeRegistry}
     * under an id, in its working variant: Dynawo writes the final values of the simulation into it. A failure throws a
     * {@link DynawoNativeException}.
     */
    static native void runJobs(String installDir, String jobsFile, String networkId);
}
