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

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Reproduces, with the typed criteria object API, the security-analysis criteria file DynaFlow ships
 * ({@code dynaflowSaCriteria.crt}, byte-identical to the DynaFlow launcher's {@code examples/SA/criteria.crt}
 * and to powsybl-dynawo's own {@code ieee14/dynamic-security-analysis/convergence/criteria.crt}): one bus
 * criteria carrying its voltage band inline, and four summed load criteria each with a nested voltage band,
 * all filtered on the same two countries.
 * <p>
 * The written file validates against the criteria schema and, once the two are normalised (the licence
 * comment dropped, whitespace collapsed, the {@code id =} spacing the Dynawo writer emits and the trailing
 * {@code .0} our {@code Double.toString} emits removed), matches the reference — so the object API produces
 * the same criteria model, the remaining differences being only cosmetic serialisation.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
class DynaFlowCriteriaReproductionTest {

    @Test
    void reproducesTheDynaFlowSecurityAnalysisCriteria() throws Exception {
        CriteriaCollection collection = new CriteriaCollection()
                .add(CriteriaCollection.Type.BUS, Criteria.builder()
                        .params(CriteriaParams.builder()
                                .id("Risque modele").scope(CriteriaScope.DYNAMIC).type(CriteriaType.LOCAL_VALUE)
                                .voltageLevel(CriteriaParamsVoltageLevel.builder().uMinPu(0.8).uNomMin(225).build())
                                .build())
                        .country("AF").country("AFGHANISTAN")
                        .build())
                .add(CriteriaCollection.Type.LOAD, loadCriteria("Risque protection", CriteriaScope.DYNAMIC, 100, 0.85))
                .add(CriteriaCollection.Type.LOAD, loadCriteria("Risque QdE", CriteriaScope.FINAL, 200, 0.92))
                .add(CriteriaCollection.Type.LOAD, loadCriteria("Risque Surete", CriteriaScope.FINAL, 1500, 0.92))
                .add(CriteriaCollection.Type.LOAD, loadCriteria("Risque IGA", CriteriaScope.FINAL, 3000, 0.92));

        StringWriter writer = new StringWriter();
        CriteriaXml.write(collection, writer);
        String written = writer.toString();

        validatesAgainstTheSchema(written);
        assertEquals(normalize(reference()), normalize(written),
                "the object API should reproduce the DynaFlow security-analysis criteria");
    }

    private static Criteria loadCriteria(String id, CriteriaScope scope, double pMax, double uMaxPu) {
        return Criteria.builder()
                .params(CriteriaParams.builder()
                        .id(id).scope(scope).type(CriteriaType.SUM).pMax(pMax)
                        .voltageLevel(CriteriaParamsVoltageLevel.builder().uMaxPu(uMaxPu).build())
                        .build())
                .country("AF").country("AFGHANISTAN")
                .build();
    }

    private static void validatesAgainstTheSchema(String xml) throws Exception {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = schemaFactory.newSchema(new StreamSource(
                DynaFlowCriteriaReproductionTest.class.getResourceAsStream("/criteria.xsd")));
        schema.newValidator().validate(new StreamSource(new StringReader(xml)));
    }

    private static String reference() throws Exception {
        return new String(DynaFlowCriteriaReproductionTest.class.getResourceAsStream("/dynaflowSaCriteria.crt")
                .readAllBytes(), StandardCharsets.UTF_8);
    }

    /**
     * Drops what only the serialisation differs on, so the comparison is of the criteria model: the licence
     * comment, the quote style, the {@code dyn} namespace prefix our writer uses where the file uses the
     * default namespace, the trailing {@code .0} our {@code Double.toString} emits, an empty element written
     * open/close rather than self-closing, {@code id =} spacing, and whitespace.
     */
    private static String normalize(String xml) {
        return xml.replaceAll("(?s)<!--.*?-->", "")
                .replace('\'', '"')
                .replace("xmlns:dyn=", "xmlns=")
                .replace("dyn:", "")
                .replaceAll(">\\s+<", "><")
                .replaceAll("<(\\w+)([^>/]*)></\\1>", "<$1$2/>")
                .replaceAll("(\\d)\\.0(?=\")", "$1")
                .replaceAll("\\s*=\\s*", "=")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
