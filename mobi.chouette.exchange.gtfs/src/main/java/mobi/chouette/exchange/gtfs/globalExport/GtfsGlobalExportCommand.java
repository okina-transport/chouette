package mobi.chouette.exchange.gtfs.globalExport;

import lombok.extern.slf4j.Slf4j;

import mobi.chouette.common.CSVUtils;
import mobi.chouette.common.Context;
import mobi.chouette.common.FileUtil;
import mobi.chouette.common.JobData;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.exchange.exporter.GlobalExportMonitoringService;
import mobi.chouette.exchange.gtfs.Constant;
import mobi.chouette.exchange.gtfs.exporter.GtfsExportParameters;
import mobi.chouette.exchange.gtfs.exporter.GtfsExporterCommand;
import mobi.chouette.exchange.importer.AbstractImporterCommand;
import mobi.chouette.exchange.report.ActionReport;
import mobi.chouette.model.admin.ExportType;
import mobi.chouette.model.admin.GlobalExportMonitoring;
import mobi.chouette.model.admin.JobStatus;
import mobi.chouette.persistence.hibernate.ContextHolder;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;


import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Stateless(name = GtfsGlobalExportCommand.COMMAND)
public class GtfsGlobalExportCommand extends AbstractImporterCommand implements Command, Constant {

    public static final String COMMAND = "GtfsGlobalExportCommand";

    private static final String MERGE_DIRECTORY = "merge";

    @EJB
    private GlobalExportMonitoringService globalExportMonitoringService;

    @Override
    public boolean execute(Context context) throws Exception {
        log.info("GtfsGlobalExportCommand started");

        Object configuration = context.get(CONFIGURATION);
        GtfsExportParameters parameters = (GtfsExportParameters) configuration;

        String exportedReferentials = parameters.getExportedReferentials();
        if (StringUtils.isEmpty(exportedReferentials)){
            log.error("No referentials found");
            return false;
        }

        String[] exportedReferentialTab = exportedReferentials.split(",");

        JobData jobData = (JobData) context.get(JOB_DATA);
        ActionReport actionReport = (ActionReport) context.get(REPORT);

        Map<String, GlobalExportMonitoring> exportMonitoringByReferential = globalExportMonitoringService.initGlobalMonitoring(exportedReferentialTab, parameters.getExportConfigurationId(), jobData.getId(), ExportType.GTFS);
        GlobalExportMonitoring globalExportMonitoring;
        for (String referential : exportedReferentialTab) {
            globalExportMonitoring = exportMonitoringByReferential.get(referential);
            globalExportMonitoring.setStatus(JobStatus.STARTED);
            globalExportMonitoringService.saveMonitoringAdminContext(globalExportMonitoring);
            try {
                executeExportByReferential(context, referential, parameters, jobData);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                globalExportMonitoring.setStatus(JobStatus.FAILED);
                globalExportMonitoringService.saveMonitoringAdminContext(globalExportMonitoring);
            }
            if("OK".equals(actionReport.getResult())){
                globalExportMonitoring.setStatus(JobStatus.OK);
            }else{
                globalExportMonitoring.setStatus(JobStatus.FAILED);
            }

            // Global status must be true. Errors on particular referential must not fail global export
            actionReport.setResult("OK");

            globalExportMonitoringService.saveMonitoringAdminContext(globalExportMonitoring);
        }

        log.info("Referential export completed. Launching merge");
        globalExportMonitoring = exportMonitoringByReferential.get("technique");
        globalExportMonitoring.setStatus(JobStatus.STARTED);
        globalExportMonitoringService.saveMonitoringAdminContext(globalExportMonitoring);
        try {
            launchMerge(jobData);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            globalExportMonitoring.setStatus(JobStatus.FAILED);
            globalExportMonitoringService.saveMonitoringAdminContext(globalExportMonitoring);
        }
        globalExportMonitoring.setStatus(JobStatus.OK);
        globalExportMonitoringService.saveMonitoringAdminContext(globalExportMonitoring);
        log.info("Merge completed successfully");

        return true;
    }

    private void executeExportByReferential(Context context, String referential, GtfsExportParameters parameters, JobData jobData) throws Exception {
        log.info("Starting export for referential : {}", referential);
        ContextHolder.setContext("mobiiti_" + referential);
        parameters.setObjectIdPrefix(referential.toUpperCase());
        InitialContext ctx = (InitialContext) context.get(INITIAL_CONTEXT);
        context.remove(EXPORTABLE_DATA);
        context.remove("line");
        context.remove("line_id");
        context.remove("referential");
        context.remove("scheduled_stop_points");
        ctx.removeFromEnvironment("scheduled_stop_points");
        Command exporterCommand = CommandFactory.create(ctx, GtfsExporterCommand.class.getName());
        exporterCommand.execute(context);

        FileUtil.renameFile(jobData.getPathName() + "/" + jobData.getOutputFilename(), referential + ".zip");
        log.info("Export for referential : {} completed successfully", referential);
    }

    private void launchMerge(JobData jobData) throws IOException, ArchiveException {
        String exportDirectory = jobData.getPathName();
        FileUtil.unzipAllFiles(exportDirectory);
        Set<String> txtFiles = FileUtil.listFilesOfType(exportDirectory, ".txt", false);
        Files.createDirectories(Paths.get(exportDirectory + "/" + MERGE_DIRECTORY));
        for (String txtFile : txtFiles) {
            if ("agency.txt".equals(txtFile) || "stops.txt".equals(txtFile)) {
                generateAggregatedFileWithFiltering(exportDirectory, txtFile);
            }else{
                generateAggregatedFileWithoutFiltering(exportDirectory, txtFile);
            }
        }
        FileUtil.deleteFilesByType(exportDirectory, ".zip");
        String destinationZip = exportDirectory + "/" + jobData.getOutputFilename();
        FileUtil.compress(exportDirectory + "/" + MERGE_DIRECTORY,destinationZip,"gtfs");
        FileUtil.deleteFilesByType(exportDirectory, ".txt");
    }

    private void generateAggregatedFileWithoutFiltering(String exportDirectory, String txtFile) throws IOException {
        log.info("Starting generation of aggregated file {}", txtFile);
        Set<String> filesToAggregate = FileUtil.getFiles(exportDirectory, txtFile);
        boolean isFirstFile = true;
        String mergedFileName = exportDirectory + "/" + MERGE_DIRECTORY + "/" + txtFile  ;


        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(mergedFileName))) {
            for (String fileToAggregate : filesToAggregate) {

                try (Stream<String> lineStream = Files.lines(Paths.get(fileToAggregate))) {
                    List<String> fileLines = lineStream.collect(Collectors.toList());

                    int startLine =  isFirstFile ? 0 : 1;

                    for (int i = startLine; i < fileLines.size(); i++) {
                        writer.write(fileLines.get(i));
                        writer.newLine();
                    }
                    isFirstFile = false;

                }
            }
        }
        log.info("Generation completed. file : {}", txtFile);
    }

    private void generateAggregatedFileWithFiltering(String exportDirectory, String txtFile) throws IOException {
        log.info("Starting generation of aggregated file {}", txtFile);
        Set<String> filesToAggregate = FileUtil.getFiles(exportDirectory, txtFile);
        String mergedFileName = exportDirectory + "/" + MERGE_DIRECTORY + "/" + txtFile  ;
        Set<CSVRecord> finalRecords = new LinkedHashSet<>();
        String headerLine = null;
        Set<String> alreadyProcessedIds = new HashSet<>();

        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(mergedFileName));
             CSVPrinter csvPrinter = new CSVPrinter(writer,
                     CSVFormat.Builder.create().setHeader(headerLine.split(",")).build())) {
            for (String fileToAggregate : filesToAggregate) {
                File currentFile = new File(fileToAggregate);
                Iterable<CSVRecord> records = CSVUtils.getRecords(currentFile);
                if (StringUtils.isEmpty(headerLine)){
                    headerLine = CSVUtils.readFirstLine(currentFile);
                }

                for (CSVRecord record : records) {
                    //  key could be stop_id or agency_id, depending on the file
                    String key = record.get(0);
                    if (!alreadyProcessedIds.contains(key)){
                        finalRecords.add(record);
                        alreadyProcessedIds.add(key);
                    }
                }
            }

            for (CSVRecord record : finalRecords) {
                csvPrinter.printRecord(record);
            }

        }
        log.info("Generation completed. file : {}", txtFile);
    }

    public static class DefaultCommandFactory extends CommandFactory {

        @Override
        protected Command create(InitialContext context) throws IOException {
            Command result = null;
            try {
                String name = "java:app/mobi.chouette.exchange.gtfs/" + COMMAND;
                result = (Command) context.lookup(name);
            } catch (NamingException e) {
                // try another way on test context
                String name = "java:module/" + COMMAND;
                try {
                    result = (Command) context.lookup(name);
                } catch (NamingException e1) {
                    log.error("Unable to find command {}", COMMAND);
                }
            }
            return result;
        }
    }

    static {
        CommandFactory.factories.put(GtfsGlobalExportCommand.class.getName(), new GtfsGlobalExportCommand.DefaultCommandFactory());
    }
}
