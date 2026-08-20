/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com/)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.xml;

import com.powsybl.commons.exceptions.UncheckedXmlStreamException;
import com.powsybl.dynawo.criteria.Criteria;
import com.powsybl.dynawo.criteria.CriteriaCollection;
import com.powsybl.dynawo.criteria.CriteriaParams;
import com.powsybl.dynawo.criteria.CriteriaParamsVoltageLevel;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.OptionalDouble;

import static com.powsybl.dynawo.xml.DynawoSimulationXmlConstants.DYN_PREFIX;
import static com.powsybl.dynawo.xml.DynawoSimulationXmlConstants.DYN_URI;

/**
 * Writes a {@link CriteriaCollection} as the Dynawo criteria file (see {@code criteria.xsd}): a {@code
 * criteria} root holding a {@code busCriteria} / {@code loadCriteria} / {@code generatorCriteria} per
 * criteria of each kind. A bus criteria carries its voltage band inline on its {@code parameters}, as the
 * schema's {@code CriteriaParamsWithVoltageLevel} does; a load or generator criteria carries its bands as
 * child {@code voltageLevel} elements.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public final class CriteriaXml {

    private CriteriaXml() {
    }

    public static void write(CriteriaCollection criteria, Path file) {
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            write(criteria, writer);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void write(CriteriaCollection criteria, Writer writer) {
        try {
            XMLStreamWriter xmlWriter = XmlStreamWriterFactory.newInstance(writer);
            try {
                xmlWriter.writeStartDocument(StandardCharsets.UTF_8.toString(), "1.0");
                xmlWriter.setPrefix(DYN_PREFIX, DYN_URI);
                xmlWriter.writeStartElement(DYN_URI, "criteria");
                xmlWriter.writeNamespace(DYN_PREFIX, DYN_URI);
                for (Criteria busCriteria : criteria.getCriteria(CriteriaCollection.Type.BUS)) {
                    writeBusCriteria(xmlWriter, busCriteria);
                }
                writeInjectionCriteria(xmlWriter, "loadCriteria", criteria.getCriteria(CriteriaCollection.Type.LOAD));
                writeInjectionCriteria(xmlWriter, "generatorCriteria", criteria.getCriteria(CriteriaCollection.Type.GENERATOR));
                xmlWriter.writeEndElement();
                xmlWriter.writeEndDocument();
            } finally {
                xmlWriter.close();
            }
        } catch (XMLStreamException e) {
            throw new UncheckedXmlStreamException(e);
        }
    }

    private static void writeBusCriteria(XMLStreamWriter writer, Criteria criteria) throws XMLStreamException {
        writer.writeStartElement(DYN_URI, "busCriteria");
        CriteriaParams params = criteria.getParams();
        writer.writeStartElement(DYN_URI, "parameters");
        writeParamsAttributes(writer, params);
        // a bus criteria carries its single voltage band inline (CriteriaParamsWithVoltageLevel)
        if (!params.getVoltageLevels().isEmpty()) {
            writeVoltageAttributes(writer, params.getVoltageLevels().get(0));
        }
        writer.writeEndElement();
        for (Criteria.ComponentRef component : criteria.getComponents()) {
            writer.writeEmptyElement(DYN_URI, "component");
            writer.writeAttribute("id", component.id());
            if (component.getVoltageLevelId().isPresent()) {
                writer.writeAttribute("voltageLevelId", component.getVoltageLevelId().get());
            }
        }
        writeCountries(writer, criteria);
        writer.writeEndElement();
    }

    private static void writeInjectionCriteria(XMLStreamWriter writer, String elementName, Iterable<Criteria> criteriaList) throws XMLStreamException {
        for (Criteria criteria : criteriaList) {
            writer.writeStartElement(DYN_URI, elementName);
            CriteriaParams params = criteria.getParams();
            writer.writeStartElement(DYN_URI, "parameters");
            writeParamsAttributes(writer, params);
            // a load or generator criteria carries its voltage bands as child elements (CriteriaParams)
            for (CriteriaParamsVoltageLevel voltageLevel : params.getVoltageLevels()) {
                writer.writeEmptyElement(DYN_URI, "voltageLevel");
                writeVoltageAttributes(writer, voltageLevel);
            }
            writer.writeEndElement();
            for (Criteria.ComponentRef component : criteria.getComponents()) {
                writer.writeEmptyElement(DYN_URI, "component");
                writer.writeAttribute("id", component.id());
            }
            writeCountries(writer, criteria);
            writer.writeEndElement();
        }
    }

    private static void writeParamsAttributes(XMLStreamWriter writer, CriteriaParams params) throws XMLStreamException {
        writer.writeAttribute("id", params.getId());
        writer.writeAttribute("scope", params.getScope().name());
        writer.writeAttribute("type", params.getType().name());
        writeOptionalAttribute(writer, "pMax", params.getPMax());
        writeOptionalAttribute(writer, "pMin", params.getPMin());
    }

    private static void writeVoltageAttributes(XMLStreamWriter writer, CriteriaParamsVoltageLevel voltageLevel) throws XMLStreamException {
        writeOptionalAttribute(writer, "uMaxPu", voltageLevel.getUMaxPu());
        writeOptionalAttribute(writer, "uMinPu", voltageLevel.getUMinPu());
        writeOptionalAttribute(writer, "uNomMax", voltageLevel.getUNomMax());
        writeOptionalAttribute(writer, "uNomMin", voltageLevel.getUNomMin());
    }

    private static void writeCountries(XMLStreamWriter writer, Criteria criteria) throws XMLStreamException {
        for (String country : criteria.getCountries()) {
            writer.writeEmptyElement(DYN_URI, "country");
            writer.writeAttribute("id", country);
        }
    }

    private static void writeOptionalAttribute(XMLStreamWriter writer, String name, OptionalDouble value) throws XMLStreamException {
        if (value.isPresent()) {
            writer.writeAttribute(name, Double.toString(value.getAsDouble()));
        }
    }
}
