/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.tools;

import com.powsybl.commons.PowsyblException;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Asks Dynawo what a compiled model expects.
 * <p>
 * A model database holds a description beside every library, but a model we have just compiled has
 * none: the description is not something the library carries, it is something the tool reads out
 * of it. This is the same step the Dynawo build takes after compiling a model, so a model we
 * generate ends up described exactly like one that was shipped.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public final class DumpModel {

    private static final long TIMEOUT_SECONDS = 60;

    private final DynawoLauncher launcher;

    public DumpModel(Path homeDir) {
        this(new DynawoLauncher(homeDir));
    }

    public DumpModel(DynawoLauncher launcher) {
        this.launcher = launcher;
    }

    /**
     * Writes the description of a compiled model beside it, and answers where it went.
     * <p>
     * A description already there is left alone: reading a library is the slow part, and the
     * description of a library that has not changed cannot have.
     */
    public Path describe(Path library) {
        Path description = library.resolveSibling(libName(library) + ".desc.xml");
        if (Files.exists(description)) {
            return description;
        }
        if (!Files.exists(library)) {
            throw new PowsyblException("No model library at " + library);
        }
        launcher.run(null, TIMEOUT_SECONDS, DynawoLauncher.DUMP_MODEL,
                "-m", library.toAbsolutePath().toString(), "-o", description.toAbsolutePath().toString());
        if (!Files.exists(description)) {
            throw new PowsyblException("dumpModel reported success but wrote no description for " + library);
        }
        return description;
    }

    /**
     * The name a library goes by, which is the name of the model it holds.
     */
    static String libName(Path library) {
        String fileName = library.getFileName().toString();
        int extension = fileName.lastIndexOf('.');
        return extension < 0 ? fileName : fileName.substring(0, extension);
    }
}
