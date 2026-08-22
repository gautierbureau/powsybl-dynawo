/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.corrections;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.TypedValue;

/**
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public final class CorrectionReports {

    private static final String CORRECTION_NAME = "correctionName";

    private CorrectionReports() {
    }

    static ReportNode createNetworkCorrectionsReportNode(ReportNode reportNode) {
        return reportNode.newReportNode()
                .withMessageTemplate("dynawo.dynasim.networkCorrections")
                .add();
    }

    static void reportCorrectionApplied(ReportNode reportNode, String correctionName) {
        reportNode.newReportNode()
                .withMessageTemplate("dynawo.dynasim.networkCorrectionApplied")
                .withTypedValue(CORRECTION_NAME, correctionName, TypedValue.ID)
                .add();
    }
}
