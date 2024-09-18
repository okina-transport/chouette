package mobi.chouette.exchange.netexprofile.parser;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.TimeUtil;
import mobi.chouette.dao.AccessibilityAssessmentDAO;
import mobi.chouette.exchange.NetexParserUtils;
import mobi.chouette.exchange.importer.Parser;
import mobi.chouette.exchange.importer.ParserFactory;
import mobi.chouette.exchange.netexprofile.Constant;
import mobi.chouette.exchange.netexprofile.importer.NetexprofileImportParameters;
import mobi.chouette.exchange.netexprofile.importer.util.NetexImportUtil;
import mobi.chouette.exchange.netexprofile.importer.util.NetexTimeConversionUtil;
import mobi.chouette.exchange.report.AnalyzeReport;
import mobi.chouette.model.DestinationDisplay;
import mobi.chouette.model.JourneyPattern;
import mobi.chouette.model.Train;
import mobi.chouette.model.VehicleJourney;
import mobi.chouette.model.*;
import mobi.chouette.model.type.TransportModeNameEnum;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.ObjectIdTypes;
import mobi.chouette.model.util.Referential;
import org.rutebanken.netex.model.AccessibilityAssessment;
import org.rutebanken.netex.model.FlexibleServiceProperties;
import org.rutebanken.netex.model.*;

import javax.xml.bind.JAXBElement;
import java.util.*;
import java.util.stream.Collectors;

@Log4j
public class ServiceJourneyParser extends NetexParser implements Parser, Constant {

	private KeyValueParser keyValueParser = new KeyValueParser();

	private ContactStructureParser contactStructureParser = new ContactStructureParser();

	@Override
	@SuppressWarnings("unchecked")
	public void parse(Context context) throws Exception {
		Referential referential = (Referential) context.get(REFERENTIAL);
		JourneysInFrame_RelStructure journeyStructs = (JourneysInFrame_RelStructure) context.get(NETEX_LINE_DATA_CONTEXT);
		NetexprofileImportParameters parameters = (NetexprofileImportParameters) context.get(CONFIGURATION);
		Map<String, Set<String>> brandingRefMap = (Map<String, Set<String>>) context.get(BRANDING_REF_MAP);

		Map<AccessibilityAssessment, List<VehicleJourney>> accessibilityMap =
				(Map<AccessibilityAssessment, List<VehicleJourney>>) context.get(NETEX_ACCESSIBILITY_MAP);
		if (accessibilityMap == null) {
			accessibilityMap = new HashMap<>();
			context.put(NETEX_ACCESSIBILITY_MAP, accessibilityMap);
		}

		List<Journey_VersionStructure> serviceJourneys = journeyStructs.getVehicleJourneyOrDatedVehicleJourneyOrNormalDatedVehicleJourney();

		for (Journey_VersionStructure journeyStruct : serviceJourneys) {
			if (! (journeyStruct instanceof ServiceJourney)) {
				log.debug("Ignoring non-ServiceJourney journey or deadrun with id: " + journeyStruct.getId());
				continue;
			}
			ServiceJourney serviceJourney = (ServiceJourney) journeyStruct;

			String serviceJourneyId = NetexImportUtil.composeObjectIdFromNetexId(context,"ServiceJourney", serviceJourney.getId());

			if (serviceJourney.getBrandingRef() != null && serviceJourney.getBrandingRef().getRef() != null){
				String brandingRef = serviceJourney.getBrandingRef().getRef();
				if (brandingRefMap.containsKey(brandingRef)){
					brandingRefMap.get(brandingRef).add(serviceJourneyId);
				}else{
					Set<String> journeyIdsByBrand = new HashSet<>();
					journeyIdsByBrand.add(serviceJourneyId);
					brandingRefMap.put(brandingRef, journeyIdsByBrand);
				}
			}

			VehicleJourney vehicleJourney = ObjectFactory.getVehicleJourney(referential,serviceJourneyId);

			if (vehicleJourney.isFilled()) {
				VehicleJourney vehicleJourneyWithVersion = ObjectFactory.getVehicleJourney(referential,
						serviceJourney.getId() + "_" + serviceJourney.getVersion());
				log.warn("Already parsed " + vehicleJourney.getObjectId() + ", will use version field as part of id to separate them: "
						+ vehicleJourneyWithVersion.getObjectId());
				vehicleJourney = vehicleJourneyWithVersion;
			}

			DayTypeRefs_RelStructure dayTypes = serviceJourney.getDayTypes();
			if (dayTypes != null) {
				for (JAXBElement<? extends DayTypeRefStructure> dayType : dayTypes.getDayTypeRef()) {
					String timetableId = NetexImportUtil.composeObjectIdFromNetexId(context, "Timetable", dayType.getValue().getRef());
					Timetable timetable = ObjectFactory.getTimetable(referential, timetableId);
					timetable.addVehicleJourney(vehicleJourney);
				}
			}

			vehicleJourney.setObjectVersion(NetexParserUtils.getVersion(serviceJourney));

			vehicleJourney.setPublishedJourneyIdentifier(serviceJourney.getPublicCode());

			if (serviceJourney.getPrivateCode() != null) {
				vehicleJourney.setPrivateCode(serviceJourney.getPrivateCode().getValue());
			}

			if (serviceJourney.getJourneyPatternRef() != null) {
				JourneyPatternRefStructure patternRefStruct = serviceJourney.getJourneyPatternRef().getValue();
				String journeyPatternId = NetexImportUtil.composeObjectIdFromNetexId("JourneyPattern", parameters.getObjectIdPrefix(), patternRefStruct.getRef());

				mobi.chouette.model.JourneyPattern journeyPattern = ObjectFactory.getJourneyPattern(referential,journeyPatternId);
				vehicleJourney.setJourneyPattern(journeyPattern);
			}

			if (serviceJourney.getName() != null) {
				vehicleJourney.setPublishedJourneyName(serviceJourney.getName().getValue());
			} else {
				JourneyPattern journeyPattern = vehicleJourney.getJourneyPattern();
				if (journeyPattern.getDepartureStopPoint() != null) {
					DestinationDisplay dd = journeyPattern.getDepartureStopPoint().getDestinationDisplay();
					if (dd != null) {
						vehicleJourney.setPublishedJourneyName(dd.getFrontText());
					}
				}
			}

			if (serviceJourney.getOperatorRef() != null) {
				String operatorIdRef = serviceJourney.getOperatorRef().getRef();
				Company company = ObjectFactory.getCompany(referential, operatorIdRef);
				vehicleJourney.setCompany(company);
			} else if (serviceJourney.getLineRef() != null) {
				String lineIdRef = serviceJourney.getLineRef().getValue().getRef();
				Company company = ObjectFactory.getLine(referential, lineIdRef).getCompany();
				vehicleJourney.setCompany(company);
			} else {
				Company company = vehicleJourney.getJourneyPattern().getRoute().getLine().getCompany();
				vehicleJourney.setCompany(company);
			}

			if (serviceJourney.getRouteRef() != null) {
				mobi.chouette.model.Route route = ObjectFactory.getRoute(referential, serviceJourney.getRouteRef().getRef());
				vehicleJourney.setRoute(route);
			} else {
				mobi.chouette.model.Route route = vehicleJourney.getJourneyPattern().getRoute();
				vehicleJourney.setRoute(route);
			}

			if (serviceJourney.getTransportMode() != null) {
				AllVehicleModesOfTransportEnumeration transportMode = serviceJourney.getTransportMode();
				TransportModeNameEnum transportModeName = NetexParserUtils.toTransportModeNameEnum(transportMode.value());
				vehicleJourney.setTransportMode(transportModeName);
			}

			vehicleJourney.setTransportSubMode(NetexParserUtils.toTransportSubModeNameEnum(serviceJourney.getTransportSubmode()));

			parseTimetabledPassingTimes(context, referential, serviceJourney, vehicleJourney);

			vehicleJourney.setKeyValues(keyValueParser.parse(serviceJourney.getKeyList()));
			vehicleJourney.setServiceAlteration(NetexParserUtils.toServiceAlterationEum(serviceJourney.getServiceAlteration()));

			if (serviceJourney.getFlexibleServiceProperties() != null) {
				vehicleJourney.setFlexibleService(true);
				mobi.chouette.model.FlexibleServiceProperties chouetteFSP = new mobi.chouette.model.FlexibleServiceProperties();
				FlexibleServiceProperties netexFSP = serviceJourney.getFlexibleServiceProperties();

				chouetteFSP.setObjectId(netexFSP.getId());
				chouetteFSP.setObjectVersion(NetexParserUtils.getVersion(netexFSP));

				chouetteFSP.setChangeOfTimePossible(netexFSP.isChangeOfTimePossible());
				chouetteFSP.setCancellationPossible(netexFSP.isCancellationPossible());
				chouetteFSP.setFlexibleServiceType(NetexParserUtils.toFlexibleServiceType(netexFSP.getFlexibleServiceType()));

				BookingArrangement bookingArrangement = new BookingArrangement();
				if (netexFSP.getBookingNote() != null) {
					bookingArrangement.setBookingNote(netexFSP.getBookingNote().getValue());
				}
				bookingArrangement.setBookingAccess(NetexParserUtils.toBookingAccess(netexFSP.getBookingAccess()));
				bookingArrangement.setBookWhen(NetexParserUtils.toPurchaseWhen(netexFSP.getBookWhen()));
				bookingArrangement.setBuyWhen(netexFSP.getBuyWhen().stream().map(NetexParserUtils::toPurchaseMoment).collect(Collectors.toList()));
				bookingArrangement.setBookingMethods(netexFSP.getBookingMethods().stream().map(NetexParserUtils::toBookingMethod).collect(Collectors.toList()));
				bookingArrangement.setLatestBookingTime(TimeUtil.toJodaLocalTime(netexFSP.getLatestBookingTime()));
				bookingArrangement.setMinimumBookingPeriod(TimeUtil.toJodaDuration(netexFSP.getMinimumBookingPeriod()));

				//bookingArrangement.setBookingContact(contactStructureParser.parse(netexFSP.getBookingContact()));


				chouetteFSP.setBookingArrangement(bookingArrangement);
				vehicleJourney.setFlexibleServiceProperties(chouetteFSP);
			}
			if (serviceJourney.getTrainNumbers() != null) {
				for (TrainNumberRefStructure trainNumberRef : serviceJourney.getTrainNumbers().getTrainNumberRef()) {
					Train train = ObjectFactory.getTrain(referential, trainNumberRef.getRef());
					vehicleJourney.getTrains().add(train);
				}
			}
			vehicleJourney.setFilled(true);

			if (serviceJourney.getAccessibilityAssessment() != null) {
				AccessibilityAssessment accessibilityAssessment = serviceJourney.getAccessibilityAssessment();
				boolean found = false;

				for (Map.Entry<AccessibilityAssessment, List<VehicleJourney>> entry : accessibilityMap.entrySet()) {
					if (compareAccessibilityAssessments(entry.getKey(), accessibilityAssessment)) {
						entry.getValue().add(vehicleJourney);
						found = true;
						break;
					}
				}

				if (!found) {
					List<VehicleJourney> serviceJourneyList = new ArrayList<>();
					serviceJourneyList.add(vehicleJourney);
					accessibilityMap.put(accessibilityAssessment, serviceJourneyList);
				}
			}
		}
	}

	private boolean compareAccessibilityAssessments(AccessibilityAssessment a1, AccessibilityAssessment a2) {
		if (a1 == null || a2 == null) return false;

		return a1.getLimitations().getAccessibilityLimitation().getWheelchairAccess() == a2.getLimitations().getAccessibilityLimitation().getWheelchairAccess() &&
				a1.getLimitations().getAccessibilityLimitation().getVisualSignsAvailable() == a2.getLimitations().getAccessibilityLimitation().getVisualSignsAvailable() &&
				a1.getLimitations().getAccessibilityLimitation().getStepFreeAccess() == a2.getLimitations().getAccessibilityLimitation().getStepFreeAccess() &&
				a1.getLimitations().getAccessibilityLimitation().getLiftFreeAccess() == a2.getLimitations().getAccessibilityLimitation().getLiftFreeAccess() &&
				a1.getLimitations().getAccessibilityLimitation().getEscalatorFreeAccess() == a2.getLimitations().getAccessibilityLimitation().getEscalatorFreeAccess() &&
				a1.getLimitations().getAccessibilityLimitation().getAudibleSignalsAvailable() == a2.getLimitations().getAccessibilityLimitation().getAudibleSignalsAvailable() &&
				a1.getMobilityImpairedAccess() == a2.getMobilityImpairedAccess();
	}

	private void parseTimetabledPassingTimes(Context context, Referential referential, ServiceJourney serviceJourney, VehicleJourney vehicleJourney) {

		NetexprofileImportParameters configuration = (NetexprofileImportParameters) context.get(CONFIGURATION);
		String journeyPatternId = NetexImportUtil.composeObjectIdFromNetexId(context,"JourneyPattern",serviceJourney.getJourneyPatternRef().getValue().getRef());

		JourneyPattern journeyPattern = referential.getJourneyPatterns().get(journeyPatternId);

		if (serviceJourney.getPassingTimes() == null){
			handleEmptyPassingTimes(context, serviceJourney);
			return;
		}

		for (int i = 0; i < serviceJourney.getPassingTimes().getTimetabledPassingTime().size(); i++) {
			TimetabledPassingTime passingTime = serviceJourney.getPassingTimes().getTimetabledPassingTime().get(i);
			String passingTimeId = passingTime.getId();

			if (passingTimeId == null) {
				// TODO profile should prevent this from happening, creating bogus
				passingTimeId = NetexParserUtils.netexId(configuration.getObjectIdPrefix(), ObjectIdTypes.VEHICLE_JOURNEY_AT_STOP_KEY, UUID.randomUUID().toString());
			}
			VehicleJourneyAtStop vehicleJourneyAtStop = ObjectFactory.getVehicleJourneyAtStop(referential, passingTimeId);
			vehicleJourneyAtStop.setObjectVersion(NetexParserUtils.getVersion(passingTime));

			if (journeyPattern.getStopPoints().size() == 0 ){
				log.warn("Empty stop points list for journey pattern : " + journeyPattern.getObjectId());
				log.warn("serviceJourney : " + serviceJourney.getId());
			}

//			StopPoint stopPoint = getStopPointFromJourneyPattern(journeyPattern, i);
			StopPoint stopPoint = journeyPattern.getStopPoints().get(i);
			vehicleJourneyAtStop.setStopPoint(stopPoint);

			parsePassingTimes(passingTime, vehicleJourneyAtStop);
			vehicleJourneyAtStop.setVehicleJourney(vehicleJourney);
		}

		vehicleJourney.getVehicleJourneyAtStops().sort(Comparator.comparingInt(o -> o.getStopPoint().getPosition()));
	}

	private void handleEmptyPassingTimes(Context context, ServiceJourney serviceJourney) {

		String fileName = (String) context.get(FILE_NAME);
		String serviceJourneyId = serviceJourney.getId();

		log.error("Empty passing times in sequence in file :" + fileName + " , serviceJourney:" + serviceJourneyId);

		if ( context.get(ANALYSIS_REPORT) == null){
			return ;
		}

		AnalyzeReport analyzeReport = (AnalyzeReport) context.get(ANALYSIS_REPORT);
		analyzeReport.addEmptyPassingTimes(fileName, serviceJourneyId);
	}


	/**
	 * Read the journeyPattern to search a stopPoint using its position
	 * @param journeyPattern
	 * 			journeyPattern to read
	 * @param index
	 * 			index of the stopPoint to recover
	 * @return
	 * 			The stopPoint
	 */
	private StopPoint getStopPointFromJourneyPattern(JourneyPattern journeyPattern, int index){

		return journeyPattern.getStopPoints().stream()
										     .filter(stopPoint -> stopPoint.getPosition().equals(index))
				                             .findFirst()
										     .orElseThrow(() -> new RuntimeException("Unable to find stopPoint with position:" + index  + " in journeyPattern:" + journeyPattern.getObjectId() + " in line : " + journeyPattern.getRoute().getLine().getObjectId()));
	}

	// TODO add support for other time zones and zone offsets, for now only handling UTC
	private void parsePassingTimes(TimetabledPassingTime timetabledPassingTime, VehicleJourneyAtStop vehicleJourneyAtStop) {

		NetexTimeConversionUtil.parsePassingTime(timetabledPassingTime, false, vehicleJourneyAtStop);
		NetexTimeConversionUtil.parsePassingTime(timetabledPassingTime, true, vehicleJourneyAtStop);

		// TODO copying missing data since Chouette pt does not properly support missing values
		if (vehicleJourneyAtStop.getArrivalTime() == null && vehicleJourneyAtStop.getDepartureTime() != null) {
			vehicleJourneyAtStop.setArrivalTime(vehicleJourneyAtStop.getDepartureTime());
			vehicleJourneyAtStop.setArrivalDayOffset(vehicleJourneyAtStop.getDepartureDayOffset());
		} else if (vehicleJourneyAtStop.getArrivalTime() != null && vehicleJourneyAtStop.getDepartureTime() == null) {
			vehicleJourneyAtStop.setDepartureTime(vehicleJourneyAtStop.getArrivalTime());
			vehicleJourneyAtStop.setDepartureDayOffset(vehicleJourneyAtStop.getArrivalDayOffset());
		}

	}

	static {
		ParserFactory.register(ServiceJourneyParser.class.getName(), new ParserFactory() {
			private ServiceJourneyParser instance = new ServiceJourneyParser();

			@Override
			protected Parser create() {
				return instance;
			}
		});
	}

}
