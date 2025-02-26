package mobi.chouette.exchange.gtfs.globalExport;

import lombok.extern.slf4j.Slf4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.FileUtil;
import mobi.chouette.common.JobData;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.exchange.gtfs.Constant;

import mobi.chouette.exchange.gtfs.exporter.GtfsExportParameters;
import mobi.chouette.exchange.gtfs.exporter.GtfsExporterCommand;
import mobi.chouette.exchange.importer.AbstractImporterCommand;
import mobi.chouette.persistence.hibernate.ContextHolder;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.lang3.StringUtils;

import javax.naming.InitialContext;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class GtfsGlobalExportCommand extends AbstractImporterCommand implements Command, Constant {

    public static final String COMMAND = "GtfsGlobalExportCommand";

    private static final String mergeDirectory = "merge";


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


        for (String referential : exportedReferentialTab) {
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

            FileUtil.renameFile(jobData.getPathName() + "/" + jobData.getOutputFilename(),referential + ".zip");
            log.info("Export for referential : {} completed successfully", referential);
        }

        log.info("Referential export completed. Launching merge");
        launchMerge(jobData);
        log.info("Merge completed successfully");

        return true;
    }

    private void launchMerge(JobData jobData) throws IOException, ArchiveException {
        String exportDirectory = jobData.getPathName().toString();
        FileUtil.unzipAllFiles(exportDirectory);
        Set<String> txtFiles = FileUtil.listFilesOfType(exportDirectory, ".txt", false);
        Files.createDirectories(Paths.get(exportDirectory + "/" + mergeDirectory));
        for (String txtFile : txtFiles) {
            if ("agency.txt".equals(txtFile) || "stops.txt".equals(txtFile)) {
                generateAggregatedFileWithFiltering(exportDirectory, txtFile);
            }else{
                generateAggregatedFileWithoutFiltering(exportDirectory, txtFile);
            }
        }
        FileUtil.deleteFilesByType(exportDirectory, ".zip");
        String destinationZip = exportDirectory + "/" + jobData.getOutputFilename();
        FileUtil.compress(exportDirectory + "/" + mergeDirectory,destinationZip,"gtfs");
        FileUtil.deleteFilesByType(exportDirectory, ".txt");
    }

    private void generateAggregatedFileWithoutFiltering(String exportDirectory, String txtFile) throws IOException {
        log.info("Starting generation of aggregated file {}", txtFile);
        Set<String> filesToAggregate = FileUtil.getFiles(exportDirectory, txtFile);
        boolean isFirstFile = true;
        String mergedFileName = exportDirectory + "/" + mergeDirectory + "/" + txtFile  ;


        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(mergedFileName));) {
            for (String fileToAggregate : filesToAggregate) {
                Stream<String> lineStream = Files.lines(Paths.get(fileToAggregate));

                List<String> fileLines = lineStream.collect(Collectors.toList());

                int startLine =  isFirstFile ? 0 : 1;

                for (int i = startLine; i < fileLines.size(); i++) {
                    writer.write(fileLines.get(i));
                    writer.newLine();
                }
                isFirstFile = false;
            }
        }
        log.info("Generation completed. file : {}", txtFile);
    }

    private void generateAggregatedFileWithFiltering(String exportDirectory, String txtFile) throws IOException {
        log.info("Starting generation of aggregated file {}", txtFile);
        Set<String> filesToAggregate = FileUtil.getFiles(exportDirectory, txtFile);
        boolean isFirstFile = true;
        String mergedFileName = exportDirectory + "/" + mergeDirectory + "/" + txtFile  ;
        Set<String> finalResults = new LinkedHashSet<>();


        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(mergedFileName));) {
            for (String fileToAggregate : filesToAggregate) {
                Stream<String> lineStream = Files.lines(Paths.get(fileToAggregate));

                List<String> fileLines = lineStream.collect(Collectors.toList());

                int startLine =  isFirstFile ? 0 : 1;

                for (int i = startLine; i < fileLines.size(); i++) {
                    finalResults.add(fileLines.get(i));
                }
                isFirstFile = false;
            }

            for (String line : finalResults) {
                writer.write(line);
                writer.newLine();
            }

        }
        log.info("Generation completed. file : {}", txtFile);
    }

    public static class DefaultCommandFactory extends CommandFactory {

        @Override
        protected Command create(InitialContext context) throws IOException {
            return new GtfsGlobalExportCommand();
        }
    }

    static {
        CommandFactory.factories.put(GtfsGlobalExportCommand.class.getName(), new GtfsGlobalExportCommand.DefaultCommandFactory());
    }
}
