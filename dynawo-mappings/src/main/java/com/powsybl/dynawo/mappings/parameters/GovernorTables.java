/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.parameters;

import com.powsybl.commons.PowsyblException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * The valve characteristic tables some governors read from a file.
 * <p>
 * Governors such as GovSteam1 or TGov3 do not carry their valve characteristic as parameters but
 * point at a file holding it, so a generated parameter set is unusable until that file exists on
 * disk. The shipped tables describe a linear characteristic, a neutral choice for a study that has
 * no measured one.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public final class GovernorTables {

    public static final String FILE_NAME = "governorTables.txt";
    private static final String RESOURCE = "/" + FILE_NAME;

    private GovernorTables() {
    }

    /**
     * Writes the tables in the given directory and returns the path to reference in the parameter
     * set. Existing tables are left untouched, so that a study can provide its own.
     */
    public static Path writeTo(Path directory) {
        Path file = directory.resolve(FILE_NAME);
        if (Files.exists(file)) {
            return file;
        }
        try (InputStream is = GovernorTables.class.getResourceAsStream(RESOURCE)) {
            if (is == null) {
                throw new PowsyblException("Governor tables resource not found");
            }
            Files.createDirectories(directory);
            Files.copy(is, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new PowsyblException("Could not write the governor tables in " + directory, e);
        }
        return file;
    }
}
