package mobi.chouette.exchange.netexprofile.parser;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.exchange.importer.Parser;
import mobi.chouette.exchange.importer.ParserFactory;
import mobi.chouette.exchange.netexprofile.Constant;
import mobi.chouette.exchange.netexprofile.ConversionUtil;
import mobi.chouette.exchange.netexprofile.importer.NetexprofileImportParameters;
import mobi.chouette.exchange.netexprofile.util.NetexObjectUtil;
import mobi.chouette.model.*;
import mobi.chouette.model.JourneyPattern;
import mobi.chouette.model.Line;
import mobi.chouette.model.VehicleJourney;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.Referential;
import org.apache.commons.collections.CollectionUtils;
import org.rutebanken.netex.model.*;
import org.rutebanken.netex.model.Branding;
import org.rutebanken.netex.model.DestinationDisplay;

import org.rutebanken.netex.model.Network;
import org.rutebanken.netex.model.Route;
import org.rutebanken.netex.model.ScheduledStopPoint;


import javax.xml.bind.JAXBElement;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j
public class PublicationDeliveryParser extends NetexParser implements Parser, Constant {

	static final String LOCAL_CONTEXT = "PublicationDelivery";
	static final String COMPOSITE_FRAME = "compositeFrame";
	static final String TIMETABLE_FRAME = "timetableFrame";
	static final String SERVICE_CALENDAR_FRAME = "serviceCalendarFrame";
	private LineParser lineParser;
	private OrganisationParser organisationParser;
	private Parser serviceCalendarParser;
	private RouteParser routeParser;
	private DirectionParser directionParser;
	private JourneyPatternParser journeyPatternParser;
	private ScheduledStopPointParser scheduledStopPointParser;
	private DestinationDisplayParser destinationDisplayParser;
	private HeadwayJourneyGroupParser headwayJourneyGroupParser;
	private ServiceJourneyParser serviceJourneyParser;
	private TemplateServiceJourneyParser templateServiceJourneyParser;
	private StopAssignmentParser stopAssignmentParser;
	private StopPlaceParser stopPlaceParser;
	private RouteLinkParser routeLinkParser;
	private SiteConnectionParser siteConnectionParser;

	public static org.rutebanken.netex.model.ObjectFactory netexFactory = null;

	public static final Comparator<StopPoint> STOP_POINT_POSITION_COMPARATOR = new Comparator<StopPoint>() {
		@Override
		public int compare(StopPoint sp1, StopPoint sp2) {
			return Integer.compare(sp1.getPosition(), sp2.getPosition());
		}
	};

	static {
		try {
			netexFactory = new org.rutebanken.netex.model.ObjectFactory();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	@Override
	public void parse(Context context) throws Exception {

		lineParser = (LineParser) ParserFactory.create(LineParser.class.getName());
		organisationParser = (OrganisationParser) ParserFactory.create(OrganisationParser.class.getName());
		serviceCalendarParser = ParserFactory.create(ServiceCalendarFrameParser.class.getName());
		routeParser = (RouteParser) ParserFactory.create(RouteParser.class.getName());
		directionParser = (DirectionParser) ParserFactory.create(DirectionParser.class.getName());
		journeyPatternParser = (JourneyPatternParser) ParserFactory.create(JourneyPatternParser.class.getName());
		scheduledStopPointParser = (ScheduledStopPointParser) ParserFactory.create(ScheduledStopPointParser.class.getName());
		destinationDisplayParser = (DestinationDisplayParser) ParserFactory.create(DestinationDisplayParser.class.getName());
		headwayJourneyGroupParser = (HeadwayJourneyGroupParser) ParserFactory.create(HeadwayJourneyGroupParser.class.getName());
		serviceJourneyParser = (ServiceJourneyParser) ParserFactory.create(ServiceJourneyParser.class.getName());
		templateServiceJourneyParser = (TemplateServiceJourneyParser) ParserFactory.create(TemplateServiceJourneyParser.class.getName());
		stopAssignmentParser = (StopAssignmentParser) ParserFactory.create(StopAssignmentParser.class.getName());
		stopPlaceParser = (StopPlaceParser) ParserFactory.create(StopPlaceParser.class.getName());
		routeLinkParser = (RouteLinkParser) ParserFactory.create(RouteLinkParser.class.getName());
		siteConnectionParser = (SiteConnectionParser) ParserFactory.create(SiteConnectionParser.class.getName());



		boolean isCommonDelivery = (boolean) context.get(NETEX_WITH_COMMON_DATA);
		Referential referential = (Referential) context.get(REFERENTIAL);
		NetexprofileImportParameters configuration = (NetexprofileImportParameters) context.get(CONFIGURATION);
		PublicationDeliveryStructure publicationDelivery = (PublicationDeliveryStructure) context.get(NETEX_DATA_JAVA);
		List<JAXBElement<? extends Common_VersionFrameStructure>> dataObjectFrames = publicationDelivery.getDataObjects().getCompositeFrameOrCommonFrame();
		List<CompositeFrame> compositeFrames = NetexObjectUtil.getFrames(CompositeFrame.class, dataObjectFrames);

		if (compositeFrames.size() > 0) {

			// parse composite frame elements
			for (CompositeFrame compositeFrame : compositeFrames) {

				parseValidityConditionsInFrame(context, compositeFrame);

				List<JAXBElement<? extends Common_VersionFrameStructure>> frames = compositeFrame.getFrames().getCommonFrame();
				List<ResourceFrame> resourceFrames = NetexObjectUtil.getFrames(ResourceFrame.class, frames);
				List<ServiceFrame> serviceFrames = NetexObjectUtil.getFrames(ServiceFrame.class, frames);
				List<SiteFrame> siteFrames = NetexObjectUtil.getFrames(SiteFrame.class, frames);
				List<ServiceCalendarFrame> serviceCalendarFrames = NetexObjectUtil.getFrames(ServiceCalendarFrame.class, frames);
				List<TimetableFrame> timetableFrames = NetexObjectUtil.getFrames(TimetableFrame.class, frames);
				List<GeneralFrame> generalFrames = NetexObjectUtil.getFrames(GeneralFrame.class, frames);

				List<ServiceCalendarFrame> generatedServiceCalendarFrames = generalFrames.stream()
																						.map(this::buildServiceCalendarFrameFromGeneralFrame)
																						.filter(Optional::isPresent)
																						.map(Optional::get)
																						.collect(Collectors.toList());
				serviceCalendarFrames.addAll(generatedServiceCalendarFrames);

				// pre processing
				preParseReferentialDependencies(context, referential, serviceFrames, timetableFrames, isCommonDelivery);

				// normal processing
				parseResourceFrames(context, resourceFrames);


				if (configuration.isParseSiteFrames()) {
					parseSiteFrames(context, siteFrames);
				}
				parseServiceFrames(context, serviceFrames, isCommonDelivery);
				parseServiceCalendarFrame(context, serviceCalendarFrames);
				parseGeneralFrames(context,generalFrames);

				if (!isCommonDelivery) {
					parseTimetableFrames(context, timetableFrames);
				}
			}
		} else {

			// no composite frame present
			List<ResourceFrame> resourceFrames = NetexObjectUtil.getFrames(ResourceFrame.class, dataObjectFrames);
			List<ServiceFrame> serviceFrames = NetexObjectUtil.getFrames(ServiceFrame.class, dataObjectFrames);
			List<SiteFrame> siteFrames = NetexObjectUtil.getFrames(SiteFrame.class, dataObjectFrames);
			List<ServiceCalendarFrame> serviceCalendarFrames = NetexObjectUtil.getFrames(ServiceCalendarFrame.class, dataObjectFrames);
			List<TimetableFrame> timetableFrames = NetexObjectUtil.getFrames(TimetableFrame.class, dataObjectFrames);
			List<GeneralFrame> generalFrames = NetexObjectUtil.getFrames(GeneralFrame.class, dataObjectFrames);

			List<ServiceCalendarFrame> generatedServiceCalendarFrames = generalFrames.stream()
																					 .map(this::buildServiceCalendarFrameFromGeneralFrame)
																					 .filter(Optional::isPresent)
																					 .map(Optional::get)
																					 .collect(Collectors.toList());
			serviceCalendarFrames.addAll(generatedServiceCalendarFrames);

			// pre processing
			preParseReferentialDependencies(context, referential, serviceFrames, timetableFrames, isCommonDelivery);

			// normal processing
			parseResourceFrames(context, resourceFrames);

			if (configuration.isParseSiteFrames()) {
				parseSiteFrames(context, siteFrames);
			}
			parseServiceFrames(context, serviceFrames, isCommonDelivery);
			parseServiceCalendarFrame(context, serviceCalendarFrames);
			parseGeneralFrames(context, generalFrames);

			if (!isCommonDelivery) {
				parseTimetableFrames(context, timetableFrames);
			}
		}

		if (!isCommonDelivery){

			for (Line currLine : referential.getLines().values()) {
				currLine.getRoutes().forEach(route -> createMissingRouteLinksForRoute(referential, route));
			}

		}

		// post processing
		// sortStopPoints(referential);
		// updateBoardingAlighting(referential);
	}


	/**
	 * Read a route and creates missing route links between stops
	 * @param referential
	 * 	the referential that contains all cached data
	 * @param currentRoute
	 * 	the route for which we need to create missing route links
	 */
	private void createMissingRouteLinksForRoute(Referential referential, mobi.chouette.model.Route currentRoute){
		currentRoute.getJourneyPatterns().forEach(journeyPattern -> createMissingRouteLinksForJourneyPattern(referential, journeyPattern));
	}

	/**
	 * Read a journey pattern and creates missing route links between stops
	 * @param referential
	 * 	the referential that contains all cached data
	 * @param journeyPattern
	 * 	the journey pattern for which we need to create missing route links
	 */
	private void createMissingRouteLinksForJourneyPattern(Referential referential, JourneyPattern journeyPattern) {
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

				String routeSectionId = NETEX_VALID_PREFIX + ":RouteSection:" + fromScheduledId.replace(NETEX_VALID_PREFIX + ":ScheduledStopPoint:", "") + "-" + toScheduledId.replace(NETEX_VALID_PREFIX + ":ScheduledStopPoint:", "");

				RouteSection routeSection = ObjectFactory.getRouteSection(referential, routeSectionId);
				routeSection.setFromScheduledStopPoint(previousStopPoint.getScheduledStopPoint());
				routeSection.setToScheduledStopPoint(stopPoint.getScheduledStopPoint());

			}
			previousStopPoint = stopPoint;
		}
	}



	private boolean checkRouteLinkPresence(Referential referential, StopPoint startPoint, StopPoint endPoint) {

		mobi.chouette.model.ScheduledStopPoint startScheduledStopPoint = startPoint.getScheduledStopPoint();
		mobi.chouette.model.ScheduledStopPoint endScheduledStopPoint = endPoint.getScheduledStopPoint();

		for (RouteSection routeSection : referential.getRouteSections().values()) {
			if (routeSection.getFromScheduledStopPoint().equals(startScheduledStopPoint) && routeSection.getToScheduledStopPoint().equals(endScheduledStopPoint)){
				return true;
			}
		}
		return false;
	}



	/**
	 * Build a resource frame with data stored in "members" element of General Frame
	 * @param generalFrame
	 *   A general frame with data in members element
	 * @return
	 *   A resource frame
	 */
	private ResourceFrame buildResourceFrameFromGeneralFrame(GeneralFrame generalFrame){
		ResourceFrame resourceFrame = netexFactory.createResourceFrame();
		List<JAXBElement<? extends EntityStructure>> members = generalFrame.getMembers().getGeneralFrameMemberOrDataManagedObjectOrEntity_Entity();

		// Organisations construction
		OrganisationsInFrame_RelStructure organisationsInMembers = netexFactory.createOrganisationsInFrame_RelStructure();
		List<JAXBElement<? extends DataManagedObjectStructure>> organisations = NetexObjectUtil.getMembersAsJaxb(org.rutebanken.netex.model.Operator.class, members);
		organisations.addAll(NetexObjectUtil.getMembersAsJaxb(org.rutebanken.netex.model.Authority.class, members));
		organisationsInMembers.withOrganisation_(organisations);
		resourceFrame.setOrganisations(organisationsInMembers);

		return resourceFrame;
	}

	/**
	 * Build a timetable frame with data stored in "members" element of general frame
	 * @param generalFrame
	 *  A general frame with data in members element
	 * @return
	 *  A timetable frame
	 */
	private TimetableFrame buildTimetableFrameFromGeneralFrame(GeneralFrame generalFrame){
		TimetableFrame timetableFrame = netexFactory.createTimetableFrame();
		List<JAXBElement<? extends EntityStructure>> members = generalFrame.getMembers().getGeneralFrameMemberOrDataManagedObjectOrEntity_Entity();

		//Journeys construction
		JourneysInFrame_RelStructure journeysInFrame_relStructure = netexFactory.createJourneysInFrame_RelStructure();
		List<Journey_VersionStructure> journeys = new ArrayList<>();
		List<ServiceJourney> netexJourneyInframe = NetexObjectUtil.getMembers(org.rutebanken.netex.model.ServiceJourney.class, members);
		journeys.addAll(netexJourneyInframe);
		journeysInFrame_relStructure.withVehicleJourneyOrDatedVehicleJourneyOrNormalDatedVehicleJourney(journeys);


		timetableFrame.setVehicleJourneys(journeysInFrame_relStructure);
		return timetableFrame;
	}


	/**
	 * Build service Frames from data in general Frame.
	 * - 1 main frame is created to store Lines, routes, directions
	 * - x frames are added for networks (because each network must be stored in a single serviceFrame)
	 *
	 * @param generalFrame
	 * 		General frame that contains all data in members element
	 * @return
	 * 		A stream of serviceFrame
	 */
	private Stream<ServiceFrame> buildServiceFramesFromGeneralFrame(GeneralFrame generalFrame){

		List<ServiceFrame> resultServiceFrames = new ArrayList<>();

		ServiceFrame mainServiceFrame = netexFactory.createServiceFrame();
		List<JAXBElement<? extends EntityStructure>> members = generalFrame.getMembers().getGeneralFrameMemberOrDataManagedObjectOrEntity_Entity();

		// Lines construction
		LinesInFrame_RelStructure linesInFrames = netexFactory.createLinesInFrame_RelStructure();
		List<JAXBElement<? extends DataManagedObjectStructure>> lines = NetexObjectUtil.getMembersAsJaxb(org.rutebanken.netex.model.Line.class, members);
		linesInFrames.withLine_(lines);
		mainServiceFrame.setLines(linesInFrames);


		//routes construction
		RoutesInFrame_RelStructure routesInFrames = netexFactory.createRoutesInFrame_RelStructure();
		List<JAXBElement<? extends DataManagedObjectStructure>> rawRoutes = NetexObjectUtil.getMembersAsJaxb(org.rutebanken.netex.model.Route.class, members);
		List<JAXBElement<? extends LinkSequence_VersionStructure>> castedRoutes = rawRoutes.stream()
																							.map(route -> (JAXBElement<? extends LinkSequence_VersionStructure>) route)
																							.collect(Collectors.toList());
		routesInFrames.withRoute_(castedRoutes);
		mainServiceFrame.setRoutes(routesInFrames);


		//direction construction
		DirectionsInFrame_RelStructure directionsInFrame = netexFactory.createDirectionsInFrame_RelStructure();
		List<Direction> directions = NetexObjectUtil.getMembers(Direction.class, members);
		directionsInFrame.withDirection(directions);
		mainServiceFrame.setDirections(directionsInFrame);
		resultServiceFrames.add(mainServiceFrame);

		//journeyPattern construction
		JourneyPatternsInFrame_RelStructure journeyPatternsInFrame = netexFactory.createJourneyPatternsInFrame_RelStructure();
		List<JAXBElement<?>> journeyPatterns = NetexObjectUtil.getMembersAsJaxb(org.rutebanken.netex.model.ServiceJourneyPattern.class, members)
																			.stream()
																			.map(journeyPattern -> (JAXBElement<?>) journeyPattern)
																			.collect(Collectors.toList());
		journeyPatternsInFrame.withJourneyPattern_OrJourneyPatternView(journeyPatterns);
		mainServiceFrame.setJourneyPatterns(journeyPatternsInFrame);


		//ScheduledStopPoints construction
		ScheduledStopPointsInFrame_RelStructure scheduledStopPointInFrame = netexFactory.createScheduledStopPointsInFrame_RelStructure();
		List<ScheduledStopPoint> scheduledStopPoints = NetexObjectUtil.getMembers(org.rutebanken.netex.model.ScheduledStopPoint.class, members);
		scheduledStopPointInFrame.withScheduledStopPoint(scheduledStopPoints);
		mainServiceFrame.setScheduledStopPoints(scheduledStopPointInFrame);


		//Destination display construction
		DestinationDisplaysInFrame_RelStructure destinationDisplayInFrame = netexFactory.createDestinationDisplaysInFrame_RelStructure();
		List<DestinationDisplay> destinationDisplays = NetexObjectUtil.getMembers(org.rutebanken.netex.model.DestinationDisplay.class, members);
		destinationDisplayInFrame.withDestinationDisplay(destinationDisplays);
		mainServiceFrame.setDestinationDisplays(destinationDisplayInFrame);


		//A service Frame is created for each network and added to the result list
		List<ServiceFrame> networkServiceFrames = buildNetworkServiceFrame(generalFrame);
		resultServiceFrames.addAll(networkServiceFrames);


		return resultServiceFrames.stream();
	}

	/**
	 * Build service frames from networks stored in "members" element of general frame
	 * (1 service frame for each network)
	 * @param generalFrame
	 * 		General frame that contains all data in members element
	 * @return
	 * 		A list of service frames for all networks
	 */
	private List<ServiceFrame> buildNetworkServiceFrame(GeneralFrame generalFrame) {
		List<JAXBElement<? extends EntityStructure>> members = generalFrame.getMembers().getGeneralFrameMemberOrDataManagedObjectOrEntity_Entity();
		List<Network> networks = NetexObjectUtil.getMembers(Network.class, members);
		List<ServiceFrame> resultNetworkFrames = new ArrayList<>();

		for (Network network : networks) {
			ServiceFrame serviceFrame = netexFactory.createServiceFrame();
			serviceFrame.setNetwork(network);
			resultNetworkFrames.add(serviceFrame);
		}

		return resultNetworkFrames;
	}

	/**
	 * Build ServiceCalendar frame with data stored in "members" element of general frame
	 * @param generalFrame
	 * 		General frame that contains all data in members element
	 * @return
	 * 		A serviceCalendar frame
	 */
	private Optional<ServiceCalendarFrame> buildServiceCalendarFrameFromGeneralFrame(GeneralFrame generalFrame){
		boolean hasServiceCalendarData = false;//


		ServiceCalendarFrame serviceCalendarFrame = netexFactory.createServiceCalendarFrame();
		List<JAXBElement<? extends EntityStructure>> members = generalFrame.getMembers().getGeneralFrameMemberOrDataManagedObjectOrEntity_Entity();

		//daytypes construction
		List<JAXBElement<? extends DataManagedObjectStructure>> jaxbDayTypes = NetexObjectUtil.getMembersAsJaxb(org.rutebanken.netex.model.DayType.class, members);
		if (!jaxbDayTypes.isEmpty()){
			DayTypesInFrame_RelStructure dayTypes = netexFactory.createDayTypesInFrame_RelStructure();
			dayTypes.withDayType_(jaxbDayTypes);
			serviceCalendarFrame.setDayTypes(dayTypes);
			hasServiceCalendarData = true;
		}


		//daytypes assignments construction
		List<DayTypeAssignment> dayTypesAssignments = NetexObjectUtil.getMembers(org.rutebanken.netex.model.DayTypeAssignment.class, members);
		if (!dayTypesAssignments.isEmpty()){
			DayTypeAssignmentsInFrame_RelStructure daytypesAssignmentsStruct = netexFactory.createDayTypeAssignmentsInFrame_RelStructure();
			daytypesAssignmentsStruct.withDayTypeAssignment(dayTypesAssignments);
			serviceCalendarFrame.setDayTypeAssignments(daytypesAssignmentsStruct);
			hasServiceCalendarData = true;
		}



		//operating period construction
		List<JAXBElement<? extends DataManagedObjectStructure>> rawOperatingPeriods = NetexObjectUtil.getMembersAsJaxb(org.rutebanken.netex.model.OperatingPeriod.class, members);
		if (!rawOperatingPeriods.isEmpty()){
			OperatingPeriodsInFrame_RelStructure operatingPeriodStruct = netexFactory.createOperatingPeriodsInFrame_RelStructure();
			List<OperatingPeriod_VersionStructure> castedOperatingPeriods = rawOperatingPeriods.stream()
					.map(jaxbElt -> (OperatingPeriod_VersionStructure) jaxbElt.getValue())
					.collect(Collectors.toList());
			operatingPeriodStruct.withOperatingPeriodOrUicOperatingPeriod(castedOperatingPeriods);
			serviceCalendarFrame.setOperatingPeriods(operatingPeriodStruct);
			hasServiceCalendarData = true;
		}



		//operating days construction
		List<OperatingDay> operatingDays = NetexObjectUtil.getMembers(org.rutebanken.netex.model.OperatingDay.class, members);
		if (!operatingDays.isEmpty()){
			OperatingDaysInFrame_RelStructure operatingDaysStruct = netexFactory.createOperatingDaysInFrame_RelStructure();
			operatingDaysStruct.withOperatingDay(operatingDays);
			serviceCalendarFrame.setOperatingDays(operatingDaysStruct);
			hasServiceCalendarData = true;
		}

		ValidityConditions_RelStructure validityConditions = netexFactory.createValidityConditions_RelStructure();


		//Add valid between to service frame
		for (ValidBetween validBetween : generalFrame.getValidBetween()) {
			validityConditions.withValidityConditionRefOrValidBetweenOrValidityCondition_(validBetween);
		}
		serviceCalendarFrame.setValidityConditions(validityConditions);

		return hasServiceCalendarData ? Optional.of(serviceCalendarFrame) : Optional.empty();
	}



	private void parseGeneralFrames(Context context, List<GeneralFrame> generalFrames) throws Exception {
		
		for (GeneralFrame generalFrame : generalFrames ){
			List<JAXBElement<? extends EntityStructure>> members = generalFrame.getMembers().getGeneralFrameMemberOrDataManagedObjectOrEntity_Entity();

			List<JAXBElement<? extends DataManagedObjectStructure>> lines = NetexObjectUtil.getMembersAsJaxb(org.rutebanken.netex.model.Line.class, members);
			parseLines(context,lines);

			List<Network> networks = NetexObjectUtil.getMembers(Network.class, members);
			for (Network network : networks) {
				parseNetwork(context,network);
			}

			List<JAXBElement<? extends DataManagedObjectStructure>> organisations = NetexObjectUtil.getMembersAsJaxb(org.rutebanken.netex.model.Operator.class, members);
			organisations.addAll(NetexObjectUtil.getMembersAsJaxb(org.rutebanken.netex.model.Authority.class, members));
			parseOrganisations(context,organisations);

			List<JAXBElement<? extends DataManagedObjectStructure>> stopAssignments = NetexObjectUtil.getMembersAsJaxb(PassengerStopAssignment.class, members);
			parseStopAssignments(context,stopAssignments);


			List<JAXBElement<? extends DataManagedObjectStructure>> rawRoutes = NetexObjectUtil.getMembersAsJaxb(org.rutebanken.netex.model.Route.class, members);


			List<JAXBElement<? extends LinkSequence_VersionStructure>> castedRoutes = rawRoutes.stream()
																								.map(route -> (JAXBElement<? extends LinkSequence_VersionStructure>) route)
																								.collect(Collectors.toList());

			parseRoutes(context,castedRoutes);

			List<Direction> directions = NetexObjectUtil.getMembers(Direction.class, members);
			parseDirection(context,directions);

			List<RouteLink> routeLinks = NetexObjectUtil.getMembers(org.rutebanken.netex.model.RouteLink.class, members);
			parseRouteLinks(context,routeLinks);


			List<JAXBElement<?>> journeyPatterns = NetexObjectUtil.getMembersAsJaxb(org.rutebanken.netex.model.ServiceJourneyPattern.class, members)
																     .stream()
																	 .map(journeyPattern -> (JAXBElement<?>) journeyPattern)
																	 .collect(Collectors.toList());
			parseJourneyPatterns(context,journeyPatterns);


			List<ScheduledStopPoint> scheduledStopPoints = NetexObjectUtil.getMembers(org.rutebanken.netex.model.ScheduledStopPoint.class, members);
			parseScheduledStopPoint(context,scheduledStopPoints);

			List<DestinationDisplay> destinationDisplays = NetexObjectUtil.getMembers(org.rutebanken.netex.model.DestinationDisplay.class, members);
			parseDestinationDisplay(context,destinationDisplays);

			List<ServiceJourney> journeys = NetexObjectUtil.getMembers(org.rutebanken.netex.model.ServiceJourney.class, members);
			parseServiceJourney(context,journeys);

			List<HeadwayJourneyGroup> headwayJourneyGroups = NetexObjectUtil.getMembers(org.rutebanken.netex.model.HeadwayJourneyGroup.class, members);
			parseHeadwayJourneyGroups(context,headwayJourneyGroups);

			List<TemplateServiceJourney> templateServiceJourneys = NetexObjectUtil.getMembers(org.rutebanken.netex.model.TemplateServiceJourney.class, members);
			parseTemplateServiceJourney(context, templateServiceJourneys);

			List<SiteConnection> siteConnections = NetexObjectUtil.getMembers(org.rutebanken.netex.model.SiteConnection.class, members);
			parseSiteConnections(context,siteConnections);

			List<StopPlace> stopPlaces = NetexObjectUtil.getMembers(org.rutebanken.netex.model.StopPlace.class, members);
			List<Quay> quays = NetexObjectUtil.getMembers(org.rutebanken.netex.model.Quay.class, members);
			parseStopPlaces(context, stopPlaces, quays);
		}
	}

	private void parseRouteLinks(Context context, List<RouteLink> netexRouteLinks) throws Exception {

		if (netexRouteLinks.isEmpty())
			return;

		RouteLinksInFrame_RelStructure routeLinksInInFrameStruct = netexFactory.createRouteLinksInFrame_RelStructure();
		routeLinksInInFrameStruct.withRouteLink(netexRouteLinks);
		context.put(NETEX_LINE_DATA_CONTEXT,routeLinksInInFrameStruct);
		routeLinkParser.parse(context);

	}

	private void parseStopAssignments(Context context, List<JAXBElement<? extends DataManagedObjectStructure>> netexStopAssignments) throws Exception {

		if (netexStopAssignments.isEmpty())
			return;

		StopAssignmentsInFrame_RelStructure stopAssignmentsInFrame = netexFactory.createStopAssignmentsInFrame_RelStructure();
		List<JAXBElement<? extends StopAssignment_VersionStructure>> castedStopAssignments = netexStopAssignments.stream()
																												.map(stopAssignment -> (JAXBElement<? extends StopAssignment_VersionStructure>) stopAssignment)
																												.collect(Collectors.toList());

		stopAssignmentsInFrame.withStopAssignment(castedStopAssignments);
		context.put(NETEX_LINE_DATA_CONTEXT,stopAssignmentsInFrame);
		stopAssignmentParser.parse(context);

	}


	private void parseHeadwayJourneyGroups(Context context, List<HeadwayJourneyGroup> headwayJourneyGroups) throws Exception {
		if (headwayJourneyGroups.isEmpty())
			return;

		FrequencyGroups_RelStructure journeyGroupRelStructure = netexFactory.createFrequencyGroups_RelStructure();

		List<HeadwayJourneyGroup_VersionStructure> listHeadwayJourneyGroup = new ArrayList<>();
		listHeadwayJourneyGroup.addAll(headwayJourneyGroups);

		journeyGroupRelStructure.withHeadwayJourneyGroupRefOrHeadwayJourneyGroupOrRhythmicalJourneyGroupRef(listHeadwayJourneyGroup);
		context.put(NETEX_LINE_DATA_CONTEXT, journeyGroupRelStructure);
		headwayJourneyGroupParser.parse(context);
	}

	private void parseServiceJourney(Context context,List<ServiceJourney> netexJourneyInframe) throws Exception {

		if (netexJourneyInframe.isEmpty())
			return;

		JourneysInFrame_RelStructure journeysInFrame_relStructure = netexFactory.createJourneysInFrame_RelStructure();

		List<Journey_VersionStructure> journeys = new ArrayList<>();
		journeys.addAll(netexJourneyInframe);
		journeysInFrame_relStructure.withVehicleJourneyOrDatedVehicleJourneyOrNormalDatedVehicleJourney(journeys);
		context.put(NETEX_LINE_DATA_CONTEXT, journeysInFrame_relStructure);
		serviceJourneyParser.parse(context);
	}

	private void parseTemplateServiceJourney(Context context, List<TemplateServiceJourney> netexJourneyInframe) throws Exception {
		if (netexJourneyInframe.isEmpty())
			return;

		JourneysInFrame_RelStructure journeysInFrame_relStructure = netexFactory.createJourneysInFrame_RelStructure();

		List<Journey_VersionStructure> journeys = new ArrayList<>();
		journeys.addAll(netexJourneyInframe);
		journeysInFrame_relStructure.withVehicleJourneyOrDatedVehicleJourneyOrNormalDatedVehicleJourney(journeys);
		context.put(NETEX_LINE_DATA_CONTEXT, journeysInFrame_relStructure);
		templateServiceJourneyParser.parse(context);
	}

	private void parseDestinationDisplay(Context context,List<DestinationDisplay> netexDestinationDisplays) throws Exception {

		if (netexDestinationDisplays.isEmpty())
			return;

		DestinationDisplaysInFrame_RelStructure destinationDisplayInFrame = netexFactory.createDestinationDisplaysInFrame_RelStructure();
		destinationDisplayInFrame.withDestinationDisplay(netexDestinationDisplays);
		context.put(NETEX_LINE_DATA_CONTEXT, destinationDisplayInFrame);
		destinationDisplayParser.parse(context);
	}

	private void parseScheduledStopPoint(Context context,List<ScheduledStopPoint> netexScheduledStopPoint) throws Exception {

		if (netexScheduledStopPoint.isEmpty())
			return;

		ScheduledStopPointsInFrame_RelStructure scheduledStopPointInFrame = netexFactory.createScheduledStopPointsInFrame_RelStructure();
		scheduledStopPointInFrame.withScheduledStopPoint(netexScheduledStopPoint);
		context.put(NETEX_LINE_DATA_CONTEXT, scheduledStopPointInFrame);
		scheduledStopPointParser.parse(context);
	}


	private void parseJourneyPatterns(Context context,List<JAXBElement<?>> netexJourneyPatterns) throws Exception {

		if (netexJourneyPatterns.isEmpty())
			return;


		JourneyPatternsInFrame_RelStructure journeyPatternsInFrame = netexFactory.createJourneyPatternsInFrame_RelStructure();
		journeyPatternsInFrame.withJourneyPattern_OrJourneyPatternView(netexJourneyPatterns);
		context.put(NETEX_LINE_DATA_CONTEXT, journeyPatternsInFrame);
		journeyPatternParser.parse(context);

	}




	private void parseOrganisations(Context context, List<JAXBElement<? extends DataManagedObjectStructure>> organisations) throws Exception {

		if (organisations.isEmpty())
			return;

		OrganisationsInFrame_RelStructure organisationsInMembers = netexFactory.createOrganisationsInFrame_RelStructure();
		organisationsInMembers.withOrganisation_(organisations);
		context.put(NETEX_LINE_DATA_CONTEXT, organisationsInMembers);
		organisationParser.parse(context);

	}

	private void parseLines(Context context, List<JAXBElement<? extends DataManagedObjectStructure>> netexLines) throws Exception {

		if (netexLines.isEmpty())
			return;

		LinesInFrame_RelStructure linesInFrames = netexFactory.createLinesInFrame_RelStructure();
		linesInFrames.withLine_(netexLines);
		context.put(NETEX_LINE_DATA_CONTEXT, linesInFrames);
		lineParser.parse(context);
		Referential referential = (Referential) context.get(REFERENTIAL);
		Map<String, Line> lines = referential.getLines();


		for (Map.Entry<String, Line> stringLineEntry : lines.entrySet()) {
			//"lines" property is cleaned each time NetexLineParserCommand is called. So, we need to save Line information coming from common files in "sharedLines" , in order to save them
			referential.getSharedLines().put(stringLineEntry.getKey(),stringLineEntry.getValue());
		}
	}

	private void parseDirection(Context context, List<Direction> netexDirections) throws Exception {

		if (netexDirections.isEmpty())
			return;

		DirectionsInFrame_RelStructure directionsInFrame = netexFactory.createDirectionsInFrame_RelStructure();
		directionsInFrame.withDirection(netexDirections);
		context.put(NETEX_LINE_DATA_CONTEXT, directionsInFrame);
		directionParser.parse(context);

	}

	private void parseRoutes(Context context, List<JAXBElement<? extends LinkSequence_VersionStructure>> netexRoutes) throws Exception {

		if (netexRoutes.isEmpty())
			return;


		RoutesInFrame_RelStructure routesInFrames = netexFactory.createRoutesInFrame_RelStructure();
		routesInFrames.withRoute_(netexRoutes);
		context.put(NETEX_LINE_DATA_CONTEXT, routesInFrames);
		routeParser.parse(context);

	}

	private void preParseReferentialDependencies(Context context, Referential referential, List<ServiceFrame> serviceFrames,
			List<TimetableFrame> timetableFrames, boolean isCommonDelivery) throws Exception {

		org.rutebanken.netex.model.Line_VersionStructure line = null;

		for (ServiceFrame serviceFrame : serviceFrames) {

			// pre parsing route points
			if (serviceFrame.getRoutePoints() != null) {
				context.put(NETEX_LINE_DATA_CONTEXT, serviceFrame.getRoutePoints());
				Parser routePointParser = ParserFactory.create(RoutePointParser.class.getName());
				routePointParser.parse(context);
			}

			// stop assignments
			if (serviceFrame.getStopAssignments() != null) {
				context.put(NETEX_LINE_DATA_CONTEXT, serviceFrame.getStopAssignments());
				stopAssignmentParser.parse(context);
			}

			if (!isCommonDelivery) {
				if (line == null) {
					line = (org.rutebanken.netex.model.Line_VersionStructure) serviceFrame.getLines().getLine_().get(0).getValue();
					context.put(PARSING_CONTEXT_LINE_ID, line.getId());
				}

				// preparsing mandatory for stop places to parse correctly
				TariffZonesInFrame_RelStructure tariffZonesStruct = serviceFrame.getTariffZones();
				if (tariffZonesStruct != null) {
					context.put(NETEX_LINE_DATA_CONTEXT, tariffZonesStruct);
					stopPlaceParser.parse(context);
				}
			}
		}

		if (!isCommonDelivery) {
			// Map<String, Set<String>> journeyDayTypeIdMap = new HashMap<>();
			//
			// for (TimetableFrame timetableFrame : timetableFrames) {
			// for (Journey_VersionStructure journeyStruct : timetableFrame.getVehicleJourneys().getDatedServiceJourneyOrDeadRunOrServiceJourney()) {
			// ServiceJourney serviceJourney = (ServiceJourney) journeyStruct;
			// Set<String> dayTypeIds = new HashSet<>();
			//
			// for (JAXBElement<? extends DayTypeRefStructure> dayTypeRefStructElement : serviceJourney.getDayTypes().getDayTypeRef()) {
			// dayTypeIds.add(dayTypeRefStructElement.getValue().getRef());
			// }
			//
			// journeyDayTypeIdMap.put(serviceJourney.getId(), dayTypeIds);
			// }
			// }
			//
			// Set<String> processedIds = new HashSet<>();
			// List<Set<String>> calendarGroups = new ArrayList<>();
			//
			// for (Map.Entry<String, Set<String>> entry1 : journeyDayTypeIdMap.entrySet()) {
			// if (!processedIds.contains(entry1.getKey())) {
			// Set<String> groupedJourneyIds = new HashSet<>();
			// groupedJourneyIds.add(entry1.getKey());
			//
			// for (Map.Entry<String, Set<String>> entry2 : journeyDayTypeIdMap.entrySet()) {
			// if (!entry1.getKey().equals(entry2.getKey())) {
			// if (CollectionUtils.isEqualCollection(entry1.getValue(), entry2.getValue())) {
			// groupedJourneyIds.add(entry2.getKey());
			// processedIds.add(entry2.getKey());
			// }
			// }
			// }
			// calendarGroups.add(groupedJourneyIds);
			// processedIds.add(entry1.getKey());
			// }
			// }
			//
			// assert line != null;
			// String[] idParts = StringUtils.split(line.getId(), ":");
			// String[] idSequence = NetexProducerUtils.generateIdSequence(calendarGroups.size());
			//
			// for (int i = 0; i < calendarGroups.size(); i++) {
			// String timetableIdSuffix = idParts[2] + "-" + StringUtils.leftPad(idSequence[i], 2, "0");
			// String timetableId = netexId(idParts[0], ObjectIdTypes.TIMETABLE_KEY, timetableIdSuffix);
			// Timetable timetable = ObjectFactory.getTimetable(referential, timetableId);
			//
			// for (String journeyId : calendarGroups.get(i)) {
			// addTimetableId(context, journeyId, timetable.getObjectId());
			// }
			// }
		}
	}

	private void parseResourceFrames(Context context, List<ResourceFrame> resourceFrames) throws Exception {
		for (ResourceFrame resourceFrame : resourceFrames) {
			OrganisationsInFrame_RelStructure organisationsInFrameStruct = resourceFrame.getOrganisations();
			if (organisationsInFrameStruct != null) {
				context.put(NETEX_LINE_DATA_CONTEXT, organisationsInFrameStruct);
				organisationParser.parse(context);
			}
			TypesOfValueInFrame_RelStructure typesOfValueInFrame = resourceFrame.getTypesOfValue();
			if (typesOfValueInFrame != null) {
				for (JAXBElement<? extends DataManagedObjectStructure> typeOfValue : typesOfValueInFrame.getValueSetOrTypeOfValue()) {
					if (typeOfValue.getValue() instanceof Branding) {
						parseBranding(context, (Branding) typeOfValue.getValue());
					}
				}
			}
		}
	}

	private void parseSiteFrames(Context context, List<SiteFrame> siteFrames) throws Exception {
		for (SiteFrame siteFrame : siteFrames) {
			StopPlacesInFrame_RelStructure stopPlacesStruct = siteFrame.getStopPlaces();
			if (stopPlacesStruct != null) {
				context.put(NETEX_LINE_DATA_CONTEXT, stopPlacesStruct);
				stopPlaceParser.parse(context);
			}
		}
	}

	private void parseServiceFrames(Context context, List<ServiceFrame> serviceFrames, boolean isCommonDelivery) throws Exception {
		for (ServiceFrame serviceFrame : serviceFrames) {

			if (serviceFrame.getNetwork() != null) {
				Network network = serviceFrame.getNetwork();
				parseNetwork(context, network);
			}

			if (serviceFrame.getAdditionalNetworks() != null && serviceFrame.getAdditionalNetworks().getNetwork() != null) {
				for (Network network : serviceFrame.getAdditionalNetworks().getNetwork()) {
					parseNetwork(context, network);
				}
			}

			if (serviceFrame.getDestinationDisplays() != null) {
				DestinationDisplaysInFrame_RelStructure destinationDisplaysInFrameStruct = serviceFrame.getDestinationDisplays();
				context.put(NETEX_LINE_DATA_CONTEXT, destinationDisplaysInFrameStruct);
				destinationDisplayParser.parse(context);

			}
			if (serviceFrame.getDirections() != null) {
				DirectionsInFrame_RelStructure directionsInFrame_RelStructure = serviceFrame.getDirections();
				context.put(NETEX_LINE_DATA_CONTEXT, directionsInFrame_RelStructure);

				directionParser.parse(context);
			}
			if (serviceFrame.getNotices() != null) {
				for (Notice notice : serviceFrame.getNotices().getNotice()) {
					parseNotice(context, notice);
				}
			}

			if(serviceFrame.getNoticeAssignments() != null) {
				for(JAXBElement<? extends DataManagedObjectStructure> assingment : serviceFrame.getNoticeAssignments().getNoticeAssignment_()) {
					NoticeAssignment a = (NoticeAssignment) assingment.getValue();
					parseNoticeAssignment(context, a);
				}
			}

			if (serviceFrame.getScheduledStopPoints() != null) {
				ScheduledStopPointsInFrame_RelStructure scheduledStopPointsInFrameStruct = serviceFrame.getScheduledStopPoints();
				context.put(NETEX_LINE_DATA_CONTEXT, scheduledStopPointsInFrameStruct);
				scheduledStopPointParser.parse(context);

			}

			if (serviceFrame.getServiceLinks() !=null) {
				ServiceLinksInFrame_RelStructure serviceLinksInFrameStruct= serviceFrame.getServiceLinks();
				context.put(NETEX_LINE_DATA_CONTEXT, serviceLinksInFrameStruct);
				ServiceLinkParser serviceLinkParser = (ServiceLinkParser) ParserFactory.create(ServiceLinkParser.class.getName());
				serviceLinkParser.parse(context);
			}


			if (!isCommonDelivery) {
				LinesInFrame_RelStructure linesInFrameStruct = serviceFrame.getLines();
				if (linesInFrameStruct != null) {
					context.put(NETEX_LINE_DATA_CONTEXT, linesInFrameStruct);
					lineParser.parse(context);
				}
				RoutesInFrame_RelStructure routesInFrameStruct = serviceFrame.getRoutes();
				if (routesInFrameStruct != null) {
					context.put(NETEX_LINE_DATA_CONTEXT, routesInFrameStruct);
					routeParser.parse(context);
				}
				JourneyPatternsInFrame_RelStructure journeyPatternStruct = serviceFrame.getJourneyPatterns();
				if (journeyPatternStruct != null) {
					context.put(NETEX_LINE_DATA_CONTEXT, journeyPatternStruct);
					journeyPatternParser.parse(context);
				}
			}
		}
	}

	private void parseNetwork(Context context, Network network) throws Exception {
		context.put(NETEX_LINE_DATA_CONTEXT, network);
		NetworkParser networkParser = (NetworkParser) ParserFactory.create(NetworkParser.class.getName());
		networkParser.parse(context);
	}

	private void parseServiceCalendarFrame(Context context, List<ServiceCalendarFrame> serviceCalendarFrames) throws Exception {
		for (ServiceCalendarFrame serviceCalendarFrame : serviceCalendarFrames) {

			parseValidityConditionsInFrame(context, serviceCalendarFrame);
			context.put(NETEX_LINE_DATA_CONTEXT, serviceCalendarFrame);
			serviceCalendarParser.parse(context);
		}
	}

	private void parseTimetableFrames(Context context, List<TimetableFrame> timetableFrames) throws Exception {
		for (TimetableFrame timetableFrame : timetableFrames) {

			parseValidityConditionsInFrame(context, timetableFrame);

			if (timetableFrame.getNotices() != null) {
				for (Notice notice : timetableFrame.getNotices().getNotice()) {
					parseNotice(context, notice);
				}
			}

			if (timetableFrame.getNoticeAssignments() != null) {
				for (JAXBElement<? extends DataManagedObjectStructure> assingment : timetableFrame.getNoticeAssignments().getNoticeAssignment_()) {
					NoticeAssignment a = (NoticeAssignment) assingment.getValue();
					parseNoticeAssignment(context, a);
				}
			}

			JourneysInFrame_RelStructure vehicleJourneysStruct = timetableFrame.getVehicleJourneys();
			context.put(NETEX_LINE_DATA_CONTEXT, vehicleJourneysStruct);
			serviceJourneyParser.parse(context);

			JourneyInterchangesInFrame_RelStructure journeyInterchangesStruct = timetableFrame.getJourneyInterchanges();
			if (journeyInterchangesStruct != null) {
				context.put(NETEX_LINE_DATA_CONTEXT, journeyInterchangesStruct);
				Parser serviceInterchangeParser = ParserFactory.create(ServiceJourneyInterchangeParser.class.getName());
				serviceInterchangeParser.parse(context);
			}

		}
	}

	private void parseNoticeAssignment(Context context, NoticeAssignment assignment) {
		Referential referential = (Referential) context.get(REFERENTIAL);

		Footnote footnote = ObjectFactory.getFootnote(referential, assignment.getNoticeRef().getRef());
		String noticedObject = assignment.getNoticedObjectRef().getRef();

		if (noticedObject.contains(":Line:")) {
			Line line = ObjectFactory.getLine(referential, noticedObject);
			line.getFootnotes().add(footnote);
		} else if (noticedObject.contains(":VehicleJourney:") || noticedObject.contains(":ServiceJourney:")) {
			VehicleJourney vehicleJourney = ObjectFactory.getVehicleJourney(referential, noticedObject);
			vehicleJourney.getFootnotes().add(footnote);
		} else if (noticedObject.contains(":JourneyPattern:") || noticedObject.contains(":ServiceJourneyPattern:")) {
			JourneyPattern journeyPattern = ObjectFactory.getJourneyPattern(referential, noticedObject);
			journeyPattern.getFootnotes().add(footnote);
		} else if (noticedObject.contains(":StopPointInJourneyPattern:")) {
			StopPoint stopPointInJourneyPattern = ObjectFactory.getStopPoint(referential, noticedObject);
			stopPointInJourneyPattern.getFootnotes().add(footnote);
		} else if (noticedObject.contains(":TimetabledPassingTime:")) {
			VehicleJourneyAtStop vjas = ObjectFactory.getVehicleJourneyAtStop(referential, noticedObject);
			vjas.getFootnotes().add(footnote);
		} else {
			log.warn("Unsupported NoticedObjectRef type: " + noticedObject);
		}
	}

	private void parseNotice(Context context, Notice notice) {
		Referential referential = (Referential) context.get(REFERENTIAL);

		Footnote footnote = ObjectFactory.getFootnote(referential, notice.getId());
		footnote.setLabel(ConversionUtil.getValue(notice.getText()));
		footnote.setCode(notice.getPublicCode());
	}

	private void parseSiteConnections(Context context, List<SiteConnection> siteConnections) throws Exception {
		if (siteConnections.isEmpty())
			return;

		context.put(NETEX_LINE_DATA_CONTEXT, siteConnections);
		siteConnectionParser.parse(context);
	}

	private void parseStopPlaces(Context context, List<StopPlace> stopPlaces, List<Quay> quays) throws Exception {
		if (stopPlaces.isEmpty() || quays.isEmpty())
			return;

		List<String> stopPlacesWithoutQuay = new ArrayList<>();
		List<String> multimodalStopPlaces = new ArrayList<>();
		for(StopPlace stopPlace : stopPlaces){
			if(stopPlace.getQuays() != null && stopPlace.getQuays().getQuayRefOrQuay().size() > 0) {
				List<Quay> quayList = quays
						.stream()
						.filter(quay -> stopPlace.getQuays().getQuayRefOrQuay()
								.stream()
								.anyMatch(o -> quay.getId().equals(((QuayRefStructure) o.getValue()).getRef())))
						.collect(Collectors.toList());

				Quays_RelStructure quays_relStructure = new Quays_RelStructure();
				quays_relStructure.getQuayRefOrQuay().addAll(quayList.stream().map(netexFactory::createQuay).collect(Collectors.toList()));
				stopPlace.withQuays(quays_relStructure);
			}
			else{
				if(stopPlace.getPlaceTypes() != null &&
						stopPlace.getPlaceTypes().getTypeOfPlaceRef().size() > 0 &&
						stopPlace.getPlaceTypes().getTypeOfPlaceRef().stream()
								.anyMatch(typeOfPlaceRefStructure -> "multimodalstopplace".equalsIgnoreCase(typeOfPlaceRefStructure.getRef()))){
					multimodalStopPlaces.add(stopPlace.getId());
				}
				else{
					stopPlacesWithoutQuay.add(stopPlace.getId());
				}
			}
		}

		context.put(STOP_PLACES_WITHOUT_QUAY, stopPlacesWithoutQuay);
		context.put(MULTIMODAL_STOP_PLACES, multimodalStopPlaces);

		StopPlacesInFrame_RelStructure stopPlacesStruct = new StopPlacesInFrame_RelStructure();
		stopPlacesStruct.getStopPlace_().addAll(stopPlaces.stream().map(netexFactory::createStopPlace).collect(Collectors.toList()));

		context.put(NETEX_LINE_DATA_CONTEXT, stopPlacesStruct);
		stopPlaceParser.parse(context);
	}

	private void parseBranding(Context context, Branding netexBranding) {
		Referential referential = (Referential) context.get(REFERENTIAL);

		mobi.chouette.model.Branding chouetteBranding = ObjectFactory.getBranding(referential, netexBranding.getId());
		if (netexBranding.getName() != null) {
			chouetteBranding.setName(netexBranding.getName().getValue());
		}
		if (netexBranding.getDescription() != null) {
			chouetteBranding.setDescription(netexBranding.getDescription().getValue());
		}
		chouetteBranding.setUrl(netexBranding.getUrl());
		chouetteBranding.setImage(netexBranding.getImage());
	}

	private void parseValidityConditionsInFrame(Context context, Common_VersionFrameStructure frameStruct) throws Exception {
		if (frameStruct instanceof CompositeFrame) {
			parseValidityConditionsInFrame(context, COMPOSITE_FRAME, frameStruct);
		} else if (frameStruct instanceof TimetableFrame) {
			parseValidityConditionsInFrame(context, TIMETABLE_FRAME, frameStruct);
		} else if (frameStruct instanceof ServiceCalendarFrame) {
			parseValidityConditionsInFrame(context, SERVICE_CALENDAR_FRAME, frameStruct);
		}
	}

	private void parseValidityConditionsInFrame(Context context, String contextKey, Common_VersionFrameStructure frameStruct) throws Exception {
		if (frameStruct.getContentValidityConditions() != null) {
			ValidBetween validBetween = getValidBetween(frameStruct.getContentValidityConditions());
			if (validBetween != null) {
				addValidBetween(context, contextKey, validBetween);
			}
		} else if (frameStruct.getValidityConditions() != null) {
			ValidBetween validBetween = getValidBetween(frameStruct.getValidityConditions());
			if (validBetween != null) {
				addValidBetween(context, contextKey, validBetween);
			}
		} else if (CollectionUtils.isNotEmpty(frameStruct.getValidBetween())) {
			ValidBetween validBetween = getValidBetween(frameStruct.getValidBetween());
			if (validBetween != null) {
				addValidBetween(context, contextKey, validBetween);
			}
		}
	}

	private void addValidBetween(Context context, String contextKey, ValidBetween validBetween) {
		Context localContext = getLocalContext(context, LOCAL_CONTEXT);

		if (localContext.containsKey(contextKey)) {
			localContext.replace(contextKey, validBetween);
		} else {
			localContext.put(contextKey, validBetween);
		}
	}

	static {
		ParserFactory.register(PublicationDeliveryParser.class.getName(), new ParserFactory() {
			private PublicationDeliveryParser instance = new PublicationDeliveryParser();

			@Override
			protected Parser create() {
				return instance;
			}
		});
	}

}
