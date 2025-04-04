package mobi.chouette.exchange.netexprofile.globalExport;

import lombok.extern.slf4j.Slf4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.FileUtil;
import mobi.chouette.common.JobData;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.exchange.exporter.GlobalExportMonitoringService;
import mobi.chouette.exchange.importer.AbstractImporterCommand;
import mobi.chouette.exchange.netexprofile.Constant;
import mobi.chouette.exchange.netexprofile.exporter.NetexprofileExportParameters;
import mobi.chouette.exchange.netexprofile.exporter.NetexprofileExporterCommand;
import mobi.chouette.exchange.report.ActionReport;
import mobi.chouette.model.admin.ExportType;
import mobi.chouette.model.admin.GlobalExportMonitoring;
import mobi.chouette.model.admin.JobStatus;
import mobi.chouette.persistence.hibernate.ContextHolder;
import org.apache.commons.compress.archivers.ArchiveException;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

@Slf4j
@Stateless(name = NetexprofileGlobalExportCommand.COMMAND)
public class NetexprofileGlobalExportCommand extends AbstractImporterCommand implements Command, Constant {

    public static final String COMMAND = "NetexprofileGlobalExportCommand";

    @EJB
    private GlobalExportMonitoringService globalExportMonitoringService;

    @Override
    public boolean execute(Context context) throws Exception {
        log.info("NetexprofileGlobalExportComand started");
        Object configuration = context.get(CONFIGURATION);
        NetexprofileExportParameters parameters = (NetexprofileExportParameters) configuration;
        String exportedReferentials = parameters.getExportedReferentials();

        String[] exportedReferentialTab = exportedReferentials.split(",");

        JobData jobData = (JobData) context.get(JOB_DATA);
        ActionReport actionReport = (ActionReport) context.get(REPORT);
        String mergedFileName = parameters.getExportedFileName();
        Map<String, GlobalExportMonitoring> exportMonitoringByReferential = globalExportMonitoringService.initGlobalMonitoring(exportedReferentialTab, parameters.getExportConfigurationId(), jobData.getId(), ExportType.NETEX);
        GlobalExportMonitoring globalExportMonitoring;

        for (String referential : exportedReferentialTab) {
            globalExportMonitoring = exportMonitoringByReferential.get(referential);
            globalExportMonitoring.setStatus(JobStatus.STARTED);
            globalExportMonitoringService.saveMonitoringAdminContext(globalExportMonitoring);
            try {
                executeExportByReferential(context, referential, jobData, parameters);
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
        log.info("Referential export completed. Launching Netex merge");
        globalExportMonitoring = exportMonitoringByReferential.get("technique");
        globalExportMonitoring.setStatus(JobStatus.STARTED);
        globalExportMonitoringService.saveMonitoringAdminContext(globalExportMonitoring);
        try {
            launchMerge(jobData, mergedFileName);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            globalExportMonitoring.setStatus(JobStatus.FAILED);
            globalExportMonitoringService.saveMonitoringAdminContext(globalExportMonitoring);
        }
        globalExportMonitoring.setStatus(JobStatus.OK);
        globalExportMonitoringService.saveMonitoringAdminContext(globalExportMonitoring);

        // Need to put back original referential to release lock
        jobData.setReferential("mobiiti_technique");
        ContextHolder.setContext("mobiiti_technique");

        log.info("NetexprofileGlobalExportComand completed");
        return true;
    }

    private static void executeExportByReferential(Context context, String referential, JobData jobData, NetexprofileExportParameters parameters) throws Exception {
        log.info("Starting export for referential : {}", referential);
        jobData.setReferential(referential);
        parameters.setExportedFileName(referential +".zip");
        parameters.setDefaultCodespacePrefix(referential.toUpperCase());
        ContextHolder.setContext("mobiiti_" + referential);
        InitialContext ctx = (InitialContext) context.get(INITIAL_CONTEXT);
        context.remove(EXPORTABLE_DATA);
        context.remove("line");
        context.remove("line_id");
        context.remove("referential");
        context.remove("netex_valid_codespaces");
        context.remove("shared_data_keys");
        context.remove("netex_referential");
        context.remove("exportable_netex_data");
        context.remove("scheduled_stop_points");
        ctx.removeFromEnvironment("scheduled_stop_points");

        Command exporterCommand = CommandFactory.create(ctx, NetexprofileExporterCommand.class.getName());
        exporterCommand.execute(context);
        log.info("Export for referential : {} completed successfully", referential);
    }

    private void launchMerge(JobData jobData, String exportedFileName) throws IOException, ArchiveException {
        String exportDirectory = jobData.getPathName();
        FileUtil.unzipAllFiles(exportDirectory);
        Set<String> xmlFiles = FileUtil.listFilesOfType(exportDirectory, ".xml", true);
        FileUtil.deleteFilesByType(exportDirectory, ".zip");
        FileUtil.createZipFromFiles(xmlFiles,exportDirectory + "/" + exportedFileName);
        FileUtil.deleteFilesByType(exportDirectory, ".xml");

    }

    public static class DefaultCommandFactory extends CommandFactory {

        @Override
        protected Command create(InitialContext context) throws IOException {
            Command result = null;
            try {
                String name = "java:app/mobi.chouette.exchange.netexprofile/" + COMMAND;
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
        CommandFactory.factories.put(NetexprofileGlobalExportCommand.class.getName(), new NetexprofileGlobalExportCommand.DefaultCommandFactory());
    }
}
