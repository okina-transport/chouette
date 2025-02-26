package mobi.chouette.exchange.netexprofile.globalExport;

import lombok.extern.slf4j.Slf4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.FileUtil;
import mobi.chouette.common.JobData;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.exchange.importer.AbstractImporterCommand;
import mobi.chouette.exchange.netexprofile.Constant;
import mobi.chouette.exchange.netexprofile.exporter.NetexprofileExportParameters;
import mobi.chouette.exchange.netexprofile.exporter.NetexprofileExporterCommand;
import mobi.chouette.persistence.hibernate.ContextHolder;
import org.apache.commons.compress.archivers.ArchiveException;


import javax.naming.InitialContext;
import java.io.IOException;
import java.util.Set;

@Slf4j
public class NetexprofileGlobalExportCommand extends AbstractImporterCommand implements Command, Constant {



    @Override
    public boolean execute(Context context) throws Exception {
        log.info("NetexprofileGlobalExportComand started");
        Object configuration = context.get(CONFIGURATION);
        NetexprofileExportParameters parameters = (NetexprofileExportParameters) configuration;
        String exportedReferentials = parameters.getExportedReferentials();

        String[] exportedReferentialTab = exportedReferentials.split(",");

        JobData jobData = (JobData) context.get(JOB_DATA);
        String mergedFileName = parameters.getExportedFileName();

        for (String referential : exportedReferentialTab) {
            log.info("Starting export for referential : {}", referential);
            jobData.setReferential(referential);
            parameters.setExportedFileName(referential+".zip");
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
        log.info("Referential export completed. Launching Netex merge");
        launchMerge(jobData, mergedFileName);


        // Need to put back original referential to release lock
        jobData.setReferential("mobiiti_technique");

        log.info("NetexprofileGlobalExportComand completed");
        return true;
    }

    private void launchMerge(JobData jobData, String exportedFileName) throws IOException, ArchiveException {
        String exportDirectory = jobData.getPathName().toString();
        FileUtil.unzipAllFiles(exportDirectory);
        Set<String> xmlFiles = FileUtil.listFilesOfType(exportDirectory, ".xml", true);
        FileUtil.deleteFilesByType(exportDirectory, ".zip");
        FileUtil.createZipFromFiles(xmlFiles,exportDirectory + "/" + exportedFileName);
        FileUtil.deleteFilesByType(exportDirectory, ".xml");

    }

    public static class DefaultCommandFactory extends CommandFactory {

        @Override
        protected Command create(InitialContext context) throws IOException {
            Command result = new NetexprofileGlobalExportCommand();
            return result;
        }
    }

    static {
        CommandFactory.factories.put(NetexprofileGlobalExportCommand.class.getName(), new NetexprofileGlobalExportCommand.DefaultCommandFactory());
    }
}
