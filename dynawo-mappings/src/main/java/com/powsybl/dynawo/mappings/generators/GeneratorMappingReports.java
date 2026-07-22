/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.generators;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.ReportNodeAdder;
import com.powsybl.commons.report.TypedValue;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * What the resolver settled on for a machine, and what it could not give it.
 * <p>
 * A machine asks for controls and capabilities, and gets the model that comes closest. Where that
 * is not the model asked for, the difference used to reach a logger only, so a study ran on a
 * machine without its transformer or its auxiliaries with nothing said where anyone would look.
 * These say it on the report node the run keeps, which is what surfaces in gridsuite and in the
 * report a study is read from afterwards.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public final class GeneratorMappingReports {

    private static final String GENERATOR_ID = "generatorId";
    private static final String CONTROLS = "controls";
    private static final String LIB = "lib";
    private static final String DROPPED = "dropped";
    private static final String REASON = "reason";

    private GeneratorMappingReports() {
    }

    /**
     * The node the resolution of a network's machines is reported under.
     */
    public static ReportNode createGeneratorMappingReportNode(ReportNode reportNode) {
        return reportNode.newReportNode()
                .withMessageTemplate("dynawo.mappings.generatorMapping")
                .add();
    }

    /**
     * A model was found providing everything the machine asked for.
     */
    public static void reportModelSelected(ReportNode reportNode, String generatorId, String lib) {
        reportNode.newReportNode()
                .withMessageTemplate("dynawo.mappings.modelSelected")
                .withTypedValue(GENERATOR_ID, generatorId, TypedValue.ID)
                .withUntypedValue(LIB, lib)
                .add();
    }

    /**
     * A model was built for the machine, nothing installed providing what it asked for.
     */
    public static void reportModelBuilt(ReportNode reportNode, String generatorId, String lib) {
        reportNode.newReportNode()
                .withMessageTemplate("dynawo.mappings.modelBuilt")
                .withTypedValue(GENERATOR_ID, generatorId, TypedValue.ID)
                .withUntypedValue(LIB, lib)
                .add();
    }

    /**
     * The machine got a model short of what it asked for, and why: either nothing was configured
     * to build the missing one, or building it did not work.
     */
    public static void reportCapabilitiesDropped(ReportNode reportNode, String generatorId, String lib,
                                                 Set<GeneratorCapability> dropped, String failure) {
        ReportNodeAdder adder = reportNode.newReportNode()
                .withTypedValue(GENERATOR_ID, generatorId, TypedValue.ID)
                .withUntypedValue(LIB, lib)
                .withUntypedValue(DROPPED, format(dropped))
                .withSeverity(TypedValue.WARN_SEVERITY);
        if (failure == null) {
            adder.withMessageTemplate("dynawo.mappings.capabilitiesDroppedNoBuilder");
        } else {
            adder.withMessageTemplate("dynawo.mappings.capabilitiesDroppedBuildFailed")
                    .withUntypedValue(REASON, failure);
        }
        adder.add();
    }

    /**
     * Nothing at all implements the controls the machine carries, so it goes unmapped.
     */
    public static void reportNoModel(ReportNode reportNode, String generatorId, String controls) {
        reportNode.newReportNode()
                .withMessageTemplate("dynawo.mappings.noModelForControls")
                .withTypedValue(GENERATOR_ID, generatorId, TypedValue.ID)
                .withUntypedValue(CONTROLS, controls)
                .withSeverity(TypedValue.WARN_SEVERITY)
                .add();
    }

    private static String format(Set<GeneratorCapability> capabilities) {
        return capabilities.stream().map(Enum::name).sorted().collect(Collectors.joining(", "));
    }
}
