package mobi.chouette.exchange.importer;


import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.model.JourneyPattern;
import mobi.chouette.model.Line;
import mobi.chouette.model.Route;
import mobi.chouette.model.RoutePoint;
import mobi.chouette.model.RouteSection;
import mobi.chouette.model.StopPoint;
import mobi.chouette.model.VehicleJourney;
import mobi.chouette.model.VehicleJourneyAtStop;
import mobi.chouette.model.util.Referential;
import org.apache.commons.lang3.StringUtils;

import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Log4j
@Stateless(name = CleanLineInCacheCommand.COMMAND)
public class CleanLineInCacheCommand implements Command {


    public static final String COMMAND = "CleanLineInCacheCommand";


    @Override
    public boolean execute(Context context) throws Exception {

        String currentLineId = (String) context.get(CURRENT_LINE_ID);

        if (StringUtils.isEmpty(currentLineId))
            return true;

        Referential referential = (Referential) context.get(REFERENTIAL);
        Referential cache = (Referential) context.get(CACHE);


        Line currentLine = referential.getLines().get(currentLineId);
        List<String> routeListToRemove = new ArrayList<>();
        List<String> routeSectionsToDelete = new ArrayList<>();
        List<String> scheduledStopPointToDelete = new ArrayList<>();
        List<String> destinationDisplayToDelete = new ArrayList<>();
        List<String> routePointsToDelete = new ArrayList<>();
        List<String> journeyPatternsToDelete = new ArrayList<>();
        List<String> vehicleJourneysToDelete = new ArrayList<>();
        List<String> stopPointsToDelete = new ArrayList<>();
        List<String> accessibilityAssessmentToDelete = new ArrayList<>();
        List<String> accessibilityLimitationToDelete = new ArrayList<>();

        for (Route route : currentLine.getRoutes()) {

            for (JourneyPattern journeyPattern : route.getJourneyPatterns()) {

                journeyPatternsToDelete.add(journeyPattern.getObjectId());

                journeyPattern.getRouteSections().stream()
                        .map(RouteSection::getObjectId)
                        .forEach(routeSectionsToDelete::add);


                for (VehicleJourney vehicleJourney : journeyPattern.getVehicleJourneys()) {

                    vehicleJourneysToDelete.add(vehicleJourney.getObjectId());

                    if(vehicleJourney.getAccessibilityAssessment() != null){
                        accessibilityAssessmentToDelete.add(vehicleJourney.getAccessibilityAssessment().getObjectId());
                        if(vehicleJourney.getAccessibilityAssessment().getAccessibilityLimitation() != null) {
                            accessibilityLimitationToDelete.add(vehicleJourney.getAccessibilityAssessment().getAccessibilityLimitation().getObjectId());
                        }
                    }

                    for (VehicleJourneyAtStop vehicleJourneyAtStop : vehicleJourney.getVehicleJourneyAtStops()) {


                        StopPoint stopPoint = vehicleJourneyAtStop.getStopPoint();
                        stopPointsToDelete.add(stopPoint.getObjectId());
                        if (stopPoint.getDestinationDisplay() != null){
                            destinationDisplayToDelete.add(stopPoint.getDestinationDisplay().getObjectId());
                        }
                        scheduledStopPointToDelete.add(stopPoint.getScheduledStopPoint().getObjectId());
                    }
                }

            }
            routeListToRemove.add(route.getObjectId());

            route.getRoutePoints().stream()
                    .map(RoutePoint::getObjectId)
                    .forEach(routePointsToDelete::add);
        }


        routeListToRemove.forEach(route -> {
            referential.getRoutes().remove(route);
            cache.getRoutes().remove(route);
        });

        routeSectionsToDelete.forEach(routeSection -> {
            referential.getRouteSections().remove(routeSection);
            referential.getSharedRouteSections().remove(routeSection);
            cache.getRouteSections().remove(routeSection);
            cache.getSharedRouteSections().remove(routeSection);
        });

        scheduledStopPointToDelete.forEach(scheduledStopPoint -> {
            referential.getScheduledStopPoints().remove(scheduledStopPoint);
            referential.getSharedScheduledStopPoints().remove(scheduledStopPoint);
            cache.getScheduledStopPoints().remove(scheduledStopPoint);
            cache.getSharedScheduledStopPoints().remove(scheduledStopPoint);
        });

        destinationDisplayToDelete.forEach(destinationDispay -> {
            referential.getDestinationDisplays().remove(destinationDispay);
            referential.getSharedDestinationDisplays().remove(destinationDispay);
            cache.getDestinationDisplays().remove(destinationDispay);
            cache.getSharedDestinationDisplays().remove(destinationDispay);
        });


        routePointsToDelete.forEach(routePoint -> {
            referential.getRoutePoints().remove(routePoint);
            referential.getSharedRoutePoints().remove(routePoint);
            cache.getRoutePoints().remove(routePoint);
            cache.getSharedRoutePoints().remove(routePoint);
        });

        journeyPatternsToDelete.forEach(journeyPattern -> {
            referential.getJourneyPatterns().remove(journeyPattern);
            cache.getJourneyPatterns().remove(journeyPattern);
        });


        vehicleJourneysToDelete.forEach(vehicleJourney ->{
            referential.getVehicleJourneys().remove(vehicleJourney);
            cache.getVehicleJourneys().remove(vehicleJourney);
        });

        stopPointsToDelete.forEach(stopPoint->{
            referential.getStopPoints().remove(stopPoint);
            cache.getStopPoints().remove(stopPoint);
        });

        accessibilityAssessmentToDelete.forEach(accessibilityAssessment->{
            referential.getAccessibilityAssessments().remove(accessibilityAssessment);
            cache.getAccessibilityAssessments().remove(accessibilityAssessment);
        });

        accessibilityLimitationToDelete.forEach(accessibilityLimitation->{
            referential.getAccessibilityLimitations().remove(accessibilityLimitation);
            cache.getAccessibilityLimitations().remove(accessibilityLimitation);
        });


        return false;
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
        CommandFactory.factories.put(CleanLineInCacheCommand.class.getName(), new CleanLineInCacheCommand.DefaultCommandFactory());
    }

}
