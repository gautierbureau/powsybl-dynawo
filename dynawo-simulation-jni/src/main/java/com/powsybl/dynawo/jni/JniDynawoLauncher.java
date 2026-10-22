/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.jni;

import com.google.auto.service.AutoService;
import com.google.common.util.concurrent.Uninterruptibles;
import com.powsybl.commons.PowsyblException;
import com.powsybl.dynawo.DynawoJniLauncher;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidmbridge.jni.IidmBridgeRegistry;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Dynawo run in this JVM through its native methods: the network is registered in the registry of iidm-bridge, where
 * Dynawo reads it through the JNI backend of iidm-bridge, and writes the final values of the simulation into it; the
 * other input files are given to Dynawo with their contents, and it gives back the contents of its output files.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
@AutoService(DynawoJniLauncher.class)
public class JniDynawoLauncher implements DynawoJniLauncher {

    /**
     * Stack of the thread that runs Dynawo, as the one of the main thread of a process that runs it
     */
    private static final long DYNAWO_THREAD_STACK_SIZE = 64L * 1024 * 1024;

    private static final String NETWORK_ID_PREFIX = "powsybl-dynawo-";

    @Override
    public String getVersion(Path dynawoHome) {
        DynawoNative.load(dynawoHome);
        return DynawoNative.getVersion();
    }

    @Override
    public void run(Path dynawoHome, Path jobsFile, Map<Path, byte[]> inputFiles, Map<Path, byte[]> outputFiles, Network network,
                    String variantId) {
        DynawoNative.load(dynawoHome);
        String[] inputPaths = new String[inputFiles.size()];
        byte[][] inputContents = new byte[inputFiles.size()][];
        int i = 0;
        for (Map.Entry<Path, byte[]> inputFile : inputFiles.entrySet()) {
            inputPaths[i] = inputFile.getKey().toAbsolutePath().toString();
            inputContents[i] = inputFile.getValue();
            i++;
        }
        String networkId = NETWORK_ID_PREFIX + UUID.randomUUID();
        Map<String, byte[]> outputs = new HashMap<>();
        IidmBridgeRegistry.register(networkId, network);
        try {
            runInDynawoThread(() -> {
                // the working variant of this thread, if the variants of the network are per thread
                network.getVariantManager().setWorkingVariant(variantId);
                DynawoNative.runJobs(dynawoHome.toAbsolutePath().toString(), jobsFile.toAbsolutePath().toString(),
                        inputPaths, inputContents, networkId, outputs);
            });
        } finally {
            IidmBridgeRegistry.unregister(networkId);
            outputs.forEach((path, content) -> outputFiles.put(Path.of(path), content));
        }
    }

    /**
     * Runs Dynawo in a thread with the stack it needs, and waits for its end: it cannot be stopped while it writes into
     * the network
     */
    private static void runInDynawoThread(Runnable dynawo) {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread thread = new Thread(null, dynawo, "dynawo", DYNAWO_THREAD_STACK_SIZE);
        thread.setUncaughtExceptionHandler((t, e) -> failure.set(e));
        thread.start();
        Uninterruptibles.joinUninterruptibly(thread);
        Throwable t = failure.get();
        if (t instanceof RuntimeException e) {
            throw e;
        }
        if (t instanceof Error e) {
            throw e;
        }
        if (t != null) {
            throw new PowsyblException("Dynawo failed", t);
        }
    }
}
