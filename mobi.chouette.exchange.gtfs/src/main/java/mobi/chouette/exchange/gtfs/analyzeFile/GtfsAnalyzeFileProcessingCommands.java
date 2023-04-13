package mobi.chouette.exchange.gtfs.analyzeFile;

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
import mobi.chouette.exchange.fileAnalysis.GeolocationCheckCommand;
import mobi.chouette.exchange.fileAnalysis.ProcessAnalyzeCommand;
import mobi.chouette.exchange.fileAnalysis.TimetableCheckCommand;
import mobi.chouette.exchange.fileAnalysis.TooManyNewStopsCheckCommand;
import mobi.chouette.exchange.gtfs.importer.GtfsImportParameters;
import mobi.chouette.exchange.gtfs.importer.GtfsInitImportCommand;
import mobi.chouette.exchange.gtfs.importer.GtfsRouteParserCommand;
import mobi.chouette.exchange.gtfs.importer.GtfsStopParserCommand;
import mobi.chouette.exchange.gtfs.importer.GtfsValidationCommand;
import mobi.chouette.exchange.gtfs.importer.GtfsValidationRulesCommand;
import mobi.chouette.exchange.gtfs.model.GtfsRoute;
import mobi.chouette.exchange.gtfs.model.importer.GtfsImporter;
import mobi.chouette.exchange.gtfs.model.importer.Index;
import mobi.chouette.exchange.importer.LineRegisterCommand;
import mobi.chouette.exchange.importer.UncompressCommand;
import mobi.chouette.exchange.parameters.CleanModeEnum;
import org.apache.commons.lang3.StringUtils;

import javax.naming.InitialContext;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Data
@Log4j
public class GtfsAnalyzeFileProcessingCommands implements ProcessingCommands, Constant {

    public static class DefaultFactory extends ProcessingCommandsFactory {

        @Override
        protected ProcessingCommands create() throws IOException {
            ProcessingCommands result = new GtfsAnalyzeFileProcessingCommands();
            return result;
        }
    }

    static {
        ProcessingCommandsFactory.factories.put(GtfsAnalyzeFileProcessingCommands.class.getName(), new DefaultFactory());
    }

    @Override
    public List<? extends Command> getPreProcessingCommands(Context context, boolean withDao) {
        InitialContext initialContext = (InitialContext) context.get(INITIAL_CONTEXT);
        GtfsImportParameters parameters = (GtfsImportParameters) context.get(CONFIGURATION);
        List<Command> commands = new ArrayList<>();
        try {
            commands.add(CommandFactory.create(initialContext, UncompressCommand.class.getName()));
            commands.add(CommandFactory.create(initialContext, GtfsValidationRulesCommand.class.getName()));
            commands.add(CommandFactory.create(initialContext, GtfsInitImportCommand.class.getName()));
            commands.add(CommandFactory.create(initialContext, GtfsValidationCommand.class.getName()));


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
        GtfsImportParameters parameters = (GtfsImportParameters) context.get(CONFIGURATION);
        boolean level3validation = context.get(VALIDATION) != null;
        List<Command> commands = new ArrayList<>();
        GtfsImporter importer = (GtfsImporter) context.get(PARSER);
        Index<GtfsRoute> index = importer.getRouteById();

        try {
            {
                Chain chain = (Chain) CommandFactory.create(initialContext, ChainCommand.class.getName());
                chain.add(CommandFactory.create(initialContext, GtfsStopParserCommand.class.getName()));
//				if (withDao && !parameters.isNoSave() && parameters.getStopAreaImportMode().shouldCreateMissingStopAreas()) {
//					Command saveArea = CommandFactory.create(initialContext, StopAreaRegisterCommand.class.getName());
//					chain.add(saveArea);
//				}
                commands.add(chain);
            }

            {
//                Chain chain = (Chain) CommandFactory.create(initialContext, ChainCommand.class.getName());
//                Command productionPeriods = CommandFactory.create(initialContext, ProductionPeriodCommand.class.getName());
//                chain.add(productionPeriods);
//                commands.add(chain);
            }

            ArrayList<String> savedLines = new ArrayList<String>();
            Integer cpt = 1;

            String splitCharacter = parameters.getSplitCharacter();
            context.put(TOTAL_NB_OF_LINES, index.getLength());
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
                parser.setPosition(cpt);
                cpt++;
                chain.add(parser);


                // register
                Command analyzeCommand = CommandFactory.create(initialContext, ProcessAnalyzeCommand.class.getName());
                chain.add(analyzeCommand);

                commands.add(chain);
            }



        } catch (Exception e) {
            log.error(e, e);
            throw new RuntimeException("unable to call factories");
        }

        return commands;
    }

    @Override
    public List<? extends Command> getStopAreaProcessingCommands(Context context, boolean withDao) {
        return new ArrayList<>();
    }

    @Override
    public List<? extends Command> getPostProcessingCommands(Context context, boolean withDao) {
        return  new ArrayList<>();
    }

    @Override
    public List<? extends Command> getPostProcessingCommands(Context context, boolean withDao, boolean allSchemas) {
        return new ArrayList<>();
    }

    @Override
    public List<? extends Command> getDisposeCommands(Context context, boolean withDao) {
        return  new ArrayList<>();
    }

    @Override
    public List<? extends Command> getMobiitiCommands(Context context, boolean b) {

        GtfsImportParameters parameters = (GtfsImportParameters) context.get(CONFIGURATION);
        List<Command> commands = new ArrayList<>();
        InitialContext initialContext = (InitialContext) context.get(INITIAL_CONTEXT);
        try {
            if (!parameters.isKeepStopGeolocalisation()) {
                Command geolocationCheckCommand = CommandFactory.create(initialContext, GeolocationCheckCommand.class.getName());
                commands.add(geolocationCheckCommand);
            }
            Command tooManyNewStopsCheckCommand = CommandFactory.create(initialContext, TooManyNewStopsCheckCommand.class.getName());
            commands.add(tooManyNewStopsCheckCommand);

            if (!CleanModeEnum.fromValue(parameters.getCleanMode()).equals(CleanModeEnum.PURGE)){
                Command timetableCheckCommand = CommandFactory.create(initialContext, TimetableCheckCommand.class.getName());
                commands.add(timetableCheckCommand);
            }

        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();        }

        return commands;
    }

}
