/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.dynawo.models;

import com.powsybl.dynawo.builders.VersionInterval;
import com.powsybl.dynawo.models.macroconnections.MacroConnectAttribute;
import com.powsybl.dynawo.models.macroconnections.MacroConnectionsAdder;
import com.powsybl.dynawo.parameters.ParametersSet;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author Florian Dupuy {@literal <florian.dupuy at rte-france.com>}
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
 */
public interface BlackBoxModel extends Model {
    String getDynamicModelId();

    String getParameterSetId();

    String getLib();

    VersionInterval getVersionInterval();

    List<VarMapping> getVarsMapping();

    void createMacroConnections(MacroConnectionsAdder adder);

    List<MacroConnectAttribute> getMacroConnectFromAttributes();

    String getDefaultParFile();

    void write(XMLStreamWriter writer) throws XMLStreamException;

    void write(XMLStreamWriter writer, String parFileName) throws XMLStreamException;

    void createDynamicModelParameters(Consumer<ParametersSet> parametersAdder);

    void updateDynamicModelParameters(ParameterUpdater parameterUpdater);

    void createNetworkParameter(ParametersSet networkParameters);

    /**
     * If <code>true</code> every line, transformer and bus in the network need to have a specific dynamic model
     */
    boolean needMandatoryDynamicModels();

    void createDynamicModelInfoExtension();

    boolean isConnected();

    /**
     * Writes the auxiliary input files this model needs beside the DYD and PAR — a table file a parameter
     * points at, for one. Called once per model when the input files are written; a model that needs none
     * (most) leaves it be.
     *
     * @param workingDir the directory the DYD and PAR are written to
     */
    default void writeAuxiliaryFiles(Path workingDir) throws IOException {
        // most models need no file of their own
    }
}
