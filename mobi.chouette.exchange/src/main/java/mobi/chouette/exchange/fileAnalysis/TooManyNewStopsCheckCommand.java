package mobi.chouette.exchange.fileAnalysis;

import com.google.common.collect.Lists;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Constant;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.StopAreaDAO;
import mobi.chouette.exchange.importer.AbstractImporterCommand;
import mobi.chouette.exchange.report.AnalyzeReport;
import mobi.chouette.model.StopArea;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Class used to check if new incoming StopAreas are too many
 * limit 20% number of new stops / number of stops in database
 */
@Log4j
@Stateless(name = TooManyNewStopsCheckCommand.COMMAND)
public class TooManyNewStopsCheckCommand extends AbstractImporterCommand implements Command, Constant {

    public static final String COMMAND = "TooManyNewStopsCheckCommand";

    static {
        CommandFactory.factories.put(TooManyNewStopsCheckCommand.class.getName(), new TooManyNewStopsCheckCommand.DefaultCommandFactory());
    }

    @EJB
    private
    StopAreaDAO stopAreaDAO;

    @Override
    public boolean execute(Context context) throws Exception {
        log.info("Starting too many new stops check :");
        AnalyzeReport analyzeReport = (AnalyzeReport) context.get(ANALYSIS_REPORT);

        List<List<StopArea>> list = Lists.partition(analyzeReport.getStops(), 1000);
        List<StopArea> newStops = new ArrayList<>();

        for (List<StopArea> stopAreasIncoming : list) {
            List<String> stopAreaOriginalIdList = stopAreasIncoming.stream()
                    .map(StopArea::getOriginalStopId)
                    .collect(Collectors.toList());

            List<StopArea> subListStopAreasAlreadyInDB = stopAreaDAO.findByOriginalIds(stopAreaOriginalIdList);

            newStops.addAll(stopAreasIncoming.stream()
                    .filter(incomingStopArea -> subListStopAreasAlreadyInDB.stream()
                            .noneMatch(stopAreaInDB ->
                                    stopAreaInDB.getOriginalStopId().equals(incomingStopArea.getOriginalStopId())
                            )
                    )
                    .collect(Collectors.toList()));
        }

        analyzeReport.getNewStops().addAll(newStops);

        if (!analyzeReport.getStops().isEmpty() && !analyzeReport.getNewStops().isEmpty()) {
            List<StopArea> allStopAreas = stopAreaDAO.findAll();
            float percentageNewStops = (float) (analyzeReport.getNewStops().size() * 100) / allStopAreas.size();
            if (percentageNewStops > 20) {
                analyzeReport.setTooManyNewStops(true);
            }
        }

        log.info("Too many new stops check completed");
        
        return SUCCESS;
    }

    public static class DefaultCommandFactory extends CommandFactory {

        @Override
        protected Command create(InitialContext context) throws IOException {
            Command result = null;
            try {
                String name = "java:app/mobi.chouette.exchange/" + COMMAND;
                result = (Command) context.lookup(name);
            } catch (NamingException e) {
                // try another way on test context
                String name = "java:module/" + COMMAND;
                try {
                    result = (Command) context.lookup(name);
                } catch (NamingException e1) {
                    log.error(e);
                }
            }
            return result;
        }
    }
}
