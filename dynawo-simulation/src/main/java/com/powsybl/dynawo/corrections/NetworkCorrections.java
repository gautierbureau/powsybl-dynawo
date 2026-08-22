/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.corrections;

import com.google.common.base.Suppliers;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.Network;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * The network corrections discovered on the classpath, and the application of the ones a study asked
 * for. Discovery mirrors {@link com.powsybl.dynawo.simplifiers.ModelSimplifiers}: a memoized
 * {@link ServiceLoader} over {@link NetworkCorrection}.
 * <p>
 * A correction is applied at most once per network: the first pass that runs it (a mapping's
 * {@code get_models}, or the run itself) marks it on the network, and later passes skip it. This
 * keeps a correction that mutates the network from being applied twice when the models are looked at
 * before the run. The corrections are applied in the order the active names are given, so a caller
 * that needs one correction to run before another only has to order its set; the framework itself
 * imposes no order.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public class NetworkCorrections {

    private static final Supplier<List<NetworkCorrection>> CORRECTIONS_SUPPLIER =
            Suppliers.memoize(() -> ServiceLoader.load(NetworkCorrection.class).stream()
                    .map(ServiceLoader.Provider::get)
                    .collect(Collectors.toList()));

    private static final String APPLIED_PROPERTY_PREFIX = "dynawo.appliedCorrection.";

    private final Map<String, NetworkCorrection> correctionsByName;

    public NetworkCorrections() {
        this.correctionsByName = new LinkedHashMap<>();
        for (NetworkCorrection correction : CORRECTIONS_SUPPLIER.get()) {
            correctionsByName.putIfAbsent(correction.getCorrectionInfo().name(), correction);
        }
    }

    public List<String> getNetworkCorrectionNames() {
        return CORRECTIONS_SUPPLIER.get().stream()
                .map(correction -> correction.getCorrectionInfo().name())
                .toList();
    }

    public List<NetworkCorrectionInfo> getNetworkCorrectionInfos() {
        return CORRECTIONS_SUPPLIER.get().stream()
                .map(NetworkCorrection::getCorrectionInfo)
                .toList();
    }

    /**
     * Applies, in the given order, the corrections named among the active ones that are not already
     * applied to the network, marking each so it is not applied again. A name matching no correction
     * is ignored, as an unknown simplifier name is.
     */
    public void applyActive(Network network, Set<String> activeNames, ReportNode reportNode) {
        ReportNode correctionsReportNode = null;
        for (String name : activeNames) {
            NetworkCorrection correction = correctionsByName.get(name);
            if (correction == null || network.hasProperty(APPLIED_PROPERTY_PREFIX + name)) {
                continue;
            }
            if (correctionsReportNode == null) {
                correctionsReportNode = CorrectionReports.createNetworkCorrectionsReportNode(reportNode);
            }
            correction.apply(network, correctionsReportNode);
            network.setProperty(APPLIED_PROPERTY_PREFIX + name, Boolean.TRUE.toString());
            CorrectionReports.reportCorrectionApplied(correctionsReportNode, name);
        }
    }
}
