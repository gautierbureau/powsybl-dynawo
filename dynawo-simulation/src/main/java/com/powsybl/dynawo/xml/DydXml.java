/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.dynawo.xml;

import com.powsybl.dynawo.commons.DynawoVersion;
import com.powsybl.dynawo.models.BlackBoxModel;
import com.powsybl.dynawo.models.VariableNames;
import com.powsybl.dynawo.models.macroconnections.MacroConnect;
import com.powsybl.dynawo.models.macroconnections.MacroConnector;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.function.UnaryOperator;

import static com.powsybl.dynawo.DynawoSimulationConstants.DYD_FILENAME;

/**
 * @author Mathieu Bague {@literal <mathieu.bague@rte-france.com>}
 */
public final class DydXml extends AbstractXmlDynawoSimulationWriter<DynawoData> {

    private final UnaryOperator<String> variableNames;

    private DydXml(String fileName, DynawoVersion dynawoVersion) {
        super(fileName, "dynamicModelsArchitecture");
        this.variableNames = VariableNames.of(dynawoVersion);
    }

    public static void write(Path workingDir, DynawoData dataSupplier) throws IOException {
        write(workingDir, dataSupplier, null);
    }

    public static void write(Path workingDir, String fileName, DynawoData dataSupplier) throws IOException {
        write(workingDir, fileName, dataSupplier, null);
    }

    /**
     * The dyd file for a version of Dynawo, that names the variables of the models ({@link VariableNames})
     */
    public static void write(Path workingDir, DynawoData dataSupplier, DynawoVersion dynawoVersion) throws IOException {
        write(workingDir, DYD_FILENAME, dataSupplier, dynawoVersion);
    }

    public static void write(Path workingDir, String fileName, DynawoData dataSupplier, DynawoVersion dynawoVersion) throws IOException {
        new DydXml(fileName, dynawoVersion).createXmlFileFromDataSupplier(workingDir, dataSupplier);
    }

    @Override
    public void write(XMLStreamWriter writer, DynawoData dataSupplier) throws XMLStreamException {
        // loop over the values of the map indexed by dynamicIds to write only once objects with the same dynamicId
        String parFileName = dataSupplier.getParFileName();
        if (parFileName == null || parFileName.isEmpty()) {
            for (BlackBoxModel model : dataSupplier.getBlackBoxDynamicModels()) {
                model.write(writer);
            }
            for (BlackBoxModel model : dataSupplier.getBlackBoxEventModels()) {
                model.write(writer);
            }
        } else {
            for (BlackBoxModel model : dataSupplier.getBlackBoxDynamicModels()) {
                model.write(writer, dataSupplier.getParFileName());
            }
            for (BlackBoxModel model : dataSupplier.getBlackBoxEventModels()) {
                model.write(writer, dataSupplier.getParFileName());
            }
        }
        for (MacroConnector macroConnector : dataSupplier.getMacroConnectors()) {
            macroConnector.write(writer, variableNames);
        }
        for (MacroStaticReference macroStaticReference : dataSupplier.getMacroStaticReferences()) {
            macroStaticReference.write(writer, variableNames);
        }
        for (MacroConnect macroConnect : dataSupplier.getMacroConnectList()) {
            macroConnect.write(writer);
        }
    }
}
