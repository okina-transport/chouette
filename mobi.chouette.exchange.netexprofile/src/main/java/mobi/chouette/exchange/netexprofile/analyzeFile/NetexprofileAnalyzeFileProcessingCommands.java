package mobi.chouette.exchange.netexprofile.analyzeFile;

import lombok.Data;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Constant;
import mobi.chouette.common.Context;
import mobi.chouette.common.FileUtil;
import mobi.chouette.common.JobData;
import mobi.chouette.common.chain.Chain;
import mobi.chouette.common.chain.ChainCommand;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.common.parallel.ParallelExecutionCommand;
import mobi.chouette.exchange.ProcessingCommands;
import mobi.chouette.exchange.ProcessingCommandsFactory;
import mobi.chouette.exchange.fileAnalysis.ProcessAnalyzeCommand;
import mobi.chouette.exchange.fileAnalysis.TimetableCheckCommand;
import mobi.chouette.exchange.importer.CleanRepositoryCommand;
import mobi.chouette.exchange.importer.UncompressCommand;
import mobi.chouette.exchange.netexprofile.importer.DuplicateIdCheckerCommand;
import mobi.chouette.exchange.netexprofile.importer.NetexCommonFilesParserCommand;
import mobi.chouette.exchange.netexprofile.importer.NetexDisposeImportCommand;
import mobi.chouette.exchange.netexprofile.importer.NetexInitImportCommand;
import mobi.chouette.exchange.netexprofile.importer.NetexInitReferentialCommand;
import mobi.chouette.exchange.netexprofile.importer.NetexLineParserCommand;
import mobi.chouette.exchange.netexprofile.importer.NetexSchemaValidationCommand;
import mobi.chouette.exchange.netexprofile.importer.NetexValidationCommand;
import mobi.chouette.exchange.netexprofile.importer.NetexprofileImportParameters;
import mobi.chouette.exchange.netexprofile.importer.NetexprofileLineDeleteCommand;
import mobi.chouette.exchange.netexprofile.importer.util.IdVersion;
import mobi.chouette.exchange.netexprofile.importer.util.NetexImportUtil;
import mobi.chouette.exchange.parameters.CleanModeEnum;
import mobi.chouette.exchange.report.ActionReporter;
import mobi.chouette.exchange.report.IO_TYPE;
import org.apache.commons.lang.StringUtils;

import javax.naming.InitialContext;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static mobi.chouette.exchange.netexprofile.Constant.NETEX_FILE_PATHS;

@Data
@Log4j
public class NetexprofileAnalyzeFileProcessingCommands implements ProcessingCommands, Constant {


    private Integer lineValidationTimeoutSeconds;

    public static class DefaultFactory extends ProcessingCommandsFactory {

        @Override
        protected ProcessingCommands create() throws IOException {
            NetexprofileAnalyzeFileProcessingCommands result = new NetexprofileAnalyzeFileProcessingCommands();

            String lineValidationTimeoutPropertyKey = "iev.netex.validation.line.parallel.execution.timeout.seconds";
            String lineValidationTimeoutString = System.getProperty(lineValidationTimeoutPropertyKey);
            if (StringUtils.isNotEmpty(lineValidationTimeoutString)) {
                result.lineValidationTimeoutSeconds = Integer.parseInt(lineValidationTimeoutString);
                log.info("Parallel execution line validation command configured with time out seconds: " + result.lineValidationTimeoutSeconds);
            }

            return result;
        }
    }

    static {
        ProcessingCommandsFactory.factories.put(NetexprofileAnalyzeFileProcessingCommands.class.getName(), new DefaultFactory());
    }

    @Override
    public List<? extends Command> getPreProcessingCommands(Context context, boolean withDao) {
        InitialContext initialContext = (InitialContext) context.get(INITIAL_CONTEXT);
        NetexprofileImportParameters parameters = (NetexprofileImportParameters) context.get(CONFIGURATION);
        List<Command> commands = new ArrayList<>();
        try {
            Chain initChain = (Chain) CommandFactory.create(initialContext, ChainCommand.class.getName());
            if (withDao && CleanModeEnum.fromValue(parameters.getCleanMode()).equals(CleanModeEnum.PURGE)) {
                initChain.add(CommandFactory.create(initialContext, CleanRepositoryCommand.class.getName()));
            }
            initChain.add(CommandFactory.create(initialContext, UncompressCommand.class.getName()));
            initChain.add(CommandFactory.create(initialContext, NetexInitImportCommand.class.getName()));
            commands.add(initChain);

            context.put(CLEAR_FOR_IMPORT, CleanModeEnum.fromValue(parameters.getCleanMode()).equals(CleanModeEnum.PURGE));

        } catch (Exception e) {
            log.error(e, e);
            throw new RuntimeException("unable to call factories");
        }

        return commands;
    }

    @Override
    public List<? extends Command> getLineProcessingCommands(Context context, boolean withDao) {
        InitialContext initialContext = (InitialContext) context.get(INITIAL_CONTEXT);
        NetexprofileImportParameters parameters = (NetexprofileImportParameters) context.get(CONFIGURATION);
        ActionReporter reporter = ActionReporter.Factory.getInstance();

        List<Command> commands = new ArrayList<>();
        JobData jobData = (JobData) context.get(JOB_DATA);
        Path path = Paths.get(jobData.getPathName(), INPUT);

        try {
            Chain mainChain = (Chain) CommandFactory.create(initialContext, ChainCommand.class.getName());
            commands.add(mainChain);


            // Report any files that are not XML files
            List<Path> excluded = FileUtil.listFiles(path, "*", "*.xml");

            if (!excluded.isEmpty()) {
                for (Path exclude : excluded) {
                    reporter.setFileState(context, exclude.getFileName().toString(), IO_TYPE.INPUT, ActionReporter.FILE_STATE.IGNORED);
                }
            }

            // stream all file paths once
            List<Path> allFilePaths = FileUtil.listFiles(path, "*.xml", ".*.xml");
            Collections.sort(allFilePaths);
            for (Path p : allFilePaths) {
                reporter.setFileState(context, p.getFileName().toString(), IO_TYPE.INPUT, ActionReporter.FILE_STATE.OK);
            }
            context.put(NETEX_FILE_PATHS, allFilePaths);

            // schema validation
            if (parameters.isValidateAgainstSchema()) {
                NetexSchemaValidationCommand schemaValidation = (NetexSchemaValidationCommand) CommandFactory.create(initialContext,
                        NetexSchemaValidationCommand.class.getName());

                mainChain.add(schemaValidation);
            }
            // common file parsing

            List<Path> commonFilePaths = allFilePaths.stream().filter(
                    filePath -> filePath.getFileName() != null && NetexImportUtil.isCommonFile(filePath.getFileName().toString()))
                                                                                                     .collect(Collectors.toList());

            ChainCommand commonFileChains = (ChainCommand) CommandFactory.create(initialContext, ChainCommand.class.getName());
            commonFileChains.setIgnored(parameters.isContinueOnLineErrors());

            mainChain.add(commonFileChains);

            context.put(mobi.chouette.exchange.netexprofile.Constant.NETEX_COMMON_FILE_IDENTIFICATORS, new HashMap<IdVersion, List<String>>());


            for (Path file : commonFilePaths) {
                Chain commonFileChain = (Chain) CommandFactory.create(initialContext, ChainCommand.class.getName());
                commonFileChains.add(commonFileChain);

                // init referentials
                NetexInitReferentialCommand initializer = (NetexInitReferentialCommand) CommandFactory.create(initialContext,
                        NetexInitReferentialCommand.class.getName());
                initializer.setPath(file);
                initializer.setLineFile(false);
                commonFileChain.add(initializer);

                // profile validation
                if(parameters.isValidateAgainstProfile()) {
                    Command validator = CommandFactory.create(initialContext, NetexValidationCommand.class.getName());
                    commonFileChain.add(validator);
                }
                NetexCommonFilesParserCommand commonFilesParser = (NetexCommonFilesParserCommand) CommandFactory.create(initialContext,
                        NetexCommonFilesParserCommand.class.getName());
                commonFileChain.add(commonFilesParser);
            }

            // Check for duplicate identifiers declared in common files
            DuplicateIdCheckerCommand duplicateIdChecker = (DuplicateIdCheckerCommand) CommandFactory.create(initialContext,
                    DuplicateIdCheckerCommand.class.getName());
            mainChain.add(duplicateIdChecker);

            // line file processing
            List<Path> lineFilePaths = allFilePaths.stream().filter(
                    filePath -> filePath.getFileName() != null && !NetexImportUtil.isCommonFile(filePath.getFileName().toString()))
                    .collect(Collectors.toList());

            context.put(TOTAL_NB_OF_LINES, lineFilePaths.size());

            // profile validation
            if (parameters.isValidateAgainstProfile()) {
                addLineValidationCommands(mainChain, context, lineFilePaths);
            }

            if (withDao && !parameters.isNoSave()) {
                addLineCommands(mainChain, context, lineFilePaths);
            }

        } catch (Exception e) {
            log.error("Error creating importer commands", e);
        }

        return commands;
    }


    private void addLineCommands(Chain mainChain, Context context, List<Path> lineFilePaths) throws IOException, ClassNotFoundException {
        InitialContext initialContext = (InitialContext) context.get(INITIAL_CONTEXT);

        if (lineFilePaths.size() == 0){
            //if no line file is available, a single "processAnalyzeCommand" is launch to count stopAreas
            Command analyzeCommand = CommandFactory.create(initialContext, ProcessAnalyzeCommand.class.getName());
            mainChain.add(analyzeCommand);
            return ;
        }



        NetexprofileImportParameters parameters = (NetexprofileImportParameters) context.get(CONFIGURATION);

        ChainCommand lineChains = (ChainCommand) CommandFactory.create(initialContext, ChainCommand.class.getName());
        lineChains.setIgnored(parameters.isContinueOnLineErrors());


        if (lineFilePaths.size() > 0){
            mainChain.add(lineChains);
        }

        for (Path file : lineFilePaths) {
            Chain lineChain = (Chain) CommandFactory.create(initialContext, ChainCommand.class.getName());
            lineChains.add(lineChain);

            // init referentials
            NetexInitReferentialCommand initializer = (NetexInitReferentialCommand) CommandFactory.create(initialContext,
                    NetexInitReferentialCommand.class.getName());
            initializer.setPath(file);
            initializer.setLineFile(true);
            lineChain.add(initializer);

            // parsing
            NetexLineParserCommand parser = (NetexLineParserCommand) CommandFactory.create(initialContext, NetexLineParserCommand.class.getName());
            parser.setPath(file);
            lineChain.add(parser);

            if (!parameters.isNoSave()) {

                Command clean = CommandFactory.create(initialContext, NetexprofileLineDeleteCommand.class.getName());
                lineChain.add(clean);

                // analyze
                Command analyzeCommand = CommandFactory.create(initialContext, ProcessAnalyzeCommand.class.getName());
                lineChain.add(analyzeCommand);

            }
        }


    }
    private void addLineValidationCommands(Chain mainChain, Context context, List<Path> lineFilePaths) throws IOException, ClassNotFoundException {

        if (lineFilePaths.size() == 0)
            return ;

        InitialContext initialContext = (InitialContext) context.get(INITIAL_CONTEXT);

        ParallelExecutionCommand lineValidationCommands = (ParallelExecutionCommand) CommandFactory.create(initialContext, ParallelExecutionCommand.class.getName());
        if (lineValidationTimeoutSeconds != null) {
            lineValidationCommands.setTimeoutSeconds(lineValidationTimeoutSeconds);
        }
        mainChain.add(lineValidationCommands);

        // Compare by file size, largest first
        List<Path> allPathsSortedLargestFirst = new ArrayList<>(lineFilePaths);
        Collections.sort(allPathsSortedLargestFirst, (o1, o2) -> (int) (o2.toFile().length() - o1.toFile().length()));
        for (Path file : allPathsSortedLargestFirst) {
            Chain lineChain = (Chain) CommandFactory.create(initialContext, ChainCommand.class.getName());

            lineValidationCommands.add(lineChain, c -> new Context(context));

            // init referentials
            NetexInitReferentialCommand initializer = (NetexInitReferentialCommand) CommandFactory.create(initialContext,
                    NetexInitReferentialCommand.class.getName());
            initializer.setPath(file);
            initializer.setLineFile(true);
            lineChain.add(initializer);

            Command validator = CommandFactory.create(initialContext, NetexValidationCommand.class.getName());
            lineChain.add(validator);

        }
    }





    @Override
    public List<? extends Command> getPostProcessingCommands(Context context, boolean withDao, boolean allSchemas) {
        return new ArrayList<>();
    }

    @Override
    public List<? extends Command> getStopAreaProcessingCommands(Context context, boolean withDao) {
        return new ArrayList<>();
    }

    @Override
    public List<? extends Command> getPostProcessingCommands(Context context, boolean withDao) {
        return new ArrayList<>();
    }

    @Override
    public List<? extends Command> getDisposeCommands(Context context, boolean withDao) {
        List<Command> commands = new ArrayList<>();
        InitialContext initialContext = (InitialContext) context.get(INITIAL_CONTEXT);
        try {
            commands.add(CommandFactory.create(initialContext, NetexDisposeImportCommand.class.getName()));
        } catch (Exception e) {
            log.error(e, e);
            throw new RuntimeException("unable to call factories");
        }
        return commands;
    }

    @Override
    public List<? extends Command> getMobiitiCommands(Context context, boolean b) {
        NetexprofileImportParameters parameters = (NetexprofileImportParameters) context.get(CONFIGURATION);
        List<Command> commands = new ArrayList<>();
        InitialContext initialContext = (InitialContext) context.get(INITIAL_CONTEXT);
        try {

            if (!CleanModeEnum.fromValue(parameters.getCleanMode()).equals(CleanModeEnum.PURGE)){
                Command timetableCheckCommand = CommandFactory.create(initialContext, TimetableCheckCommand.class.getName());
                commands.add(timetableCheckCommand);
            }

        } catch (ClassNotFoundException | IOException e) {
            log.error(e.getStackTrace());
        }

        return commands;
    }

}
