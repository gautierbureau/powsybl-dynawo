/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.computation.AbstractExecutionHandler;
import com.powsybl.computation.Command;
import com.powsybl.computation.CommandExecution;
import com.powsybl.computation.ExecutionReport;
import com.powsybl.computation.local.LocalComputationConfig;
import com.powsybl.dynamicsimulation.DynamicSimulationResult;
import com.powsybl.dynamicsimulation.DynamicSimulationResultImpl;
import com.powsybl.dynamicsimulation.TimelineEvent;
import com.powsybl.dynawo.commons.CommonReports;
import com.powsybl.dynawo.commons.ExportMode;
import com.powsybl.dynawo.commons.NetworkExporter;
import com.powsybl.dynawo.commons.NetworkResultsUpdater;
import com.powsybl.dynawo.commons.SolvedPositions;
import com.powsybl.dynawo.commons.dynawologs.CsvLogParser;
import com.powsybl.dynawo.commons.timeline.TimeLineParser;
import com.powsybl.dynawo.outputvariables.CsvFsvParser;
import com.powsybl.dynawo.xml.JobsXml;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.serde.NetworkSerDe;
import com.powsybl.timeseries.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.powsybl.dynawo.DynawoFilesUtils.*;
import static com.powsybl.dynawo.DynawoSimulationConstants.*;
import static com.powsybl.dynawo.commons.DynawoConstants.*;
import static com.powsybl.dynawo.commons.DynawoUtil.getCommandExecutions;

/**
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
 */
public final class DynawoSimulationHandler extends AbstractExecutionHandler<DynamicSimulationResult> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynawoSimulationHandler.class);
    private static final String OUTPUT_DUMP_FILENAME = "outputState.dmp";
    private static final String ERROR_FILENAME = "dyn_fs_0.err";
    private static final String DYNAWO_ERROR_PATTERN = "DYN Error: ";

    private final DynawoSimulationContext context;
    private final Command command;
    private final DynawoJniLauncher jniLauncher;
    private final Path dynawoHome;
    private final boolean debug;
    private final ReportNode reportNode;

    private final List<TimelineEvent> timeline = new ArrayList<>();
    private final Map<String, DoubleTimeSeries> curves = new LinkedHashMap<>();
    private final Map<String, Double> fsv = new LinkedHashMap<>();
    private final Map<Path, byte[]> jniOutputFiles = new LinkedHashMap<>();
    private DynamicSimulationResult.Status status = DynamicSimulationResult.Status.SUCCESS;
    private String statusText = "";
    private String jniFailure;

    public DynawoSimulationHandler(DynawoSimulationContext context, Command command, ReportNode reportNode) {
        this(context, Objects.requireNonNull(command), null, null, false, reportNode);
    }

    private DynawoSimulationHandler(DynawoSimulationContext context, Command command, DynawoJniLauncher jniLauncher,
                                    Path dynawoHome, boolean debug, ReportNode reportNode) {
        this.context = context;
        this.command = command;
        this.jniLauncher = jniLauncher;
        this.dynawoHome = dynawoHome;
        this.debug = debug;
        this.reportNode = reportNode;
    }

    /**
     * Dynawo run in this JVM through JNI, on the network of the context itself: no IIDM file is written or read, the
     * other input files are given to Dynawo in memory, its outputs are read from memory but its dump files, and there
     * is no command to execute. In debug, the input and output files are written in the working directory after the
     * run, for inspection.
     */
    public static DynawoSimulationHandler withJniLauncher(DynawoSimulationContext context, DynawoJniLauncher jniLauncher,
                                                          Path dynawoHome, boolean debug, ReportNode reportNode) {
        return new DynawoSimulationHandler(context, null, Objects.requireNonNull(jniLauncher), Objects.requireNonNull(dynawoHome), debug, reportNode);
    }

    @Override
    public List<CommandExecution> before(Path workingDir) throws IOException {
        Path basePath = workingDir.resolve(OUTPUTS_FOLDER);
        deleteExistingFile(basePath, FINAL_STATE_FOLDER, OUTPUT_IIDM_FILENAME);
        deleteExistingFile(basePath, CURVES_OUTPUT_PATH, CURVES_FILENAME);
        deleteExistingFile(basePath, FSV_OUTPUT_PATH, FSV_OUTPUT_FILENAME);
        if (jniLauncher != null) {
            Map<Path, byte[]> inputFiles = writeInputFilesInMemory(workingDir);
            writeExecTmpFile(workingDir);
            runThroughJni(workingDir, inputFiles);
            if (debug) {
                writeFiles(inputFiles);
                writeFiles(jniOutputFiles);
            }
            return Collections.emptyList();
        }
        writeInputFiles(workingDir);
        writeExecTmpFile(workingDir);
        return getCommandExecutions(command);
    }

    private static void writeExecTmpFile(Path workingDir) throws IOException {
        Path tmpExecFile = LocalComputationConfig.load().getLocalDir().resolve(EXEC_TMP_FILENAME);
        Files.writeString(tmpExecFile, workingDir.toAbsolutePath().toString());
    }

    private void runThroughJni(Path workingDir, Map<Path, byte[]> inputFiles) {
        Network network = context.getNetwork();
        network.getVariantManager().setWorkingVariant(context.getWorkingVariantId());
        SolvedPositions solvedPositions = SolvedPositions.save(network);
        try {
            jniLauncher.run(dynawoHome, workingDir.resolve(JOBS_FILENAME), inputFiles, jniOutputFiles, network, context.getWorkingVariantId());
            network.getVariantManager().setWorkingVariant(context.getWorkingVariantId());
            solvedPositions.applySolved();
        } catch (PowsyblException e) {
            // a failure of the simulation, as Dynawo prints it in the error file of its process
            String message = String.valueOf(e.getMessage());
            LOGGER.warn("Dynawo failed: {}", message);
            jniFailure = message.startsWith(DYNAWO_ERROR_PATTERN) ? message.substring(DYNAWO_ERROR_PATTERN.length()) : message;
        }
    }

    @Override
    public DynamicSimulationResult after(Path workingDir, ExecutionReport report) throws IOException {
        if (jniLauncher == null) {
            return getResult(workingDir, workingDir.resolve(OUTPUTS_FOLDER));
        }
        // the outputs that Dynawo wrote in memory, read from a copy of the working directory in memory
        try (FileSystem memory = Jimfs.newFileSystem(Configuration.unix())) {
            Path memoryDir = Files.createDirectory(memory.getPath("/" + workingDir.getFileName()));
            Path absoluteWorkingDir = workingDir.toAbsolutePath().normalize();
            for (Map.Entry<Path, byte[]> outputFile : jniOutputFiles.entrySet()) {
                Path file = memoryDir.resolve(absoluteWorkingDir.relativize(outputFile.getKey().toAbsolutePath().normalize()).toString());
                Files.createDirectories(file.getParent());
                Files.write(file, outputFile.getValue());
            }
            return getResult(workingDir, memoryDir.resolve(OUTPUTS_FOLDER));
        }
    }

    /**
     * The result of the simulation, from the outputs of Dynawo in a folder and from its dump files in the working
     * directory
     */
    private DynamicSimulationResult getResult(Path workingDir, Path outputsFolder) throws IOException {
        context.getNetwork().getVariantManager().setWorkingVariant(context.getWorkingVariantId());
        setDynawoLog(outputsFolder, context.getDynawoSimulationParameters().getSpecificLogs());
        setTimeline(outputsFolder);
        if (context.withCurveVariables()) {
            setCurves(outputsFolder);
        }
        if (context.withFsvVariables()) {
            setFinalStateValues(outputsFolder);
        }
        Path errorFile = workingDir.resolve(ERROR_FILENAME);
        if (jniLauncher != null) {
            if (jniFailure == null) {
                setSuccessOutputs(workingDir);
            } else {
                status = DynamicSimulationResult.Status.FAILURE;
                statusText = jniFailure;
            }
        } else if (Files.exists(errorFile)) {
            Matcher errorMatcher = Pattern.compile(DYNAWO_ERROR_PATTERN + "(.*)").matcher(Files.readString(errorFile));
            if (!errorMatcher.find()) {
                setSuccessOutputs(workingDir);
            } else {
                status = DynamicSimulationResult.Status.FAILURE;
                statusText = errorMatcher.group().substring(DYNAWO_ERROR_PATTERN.length());
            }
        } else {
            LOGGER.warn("Error file not found");
            status = DynamicSimulationResult.Status.FAILURE;
            statusText = "Dynawo error log file not found";
        }
        return new DynamicSimulationResultImpl(status, statusText, curves, fsv, timeline);
    }

    private void setSuccessOutputs(Path workingDir) throws IOException {
        // through JNI, Dynawo has written the final state into the network itself
        if (jniLauncher == null) {
            updateNetwork(workingDir);
        }
        DumpFileParameters dumpFileParameters = context.getDynawoSimulationParameters().getDumpFileParameters();
        if (dumpFileParameters.exportDumpFile()) {
            // written in the working directory, also through JNI
            setDumpFile(workingDir.resolve(OUTPUTS_FOLDER), dumpFileParameters.dumpFileFolder(), workingDir.getFileName());
        }
    }

    private void setDynawoLog(Path outputsFolder, Set<DynawoSimulationParameters.SpecificLog> specificLogs) throws IOException {
        Path logFolder = outputsFolder.resolve(LOGS_FOLDER);
        if (Files.exists(logFolder)) {
            Path logFile = logFolder.resolve(LOGS_FILENAME);
            if (Files.exists(logFile)) {
                ReportNode logReportNode = CommonReports.createDynawoLogReportNode(reportNode);
                new CsvLogParser().parse(logFile).forEach(e -> CommonReports.reportLogEntry(logReportNode, e));
            }
            for (DynawoSimulationParameters.SpecificLog specificLog : specificLogs) {
                Path specificLogFile = logFolder.resolve(specificLog.getFileName());
                if (Files.exists(specificLogFile)) {
                    ReportNode logReport = DynawoSimulationReports.createDynawoSpecificLogReportNode(reportNode, specificLog);
                    DynawoSimulationReports.reportSpecificLogEntry(logReport, Files.readString(specificLogFile));
                }
            }
        } else {
            LOGGER.warn("Dynawo logs file not found");
        }
    }

    private void updateNetwork(Path workDir) {
        Path outputNetworkFile = workDir.resolve(OUTPUT_IIDM_FILENAME_PATH);
        if (Files.exists(outputNetworkFile)) {
            NetworkResultsUpdater.update(context.getNetwork(), NetworkSerDe.read(outputNetworkFile), context.getDynawoSimulationParameters().isMergeLoads());
        } else {
            LOGGER.warn("Output IIDM file not found");
            status = DynamicSimulationResult.Status.FAILURE;
            statusText = "Dynawo Output IIDM file not found";
        }
    }

    private void setDumpFile(Path outputsFolder, Path dumpFileFolder, Path fileName) throws IOException {
        Path outputDumpFile = outputsFolder.resolve(FINAL_STATE_FOLDER).resolve(OUTPUT_DUMP_FILENAME);
        if (Files.exists(outputDumpFile)) {
            Files.copy(outputDumpFile, dumpFileFolder.resolve(fileName + "_" + OUTPUT_DUMP_FILENAME), StandardCopyOption.REPLACE_EXISTING);
        } else {
            LOGGER.warn("Dump file {} not found, export will be skipped", OUTPUT_DUMP_FILENAME);
        }
    }

    private void setTimeline(Path outputsFolder) {
        ExportMode exportMode = context.getDynawoSimulationParameters().getTimelineExportMode();
        Path timelineFile = outputsFolder.resolve(TIMELINE_FOLDER).resolve(TIMELINE_FILENAME + exportMode.getFileExtension());
        if (Files.exists(timelineFile)) {
            TimeLineParser.parse(timelineFile, exportMode)
                        .forEach(e -> timeline.add(new TimelineEvent(e.time(), e.modelName(), e.message())));
        } else {
            LOGGER.warn("Timeline file not found");
        }
    }

    private void setCurves(Path workingDir) throws IOException {
        Path curvesPath = workingDir.resolve(CURVES_OUTPUT_PATH).resolve(CURVES_FILENAME);
        if (Files.exists(curvesPath)) {
            if (Files.size(curvesPath) > 0) {
                TimeSeries.parseCsv(curvesPath, new TimeSeriesCsvConfig(TimeSeriesConstants.DEFAULT_SEPARATOR, false,
                                TimeSeries.TimeFormat.FRACTIONS_OF_SECOND, true)).values()
                        .forEach(l -> l.forEach(curve -> curves.put(curve.getMetadata().getName(), (DoubleTimeSeries) curve)));
            } else {
                LOGGER.warn("CRV file is empty");
                status = DynamicSimulationResult.Status.FAILURE;
                statusText = "CRV file is empty";
            }
        } else {
            LOGGER.warn("Curves folder not found");
            status = DynamicSimulationResult.Status.FAILURE;
            statusText = "Dynawo curves folder not found";
        }
    }

    private void setFinalStateValues(Path workingDir) {
        Path fsvPath = workingDir.resolve(FSV_OUTPUT_PATH).resolve(FSV_OUTPUT_FILENAME);
        if (Files.exists(fsvPath)) {
            new CsvFsvParser(';').parse(fsvPath).forEach(e -> fsv.put(e.model() + "_" + e.variable(), e.value()));
        } else {
            LOGGER.warn("Final state values folder not found");
            status = DynamicSimulationResult.Status.FAILURE;
            statusText = "Dynawo final state values folder not found";
        }
    }

    private void writeInputFiles(Path workingDir) throws IOException {
        NetworkExporter.writeIidm(context.getNetwork(), workingDir.resolve(NETWORK_FILENAME),
                context.getCurrentDynawoVersion(), context.getDynawoSimulationParameters().isMergeLoads());
        JobsXml.write(workingDir, context);
        DynawoFilesUtils.writeInputFiles(workingDir, context);
    }

    /**
     * Through JNI, the input files are given to Dynawo in memory, by the paths it reads them from in the working
     * directory, without being written there. The network is not one of them: Dynawo reads it itself, and writes the
     * final state into it, with no IIDM file of the final state. Only the dump file of an initial state, that Dynawo
     * reads from the file system, is written in the working directory.
     */
    private Map<Path, byte[]> writeInputFilesInMemory(Path workingDir) throws IOException {
        try (FileSystem memory = Jimfs.newFileSystem(Configuration.unix())) {
            Path memoryDir = Files.createDirectory(memory.getPath("/" + workingDir.getFileName()));
            JobsXml.write(memoryDir, context, false);
            DynawoFilesUtils.writeInputFiles(memoryDir, workingDir, context);
            Map<Path, byte[]> inputFiles = new LinkedHashMap<>();
            try (Stream<Path> files = Files.walk(memoryDir)) {
                for (Path file : files.filter(Files::isRegularFile).sorted().toList()) {
                    inputFiles.put(workingDir.resolve(memoryDir.relativize(file).toString()), Files.readAllBytes(file));
                }
            }
            return inputFiles;
        }
    }

    /**
     * The files that Dynawo read or wrote in memory, written at their paths
     */
    private static void writeFiles(Map<Path, byte[]> files) throws IOException {
        for (Map.Entry<Path, byte[]> file : files.entrySet()) {
            Files.createDirectories(file.getKey().getParent());
            Files.write(file.getKey(), file.getValue());
        }
    }
}
