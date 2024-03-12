package mobi.chouette.exchange.netexprofile.exporter;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.naming.InitialContext;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Color;
import mobi.chouette.common.Context;
import mobi.chouette.common.JobData;
import mobi.chouette.common.chain.ChainCommand;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.exchange.ProcessingCommands;
import mobi.chouette.exchange.ProcessingCommandsFactory;
import mobi.chouette.exchange.netexprofile.Constant;
import mobi.chouette.exchange.netexprofile.importer.NetexImporterProcessingCommands;
import mobi.chouette.exchange.netexprofile.importer.NetexInitImportCommand;
import mobi.chouette.exchange.netexprofile.importer.NetexprofileImportParameters;

@Log4j
public class NetexValidateExportCommand implements Command, Constant {

    public static final String COMMAND = "NetexValidateExportCommand";

    @Override
    public boolean execute(Context context) throws Exception {
        boolean result = ERROR;

        Monitor monitor = MonitorFactory.start(COMMAND);

        try {
            Context validateExportContext = new Context();
            validateExportContext.putAll(context);

            validateExportContext.put(STREAM_TO_CLOSE, new ArrayList<>());


            NetexprofileExportParameters configuration = (NetexprofileExportParameters) context.get(CONFIGURATION);
            

            NetexprofileImportParameters parameters = new NetexprofileImportParameters();
            parameters.setOrganisationName(configuration.getOrganisationName());
            parameters.setUserName(configuration.getUserName());
            parameters.setName(configuration.getName());
            parameters.setNoSave(true);
            parameters.setReferentialName(configuration.getReferentialName());
            parameters.setParseSiteFrames(configuration.isExportStops());
            parameters.setContinueOnLineErrors(true);

            validateExportContext.put(CONFIGURATION, parameters);
            validateExportContext.put(REPORT, context.get(REPORT));

            JobData jobData = (JobData) context.get(JOB_DATA);
            String pathName = jobData.getPathName();
            File output = new File(pathName, OUTPUT);
            File input = new File(pathName, INPUT);

            if (!output.renameTo(input)) {
                log.error("rename failed");
            }

            output = new File(pathName, OUTPUT);
            InitialContext initialContext = (InitialContext) context.get(INITIAL_CONTEXT);

            try {

                ChainCommand validationChainCommand = (ChainCommand) CommandFactory.create(initialContext, ChainCommand.class.getName());
                validationChainCommand.add(CommandFactory.create(initialContext, NetexInitImportCommand.class.getName()));

        		ProcessingCommands commands = ProcessingCommandsFactory.create(NetexImporterProcessingCommands.class.getName());
        		commands.getLineProcessingCommands(validateExportContext, false).stream().forEach(e -> validationChainCommand.add(e));
        		
        		result = validationChainCommand.execute(validateExportContext);
                input.renameTo(output);
        		
            } catch (Exception ex) {
                log.error("Exception during validation of exported files : ", ex);
                throw ex;
            }
            context.put(VALIDATION_REPORT, validateExportContext.get(VALIDATION_REPORT));

            closeOpenSteams(validateExportContext);


        } catch (Exception e) {
            log.error(e, e);
            throw e;
        } finally {
            log.info(Color.MAGENTA + monitor.stop() + Color.NORMAL);
        }

        return result;
    }

    private void closeOpenSteams(Context context){
        List<BufferedInputStream> streamsToClose = (List<BufferedInputStream>)context.get(STREAM_TO_CLOSE);

        if (streamsToClose != null){
            for (BufferedInputStream bufferedInputStream : streamsToClose) {
                if(bufferedInputStream != null){
                    try {
                        bufferedInputStream.close();
                    } catch (IOException e) {
                        log.error("Error while closing stream", e);
                    }
                }
            }
        }
    }

    public static class DefaultCommandFactory extends CommandFactory {

        @Override
        protected Command create(InitialContext context) throws IOException {
            Command result = new NetexValidateExportCommand();
            return result;
        }
    }

    static {
        CommandFactory.factories.put(NetexValidateExportCommand.class.getName(), new NetexValidateExportCommand.DefaultCommandFactory());
    }

}
