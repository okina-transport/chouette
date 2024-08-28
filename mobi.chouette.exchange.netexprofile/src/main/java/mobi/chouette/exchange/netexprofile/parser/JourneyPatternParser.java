package mobi.chouette.exchange.netexprofile.parser;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.exchange.NetexParserUtils;
import mobi.chouette.exchange.importer.Parser;
import mobi.chouette.exchange.importer.ParserFactory;
import mobi.chouette.exchange.netexprofile.Constant;
import mobi.chouette.exchange.netexprofile.importer.util.NetexImportUtil;
import mobi.chouette.exchange.report.AnalyzeReport;
import mobi.chouette.model.*;
import mobi.chouette.model.DestinationDisplay;
import mobi.chouette.model.JourneyPattern;
import mobi.chouette.model.ScheduledStopPoint;
import mobi.chouette.model.type.AlightingPossibilityEnum;
import mobi.chouette.model.type.BoardingPossibilityEnum;
import mobi.chouette.model.type.SectionStatusEnum;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.Referential;
import org.apache.commons.collections.CollectionUtils;
import org.rutebanken.netex.model.*;

import javax.xml.bind.JAXBElement;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Log4j
public class JourneyPatternParser extends NetexParser implements Parser, Constant {


    private ContactStructureParser contactStructureParser = new ContactStructureParser();

    private KeyValueParser keyValueParser = new KeyValueParser();

    @Override
    public void parse(Context context) throws Exception {
        Referential referential = (Referential) context.get(REFERENTIAL);
        JourneyPatternsInFrame_RelStructure journeyPatternStruct = (JourneyPatternsInFrame_RelStructure) context.get(NETEX_LINE_DATA_CONTEXT);

        List<Map<String, List<Map<String, List<String>>>>> wrongStopPointsOrderList = new ArrayList<>();

        for (JAXBElement<?> journeyPatternElement : journeyPatternStruct.getJourneyPattern_OrJourneyPatternView()) {
            JourneyPattern_VersionStructure netexJourneyPattern = (JourneyPattern_VersionStructure) journeyPatternElement.getValue();

            String journeyPatternId = NetexImportUtil.composeObjectIdFromNetexId(context, "JourneyPattern", netexJourneyPattern.getId());
            mobi.chouette.model.JourneyPattern chouetteJourneyPattern = ObjectFactory.getJourneyPattern(referential, journeyPatternId);

            chouetteJourneyPattern.setObjectVersion(NetexParserUtils.getVersion(netexJourneyPattern));

            if (netexJourneyPattern.getRouteRef() == null) {
                log.warn("journeyPattern sans route ref:" + netexJourneyPattern.getId());
            } else {
                String routeIdRef = NetexImportUtil.composeObjectIdFromNetexId(context, "Route", netexJourneyPattern.getRouteRef().getRef());
                mobi.chouette.model.Route route = ObjectFactory.getRoute(referential, routeIdRef);
                chouetteJourneyPattern.setRoute(route);

                if (netexJourneyPattern.getName() != null) {
                    chouetteJourneyPattern.setName(netexJourneyPattern.getName().getValue());
                } else if (netexJourneyPattern.getDestinationDisplayRef() != null) {
                    String destinationDisplayRef = netexJourneyPattern.getDestinationDisplayRef().getRef();
                    mobi.chouette.model.DestinationDisplay chouetteDestinationDisplay = ObjectFactory.getDestinationDisplay(referential, destinationDisplayRef);
                    chouetteJourneyPattern.setName(chouetteDestinationDisplay.getFrontText());
                } else {
                    chouetteJourneyPattern.setName(route.getName());
                }
            }

            if (netexJourneyPattern.getPrivateCode() != null) {
                chouetteJourneyPattern.setRegistrationNumber(netexJourneyPattern.getPrivateCode().getValue());
            }

            if (netexJourneyPattern.getDestinationDisplayRef() != null) {
                String destinationDisplayId = netexJourneyPattern.getDestinationDisplayRef().getRef();
                DestinationDisplay destinationDisplay = ObjectFactory.getDestinationDisplay(referential, destinationDisplayId);
                chouetteJourneyPattern.setDestinationDisplay(destinationDisplay);
            }

            List<Map<String, List<String>>> wrongOrders = parseStopPointsToFindAndCorrectStopPointOrder(context, referential, netexJourneyPattern);
            if (!wrongOrders.isEmpty()) {
                Map<String, List<Map<String, List<String>>>> finalWrongOrdersMap = new HashMap<>();
                finalWrongOrdersMap.put(chouetteJourneyPattern.getObjectId(), wrongOrders);
                wrongStopPointsOrderList.add(finalWrongOrdersMap);
            }

            parseStopPointsInJourneyPattern(context, referential, netexJourneyPattern, chouetteJourneyPattern);
            parseServiceLinksInJourneyPattern(referential, netexJourneyPattern, chouetteJourneyPattern);
            chouetteJourneyPattern.setFilled(true);
            initRouteSections(referential, chouetteJourneyPattern);
            chouetteJourneyPattern.setKeyValues(keyValueParser.parse(netexJourneyPattern.getKeyList()));
        }

        mergeWrongStopPointsOrderAndSetContext(context, wrongStopPointsOrderList);
    }

    /**
     * This method processes a list of maps containing StopPoints and their associated integer lists.
     * It merges the lists for each StopPoint within each map, ensuring there are no duplicate values,
     * and sets the result in the given context under a specific key.
     *
     * @param context      The context in which the result will be set.
     * @param stopPointList The list of maps containing StopPoints and their associated integer lists.
     */
    private void mergeWrongStopPointsOrderAndSetContext(Context context, List<Map<String, List<Map<String, List<String>>>>> stopPointList) {
        Map<String, List<String>> stopPointOrders = new HashMap<>();
        Map<String, List<String>> jpList = new HashMap<>();

        collectStopPointOrders(stopPointList, stopPointOrders, jpList);
        List<Map<String, Map<String, String>>> result = processStopPointOrders(context, stopPointOrders, jpList);

        if (!result.isEmpty()) {
            context.put(WRONG_STOP_POINT_ORDER_IN_JOUNEY_PATTERN, result);
        }
    }

    /**
     * Collects all stop point orders and associated journey patterns.
     *
     * @param stopPointList    The list of stop points with their associated orders and journey patterns.
     * @param stopPointOrders  The map where stop point IDs will be associated with their orders.
     * @param jpList           The map where stop point IDs will be associated with their journey patterns.
     */
    private void collectStopPointOrders(
            List<Map<String, List<Map<String, List<String>>>>> stopPointList,
            Map<String, List<String>> stopPointOrders,
            Map<String, List<String>> jpList) {

        for (Map<String, List<Map<String, List<String>>>> journeyPatternMap : stopPointList) {
            for (Map.Entry<String, List<Map<String, List<String>>>> entry : journeyPatternMap.entrySet()) {
                String journeyPatternId = entry.getKey();
                List<Map<String, List<String>>> stopPoints = entry.getValue();

                for (Map<String, List<String>> stopPointMap : stopPoints) {
                    for (Map.Entry<String, List<String>> stopPointEntry : stopPointMap.entrySet()) {
                        String stopPointId = stopPointEntry.getKey();
                        List<String> orders = stopPointEntry.getValue();

                        stopPointOrders.computeIfAbsent(stopPointId, k -> new ArrayList<>()).addAll(orders);
                        jpList.computeIfAbsent(stopPointId, k -> new ArrayList<>()).add(journeyPatternId);
                    }
                }
            }
        }
    }

    /**
     * Processes stop point orders to identify and document incorrect orders.
     *
     * @param context          The context where results will be stored.
     * @param stopPointOrders  The map of stop point orders to process.
     * @param jpList           The map of journey patterns associated with each stop point.
     * @return                 A list of result maps documenting incorrect stop point orders.
     */
    private List<Map<String, Map<String, String>>> processStopPointOrders(
            Context context,
            Map<String, List<String>> stopPointOrders,
            Map<String, List<String>> jpList) {

        List<Map<String, Map<String, String>>> result = new ArrayList<>();

        for (Map.Entry<String, List<String>> stopPointEntry : stopPointOrders.entrySet()) {
            String stopPointId = stopPointEntry.getKey();
            List<String> orders = stopPointEntry.getValue();

            if (!allValuesIdentical(orders)) {
                Map<String, Long> orderCounts = orders.stream()
                        .collect(Collectors.groupingBy(e -> e, Collectors.counting()));

                String mainOrderFound = Collections.max(orderCounts.entrySet(), Map.Entry.comparingByValue()).getKey();
                Long mainOrderCount = orderCounts.get(mainOrderFound);

                Map<String, String> orderDetails = createOrderDetails(context, stopPointId, mainOrderFound, mainOrderCount, jpList, orderCounts);

                if (!resultContainsDuplicate(result, stopPointId, orderDetails)) {
                    Map<String, Map<String, String>> resultMap = new HashMap<>();
                    resultMap.put(stopPointId, orderDetails);
                    result.add(resultMap);
                }
            }
        }

        return result;
    }

    /**
     * Creates the details map for a stop point order, including main and incorrect orders.
     *
     * @param context          The context containing the file name.
     * @param stopPointId      The ID of the stop point.
     * @param mainOrderFound   The main order that was found.
     * @param mainOrderCount   The count of the main order occurrences.
     * @param jpList           The list of journey patterns associated with the stop point.
     * @param orderCounts      The counts of all orders associated with the stop point.
     * @return                 A map containing the order details for the stop point.
     */
    private Map<String, String> createOrderDetails(
            Context context,
            String stopPointId,
            String mainOrderFound,
            Long mainOrderCount,
            Map<String, List<String>> jpList,
            Map<String, Long> orderCounts) {

        Map<String, String> orderDetails = new HashMap<>();
        orderDetails.put("fileName", context.get("file_name").toString());
        orderDetails.put("stopPointId", stopPointId);
        orderDetails.put("mainOrderFound", mainOrderFound);
        orderDetails.put("mainOrderFoundCount", mainOrderCount.toString());

        for (Map.Entry<String, List<String>> jp : jpList.entrySet()) {
            if (jp.getKey().equals(stopPointId)) {
                orderDetails.put("journeyPatternList", jp.getValue().toString());
            }
        }

        for (Map.Entry<String, Long> countEntry : orderCounts.entrySet()) {
            if (!countEntry.getKey().equals(mainOrderFound)) {
                orderDetails.put("wrongOrder", countEntry.getKey());
                orderDetails.put("wrongOrderCount", countEntry.getValue().toString());
            }
        }

        return orderDetails;
    }

    /**
     * Checks if a result map already contains a specific stop point with the given details.
     *
     * @param result           The list of result maps.
     * @param stopPointId      The stop point ID to check for.
     * @param orderDetails     The order details to compare.
     * @return                 True if the result already contains the stop point with these details, false otherwise.
     */
    private boolean resultContainsDuplicate(
            List<Map<String, Map<String, String>>> result,
            String stopPointId,
            Map<String, String> orderDetails) {

        Map<String, Map<String, String>> resultMap = new HashMap<>();
        resultMap.put(stopPointId, orderDetails);

        return result.stream().anyMatch(map -> map.equals(resultMap));
    }

    /**
     * Checks if all values in a list are identical.
     *
     * @param values  The list of values to check.
     * @return        True if all values are identical, false otherwise.
     */
    private boolean allValuesIdentical(List<String> values) {
        return values.stream().distinct().count() <= 1;
    }


    /**
     * Recover all routeSections of a journey pattern and set it to the journey pattern
     *
     * @param referential    Referential that contains all routeSections
     * @param journeyPattern Journey pattern on which routeSection must be initialized
     */
    private void initRouteSections(Referential referential, mobi.chouette.model.JourneyPattern journeyPattern) {

        List<StopPoint> orderedPoints = journeyPattern.getStopPoints().stream()
                .sorted(Comparator.comparing(StopPoint::getPosition))
                .collect(Collectors.toList());

        List<RouteSection> routeSections = new ArrayList<>();

        for (int i = 0; i < orderedPoints.size() - 1; i++) {
            StopPoint sectionStartPoint = orderedPoints.get(i);
            StopPoint sectionEndPoint = orderedPoints.get(i + 1);
            Optional<RouteSection> routeSectionOpt = getRouteSection(referential, sectionStartPoint, sectionEndPoint);
            routeSectionOpt.ifPresent(routeSections::add);
        }

        if (!routeSections.isEmpty()) {
            journeyPattern.setRouteSections(routeSections);
            journeyPattern.setSectionStatus(SectionStatusEnum.Completed);
        }
    }

    /**
     * Recover a routeSection from referential, using start point and end point
     *
     * @param referential       Referential that contains all routeSections
     * @param sectionStartPoint Start point of the section
     * @param sectionEndPoint   End point of the section
     * @return - An empty optional if no routeSection has been found
     * - An optional with the recovered routeSection
     */
    private Optional<RouteSection> getRouteSection(Referential referential, StopPoint sectionStartPoint, StopPoint sectionEndPoint) {
        String startScheduledPointId = sectionStartPoint.getScheduledStopPoint().getObjectId();
        String endScheduledPointId = sectionEndPoint.getScheduledStopPoint().getObjectId();


        return referential.getRouteSections().values().stream()
                .filter(routeSection ->
                        routeSection.getFromScheduledStopPoint().getObjectId().equals(startScheduledPointId) &&
                                routeSection.getToScheduledStopPoint().getObjectId().equals(endScheduledPointId))
                .findFirst();
    }


    private void parseServiceLinksInJourneyPattern(Referential referential, org.rutebanken.netex.model.JourneyPattern_VersionStructure netexJourneyPattern,
                                                   mobi.chouette.model.JourneyPattern chouetteJourneyPattern) {

        if (netexJourneyPattern.getLinksInSequence() == null || netexJourneyPattern.getLinksInSequence().getServiceLinkInJourneyPatternOrTimingLinkInJourneyPattern() == null) {
            return;
        }
        List<LinkInLinkSequence_VersionedChildStructure> linksInLinkSequence = netexJourneyPattern.getLinksInSequence()
                .getServiceLinkInJourneyPatternOrTimingLinkInJourneyPattern();

        for (LinkInLinkSequence_VersionedChildStructure linkInLinkSequence : linksInLinkSequence) {
            if (linkInLinkSequence instanceof ServiceLinkInJourneyPattern_VersionedChildStructure) {

                ServiceLinkInJourneyPattern_VersionedChildStructure serviceLinkInJourneyPattern = (ServiceLinkInJourneyPattern_VersionedChildStructure) linkInLinkSequence;

                if (serviceLinkInJourneyPattern.getServiceLinkRef() != null && serviceLinkInJourneyPattern.getServiceLinkRef().getRef() != null) {
                    chouetteJourneyPattern.getRouteSections().add(ObjectFactory.getRouteSection(referential, serviceLinkInJourneyPattern.getServiceLinkRef().getRef()));
                }
            } else {
                log.warn("Got unexpected linkInLinkSequence element: " + linkInLinkSequence);
            }

        }

        if (chouetteJourneyPattern.hasCompleteRouteSections()) {
            chouetteJourneyPattern.setSectionStatus(SectionStatusEnum.Completed);
        }

    }

    /**
     * Finds stop points with incorrect order in the journey pattern and updates their positions if needed.
     *
     * This method iterates over the stop points in the provided journey pattern and checks if their order matches
     * their position. If an incorrect order is found, it adds the discrepancies to a list.
     *
     * @param context The context containing necessary configurations and objects for parsing.
     * @param referential The referential object containing the stop points and other related data.
     * @param netexJourneyPattern The journey pattern structure containing the stop points sequence.
     * @return A list of maps containing stop points with incorrect orders and their expected positions.
     */
    private List<Map<String, List<String>>> parseStopPointsToFindAndCorrectStopPointOrder(Context context,
                                                                                          Referential referential,
                                                                                          JourneyPattern_VersionStructure netexJourneyPattern) {

        List<PointInLinkSequence_VersionedChildStructure> pointsInLinkSequence = netexJourneyPattern.getPointsInSequence()
                .getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern();

        Map<String, List<String>> wrongStopPointsOrder = new HashMap<>();
        List<Map<String, List<String>>> wrongStopPointsOrderList = new ArrayList<>();

        for (int i = 0; i < pointsInLinkSequence.size(); i++) {
            PointInLinkSequence_VersionedChildStructure pointInSequence = pointsInLinkSequence.get(i);
            StopPointInJourneyPattern pointInPattern = (StopPointInJourneyPattern) pointInSequence;

            String stopPointId = NetexImportUtil.composeObjectIdFromNetexId(context, "StopPoint", pointInPattern.getId());
            StopPoint stopPointInJourneyPattern = ObjectFactory.getStopPoint(referential, stopPointId);

            // Check and compare the order
            if (stopPointInJourneyPattern.getPosition() != null && !pointInPattern.getOrder().equals(stopPointInJourneyPattern.getPosition())) {
                List<String> orders = wrongStopPointsOrder.computeIfAbsent(stopPointInJourneyPattern.getObjectId(), k -> new ArrayList<>());
                orders.add(pointInPattern.getOrder().toString());
                orders.add(stopPointInJourneyPattern.getPosition().toString());
            } else {
                stopPointInJourneyPattern.setPosition(pointInPattern.getOrder().intValue());
            }
        }

        if (!wrongStopPointsOrder.isEmpty()) {
            wrongStopPointsOrderList.add(wrongStopPointsOrder);
        }

        return wrongStopPointsOrderList;
    }

    private void parseStopPointsInJourneyPattern(Context context,
                                                 Referential referential,
                                                 JourneyPattern_VersionStructure netexJourneyPattern,
                                                 JourneyPattern chouetteJourneyPattern) {
        if (netexJourneyPattern.getPointsInSequence() == null) {
            handleEmptyPointsInSequence(context, netexJourneyPattern);
            return;
        }

        List<PointInLinkSequence_VersionedChildStructure> pointsInLinkSequence = netexJourneyPattern.getPointsInSequence()
                .getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern();

        for (int i = 0; i < pointsInLinkSequence.size(); i++) {
            PointInLinkSequence_VersionedChildStructure pointInSequence = pointsInLinkSequence.get(i);
            StopPointInJourneyPattern pointInPattern = (StopPointInJourneyPattern) pointInSequence;

            String stopPointId = NetexImportUtil.composeObjectIdFromNetexId(context, "StopPoint", pointInPattern.getId());
            StopPoint stopPointInJourneyPattern = ObjectFactory.getStopPoint(referential, stopPointId);
            ScheduledStopPointRefStructure scheduledStopPointRef = pointInPattern.getScheduledStopPointRef().getValue();
            String scheduledStopPointId = NetexImportUtil.composeObjectIdFromNetexId(context, "ScheduledStopPoint", scheduledStopPointRef.getRef());

            ScheduledStopPoint scheduledStopPoint = ObjectFactory.getScheduledStopPoint(referential, scheduledStopPointId);
            stopPointInJourneyPattern.setScheduledStopPoint(scheduledStopPoint);

            checkAndRecordInvalidStopPointCoordinates(context, scheduledStopPoint, stopPointId);

            stopPointInJourneyPattern.setObjectVersion(NetexParserUtils.getVersion(pointInPattern.getVersion()));

            if (pointInPattern.isForAlighting() != null && !pointInPattern.isForAlighting()) {
                stopPointInJourneyPattern.setForAlighting(AlightingPossibilityEnum.forbidden);
            } else if (Boolean.TRUE.equals(pointInPattern.isRequestStop())) {
                stopPointInJourneyPattern.setForAlighting(AlightingPossibilityEnum.request_stop);
            } else {
                stopPointInJourneyPattern.setForAlighting(AlightingPossibilityEnum.normal);
            }

            if (pointInPattern.isForBoarding() != null && !pointInPattern.isForBoarding()) {
                stopPointInJourneyPattern.setForBoarding(BoardingPossibilityEnum.forbidden);
            } else if (Boolean.TRUE.equals(pointInPattern.isRequestStop())) {
                stopPointInJourneyPattern.setForBoarding(BoardingPossibilityEnum.request_stop);
            } else {
                stopPointInJourneyPattern.setForBoarding(BoardingPossibilityEnum.normal);
            }

            chouetteJourneyPattern.addStopPoint(stopPointInJourneyPattern);
            stopPointInJourneyPattern.setRoute(chouetteJourneyPattern.getRoute());

            if (pointInPattern.getDestinationDisplayRef() != null) {
                String destinationDisplayId = pointInPattern.getDestinationDisplayRef().getRef();
                DestinationDisplay destinationDisplay = ObjectFactory.getDestinationDisplay(referential, destinationDisplayId);

                String lineNumber = referential.getLines().values().iterator().next().getNumber();
                if (destinationDisplay.getFrontText().startsWith(lineNumber + " ")) {
                    String modifiedDestinationDisplayId = destinationDisplayId + "-NOLINENUMBER";
                    DestinationDisplay modifiedDestinationDisplay = referential.getSharedDestinationDisplays().get(modifiedDestinationDisplayId);
                    if (modifiedDestinationDisplay == null) {
                        modifiedDestinationDisplay = ObjectFactory.getDestinationDisplay(referential, modifiedDestinationDisplayId);
                        modifiedDestinationDisplay.setName(destinationDisplay.getName() == null ? "" : destinationDisplay.getName() + " (stripped number)");
                        modifiedDestinationDisplay.setFrontText(destinationDisplay.getFrontText().substring(lineNumber.length() + 1));
                        modifiedDestinationDisplay.setSideText(destinationDisplay.getSideText());
                        modifiedDestinationDisplay.getVias().addAll(destinationDisplay.getVias());
                    }
                    stopPointInJourneyPattern.setDestinationDisplay(modifiedDestinationDisplay);
                } else {
                    stopPointInJourneyPattern.setDestinationDisplay(destinationDisplay);
                }
            }
        }

        List<StopPoint> patternStopPoints = chouetteJourneyPattern.getStopPoints();
        if (CollectionUtils.isNotEmpty(patternStopPoints)) {
            chouetteJourneyPattern.getStopPoints().sort(Comparator.comparingInt(StopPoint::getPosition));
            chouetteJourneyPattern.setDepartureStopPoint(patternStopPoints.get(0));
            chouetteJourneyPattern.setArrivalStopPoint(patternStopPoints.get(patternStopPoints.size() - 1));
        }
    }

    /**
     * Checks if the given scheduled stop point has invalid coordinates (longitude and latitude set to zero).
     * If the coordinates are invalid, the stop point ID is recorded in the context under the key for
     * wrong schedule stop point coordinates.
     *
     * @param context The context containing necessary configurations and objects for parsing.
     * @param scheduledStopPoint The scheduled stop point to be checked for invalid coordinates.
     * @param stopPointId The ID of the stop point to be recorded if the coordinates are invalid.
     */
    private void checkAndRecordInvalidStopPointCoordinates(Context context,
                                                           ScheduledStopPoint scheduledStopPoint,
                                                           String stopPointId) {
        if (scheduledStopPoint != null &&
                scheduledStopPoint.getContainedInStopAreaRef() != null &&
                scheduledStopPoint.getContainedInStopAreaRef().getObject() != null &&
                scheduledStopPoint.getContainedInStopAreaRef().getObject().getLongitude().equals(BigDecimal.ZERO) &&
                scheduledStopPoint.getContainedInStopAreaRef().getObject().getLatitude().equals(BigDecimal.ZERO)) {

            List<String> wrongStopPointCoordinatesList = (List<String>) context.get(WRONG_SCHEDULE_STOP_POINT_COORDINATES);
            if (wrongStopPointCoordinatesList != null) {
                wrongStopPointCoordinatesList.add(stopPointId);
                context.put(WRONG_SCHEDULE_STOP_POINT_COORDINATES, wrongStopPointCoordinatesList);
            } else {
                List<String> wrongStopPointCreateList = new ArrayList<>();
                wrongStopPointCreateList.add(stopPointId);
                context.put(WRONG_SCHEDULE_STOP_POINT_COORDINATES, wrongStopPointCreateList);
            }
        }
    }


    private void handleEmptyPointsInSequence(Context context, JourneyPattern_VersionStructure netexJourneyPattern) {


        String fileName = (String) context.get(FILE_NAME);
        String journeyPatternId = netexJourneyPattern.getId();

        log.error("Empty points in sequence in file :" + fileName + " , journeyPattern:" + journeyPatternId);

        if (context.get(ANALYSIS_REPORT) == null) {
            return;
        }

        AnalyzeReport analyzeReport = (AnalyzeReport) context.get(ANALYSIS_REPORT);
        analyzeReport.addEmptyPointsInSequence(fileName, journeyPatternId);
    }

    static {
        ParserFactory.register(JourneyPatternParser.class.getName(), new ParserFactory() {
            private JourneyPatternParser instance = new JourneyPatternParser();

            @Override
            protected Parser create() {
                return instance;
            }
        });
    }

}
