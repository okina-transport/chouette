package mobi.chouette.exchange.fileAnalysis;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Constant;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.StopAreaDAO;
import mobi.chouette.exchange.importer.AbstractImporterCommand;
import mobi.chouette.exchange.report.AnalyzeReport;
import mobi.chouette.exchange.validation.checkpoint.AbstractValidation;
import mobi.chouette.model.StopArea;
import org.apache.commons.lang3.tuple.Pair;


import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.util.List;

/**
 *  Class used to check if incoming StopArea is too far from existing StopArea
 *  (move is allowed if distance between old and new stopArea is < to MAX_ALLOWED_DISTANCE
 */
@Log4j
@Stateless(name = GeolocationCheckCommand.COMMAND)
public class GeolocationCheckCommand extends AbstractImporterCommand implements Command, Constant {

    public static final String COMMAND = "GeolocationCheckCommand";

    private static final double MAX_ALLOWED_DISTANCE = 200d;

    @EJB
    private
    StopAreaDAO stopAreaDAO;


    @Override
    public boolean execute(Context context) throws Exception {
        AnalyzeReport analyzeReport = (AnalyzeReport)context.get(ANALYSIS_REPORT);
        List<StopArea> stopList = analyzeReport.getStops();

        for (StopArea incomingStopArea : stopList) {
            String originalStopId = incomingStopArea.getOriginalStopId();

            List<StopArea> existingStops = stopAreaDAO.findByOriginalId(originalStopId);

            if (existingStops.isEmpty()){
                continue;
            }

            if (existingStops.size() > 1){
                log.error("Multiple points for originalStopId : " + originalStopId);
                //block import launch because DB has inconsistent data
                analyzeReport.getDuplicateOriginalStopIds().add(originalStopId);
            }



            StopArea existingStop = existingStops.get(0);

            double distance = AbstractValidation.quickDistanceFromCoordinates(existingStop.getLatitude().doubleValue(), incomingStopArea.getLatitude().doubleValue(),
                                                                              existingStop.getLongitude().doubleValue(), incomingStopArea.getLongitude().doubleValue());


            if (Double.compare(distance, MAX_ALLOWED_DISTANCE) > 0){
                //Distance between incoming StopArea and existing StopArea is superior to max allowed distance.
                //StopArea is added to wrongGeolocList
                analyzeReport.getWrongGeolocStopAreas().add(Pair.of(existingStop, incomingStopArea));
            }
        }

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


    static {
        CommandFactory.factories.put(GeolocationCheckCommand.class.getName(), new GeolocationCheckCommand.DefaultCommandFactory());
    }
}
