package mobi.chouette.exchange.fileAnalysis;

import com.google.common.collect.Lists;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Constant;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.StopAreaDAO;
import mobi.chouette.exchange.importer.AbstractImporterCommand;
import mobi.chouette.exchange.parameters.AbstractImportParameter;
import mobi.chouette.exchange.report.AnalyzeReport;
import mobi.chouette.exchange.validation.checkpoint.AbstractValidation;
import mobi.chouette.model.StopArea;
import org.apache.commons.lang3.tuple.Pair;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 *  Class used to check if incoming StopArea is too far from existing StopArea
 *  (move is allowed if distance between old and new stopArea is < to MAX_ALLOWED_DISTANCE
 */
@Log4j
@Stateless(name = GeolocationCheckCommand.COMMAND)
public class GeolocationCheckCommand extends AbstractImporterCommand implements Command, Constant {

    public static final String COMMAND = "GeolocationCheckCommand";

    @EJB
    private
    StopAreaDAO stopAreaDAO;


    @Override
    public boolean execute(Context context) throws Exception {
        log.info("Starting geolocation check :");
        AnalyzeReport analyzeReport = (AnalyzeReport) context.get(ANALYSIS_REPORT);
        AbstractImportParameter configuration = (AbstractImportParameter) context.get(CONFIGURATION);

        List<StopArea> stopList = analyzeReport.getStops();
        List<String> originalStopIds = analyzeReport.getStops().stream()
                .map(StopArea::getOriginalStopId)
                .collect(Collectors.toList());

        List<List<String>> list = Lists.partition(originalStopIds, 1000);

        List<StopArea> existingStops = new ArrayList<>();
        for(List<String> subList : list){
            existingStops.addAll(stopAreaDAO.findByOriginalIds(subList));
        }

        for (StopArea stopArea : existingStops) {
            boolean moreOne = existingStops.stream()
                    .filter(stopArea1 -> stopArea.getOriginalStopId().equals(stopArea1.getOriginalStopId()))
                    .count() > 1;

            if (moreOne) {
                log.error("Multiple points for originalStopId : " + stopArea.getOriginalStopId());
                //block import launch because DB has inconsistent data
                analyzeReport.getDuplicateOriginalStopIds().add(stopArea.getOriginalStopId());
            }
        }


        for (StopArea incomingStopArea : stopList) {
            for (StopArea existingStop : existingStops) {
                if (incomingStopArea.getOriginalStopId().equals(existingStop.getOriginalStopId())) {
                    double distance = AbstractValidation.quickDistanceFromCoordinates(existingStop.getLatitude().doubleValue(), incomingStopArea.getLatitude().doubleValue(),
                            existingStop.getLongitude().doubleValue(), incomingStopArea.getLongitude().doubleValue());

                    double distanceGeolocation = configuration.getDistanceGeolocation() != null ? configuration.getDistanceGeolocation() : 200d;
                    if (Double.compare(distance, distanceGeolocation) > 0) {
                        //Distance between incoming StopArea and existing StopArea is superior to max allowed distance.
                        //StopArea is added to wrongGeolocList
                        analyzeReport.getWrongGeolocStopAreas().add(Pair.of(existingStop, incomingStopArea));
                    }


                    if (configuration != null && !configuration.isKeepStopNames() && !existingStop.getName().equals(incomingStopArea.getName())) {
                        //if the stop area name has changed, user will be notified
                        analyzeReport.getChangedNameStopAreas().add(Pair.of(existingStop, incomingStopArea));
                    }

                }

            }
        }

        log.info("Geolocation check finished");

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
