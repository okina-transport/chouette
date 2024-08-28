package mobi.chouette.exchange.fileAnalysis;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Constant;
import mobi.chouette.common.Context;
import mobi.chouette.common.JobData;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.StopAreaDAO;
import mobi.chouette.dao.VehicleJourneyDAO;
import mobi.chouette.exchange.importer.AbstractImporterCommand;
import mobi.chouette.exchange.report.AnalyzeReport;
import mobi.chouette.model.*;
import mobi.chouette.model.type.TransportModeNameEnum;
import mobi.chouette.model.type.Utils;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.Referential;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.LocalDate;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Log4j
@Stateless(name = ProcessAnalyzeCommand.COMMAND)
public class ProcessAnalyzeCommand extends AbstractImporterCommand implements Command, Constant {

    public static final String COMMAND = "ProcessAnalyzeCommand";
    private AnalyzeReport analyzeReport;
    private boolean cleanRepository;
    private String currentFileName;
    private Map<String, Set<String>> missingRouteLinks;
    private Map<String, Set<String>> wrongRouteLinksUsedInMutipleFiles;
    private Map<String, Set<String>> wrongRouteLinksUsedMutipleTimesInTheSameFile;
    private Map<String, Set<String>> wrongRouteLinksUsedSameFromAndToScheduledStopPoint;
    private Map<String, Set<String>> wrongRefStopAreaInScheduleStopPoint;
    private List<Map<String, Map<String, String>>> wrongStopPointOrderInJourneyPattern;
    private Map<String, Set<String>> wrongScheduleStopPointCoordinates;
    public static final String _1_NETEX_MISSING_LINE_NETWORK_ASSOCIATION = "1-NETEXPROFILE-MissingLineNetworkAssociation";

    private Map<String, String> originalIdMap = new HashMap<>();

    @EJB
    private VehicleJourneyDAO vehicleJourneyDAO;

    @EJB
    StopAreaDAO stopAreaDAO;

    public static final Comparator<StopPoint> STOP_POINT_POSITION_COMPARATOR = new Comparator<StopPoint>() {
        @Override
        public int compare(StopPoint sp1, StopPoint sp2) {
            return Integer.compare(sp1.getPosition(), sp2.getPosition());
        }
    };

    public static final Comparator<VehicleJourneyAtStop> VEHICLE_AT_STOP_COMPARATOR = new Comparator<VehicleJourneyAtStop>() {
        @Override
        public int compare(VehicleJourneyAtStop vas1, VehicleJourneyAtStop vas2) {
            return vas1.getDepartureTime().compareTo(vas2.getDepartureTime());
        }
    };

    @Override
    public boolean execute(Context context) throws Exception {
        boolean result = ERROR;

        DateTime startingTime = new DateTime();
        int currentLineNb = context.get(CURRENT_LINE_NB) == null ? 1 : (int) context.get(CURRENT_LINE_NB) + 1;
        context.put(CURRENT_LINE_NB, currentLineNb);

        
        boolean detectChangedTrips = context.get(DETECT_CHANGED_TRIPS) == null ? false : (boolean) context.get(DETECT_CHANGED_TRIPS);

        cleanRepository = (boolean) context.get(CLEAR_FOR_IMPORT);

        log.info("Starting analysis " + currentLineNb + "/" + context.get(TOTAL_NB_OF_LINES));
        Referential cache = new Referential();
        context.put(CACHE, cache);
        context.put(OPTIMIZED, Boolean.FALSE);
        analyzeReport = (AnalyzeReport) context.get(ANALYSIS_REPORT);
        currentFileName =  (String) context.get(FILE_NAME);

        List<String> stopPlacesWithoutQuayList = (List<String>) context.get(STOP_PLACES_WITHOUT_QUAY);
        if(stopPlacesWithoutQuayList != null && stopPlacesWithoutQuayList.size() > 0){
            analyzeReport.setStopPlacesWithoutQuay(stopPlacesWithoutQuayList);
        }

        List<String> multimodalStopPlacesList = (List<String>) context.get(MULTIMODAL_STOP_PLACES);
        if(multimodalStopPlacesList != null && multimodalStopPlacesList.size() > 0){
            analyzeReport.setMultimodalStopPlaces(multimodalStopPlacesList);
        }

        missingRouteLinks = analyzeReport.getMissingRouteLinks();
        wrongRouteLinksUsedInMutipleFiles = analyzeReport.getWrongRouteLinksUsedInMutipleFiles();
        wrongRouteLinksUsedMutipleTimesInTheSameFile = analyzeReport.getWrongRouteLinksUsedMutipleTimesInTheSameFile();
        wrongRouteLinksUsedSameFromAndToScheduledStopPoint = analyzeReport.getWrongRouteLinksUsedSameFromAndToScheduledStopPoint();
        wrongRefStopAreaInScheduleStopPoint = analyzeReport.getWrongRefStopAreaInScheduleStopPoint();
        wrongStopPointOrderInJourneyPattern = analyzeReport.getWrongStopPointOrderInJourneyPattern();
        wrongScheduleStopPointCoordinates = analyzeReport.getWrongScheduleStopPointCoordinates();

        Referential referential = (Referential) context.get(REFERENTIAL);

        Line newValue  = referential.getLines().values().iterator().next();

        feedAnalysisWithLineData(context, newValue);
        feedAnalysisWithStopAreaData(newValue);

        containsRouteLinksUsedInMutipleFiles(context);
        containsRouteLinksUsedMutipleTimesInTheSameFile(context);
        containsRouteLinksUsedSameFromAndToScheduledStopPoint(context);
        containsStopAreaRefNullScheduleStopPoint(context);
        containsStopPointInJourneyPattern(context);
        constainsStopAreaWrongCoordinatesScheduleStopPoint(context);
        checkRouteLinksIfNeeded(context, newValue);
        if (detectChangedTrips){
            launchTripAnalyze(newValue);
        }


        DateTime endingTime = new DateTime();

        Duration duration = new Duration(endingTime, startingTime);
        log.info("analysis completed in:" + duration.toString());
        result = SUCCESS;


        return result;
    }

    /**
     * Analyze if trips changed between existing trip in BDD and incoming trip from file
     * @param newValue
     *  new line incoming from file
     */
    private void launchTripAnalyze(Line newValue) {

        for (Route route : newValue.getRoutes()) {
            for (JourneyPattern journeyPattern : route.getJourneyPatterns()) {
                for (VehicleJourney vehicleJourney : journeyPattern.getVehicleJourneys()) {

                    String objectId = vehicleJourney.getObjectId();
                    VehicleJourney existingVehicleJourney = vehicleJourneyDAO.findByObjectId(objectId);

                    if (existingVehicleJourney == null){
                        continue;
                    }

                    if(!areTripEquals(existingVehicleJourney.getVehicleJourneyAtStops(),vehicleJourney.getVehicleJourneyAtStops())){
                        writeDifferentTrips(vehicleJourney.getObjectId(), existingVehicleJourney.getVehicleJourneyAtStops(), vehicleJourney.getVehicleJourneyAtStops());
                    }

                    List<VehicleJourneyAtStop> existingVehAtStops = existingVehicleJourney.getVehicleJourneyAtStops();
                    Collections.sort(existingVehAtStops, VEHICLE_AT_STOP_COMPARATOR);
                }
            }
        }
    }

    /**
     * Get an originalStopId from an objectId
     * @param objectId
     *  an object id : MOBIITI:Quay:xxxx
     * @return
     *  an original id
     */
    private String getOrRecoverOriginalId(String objectId){

        if (originalIdMap.containsKey(objectId)){
            return originalIdMap.get(objectId);
        }else{
            StopArea existingStopArea = stopAreaDAO.findByObjectId(objectId);
            originalIdMap.put(existingStopArea.getObjectId(), existingStopArea.getOriginalStopId());
            return existingStopArea.getOriginalStopId();
        }
    }

    /**
     * Write trip mismatch to analyze report
     * @param tripId
     *  the trip id for which mismatch has been detected
     * @param existingStops
     *  list of stops from existing trip in BDD
     * @param incomingStops
     *  list of stops from incoming file
     */
    private void writeDifferentTrips(String tripId, List<VehicleJourneyAtStop> existingStops, List<VehicleJourneyAtStop> incomingStops) {
        StringBuilder existingTrip = new StringBuilder();

        for (VehicleJourneyAtStop vehicleJourneyAtStop : existingStops) {
            if (StringUtils.isNotEmpty(existingTrip.toString())){
                existingTrip.append("-");
            }
            String objectId = vehicleJourneyAtStop.getStopPoint().getScheduledStopPoint().getContainedInStopAreaRef().getObjectId();
            existingTrip.append(getOrRecoverOriginalId(objectId));
        }

        StringBuilder incomingTrip = new StringBuilder();

        for (VehicleJourneyAtStop vehicleJourneyAtStop : incomingStops) {
            if (StringUtils.isNotEmpty(incomingTrip.toString())){
                incomingTrip.append("-");
            }
            incomingTrip.append(vehicleJourneyAtStop.getStopPoint().getScheduledStopPoint().getContainedInStopAreaRef().getObjectId().split(":Quay:")[1]);
        }
        analyzeReport.recordChangedTrip(tripId.split(":VehicleJourney:")[1], existingTrip.toString(), incomingTrip.toString());

    }

    /**
     * Tells if 2 trips has the same structure or not (all stops must be equals)
     * @param existingStops
     *  existingStop from BDD
     * @param incomingStops
     *  incoming stops from file
     * @return
     *  true : trips are equals
     *  false :trips are differents
     */
    private boolean areTripEquals(List<VehicleJourneyAtStop> existingStops, List<VehicleJourneyAtStop> incomingStops) {

        if (existingStops.size() != incomingStops.size()){
            return false;
        }

        for (int i = 0 ; i < existingStops.size(); i++){
            VehicleJourneyAtStop existing = existingStops.get(i);
            VehicleJourneyAtStop incoming = incomingStops.get(i);

            String existingStopAreaObjectId = existing.getStopPoint().getScheduledStopPoint().getContainedInStopAreaRef().getObjectId();
            String existingOriginalId = getOrRecoverOriginalId(existingStopAreaObjectId);
            String incomingStopArea = incoming.getStopPoint().getScheduledStopPoint().getContainedInStopAreaRef().getObjectId();

            if (!existingOriginalId.equals(incomingStopArea.split(":Quay:")[1]))
                return false;
        }
        // all stops have been checked. trips are equals
        return true;
    }

    private void checkRouteLinksIfNeeded(Context context, Line line){
        Referential referential = (Referential) context.get(REFERENTIAL);
        JobData jobData = (JobData) context.get(JOB_DATA);
        if(jobData.getType().equals("netexprofile")){
            line.getRoutes().forEach(route -> checkRouteLinksInRoute(referential, route));
        }

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


    private void containsRouteLinksUsedInMutipleFiles(Context context) {
        Map<String, Set<String>> routeSectionsMultipleFiles = new HashMap<>();
        Map<String, Set<String>> routeSections = (Map<String, Set<String>>) context.get(ROUTE_LINKS_USED_IN_MULTIPLE_FILES);
        if(routeSections != null){
            for(String fileName : routeSections.keySet()){
                if(routeSections.get(fileName).size() > 1){
                    routeSectionsMultipleFiles.put(fileName, routeSections.get(fileName));
                }
            }
            wrongRouteLinksUsedInMutipleFiles.putAll(routeSectionsMultipleFiles);
        }
    }

    private void containsRouteLinksUsedMutipleTimesInTheSameFile(Context context) {
        List<String> routeSectionsUsedMutipleTimesInTheSameFile = (List<String>) context.get(ROUTE_LINKS_USED_MULTIPLE_TIMES_IN_THE_SAME_FILE);

        if (routeSectionsUsedMutipleTimesInTheSameFile != null) {
            Set<String> routeLinks = new HashSet<>();
            for (String rsId : routeSectionsUsedMutipleTimesInTheSameFile) {
                boolean isRouteSectionsUsedMutipleTimesInTheSameFile = routeSectionsUsedMutipleTimesInTheSameFile.stream().filter(s -> s.equals(rsId)).count() > 1;
                if(isRouteSectionsUsedMutipleTimesInTheSameFile){
                    routeLinks.add(rsId);
                }
            }
            if(routeLinks.size() > 0)
            wrongRouteLinksUsedMutipleTimesInTheSameFile.put(currentFileName, routeLinks);
        }
    }

    private void containsRouteLinksUsedSameFromAndToScheduledStopPoint(Context context) {
        List<String> routeSectionsUsedSameFromAndToScheduledStopPoint = (List<String>) context.get(ROUTE_LINKS_USED_SAME_FROM_AND_TO_SCHEDULED_STOP_POINT);

        if (routeSectionsUsedSameFromAndToScheduledStopPoint != null) {
            Set<String> routeLinks = new HashSet<>(routeSectionsUsedSameFromAndToScheduledStopPoint);
            wrongRouteLinksUsedSameFromAndToScheduledStopPoint.put(currentFileName, routeLinks);
        }
    }

    private void containsStopAreaRefNullScheduleStopPoint(Context context) {
        List<String> scheduleStopPointUsedWrongRefStopArea = (List<String>) context.get(SCHEDULE_STOP_POINT_STOP_AREA_NULL);

        if (scheduleStopPointUsedWrongRefStopArea != null) {
            Set<String> stopAreas = new HashSet<>(scheduleStopPointUsedWrongRefStopArea);
            wrongRefStopAreaInScheduleStopPoint.put(currentFileName, stopAreas);
        }
    }

    private void containsStopPointInJourneyPattern(Context context) {
        List<Map<String, Map<String, String>>> result = (List<Map<String, Map<String, String>>>) context.get(WRONG_STOP_POINT_ORDER_IN_JOUNEY_PATTERN);

        if (result != null) {
            // Pour éviter les doublons
            for (Map<String, Map<String, String>> entry : result) {
                boolean exists = wrongStopPointOrderInJourneyPattern.stream()
                        .anyMatch(e -> e.equals(entry));
                if (!exists) {
                    wrongStopPointOrderInJourneyPattern.add(entry);
                }
            }
        }
    }

    private void constainsStopAreaWrongCoordinatesScheduleStopPoint(Context context) {
        List<String> result = (List<String>) context.get(WRONG_SCHEDULE_STOP_POINT_COORDINATES);

        if (result != null) {
            Set<String> stopAreas = new HashSet<>(result);
            wrongScheduleStopPointCoordinates.put(currentFileName, stopAreas);
        }
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
                        checkQuayTransportMode(stopArea, line);
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

        String networkName = "";




        if (line.getNetwork() == null ) {
            Referential referential = (Referential) context.get(REFERENTIAL);
            mobi.chouette.model.Network defaultNetwork = ObjectFactory.getPTNetwork(referential, NETEX_VALID_PREFIX + ":Network:DefaultNetwork");
            defaultNetwork.setName("DefaultNetwork");
            line.setNetwork(defaultNetwork);
            networkName = defaultNetwork.getName();

        }else{
            networkName = line.getNetwork().getName();
        }

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
                            String timetableName = timetable.getComment() != null ? timetable.getComment() : timetable.getObjectId();
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
    private void checkQuayTransportMode(StopArea quay, Line line){
        TransportModeNameEnum transportMode = line.getTransportModeName();
        String stopId = StringUtils.isNotEmpty(quay.getOriginalStopId()) ? quay.getOriginalStopId() : quay.getObjectId().split(":")[2];
        Set<String> lineUse;
        String lineBaseName = StringUtils.isNotEmpty( line.getRegistrationNumber()) ?  line.getRegistrationNumber() : line.getName();
        String lineAndTransportString = lineBaseName + "(" + transportMode + ")";

        if (!analyzeReport.getQuayLineUse().containsKey(stopId)){
            lineUse = new HashSet<>();
            analyzeReport.getQuayLineUse().put(stopId, lineUse);
        } else{
            lineUse = analyzeReport.getQuayLineUse().get(stopId);
        }
        lineUse.add(lineAndTransportString);

        if (!analyzeReport.getQuayTransportMode().containsKey(stopId)){
            analyzeReport.getQuayTransportMode().put(stopId,transportMode);
        } else if (!analyzeReport.getQuayTransportMode().get(stopId).equals(transportMode)){
            // Same quay has been detected on 2 lines with different transport mode
            analyzeReport.getQuayWithDifferentTransportModes().add(stopId);
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
