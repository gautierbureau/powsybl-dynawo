/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.preassembled;

import com.powsybl.commons.PowsyblException;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Writes an assembly as the definition {@code generate-preassembled} compiles.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public final class PreassembledModelXml {

    private static final String NAMESPACE = "http://www.rte-france.com/dynawo";

    private PreassembledModelXml() {
    }

    public static String toXml(PreassembledModel model) {
        StringWriter writer = new StringWriter();
        write(model, writer);
        return writer.toString();
    }

    public static void write(PreassembledModel model, Path file) {
        try (Writer writer = Files.newBufferedWriter(file)) {
            write(model, writer);
        } catch (IOException e) {
            throw new PowsyblException("Could not write the model definition in " + file, e);
        }
    }

    public static void write(PreassembledModel model, Writer writer) {
        try {
            writer.write("<?xml version='1.0' encoding='UTF-8'?>\n");
            writer.write("<dyn:dynamicModelsArchitecture xmlns:dyn=\"" + NAMESPACE + "\">\n");
            writer.write("  <dyn:modelicaModel id=\"" + model.getId() + "\">\n");
            for (UnitModel unit : model.getUnits()) {
                writer.write("    <dyn:unitDynamicModel id=\"" + unit.getId() + "\" name=\"" + unit.getName() + "\"");
                if (unit.getInitName() != null) {
                    writer.write(" initName=\"" + unit.getInitName() + "\"");
                }
                writer.write("/>\n");
            }
            for (UnitConnection connection : model.getConnections()) {
                writer.write("    <dyn:" + (connection.initialisation() ? "initConnect" : "connect")
                        + " id1=\"" + connection.id1() + "\" var1=\"" + connection.var1()
                        + "\" id2=\"" + connection.id2() + "\" var2=\"" + connection.var2() + "\"/>\n");
            }
            writer.write("  </dyn:modelicaModel>\n");
            writer.write("</dyn:dynamicModelsArchitecture>\n");
        } catch (IOException e) {
            throw new PowsyblException("Could not write the model definition", e);
        }
    }
}
