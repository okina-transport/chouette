package mobi.chouette.exchange.gtfs.importer;

import lombok.Data;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Constant;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Chain;
import mobi.chouette.common.chain.ChainCommand;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.exchange.ProcessingCommands;
import mobi.chouette.exchange.ProcessingCommandsFactory;
import mobi.chouette.exchange.gtfs.model.GtfsRoute;
import mobi.chouette.exchange.gtfs.model.importer.GtfsImporter;
import mobi.chouette.exchange.gtfs.model.importer.Index;
import mobi.chouette.exchange.importer.*;
import mobi.chouette.exchange.parameters.CleanModeEnum;
import mobi.chouette.exchange.validation.ImportedLineValidatorCommand;
import mobi.chouette.exchange.validation.SharedDataValidatorCommand;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import javax.naming.InitialContext;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Data
@Log4j
public class GtfsImporterProcessingCommands implements ProcessingCommands, Constant {

    private static final String CHOUETTE_DISABLE_MOBIITI_IMPORT_COMMAND = "CHOUETTE_DISABLE_MOBIITI_IMPORT_COMMAND";

    public static class DefaultFactory extends ProcessingCommandsFactory {

        @Override
        protected ProcessingCommands create() throws IOException {
            ProcessingCommands result = new GtfsImporterProcessingCommands();
            return result;
        }
    }

    static {
        ProcessingCommandsFactory.factories.put(GtfsImporterProcessingCommands.class.getName(), new DefaultFactory());
    }

    @Override
    public List<? extends Command> getPreProcessingCommands(Context context, boolean withDao) {
        InitialContext initialContext = (InitialContext) context.get(INITIAL_CONTEXT);
        GtfsImportParameters parameters = (GtfsImportParameters) context.get(CONFIGURATION);
        List<Command> commands = new ArrayList<>();
        try {
            if (withDao && CleanModeEnum.fromValue(parameters.getCleanMode()).equals(CleanModeEnum.PURGE)) {
                context.put(CLEAR_FOR_IMPORT, Boolean.TRUE);
                commands.add(CommandFactory.create(initialContext, CleanRepositoryCommand.class.getName()));
            }
            commands.add(CommandFactory.create(initialContext, UncompressCommand.class.getName()));
            commands.add(CommandFactory.create(initialContext, GtfsValidationRulesCommand.class.getName()));
            commands.add(CommandFactory.create(initialContext, GtfsInitImportCommand.class.getName()));
            commands.add(CommandFactory.create(initialContext, GtfsValidationCommand.class.getName()));
        } catch (Exception e) {
            log.error(e, e);
            throw new RuntimeException("unable to call factories");
        }
        return commands;
    }

    @Override
    public List<? extends Command> getLineProcessingCommands(Context context, boolean withDao) {
        InitialContext initialContext = (InitialContext) context.get(INITIAL_CONTEXT);
        GtfsImportParameters parameters = (GtfsImportParameters) context.get(CONFIGURATION);
        boolean level3validation = context.get(VALIDATION) != null;
        List<Command> commands = new ArrayList<>();
        GtfsImporter importer = (GtfsImporter) context.get(PARSER);
        Index<GtfsRoute> index = importer.getRouteById();

        try {
            {
                Chain chain = (Chain) CommandFactory.create(initialContext, ChainCommand.class.getName());
                chain.add(CommandFactory.create(initialContext, GtfsStopParserCommand.class.getName()));
                commands.add(chain);
            }


            if (CleanModeEnum.fromValue(parameters.getCleanMode()).equals(CleanModeEnum.CONTIGUOUS)){
                Chain chain = (Chain) CommandFactory.create(initialContext, ChainCommand.class.getName());
                Command productionPeriods = CommandFactory.create(initialContext, ProductionPeriodCommand.class.getName());
                chain.add(productionPeriods);
                commands.add(chain);
            }

           ArrayList<String> savedLines = new ArrayList<String>();
           Integer cpt = 1;

            String splitCharacter = parameters.getSplitCharacter();
            context.put(TOTAL_NB_OF_LINES,index.getLength());
            for (GtfsRoute gtfsRoute : index) {

                if (StringUtils.isNotEmpty(splitCharacter)){
                    String newRouteId = gtfsRoute.getRouteId().split(parameters.getSplitCharacter())[0];
                    if(parameters.getRouteMerge() && savedLines.contains(newRouteId)) continue;
                    savedLines.add(newRouteId);
                    gtfsRoute.setRouteId(newRouteId.replaceFirst("^"+parameters.getLinePrefixToRemove(),""));
                }



                Chain chain = (Chain) CommandFactory.create(initialContext, ChainCommand.class.getName());

                GtfsRouteParserCommand parser = (GtfsRouteParserCommand) CommandFactory.create(initialContext,
                        GtfsRouteParserCommand.class.getName());
                parser.setGtfsRouteId(gtfsRoute.getRouteId().replaceFirst("^"+parameters.getLinePrefixToRemove(),""));
                if (parameters.isRouteSortOrder()) {
                    parser.setPosition(gtfsRoute.getPosition());
                } else {
                    parser.setPosition(cpt);
                }

                cpt++;
                chain.add(parser);
                if (withDao && !parameters.isNoSave()) {

                    // register
                    Command register = CommandFactory.create(initialContext, LineRegisterCommand.class.getName());
                    chain.add(register);

                    Command copy = CommandFactory.create(initialContext, CopyCommand.class.getName());
                    chain.add(copy);
                }
                if (level3validation) {
                    // add validation
                    Command validate = CommandFactory.create(initialContext,
                            ImportedLineValidatorCommand.class.getName());
                    chain.add(validate);
                }


                Command clean = CommandFactory.create(initialContext, CleanLineInCacheCommand.class.getName());
                chain.add(clean);

                commands.add(chain);
            }
            Chain chain = (Chain) CommandFactory.create(initialContext, ChainCommand.class.getName());

            commands.add(CommandFactory.create(initialContext, ConnectionLinkPersisterCommand.class.getName()));

        } catch (Exception e) {
            log.error(e, e);
            throw new RuntimeException("unable to call factories");
        }

        return commands;
    }

    @Override
    public List<? extends Command> getStopAreaProcessingCommands(Context context, boolean withDao) {
        InitialContext initialContext = (InitialContext) context.get(INITIAL_CONTEXT);
        GtfsImportParameters parameters = (GtfsImportParameters) context.get(CONFIGURATION);

        List<Command> commands = new ArrayList<>();
        try {
            Chain chain = (Chain) CommandFactory.create(initialContext, ChainCommand.class.getName());

            GtfsStopParserCommand parser = (GtfsStopParserCommand) CommandFactory.create(initialContext,
                    GtfsStopParserCommand.class.getName());
            chain.add(parser);
            if (withDao && !parameters.isNoSave() && parameters.getStopAreaImportMode().shouldCreateMissingStopAreas()) {

                // register
                Command register = CommandFactory.create(initialContext, StopAreaRegisterCommand.class.getName());
                chain.add(register);
            }
            commands.add(chain);

        } catch (Exception e) {
            log.error(e, e);
            throw new RuntimeException("unable to call factories");
        }
        return commands;
    }

    @Override
    public List<? extends Command> getPostProcessingCommands(Context context, boolean withDao) {
        return new ArrayList<>();
    }

    @Override
    public List<? extends Command> getPostProcessingCommands(Context context, boolean withDao, boolean allSchemas) {
        InitialContext initialContext = (InitialContext) context.get(INITIAL_CONTEXT);
        boolean level3validation = context.get(VALIDATION) != null;
        GtfsImportParameters parameters = (GtfsImportParameters) context.get(CONFIGURATION);

        List<Command> commands = new ArrayList<>();
        try {
            if (level3validation && !(parameters.getReferencesType().equalsIgnoreCase("stop_area"))) {
                // add shared data validation
                commands.add(CommandFactory.create(initialContext, SharedDataValidatorCommand.class.getName()));
            }
            if (!CollectionUtils.isEmpty(parameters.getGenerateMissingRouteSectionsForModes())) {
                commands.add(CommandFactory.create(initialContext, GenerateRouteSectionsCommand.class.getName()));
            }
        } catch (Exception e) {
            log.error(e, e);
            throw new RuntimeException("unable to call factories");
        }
        return commands;
    }

    @Override
    public List<? extends Command> getDisposeCommands(Context context, boolean withDao) {
        InitialContext initialContext = (InitialContext) context.get(INITIAL_CONTEXT);
        List<Command> commands = new ArrayList<>();
        try {
            commands.add(CommandFactory.create(initialContext, GtfsDisposeImportCommand.class.getName()));

        } catch (Exception e) {
            log.error(e, e);
            throw new RuntimeException("unable to call factories");
        }
        return commands;
    }

    @Override
    public List<? extends Command> getMobiitiCommands(Context context, boolean b) {
        // Ignore les commandes Mobiiti selon paramètre
        if (Optional.ofNullable(System.getenv(CHOUETTE_DISABLE_MOBIITI_IMPORT_COMMAND)).map(Boolean::parseBoolean).orElse(false)) {
            return new ArrayList<>();
        }

        InitialContext initialContext = (InitialContext) context.get(INITIAL_CONTEXT);
        GtfsImportParameters parameters = (GtfsImportParameters) context.get(CONFIGURATION);

        List<Command> commands = new ArrayList<>();

        try {
            commands.add(CommandFactory.create(initialContext, GenerateAttributionsCommand.class.getName()));
            commands.add(CommandFactory.create(initialContext, DeleteLineWithoutOfferCommand.class.getName()));
            if (parameters.getRouteMerge()){
                commands.add(CommandFactory.create(initialContext, MergeTripIdCommand.class.getName()));
            }
//            commands.add(CommandFactory.create(initialContext, MergeDuplicatedJourneyPatternsCommand.class.getName()));
            commands.add(CommandFactory.create(initialContext, AccessibilityCommand.class.getName()));
            commands.add(CommandFactory.create(initialContext, UpdateLineInfosCommand.class.getName()));
            if (parameters.isRoutesReorganization()){
                commands.add(CommandFactory.create(initialContext, RouteMergerCommand.class.getName()));
            }
//            if (parameters.isRouteSortOrder()) {
//                commands.add(CommandFactory.create(initialContext, RouteSortOrderCommand.class.getName()));
//            }




        } catch (Exception e) {
            log.error(e, e);
            throw new RuntimeException("unable to call factories");
        }

        return commands;
    }

}
