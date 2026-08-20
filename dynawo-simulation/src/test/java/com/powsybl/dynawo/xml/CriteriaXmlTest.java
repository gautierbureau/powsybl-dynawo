/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com/)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.xml;

import com.powsybl.dynawo.criteria.Criteria;
import com.powsybl.dynawo.criteria.CriteriaCollection;
import com.powsybl.dynawo.criteria.CriteriaParams;
import com.powsybl.dynawo.criteria.CriteriaParamsVoltageLevel;
import com.powsybl.dynawo.criteria.CriteriaScope;
import com.powsybl.dynawo.criteria.CriteriaType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
class CriteriaXmlTest {

    @Test
    void writesEachKindOfCriteriaAndValidatesAgainstTheSchema(@TempDir Path tmpDir) throws Exception {
        CriteriaCollection collection = new CriteriaCollection()
                .add(CriteriaCollection.Type.BUS, Criteria.builder()
                        .params(CriteriaParams.builder()
                                .id("hv_voltage").scope(CriteriaScope.FINAL).type(CriteriaType.LOCAL_VALUE)
                                .voltageLevel(CriteriaParamsVoltageLevel.builder()
                                        .uNomMin(225).uNomMax(400).uMinPu(0.85).uMaxPu(1.15).build())
                                .build())
                        .component("B1", "VL1")
                        .country("FR")
                        .build())
                .add(CriteriaCollection.Type.LOAD, Criteria.builder()
                        .params(CriteriaParams.builder()
                                .id("load_p").scope(CriteriaScope.FINAL).type(CriteriaType.SUM).pMax(500).build())
                        .component("L1")
                        .build())
                .add(CriteriaCollection.Type.GENERATOR, Criteria.builder()
                        .params(CriteriaParams.builder()
                                .id("gen_p").scope(CriteriaScope.DYNAMIC).type(CriteriaType.SUM).pMin(-100).pMax(1000)
                                .voltageLevel(CriteriaParamsVoltageLevel.builder().uNomMin(63).build())
                                .build())
                        .component("G1")
                        .build());

        Path file = tmpDir.resolve("criteria.crt");
        CriteriaXml.write(collection, file);

        // the written file must validate against Dynawo's criteria.xsd
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = factory.newSchema(new StreamSource(getClass().getResourceAsStream("/criteria.xsd")));
        schema.newValidator().validate(new StreamSource(Files.newInputStream(file)));

        String xml = Files.readString(file);
        // a bus criteria carries its voltage band inline on its parameters, and its component names a voltage level
        assertTrue(xml.contains("<dyn:busCriteria>"), xml);
        assertTrue(busParameters(xml).contains("uMinPu=\"0.85\"") && busParameters(xml).contains("uMaxPu=\"1.15\""), xml);
        assertTrue(xml.contains("<dyn:component id=\"B1\" voltageLevelId=\"VL1\"/>"), xml);
        assertTrue(xml.contains("<dyn:country id=\"FR\"/>"), xml);
        // a load/generator criteria carries its bands as child elements and its component is id-only
        assertTrue(xml.contains("<dyn:loadCriteria>"), xml);
        assertTrue(xml.contains("pMax=\"500.0\""), xml);
        assertTrue(xml.contains("<dyn:generatorCriteria>"), xml);
        assertTrue(xml.contains("pMin=\"-100.0\"") && xml.contains("<dyn:voltageLevel uNomMin=\"63.0\"/>"), xml);
        assertTrue(xml.contains("<dyn:component id=\"G1\"/>"), xml);
    }

    /** The bus criteria's {@code <parameters …>} element, where its voltage band is written inline. */
    private static String busParameters(String xml) {
        int from = xml.indexOf("<dyn:parameters id=\"hv_voltage\"");
        return from < 0 ? "" : xml.substring(from, xml.indexOf('>', from));
    }
}
