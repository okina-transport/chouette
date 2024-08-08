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
            JourneyPattern_VersionStructure netexJourneyPattern = (org.rutebanken.netex.model.JourneyPattern_VersionStructure) journeyPatternElement.getValue();

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
                }else{
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

            Map<String, List<Map<String, List<String>>>> toADdList = new HashMap<>();
            toADdList.putIfAbsent(chouetteJourneyPattern.getObjectId(), parseStopPointsInJourneyPattern(context, referential, (JourneyPattern_VersionStructure) journeyPatternElement.getValue(), chouetteJourneyPattern));
            wrongStopPointsOrderList.add(toADdList);

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
        // A map to hold aggregated results by JourneyPattern key
        Map<String, Map<String, Set<String>>> aggregatedResult = new HashMap<>();

        // Iterate over each JourneyPattern map in the provided list
        for (Map<String, List<Map<String, List<String>>>> journeyPatternMap : stopPointList) {
            // Iterate over each entry in the JourneyPattern map
            for (Map.Entry<String, List<Map<String, List<String>>>> journeyPatternEntry : journeyPatternMap.entrySet()) {
                String journeyPatternKey = journeyPatternEntry.getKey();
                List<Map<String, List<String>>> stopPointsMaps = journeyPatternEntry.getValue();

                if (stopPointsMaps.size() > 0 ) {
                    // Initialize the map for StopPoints if it's not present
                    aggregatedResult.putIfAbsent(journeyPatternKey, new HashMap<>());

                    // Iterate over each map in the list of StopPoint maps
                    for (Map<String, List<String>> stopPointMap : stopPointsMaps) {
                        // Iterate over each entry in the StopPoint map
                        for (Map.Entry<String, List<String>> entry : stopPointMap.entrySet()) {
                            String stopPoint = entry.getKey();
                            List<String> value = entry.getValue();

                            // Merge lists for the same StopPoint, avoiding duplicates
                            aggregatedResult.get(journeyPatternKey).merge(stopPoint, new HashSet<>(value), (oldSet, newSet) -> {
                                oldSet.addAll(newSet);
                                return oldSet;
                            });
                        }
                    }
                }
            }
        }

        // Convert the aggregatedResult to the required structure
        List<Map<String, List<Map<String, List<String>>>>> result = aggregatedResult.entrySet().stream()
                .map(entry -> {
                    Map<String, List<Map<String, List<String>>>> journeyPatternMap = new HashMap<>();

                    List<Map<String, List<String>>> stopPointListForJourneyPattern = entry.getValue().entrySet().stream()
                            .map(stopPointEntry -> {
                                Map<String, List<String>> stopPointMap = new HashMap<>();
                                stopPointMap.put(stopPointEntry.getKey(), new ArrayList<>(stopPointEntry.getValue()));
                                return stopPointMap;
                            })
                            .collect(Collectors.toList());

                    journeyPatternMap.put(entry.getKey(), stopPointListForJourneyPattern);
                    return journeyPatternMap;
                })
                .collect(Collectors.toList());

        // Set the result in the context if it's not empty
        if (!result.isEmpty()) {
            context.put(WRONG_STOP_POINT_ORDER_IN_JOUNEY_PATTERN, result);
        }
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

    private List<Map<String, List<String>>> parseStopPointsInJourneyPattern(Context context, Referential referential, JourneyPattern_VersionStructure netexJourneyPattern,
                                                            JourneyPattern chouetteJourneyPattern) {
        if (netexJourneyPattern.getPointsInSequence() == null) {
            handleEmptyPointsInSequence(context, netexJourneyPattern);
            return new ArrayList<>();
        }

        List<PointInLinkSequence_VersionedChildStructure> pointsInLinkSequence = netexJourneyPattern.getPointsInSequence()
                .getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern();

        Map<String, List<String>> wrongStopPointsOrder = new HashMap<>();
        List<Map<String, List<String>>> wrongStopPointsOrderList = new ArrayList<>();

        for (int i = 0; i < pointsInLinkSequence.size(); i++) {
            PointInLinkSequence_VersionedChildStructure pointInSequence = pointsInLinkSequence.get(i);
            StopPointInJourneyPattern pointInPattern = (StopPointInJourneyPattern) pointInSequence;

            String stopPointId = NetexImportUtil.composeObjectIdFromNetexId(context, "StopPoint", pointInPattern.getId());
            StopPoint stopPointInJourneyPattern = ObjectFactory.getStopPoint(referential, stopPointId);
            ScheduledStopPointRefStructure scheduledStopPointRef = pointInPattern.getScheduledStopPointRef().getValue();
            String scheduledStopPointId = NetexImportUtil.composeObjectIdFromNetexId(context, "ScheduledStopPoint", scheduledStopPointRef.getRef());

            ScheduledStopPoint scheduledStopPoint = ObjectFactory.getScheduledStopPoint(referential, scheduledStopPointId);
            stopPointInJourneyPattern.setScheduledStopPoint(scheduledStopPoint);

            // Check if the position of stopPointInJourneyPattern is not null and if it does not match the order of pointInPattern
            if (stopPointInJourneyPattern.getPosition() != null && !pointInPattern.getOrder().equals(stopPointInJourneyPattern.getPosition())) {
                List<String> integerList = new ArrayList<>();
//                integerList.add(chouetteJourneyPattern.getObjectId());
                if (!wrongStopPointsOrder.containsValue(pointInPattern.getOrder().intValue())) {
                    integerList.add(pointInPattern.getOrder().toString());
                }
                if (!wrongStopPointsOrder.containsValue(stopPointInJourneyPattern.getPosition())) {
                    integerList.add(stopPointInJourneyPattern.getPosition().toString());
                }
                wrongStopPointsOrder.put(stopPointInJourneyPattern.getObjectId(), integerList);
                wrongStopPointsOrderList.add(wrongStopPointsOrder);
            }
            else {
                stopPointInJourneyPattern.setPosition(pointInPattern.getOrder().intValue());
            }

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

                // HACK TODO HACK
                // Remove Line/PublicCode from DestinationDisplay if FrontText starts with it
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

/*			if (pointInPattern.getBookingArrangements()!=null) {
				BookingArrangementsStructure netexBookingArrangement = pointInPattern.getBookingArrangements();
				BookingArrangement bookingArrangement = new BookingArrangement();
				if (netexBookingArrangement.getBookingNote() != null) {
					bookingArrangement.setBookingNote(netexBookingArrangement.getBookingNote().getValue());
				}
				bookingArrangement.setBookingAccess(NetexParserUtils.toBookingAccess(netexBookingArrangement.getBookingAccess()));
				bookingArrangement.setBookWhen(NetexParserUtils.toPurchaseWhen(netexBookingArrangement.getBookWhen()));
				bookingArrangement.setBuyWhen(netexBookingArrangement.getBuyWhen().stream().map(NetexParserUtils::toPurchaseMoment).collect(Collectors.toList()));
				bookingArrangement.setBookingMethods(netexBookingArrangement.getBookingMethods().stream().map(NetexParserUtils::toBookingMethod).collect(Collectors.toList()));
				bookingArrangement.setLatestBookingTime(TimeUtil.toJodaLocalTime(netexBookingArrangement.getLatestBookingTime()));
				bookingArrangement.setMinimumBookingPeriod(TimeUtil.toJodaDuration(netexBookingArrangement.getMinimumBookingPeriod()));

				bookingArrangement.setBookingContact(contactStructureParser.parse(netexBookingArrangement.getBookingContact()));

				stopPointInJourneyPattern.setBookingArrangement(bookingArrangement);
			}*/

            chouetteJourneyPattern.addStopPoint(stopPointInJourneyPattern);
        }

        List<StopPoint> patternStopPoints = chouetteJourneyPattern.getStopPoints();
        if (CollectionUtils.isNotEmpty(patternStopPoints)) {
            chouetteJourneyPattern.getStopPoints().sort(Comparator.comparingInt(StopPoint::getPosition));
            chouetteJourneyPattern.setDepartureStopPoint(patternStopPoints.get(0));
            chouetteJourneyPattern.setArrivalStopPoint(patternStopPoints.get(patternStopPoints.size() - 1));
        }

//        Route chouetteRoute = chouetteJourneyPattern.getRoute();
//
//        if (chouetteRoute != null){
//            chouetteRoute.getStopPoints().forEach(stopPoint -> stopPoint.setPosition(chouetteRoute.getStopPoints().indexOf(stopPoint)));
//            chouetteRoute.getStopPoints().sort(Comparator.comparingInt(StopPoint::getPosition));
//            chouetteRoute.setFilled(true);
//        }

        return wrongStopPointsOrderList;
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
