package mobi.chouette.exchange.netexprofile.analyzeFile;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.FileUtil;
import mobi.chouette.common.JobData;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.exchange.ProcessingCommands;
import mobi.chouette.exchange.ProcessingCommandsFactory;
import mobi.chouette.exchange.ProgressionCommand;
import mobi.chouette.exchange.netexprofile.Constant;
import mobi.chouette.exchange.importer.AbstractImporterCommand;
import mobi.chouette.exchange.netexprofile.importer.NetexprofileImportParameters;
import mobi.chouette.exchange.netexprofile.importer.util.NetexImportUtil;
import mobi.chouette.exchange.report.ActionReport;
import mobi.chouette.exchange.report.ActionReporter;
import mobi.chouette.exchange.report.AnalyzeReport;

import javax.naming.InitialContext;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Log4j
public class NetexprofileAnalyzeFileCommand extends AbstractImporterCommand implements Command, Constant {

    public static final String COMMAND = "NetexprofileAnalyzeFileCommand";


    @Override
    public boolean execute(Context context) throws Exception {

        boolean result = ERROR;
        InitialContext initialContext = (InitialContext) context.get(INITIAL_CONTEXT);
        ActionReport report = (ActionReport) context.get(REPORT);
        ProgressionCommand progression = (ProgressionCommand) CommandFactory.create(initialContext, ProgressionCommand.class.getName());
        ActionReporter reporter = ActionReporter.Factory.getInstance();

        AnalyzeReport analyzeReport = new AnalyzeReport();
        context.put(ANALYSIS_REPORT, analyzeReport);
        context.put(INCOMING_LINE_LIST, new ArrayList());

        // check params
        Object configuration = context.get(CONFIGURATION);
        if (!(configuration instanceof NetexprofileImportParameters)) {
            reporter.setActionError(context, ActionReporter.ERROR_CODE.INVALID_PARAMETERS,"invalid parameters for netexprofile import " + configuration.getClass().getName());
            return ERROR;
        }

        NetexprofileImportParameters parameters = (NetexprofileImportParameters) configuration;

        ProcessingCommands commands = ProcessingCommandsFactory.create(NetexprofileAnalyzeFileProcessingCommands.class.getName());

        try{
            Mode mode = getMode(context);
            result = process(context, commands, progression, true, mode);
            report.setResult("OK");
            progression.saveAnalyzeReport(context,true);
            log.info("Netex analysis completed");
        }catch(Exception e){
            log.error("Error in netex analysis", e);
            report.setResult("NOK");
        }
        return result;
    }


    /**
     * Determine the import mode.
     * @param context
     * @return
     *      - Line : at least one line file is existing in the zip
     *      - StopAreas : otherwie
     */
    private Mode getMode(Context context) throws IOException {

        NetexprofileImportParameters parameters = (NetexprofileImportParameters) context.get(CONFIGURATION);
        return parameters.getImportMode().equals("LINE") ? Mode.line : Mode.stopareas;


    }

    public static class DefaultCommandFactory extends CommandFactory {

        @Override
        protected Command create(InitialContext context) throws IOException {
            Command result = new NetexprofileAnalyzeFileCommand();
            return result;
        }
    }

    static {
        CommandFactory.factories.put(NetexprofileAnalyzeFileCommand.class.getName(), new NetexprofileAnalyzeFileCommand.DefaultCommandFactory());
    }
}
