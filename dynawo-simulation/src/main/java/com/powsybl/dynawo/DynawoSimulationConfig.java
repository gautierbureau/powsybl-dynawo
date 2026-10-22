/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.dynawo;

import com.powsybl.commons.config.ModuleConfig;
import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.dynawo.commons.AbstractDynawoConfig;

import java.nio.file.Path;
import java.util.Objects;

/**
 * @author Marcos de Miguel {@literal <demiguelm at aia.es>}
 */
public class DynawoSimulationConfig extends AbstractDynawoConfig {

    /**
     * How Dynawo is launched
     */
    public enum Launcher {
        /**
         * dynawo.sh, run by the computation manager: the network is exported to an IIDM file, and the results read
         * back from the IIDM file of the final state
         */
        PROCESS,
        /**
         * Dynawo run in this JVM through JNI ({@link DynawoJniLauncher}), on the network itself: no IIDM file
         */
        JNI
    }

    public static final String DYNAWO_LAUNCHER_PROGRAM_NAME = "dynawo";
    private static final String DYNAWO_MODULE_NAME = "dynawo";
    private static final Launcher DEFAULT_LAUNCHER = Launcher.PROCESS;

    private final Launcher launcher;

    public static DynawoSimulationConfig load() {
        return load(DynawoSimulationConfig::new, DYNAWO_MODULE_NAME);
    }

    public static DynawoSimulationConfig load(PlatformConfig platformConfig) {
        return load(DynawoSimulationConfig::new, DYNAWO_MODULE_NAME, platformConfig);
    }

    public DynawoSimulationConfig(Path homeDir, boolean debug) {
        this(homeDir, debug, DEFAULT_LAUNCHER);
    }

    public DynawoSimulationConfig(Path homeDir, boolean debug, Launcher launcher) {
        super(homeDir, debug);
        this.launcher = Objects.requireNonNull(launcher);
    }

    protected DynawoSimulationConfig(ModuleConfig config) {
        super(config);
        this.launcher = config.getEnumProperty("launcher", Launcher.class, DEFAULT_LAUNCHER);
    }

    public Launcher getLauncher() {
        return launcher;
    }

    @Override
    public String getProgram() {
        return getProgram(DYNAWO_LAUNCHER_PROGRAM_NAME);
    }
}
