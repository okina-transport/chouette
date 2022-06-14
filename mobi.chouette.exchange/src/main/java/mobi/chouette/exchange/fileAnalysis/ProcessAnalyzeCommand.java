package mobi.chouette.exchange.fileAnalysis;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Constant;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.exchange.importer.AbstractImporterCommand;
import mobi.chouette.exchange.report.ActionReporter;
import mobi.chouette.exchange.report.AnalyzeReport;
import mobi.chouette.exchange.validation.report.DataLocation;
import mobi.chouette.model.CalendarDay;
import mobi.chouette.model.JourneyPattern;
import mobi.chouette.model.Line;
import mobi.chouette.model.Period;
import mobi.chouette.model.Route;
import mobi.chouette.model.RouteSection;
import mobi.chouette.model.ScheduledStopPoint;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.StopPoint;
import mobi.chouette.model.Timetable;
import mobi.chouette.model.VehicleJourney;
import mobi.chouette.model.type.TransportModeNameEnum;
import mobi.chouette.model.type.Utils;
import mobi.chouette.model.util.Referential;
import org.apache.commons.lang3.tuple.Pair;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.LocalDate;

import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Log4j
@Stateless(name = ProcessAnalyzeCommand.COMMAND)
public class ProcessAnalyzeCommand extends AbstractImporterCommand implements Command, Constant {

    public static final String COMMAND = "ProcessAnalyzeCommand";
    private AnalyzeReport analyzeReport;
    private boolean cleanRepository;
    private String currentFileName;
    private Map<String, Set<String>> missingRouteLinks;


    public static final Comparator<StopPoint> STOP_POINT_POSITION_COMPARATOR = new Comparator<StopPoint>() {
        @Override
        public int compare(StopPoint sp1, StopPoint sp2) {
            return Integer.compare(sp1.getPosition(), sp2.getPosition());
        }
    };



    @Override
    public boolean execute(Context context) throws Exception {
        boolean result = ERROR;

        DateTime startingTime = new DateTime();
        int currentLineNb = context.get(CURRENT_LINE_NB) == null ? 1 : (int) context.get(CURRENT_LINE_NB) + 1;
        context.put(CURRENT_LINE_NB, currentLineNb);

        cleanRepository = (boolean) context.get(CLEAR_FOR_IMPORT);

        log.info("Starting analysis " + currentLineNb + "/" + context.get(TOTAL_NB_OF_LINES));
        Referential cache = new Referential();
        context.put(CACHE, cache);
        context.put(OPTIMIZED, Boolean.FALSE);
        analyzeReport = (AnalyzeReport)context.get(ANALYSIS_REPORT);
        currentFileName =  (String) context.get(FILE_NAME);

        missingRouteLinks =analyzeReport.getMissingRouteLinks();


        Referential referential = (Referential) context.get(REFERENTIAL);

        Line newValue  = referential.getLines().values().iterator().next();

        feedAnalysisWithLineData(context, newValue);
        feedAnalysisWithStopAreaData(newValue);

        newValue.getRoutes().forEach(route -> checkRouteLinksInRoute(referential, route));

        DateTime endingTime = new DateTime();

        Duration duration = new Duration(endingTime, startingTime);
        log.info("analysis completed in:" + duration.toString());
        result = SUCCESS;


        return result;
    }

    private void checkRouteLinksInRoute(Referential referential, Route currentRoute){
        currentRoute.getJourneyPatterns().forEach(journeyPattern -> checkRouteLinksForJourneyPattern(referential, journeyPattern));
    }

    private void checkRouteLinksForJourneyPattern(Referential referential, JourneyPattern journeyPattern) {
        journeyPattern.getStopPoints().sort(STOP_POINT_POSITION_COMPARATOR);

        StopPoint previousStopPoint = null;

        for (StopPoint stopPoint : journeyPattern.getStopPoints()) {

            if (previousStopPoint == null){
                previousStopPoint = stopPoint;
                continue;
            }

            if (!checkRouteLinkPresence(referential, previousStopPoint, stopPoint)){

                String fromScheduledId = previousStopPoint.getScheduledStopPoint().getObjectId();
                String toScheduledId = stopPoint.getScheduledStopPoint().getObjectId();

                Set<String> routeLinks = null;
                if (missingRouteLinks.containsKey(currentFileName)){
                    routeLinks = missingRouteLinks.get(currentFileName);
                }else{
                    routeLinks = new HashSet<>();
                    missingRouteLinks.put(currentFileName, routeLinks);
                }

                routeLinks.add( fromScheduledId + "->" + toScheduledId);
            }
            previousStopPoint = stopPoint;
        }

    }

    private boolean checkRouteLinkPresence(Referential referential, StopPoint startPoint, StopPoint endPoint) {

        ScheduledStopPoint startScheduledStopPoint = startPoint.getScheduledStopPoint();
        ScheduledStopPoint endScheduledStopPoint = endPoint.getScheduledStopPoint();

        for (RouteSection routeSection : referential.getRouteSections().values()) {
            if (routeSection.getFromScheduledStopPoint().equals(startScheduledStopPoint) && routeSection.getToScheduledStopPoint().equals(endScheduledStopPoint)){
                return true;
            }
        }
        return false;
    }



    /**
     * recover all data of stopAreas and write analysis results into analyzeReport     *
     * @param line
     */
    private void feedAnalysisWithStopAreaData(Line line){
        List<StopArea> stopAreaList = new ArrayList<>();


        for (Route route : line.getRoutes()) {
            for (JourneyPattern journeyPattern : route.getJourneyPatterns()) {
                for (StopPoint stopPoint : journeyPattern.getStopPoints()) {
                    Optional<StopArea> stopAreaOpt = Utils.getStopAreaFromScheduledStopPoint(stopPoint);
                    if (stopAreaOpt.isPresent()){
                        StopArea stopArea = stopAreaOpt.get();
                        stopAreaList.add(stopArea);
                        checkQuayTransportMode(stopArea,line);
                    }
                }
            }
        }


        stopAreaList.forEach(stopArea -> {
            if (!analyzeReport.getStops().contains(stopArea)) {
                analyzeReport.getStops().add(stopArea);
            }
        });
    }


    /**
     * Read the context to recover all data of the files and write analysis results into analyzeReport
     * @param context
     */
    private void feedAnalysisWithLineData(Context context, Line line){
        List incomingLineList = (List) context.get(INCOMING_LINE_LIST);

        List<String> vehicleJourneys = new ArrayList<>();

        String networkName = line.getNetwork().getName();

        Map<String, Set<String>> networksByTimetable = analyzeReport.getNetworksByTimetable();

        String lineName = line.getName();

        //If line is not part of the incoming file or if line has already been analyzed, we skip it
        if (!incomingLineList.contains(line.getObjectId()) || analyzeReport.getLines().contains(lineName))
            return;


        analyzeReport.getLines().add(lineName);
        analyzeReport.addLineTextColor(lineName,line.getTextColor());
        analyzeReport.addLineBackgroundColor(lineName,line.getColor());
        analyzeReport.addLineShortName(lineName,line.getNumber());

        for (Route route : line.getRoutes()) {

            for (JourneyPattern journeyPattern : route.getJourneyPatterns()) {

                for (VehicleJourney vehicleJourney : journeyPattern.getVehicleJourneys()) {
                    vehicleJourneys.add(vehicleJourney.getObjectId());

                    for (Timetable timetable : vehicleJourney.getTimetables()) {

                        if (!cleanRepository){
                            String timetableName = timetable.getComment();
                            networksByTimetable.putIfAbsent(timetableName, new HashSet<>());
                            Set<String> networkSet = networksByTimetable.get(timetableName);
                            networkSet.add(networkName);
                        }


                        Optional<LocalDate> startOfPeriod = getMinDateOfTimeTable(timetable);
                        Optional<LocalDate> endOfPeriod = getMaxDateOfTimeTable(timetable);

                        if (startOfPeriod.isPresent() && (analyzeReport.getOldestPeriodOfCalendars() == null || (analyzeReport.getOldestPeriodOfCalendars().isAfter(startOfPeriod.get())))){
                            analyzeReport.setOldestPeriodOfCalendars(startOfPeriod.get());
                        }

                        if (endOfPeriod.isPresent() && (analyzeReport.getNewestPeriodOfCalendars() == null || (analyzeReport.getNewestPeriodOfCalendars().isBefore(endOfPeriod.get())))){
                            analyzeReport.setNewestPeriodOfCalendars(endOfPeriod.get());
                        }
                    }
                }
            }
        }

        vehicleJourneys.forEach(vehicleJourney->{
            if(!analyzeReport.getJourneys().contains(vehicleJourney)){
                analyzeReport.getJourneys().add(vehicleJourney);
            }
        });
    }


    private Optional<LocalDate> getMinDateOfTimeTable(Timetable timetable ){

        List<LocalDate> startPeriodList = timetable.getPeriods().stream()
                .map(Period::getStartDate)
                .collect(Collectors.toList());


        List<LocalDate> calendarDates = timetable.getCalendarDays().stream()
                .filter(CalendarDay::getIncluded)
                .map(CalendarDay::getDate)
                .collect(Collectors.toList());

        startPeriodList.addAll(calendarDates);

        return startPeriodList.isEmpty() ? Optional.empty() : startPeriodList.stream().min(LocalDate::compareTo);
    }

    private Optional<LocalDate> getMaxDateOfTimeTable(Timetable timetable ){

        List<LocalDate> endPeriodList = timetable.getPeriods().stream()
                .map(Period::getEndDate)
                .collect(Collectors.toList());


        List<LocalDate> calendarDates = timetable.getCalendarDays().stream()
                .filter(CalendarDay::getIncluded)
                .map(CalendarDay::getDate)
                .collect(Collectors.toList());

        endPeriodList.addAll(calendarDates);
        return endPeriodList.isEmpty() ? Optional.empty() : endPeriodList.stream().max(LocalDate::compareTo);
    }


    /***
     * Checks if the transportMode has changed, for the same quay
     * @param quay
     *     quay to check
     * @param line
     *  line on which the quay is used
     */
    private void checkQuayTransportMode( StopArea quay, Line line){
        TransportModeNameEnum transportMode = line.getTransportModeName();
        String originalStopId = quay.getOriginalStopId();
        Set<String> lineUse;
        String lineAndTransportString = line.getRegistrationNumber() + "(" + transportMode + ")";

        if (!analyzeReport.getQuayLineUse().containsKey(originalStopId)){
            lineUse = new HashSet<>();
            analyzeReport.getQuayLineUse().put(originalStopId,lineUse);
        }else{
            lineUse = analyzeReport.getQuayLineUse().get(originalStopId);
        }
        lineUse.add(lineAndTransportString);

        if (! analyzeReport.getQuayTransportMode().containsKey(originalStopId)){
            analyzeReport.getQuayTransportMode().put(originalStopId,transportMode);
        }else if (!analyzeReport.getQuayTransportMode().get(originalStopId).equals(transportMode)){
            // Same quay has been detected on 2 lines with different transport mode
            analyzeReport.getQuayWithDifferentTransportModes().add(originalStopId);
        }
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
        CommandFactory.factories.put(ProcessAnalyzeCommand.class.getName(), new ProcessAnalyzeCommand.DefaultCommandFactory());
    }
}
