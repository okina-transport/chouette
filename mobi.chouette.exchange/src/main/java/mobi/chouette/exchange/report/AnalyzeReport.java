package mobi.chouette.exchange.report;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.Data;
import lombok.EqualsAndHashCode;
import mobi.chouette.common.Constant;
import mobi.chouette.common.Context;
import mobi.chouette.model.CalendarDay;
import mobi.chouette.model.Period;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.Timetable;
import mobi.chouette.model.type.TransportModeNameEnum;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.joda.time.LocalDate;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


@XmlRootElement(name = "analyze_report")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"exploitationPeriod", "files", "lines", "journeys", "stops"})
@Data
@EqualsAndHashCode(callSuper = false)
public class AnalyzeReport extends AbstractReport implements Constant, Report {

    @XmlElement(name = "exploitationPeriod")
    private String exploitationPeriod;

    @XmlElement(name = "oldestPeriod")
    private LocalDate oldestPeriodOfCalendars;

    @XmlElement(name = "newestPeriod")
    private LocalDate newestPeriodOfCalendars;

    @XmlElement(name = "files")
    private List<FileReport> files = new ArrayList<>();

    @XmlElement(name = "lines")
    private List<String> lines = new ArrayList<>();

    @XmlElement(name = "journeys")
    private List<String> journeys = new ArrayList<>();

    @XmlElement(name = "stops")
    private List<StopArea> stops = new ArrayList<>();

    @XmlElement(name = "newStops")
    private List<StopArea> newStops = new ArrayList<>();

    @XmlElement(name = "stopPlacesWithoutQuay")
    private List<String> stopPlacesWithoutQuay = new ArrayList<>();

    @XmlElement(name = "multimodalStopPlaces")
    private List<String> multimodalStopPlaces = new ArrayList<>();

    @XmlElement(name = "wrongGeolocStopAreas")
    private List<Pair<StopArea, StopArea>> wrongGeolocStopAreas = new ArrayList<>();

    @XmlElement(name = "changedNameStopAreas")
    private List<Pair<StopArea, StopArea>> changedNameStopAreas = new ArrayList<>();

    @XmlElement(name = "multipleUsedTimetables")
    private Map<String, Set<String>> multipleUsedTimetables = new HashMap<>();

    @XmlElement(name = "duplicateOriginalStopIds")
    private List<String> duplicateOriginalStopIds = new ArrayList<>();

    @XmlElement(name = "canLaunchImport")
    private boolean canLaunchImport = true;

    @XmlElement(name = "tooManyNewStops")
    private boolean tooManyNewStops = false;

    @XmlElement(name = "networksByTimetable")
    private Map<String, Set<String>> networksByTimetable = new HashMap<>();

    @XmlElement(name = "modifiedTimetables")
    private List<Pair<Timetable, Timetable>> modifiedTimetables = new ArrayList<>();

    @XmlElement(name = "missingRouteLinks")
    private Map<String, Set<String>> missingRouteLinks = new HashMap<>();

    @XmlElement(name = "wrongRouteLinksUsedInMutipleFiles")
    private Map<String, Set<String>> wrongRouteLinksUsedInMutipleFiles = new HashMap<>();

    @XmlElement(name = "wrongRouteLinksUsedMutipleTimesInTheSameFile")
    private Map<String, Set<String>> wrongRouteLinksUsedMutipleTimesInTheSameFile = new HashMap<>();

    @XmlElement(name = "wrongRouteLinksUsedSameFromAndToScheduledStopPoint")
    private Map<String, Set<String>> wrongRouteLinksUsedSameFromAndToScheduledStopPoint = new HashMap<>();

    @XmlElement(name = "emptyPointsInSequence")
    private Map<String, Set<String>> emptyPointsInSequence = new HashMap<>();

    @XmlElement(name = "emptyPassingTimes")
    private Map<String, Set<String>> emptyPassingTimes = new HashMap<>();

    @XmlElement(name = "wrongRefStopAreaInScheduleStopPoint")
    private  Map<String, Set<String>> wrongRefStopAreaInScheduleStopPoint = new HashMap<>();

    @XmlElement(name = "changedTrips")
    private Map<String, Pair<String, String>> changedTrips = new HashMap<>();

    @XmlTransient
    private Date date = new Date(0);

    private Map<String, String> lineTextColorMap = new HashMap<>();
    private Map<String, String> lineBackgroundColorMap = new HashMap<>();
    private Map<String, String> lineShortNameMap = new HashMap<>();


    // used to store each quay transport mode
    private Map<String, TransportModeNameEnum> quayTransportMode = new HashMap<>();

    // used to store all lines using a specific quay (key = quay id, value = list of lines using this quay)
    private Map<String, Set<String>> quayLineUse = new HashMap<>();

    // used to store stopPlace with multiple transport modes
    private Set<String> stopPlaceWithMultipleTransportModes = new HashSet();

    //Used to store all quay in error (that are on 2 lines with different transport mode)
    private Set<String> quayWithDifferentTransportModes = new HashSet();

    // used to store all stop place childrens with transportModes (key = stop place id, value = list of quays under the stop place)
    private Map<String, Set<String>> stopPlaceChildrens = new HashMap<>();


    public Map<String, TransportModeNameEnum> getQuayTransportMode() {
        return quayTransportMode;
    }


    public Map<String, Set<String>> getQuayLineUse() {
        return quayLineUse;
    }

    public Set<String> getQuayWithDifferentTransportModes() {
        return quayWithDifferentTransportModes;
    }


    /**
     * @param file
     */
    protected void addFileReport(FileReport file) {
        files.add(file);
    }



    /**
     * Create a list with all quays ids and transport modes, for a specifid stop place
     *
     * @param stopPlaceId The stop place for which the list must be created
     * @return The list of quays with transport modes
     */
    private String buildQuayListWithTransportModes(String stopPlaceId) {

        return stopPlaceChildrens.get(stopPlaceId).stream()
                .map(quayId -> quayId + "(" + quayTransportMode.get(quayId).toString() + ")")
                .collect(Collectors.joining(","));
    }

    @Override
    public boolean isEmpty() {
        // used to know if report has to be saved
        // Analyze Report has to be saved any time
        return false;
    }

    public void recordChangedTrip(String tripId, String existingTrip, String incomingTrip){
        Pair<String, String> difference = Pair.of(existingTrip, incomingTrip);
        changedTrips.put(tripId,difference);
    }


    public void addLineTextColor(String lineName, String lineTextColor) {
        if (!lineTextColorMap.containsKey(lineName))
            lineTextColorMap.put(lineName, lineTextColor);
    }

    public void addLineBackgroundColor(String lineName, String lineBackgroundColor) {
        if (!lineBackgroundColorMap.containsKey(lineName))
            lineBackgroundColorMap.put(lineName, lineBackgroundColor);
    }

    public void addLineShortName(String lineName, String shortName) {
        if (!lineShortNameMap.containsKey(lineName))
            lineShortNameMap.put(lineName, shortName);
    }


    @Override
    public void print(PrintStream out, StringBuilder ret, int level, boolean first) {
        ret.setLength(0);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        Map<String, Object> mainMap = new HashMap<>();
        Map<String, Object> analyzeReportMap = new HashMap<>();

        Map<String, Object> explorationPeriodMap = new HashMap<>();
        explorationPeriodMap.put("start", oldestPeriodOfCalendars == null ? "" : oldestPeriodOfCalendars.toString());
        explorationPeriodMap.put("end", newestPeriodOfCalendars == null ? "" : newestPeriodOfCalendars.toString());
        analyzeReportMap.put("exploitation_period",explorationPeriodMap);


        if (!files.isEmpty())
            analyzeReportMap.put("files", files);

        if (!lines.isEmpty())
            analyzeReportMap.put("lines", buildLinesList());


        analyzeReportMap.put("journeys_count",journeys.size());

        if (!stops.isEmpty()) {
            analyzeReportMap.put("stops", buildStopList(stops));
        }

        if (!newStops.isEmpty()) {
            analyzeReportMap.put("newStops", buildStopList(newStops));
        }

        if (!stopPlacesWithoutQuay.isEmpty()) {
            canLaunchImport = false;
            analyzeReportMap.put("stopPlacesWithoutQuay", buildStringList(stopPlacesWithoutQuay, "stopId"));
        }

        if (!multimodalStopPlaces.isEmpty()) {
            analyzeReportMap.put("multimodalStopPlaces", buildStringList(multimodalStopPlaces, "stopId"));
        }

        if (!wrongGeolocStopAreas.isEmpty()) {
            canLaunchImport = false;
            analyzeReportMap.put("wrongGeolocStopAreas", buildWrongGeolocList());
        }

        if (!changedTrips.isEmpty()){
            canLaunchImport = false;
            analyzeReportMap.put("changedTrips", buildChangedTrips());
        }

        if (!changedNameStopAreas.isEmpty()) {
            analyzeReportMap.put("changedNameStopAreas", buildChangedNameList());
        }

        if (!multipleUsedTimetables.isEmpty()) {
            List<Map.Entry<String, Set<String>>> entrySetList = multipleUsedTimetables.entrySet().stream().collect(Collectors.toList());
            analyzeReportMap.put("multipleUsedTimetablesFromDB", buildMultipleUsedTimetablesList(entrySetList));
        }

        List<Map.Entry<String, Set<String>>> multipleusedTimetablesInInputFile = networksByTimetable.entrySet()
                .stream()
                .filter(entry -> entry.getValue().size() > 1)
                .collect(Collectors.toList());


        if (multipleusedTimetablesInInputFile.size() > 0) {
            analyzeReportMap.put("multipleUsedTimetablesFromInputFile", buildMultipleUsedTimetablesList(multipleusedTimetablesInInputFile));
        }


        if (!missingRouteLinks.isEmpty()){
            canLaunchImport = false;
            analyzeReportMap.put("missingRouteLinks", buildMissingRouteLinksList());
        }

        if (!wrongRouteLinksUsedInMutipleFiles.isEmpty()) {
            canLaunchImport = false;
            analyzeReportMap.put("wrongRouteLinksUsedInMutipleFiles", buildWrongRouteLinksMultipleFilesList());
        }

        if (!wrongRouteLinksUsedMutipleTimesInTheSameFile.isEmpty()) {
            canLaunchImport = false;
            analyzeReportMap.put("wrongRouteLinksUsedMutipleTimesInTheSameFile", buildMapList(wrongRouteLinksUsedMutipleTimesInTheSameFile,"fileName","routeLinks"));
        }

        if (!wrongRouteLinksUsedSameFromAndToScheduledStopPoint.isEmpty()) {
            canLaunchImport = false;
            analyzeReportMap.put("wrongRouteLinksUsedSameFromAndToScheduledStopPoint", buildMapList(wrongRouteLinksUsedSameFromAndToScheduledStopPoint,"fileName","routeLinks"));
        }

        if (!modifiedTimetables.isEmpty()) {
            analyzeReportMap.put("modifiedTimetables", buildModifiedTimeTables());
        }

        if (!duplicateOriginalStopIds.isEmpty()) {
            canLaunchImport = false;
            analyzeReportMap.put("duplicateOriginalStopId", buildStringList(duplicateOriginalStopIds,"originalStopId"));
        }

        if (!quayWithDifferentTransportModes.isEmpty()) {
            analyzeReportMap.put("quays_with_different_transport_modes", buildQuayWithDifferentTransportModeList());
        } else {
            checkStopPlaceTransportModes();
            if (!stopPlaceWithMultipleTransportModes.isEmpty()){
                analyzeReportMap.put("stopplaces_with_different_transport_modes", buildStopPlacesWithDifferentTransportModeList());
            }
        }

        if (!emptyPointsInSequence.isEmpty()){
            canLaunchImport = false;
            analyzeReportMap.put("empty_point_in_sequence", buildMapList(emptyPointsInSequence,"file_name", "journey_pattern_id"));
        }

        if (!emptyPassingTimes.isEmpty()){
            canLaunchImport = false;
            analyzeReportMap.put("empty_passing_times", buildMapList(emptyPassingTimes,"file_name", "service_journey_id"));
        }

        if (!wrongRefStopAreaInScheduleStopPoint.isEmpty()){
            canLaunchImport = false;
            analyzeReportMap.put("wrongRefStopAreaInScheduleStopPoint", buildMapList(wrongRefStopAreaInScheduleStopPoint,"file_name", "stopArea"));
        }



        analyzeReportMap.put("canLaunchImport", canLaunchImport);
        analyzeReportMap.put("tooManyNewStops", tooManyNewStops);

        mainMap.put("analyze_report",analyzeReportMap);
        try {
            out.print(objectMapper.writeValueAsString(mainMap));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    private List<Object> buildChangedTrips() {
        List<Object> resultList = new ArrayList<>();

        for (Map.Entry<String, Pair<String, String>> changedTrip : changedTrips.entrySet()) {
            String tripId = changedTrip.getKey();
            Pair<String, String> differences = changedTrip.getValue();
            Map<String,String> differenceMap = new HashMap<>();
            differenceMap.put("trip_id", tripId);
            differenceMap.put("existing", differences.getLeft());
            differenceMap.put("incoming", differences.getRight());
            resultList.add(differenceMap);
        }
        return resultList;
    }

    private List<Object> buildStopPlacesWithDifferentTransportModeList() {
        List<Object> resultList = new ArrayList<>();
        String[] stopPlacesInErrorArray = stopPlaceWithMultipleTransportModes.toArray(new String[stopPlaceWithMultipleTransportModes.size()]);

        for (int i = 0; i < stopPlacesInErrorArray.length; i++) {
            String stopPlaceId = stopPlacesInErrorArray[i];
            Map<String,Object> stopPlaceMap = new HashMap<>();
            stopPlaceMap.put("stop_place_id", stopPlaceId);
            stopPlaceMap.put("quays_with_transportModes",  buildQuayListWithTransportModes(stopPlaceId));
            resultList.add(stopPlaceMap);
        }
        return resultList;
    }

    private List<Object> buildQuayWithDifferentTransportModeList() {
        List<Object> resultList = new ArrayList<>();
        String[] quaysInErrorArray = quayWithDifferentTransportModes.toArray(new String[quayWithDifferentTransportModes.size()]);

        for (int i = 0; i < quaysInErrorArray.length; i++) {
            String quayId = quaysInErrorArray[i];

            Map<String,Object> quayMap = new HashMap<>();
            quayMap.put("quay_id", quayId);
            String quayLineInUseStr = quayLineUse.get(quayId).stream().collect(Collectors.joining(","));
            quayMap.put("used_in_lines", quayLineInUseStr);
            resultList.add(quayMap);
        }
        return resultList;
    }

    private List<Object> buildModifiedTimeTables() {
        List<Object> resultList = new ArrayList<>();

        for (int i = 0; i < modifiedTimetables.size(); i++) {

            Pair<Timetable, Timetable> modifiedTimetable = modifiedTimetables.get(i);
            Timetable existingTimetable = modifiedTimetable.getLeft();
            Timetable incomingTimetable = modifiedTimetable.getRight();

            Map<String,Object> wrongRouteLinkMap = new HashMap<>();
            wrongRouteLinkMap.put("timetableName",  existingTimetable.getComment() );
            wrongRouteLinkMap.put("existingPeriods", buildPeriodList(existingTimetable.getPeriods()));
            wrongRouteLinkMap.put("incomingPeriods", buildPeriodList(incomingTimetable.getPeriods()));

            wrongRouteLinkMap.put("existingDates", buildDatesList(existingTimetable.getCalendarDays()));
            wrongRouteLinkMap.put("incomingDates", buildDatesList(incomingTimetable.getCalendarDays()));
            resultList.add(wrongRouteLinkMap);
        }
        return resultList;
    }

    private List<String> buildDatesList(List<CalendarDay> calendarDays) {
        List<String> resultList = new ArrayList<>();

        for (int i = 0; i < calendarDays.size(); i++) {
            CalendarDay calendarDay = calendarDays.get(i);
            String inclOrExcl = calendarDay.getIncluded() ? "incl" : "excl";
            resultList.add(calendarDay.getDate().toString() + " " + inclOrExcl);
        }
        return resultList;
    }

    private List<String> buildPeriodList(List<Period> periods) {
        List<String> resultList = new ArrayList<>();
        for (int i = 0; i < periods.size(); i++) {
            Period period = periods.get(i);
            resultList.add("du " + period.getStartDate().toString() + " au " + period.getEndDate().toString());
        }
        return resultList;
    }

    private List<Object> buildMapList(Map<String, Set<String>> mapToWrite, String keyEltName, String valueEltName) {
        List<Object> resultList = new ArrayList<>();

        for (Map.Entry<String, Set<String>> entry : mapToWrite.entrySet()) {
            Map<String,Object> map = new HashMap<>();
            map.put(keyEltName, entry.getKey() );
            map.put(valueEltName, entry.getValue());
            resultList.add(map);
        }
        return resultList;
    }

    private List<Object> buildWrongRouteLinksMultipleFilesList() {
        List<Object> resultList = new ArrayList<>();

        for (Map.Entry<String, Set<String>> wrongRouteLinkEntry : wrongRouteLinksUsedInMutipleFiles.entrySet()) {
            Map<String,Object> wrongRouteLinkMap = new HashMap<>();
            wrongRouteLinkMap.put("routeLinks", wrongRouteLinkEntry.getKey());
            wrongRouteLinkMap.put("fileNames", wrongRouteLinkEntry.getValue());
            resultList.add(wrongRouteLinkMap);
        }
        return resultList;
    }

    private List<Object> buildMissingRouteLinksList() {
        List<Object> resultList = new ArrayList<>();

        for (Map.Entry<String, Set<String>> missingRouteLinkEntry : missingRouteLinks.entrySet()) {

            Map<String,Object> missingLinkMap = new HashMap<>();
            missingLinkMap.put("fileName", missingRouteLinkEntry.getKey());
            missingLinkMap.put("routeLinks", missingRouteLinkEntry.getValue());
            resultList.add(missingLinkMap);
        }
        return resultList;
    }

    private List<Object> buildMultipleUsedTimetablesList( List<Map.Entry<String, Set<String>>> entrySetList) {
        List<Object> resultList = new ArrayList<>();

        for (int i = 0; i < entrySetList.size(); i++) {
            Map.Entry<String, Set<String>> stringListEntry = entrySetList.get(i);

            Map<String, String> timetableMap = new HashMap<>();
            timetableMap.put("timetableName",  stringListEntry.getKey());
            timetableMap.put("networks", buildNetworkListString(stringListEntry.getValue()));
            resultList.add(timetableMap);
        }
        return resultList;
    }

    private List<Object> buildChangedNameList() {
        List<Object>  resultList = new ArrayList<>();
        for (int i = 0; i < changedNameStopAreas.size(); i++) {
            StopArea existingStop = changedNameStopAreas.get(i).getLeft();
            StopArea incomingStop = changedNameStopAreas.get(i).getRight();

            Map<String, String> changedNameMap = new HashMap<>();
            changedNameMap.put("original_stop_id",incomingStop.getOriginalStopId());
            changedNameMap.put("existing_name",existingStop.getName());
            changedNameMap.put("incoming_name",incomingStop.getName());
            resultList.add(changedNameMap);
        }
        return resultList;
    }

    private List<Object> buildWrongGeolocList() {
        List<Object> resultList = new ArrayList<>();

        for (int i = 0; i < wrongGeolocStopAreas.size(); i++) {

            StopArea existingStop = wrongGeolocStopAreas.get(i).getLeft();
            StopArea incomingStop = wrongGeolocStopAreas.get(i).getRight();

            Map<String, Object> wrongGeolocMap = new HashMap<>();
            wrongGeolocMap.put("original_stop_id", incomingStop.getOriginalStopId());
            wrongGeolocMap.put("existing_name", existingStop.getName());
            wrongGeolocMap.put("existing_latitude", existingStop.getLatitude());
            wrongGeolocMap.put("existing_longitude", existingStop.getLongitude());

            wrongGeolocMap.put("incoming_name", incomingStop.getName());
            wrongGeolocMap.put("incoming_latitude", incomingStop.getLatitude());
            wrongGeolocMap.put("incoming_longitude", incomingStop.getLongitude());
            resultList.add(wrongGeolocMap);
        }

        return resultList;
    }

    private List<Object> buildStringList(List<String> listToWrite, String elementName) {
        List<Object> resultList = new ArrayList<>();

        for (String stringToWrite : listToWrite) {
            Map<String,String> newMap = new HashMap<>();
            newMap.put(elementName,stringToWrite);
            resultList.add(newMap);
        }
        return resultList;
    }

    private List<Object> buildStopList(List<StopArea> stopsListToWrite ) {

        List<Object> stopResults = new ArrayList<>();

        stopsListToWrite.stream().forEach(stopArea -> {
            Map<String, String> stopMap = new HashMap<>();
            stopMap.put("stopName", StringUtils.isEmpty(stopArea.getName()) ? "Aucun nom" :  stopArea.getName());
            stopMap.put("stopId", stopArea.getOriginalStopId());
            stopResults.add(stopMap);
        });
        return stopResults;
    }

    private List<Object> buildLinesList() {
        List<Object> lineList = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            Map<String,Object> lineMap = new HashMap<>();

            String lineName = lines.get(i);
            String lineTextColor = lineTextColorMap.containsKey(lineName) ? lineTextColorMap.get(lineName) : "000000";
            String lineBackgroundColor = lineBackgroundColorMap.containsKey(lineName) ? lineBackgroundColorMap.get(lineName) : "FFFFFF";
            String lineShortName = lineShortNameMap.containsKey(lineName) ? lineShortNameMap.get(lineName) : "";



            lineMap.put("lineName", lines.get(i) );
            lineMap.put("lineTextColor", lineTextColor);
            lineMap.put("lineBackgroundColor", lineBackgroundColor);
            lineMap.put("shortName", lineShortName);
            lineList.add(lineMap);
        }

        return lineList;
    }





    /**
     * Write timetables used by multiple networks
     *
     * @param out          the output stream
     * @param entrySetList list of timetables defined in multiple networks (with the associated networks)
     * @param jsonName     the name of the json object under which the  list must be written
     */
    private void printMultipleUsedTimetables(PrintStream out, List<Map.Entry<String, Set<String>>> entrySetList, String jsonName) {
        out.print(",\n");
        out.print("\"" + jsonName + "\": [\n");
        String endOfline;

        for (int i = 0; i < entrySetList.size(); i++) {
            Map.Entry<String, Set<String>> stringListEntry = entrySetList.get(i);
            endOfline = i == entrySetList.size() - 1 ? "\" }\n" : "\" },\n";
            out.print("{ \"timetableName\": \"" + stringListEntry.getKey() + "\",\n");
            out.print(" \"networks\": \"" + buildNetworkListString(stringListEntry.getValue()) + endOfline);
        }

        out.println("]");
    }

    private String buildNetworkListString(Set<String> networkList) {
        return networkList.stream()
                .collect(Collectors.joining(","));
    }

    /**
     * Read each stopPlace and checks that all children quays have the same transport mode
     */
    private void checkStopPlaceTransportModes() {

        buildStopPlaceChildren();

        for (Map.Entry<String, Set<String>> stopPlaceEntry : stopPlaceChildrens.entrySet()) {
            String stopPlaceId = stopPlaceEntry.getKey();


            List<TransportModeNameEnum> transportPortModeList = stopPlaceEntry.getValue().stream()
                    .map(quayTransportMode::get)
                    .distinct()
                    .collect(Collectors.toList());

            if (transportPortModeList.size() > 1) {
                //multiple transport modes has been found for a single StopPlace -> error
                stopPlaceWithMultipleTransportModes.add(stopPlaceId);
            }
        }
    }

    /**
     * Read all quays and build a map to associate a stop place with all its children
     */
    private void buildStopPlaceChildren() {
        for (StopArea stop : stops) {
            String originalStopId = stop.getOriginalStopId();

            if (stop.getParent() != null) {
                StopArea parent = stop.getParent();
                String parentId = parent.getOriginalStopId();

                if (StringUtils.isEmpty(parentId))
                    continue;


                //  We store all childrens
                Set<String> childrens;
                if (!stopPlaceChildrens.containsKey(parentId)) {
                    childrens = new HashSet<>();
                    stopPlaceChildrens.put(parentId, childrens);
                } else {
                    childrens = stopPlaceChildrens.get(parentId);
                }
                childrens.add(originalStopId);
            }
        }
    }

    public void addEmptyPointsInSequence (String fileName, String journeyPatternId){
        if (emptyPointsInSequence.containsKey(fileName)){
            emptyPointsInSequence.get(fileName).add(journeyPatternId);
        }else{
            Set<String> journeyPatternSet = new HashSet<>();
            journeyPatternSet.add(journeyPatternId);
            emptyPointsInSequence.put(fileName, journeyPatternSet);
        }
    }

    public void addEmptyPassingTimes(String fileName, String serviceJourneyId){
        if (emptyPassingTimes.containsKey(fileName)){
            emptyPassingTimes.get(fileName).add(serviceJourneyId);
        }else{
            Set<String> serviceJourneySet = new HashSet<>();
            serviceJourneySet.add(serviceJourneyId);
            emptyPassingTimes.put(fileName, serviceJourneySet);
        }
    }


    @Override
    public void print(PrintStream stream) {
        print(stream, new StringBuilder(), 1, true);

    }
}
