package mobi.chouette.exchange.neptune.exporter.producer;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.List;

import mobi.chouette.common.TimeUtil;
import mobi.chouette.exchange.neptune.JsonExtension;
import mobi.chouette.model.Footnote;
import mobi.chouette.model.VehicleJourney;
import mobi.chouette.model.VehicleJourneyAtStop;
import mobi.chouette.model.type.BoardingAlightingPossibilityEnum;
import mobi.chouette.model.type.DropOffTypeEnum;
import mobi.chouette.model.type.PickUpTypeEnum;
import mobi.chouette.model.type.TransportModeNameEnum;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.joda.time.LocalTime;
import org.trident.schema.trident.BoardingAlightingPossibilityType;
import org.trident.schema.trident.TransportModeNameType;
import org.trident.schema.trident.VehicleJourneyAtStopType;
import org.trident.schema.trident.VehicleJourneyType;

public class VehicleJourneyProducer extends AbstractJaxbNeptuneProducer<VehicleJourneyType, VehicleJourney> implements
		JsonExtension {

	private static Comparator<VehicleJourneyAtStop> VEHICLE_JOURNEY_AT_STOP_SORTER = Comparator.comparingInt(o -> o.getStopPoint().getPosition());
	
	// @Override
	public VehicleJourneyType produce(VehicleJourney vehicleJourney, boolean addExtension, String timetableObjectId) {
		return produce(vehicleJourney, addExtension, 0, timetableObjectId);
	}

	public VehicleJourneyType produce(VehicleJourney vehicleJourney, boolean addExtension, int count, String timetableObjectId) {
		VehicleJourneyType jaxbVehicleJourney = tridentFactory.createVehicleJourneyType();

		//
		populateFromModel(jaxbVehicleJourney, vehicleJourney);

		if(timetableObjectId != null){
			String[] timetableObjectIdSplit = timetableObjectId.split(":");
			jaxbVehicleJourney.setObjectId(jaxbVehicleJourney.getObjectId() + "-" + timetableObjectIdSplit[2]);
		}

		if (count > 0)
			jaxbVehicleJourney.setObjectId(jaxbVehicleJourney.getObjectId()+"-"+count);

		jaxbVehicleJourney.setComment(buildComment(vehicleJourney, addExtension));

		jaxbVehicleJourney.setFacility(getNotEmptyString(vehicleJourney.getFacility()));
		jaxbVehicleJourney.setJourneyPatternId(getNonEmptyObjectId(vehicleJourney.getJourneyPattern()));
		if (vehicleJourney.getNumber() != null)
			jaxbVehicleJourney.setNumber(BigInteger.valueOf(vehicleJourney.getNumber()));
		jaxbVehicleJourney.setOperatorId(getNonEmptyObjectId(vehicleJourney.getCompany()));
		jaxbVehicleJourney.setPublishedJourneyIdentifier(getNotEmptyString(vehicleJourney
				.getPublishedJourneyIdentifier()));
		jaxbVehicleJourney.setPublishedJourneyName(getNotEmptyString(vehicleJourney.getPublishedJourneyName()));
		jaxbVehicleJourney.setRouteId(getNonEmptyObjectId(vehicleJourney.getRoute()));

		if (vehicleJourney.getTransportMode() != null) {
			TransportModeNameEnum transportMode = vehicleJourney.getTransportMode();
			try {
				jaxbVehicleJourney.setTransportMode(TransportModeNameType.fromValue(transportMode.name()));
			} catch (IllegalArgumentException e) {
				// TODO generate report
			}
		}
		jaxbVehicleJourney.setVehicleTypeIdentifier(vehicleJourney.getVehicleTypeIdentifier());

		if (vehicleJourney.getVehicleJourneyAtStops() != null) {
			List<VehicleJourneyAtStop> lvjas = vehicleJourney.getVehicleJourneyAtStops();
			lvjas.sort(VEHICLE_JOURNEY_AT_STOP_SORTER);
			int order = 1;
			LocalTime firstDeparture = null;
			for (VehicleJourneyAtStop vehicleJourneyAtStop : vehicleJourney.getVehicleJourneyAtStops()) {
				if (vehicleJourneyAtStop != null) {
					VehicleJourneyAtStopType jaxbVehicleJourneyAtStop = tridentFactory.createVehicleJourneyAtStopType();
					jaxbVehicleJourneyAtStop.setBoardingAlightingPossibility(buildBoardingAndAlightingPossibility(vehicleJourneyAtStop));
					jaxbVehicleJourneyAtStop.setOrder(BigInteger.valueOf(order++));
					jaxbVehicleJourneyAtStop.setStopPointId(getNonEmptyObjectId(vehicleJourneyAtStop.getStopPoint()));
					jaxbVehicleJourneyAtStop.setVehicleJourneyId(jaxbVehicleJourney.getObjectId());
					switch(vehicleJourney.getJourneyCategory()) {
					case Timesheet:
						if (vehicleJourneyAtStop.getArrivalTime() != null) {
							jaxbVehicleJourneyAtStop.setArrivalTime(toCalendar(vehicleJourneyAtStop.getArrivalTime()));
						}
						if (vehicleJourneyAtStop.getDepartureTime() != null) {
							jaxbVehicleJourneyAtStop.setDepartureTime(toCalendar(vehicleJourneyAtStop.getDepartureTime()));
						}
						break;
					case Frequency:
						if (firstDeparture == null)
							firstDeparture = vehicleJourneyAtStop.getDepartureTime();
						
						jaxbVehicleJourneyAtStop.setElapseDuration(toDuration(TimeUtil.subtract(vehicleJourneyAtStop.getDepartureTime(), firstDeparture)));
						jaxbVehicleJourneyAtStop.setHeadwayFrequency(toDuration(vehicleJourney.getJourneyFrequencies().get(count).getScheduledHeadwayInterval()));
					}
					jaxbVehicleJourney.getVehicleJourneyAtStop().add(jaxbVehicleJourneyAtStop);
				}
			}
		}
		return jaxbVehicleJourney;
	}

	protected String buildComment(VehicleJourney vj, boolean addExtension) {
		if (!addExtension)
			return getNotEmptyString(vj.getComment());
		try {
			JSONObject jsonComment = new JSONObject();
			if (!isEmpty(vj.getFootnotes())) {
				JSONArray noteRefs = new JSONArray();
				for (Footnote footNote : vj.getFootnotes()) {
					noteRefs.put(footNote.getKey());
				}
				jsonComment.put(FOOTNOTE_REFS, noteRefs);
			}
			if (vj.getFlexibleService() != null) {
				jsonComment.put(FLEXIBLE_SERVICE, vj.getFlexibleService());
			}
			if (vj.getMobilityRestrictedSuitability() != null) {
				jsonComment.put(MOBILITY_RESTRICTION, vj.getMobilityRestrictedSuitability());
			}

			if (jsonComment.length() == 0) {
				return getNotEmptyString(vj.getComment());
			} else {
				if (!isEmpty(vj.getComment())) {
					jsonComment.put(COMMENT, vj.getComment().trim());
				}
			}
			return jsonComment.toString();
		} catch (Exception e) {
			return getNotEmptyString(vj.getComment());
		}
	}

	protected BoardingAlightingPossibilityType buildBoardingAndAlightingPossibility(VehicleJourneyAtStop vehicleJourneyAtStop) {
		if (vehicleJourneyAtStop.getBoardingAlightingPossibility() == null)
			return null;

		BoardingAlightingPossibilityEnum boardingAlightingPossibility = vehicleJourneyAtStop.getBoardingAlightingPossibility();

		DropOffTypeEnum dropOffType = boardingAlightingPossibility.getDropOffType();
		PickUpTypeEnum pickUpType = boardingAlightingPossibility.getPickUpType();

		if(dropOffType.equals(DropOffTypeEnum.Scheduled) && pickUpType.equals(PickUpTypeEnum.Scheduled)){
			return BoardingAlightingPossibilityType.BOARD_AND_ALIGHT;
		}

		if(dropOffType.equals(DropOffTypeEnum.Scheduled) && pickUpType.equals(PickUpTypeEnum.NoAvailable)){
			return BoardingAlightingPossibilityType.ALIGHT_ONLY;
		}

		if(dropOffType.equals(DropOffTypeEnum.NoAvailable) && pickUpType.equals(PickUpTypeEnum.Scheduled)){
			return BoardingAlightingPossibilityType.BOARD_ONLY;
		}

		if(dropOffType.equals(DropOffTypeEnum.NoAvailable) && pickUpType.equals(PickUpTypeEnum.NoAvailable)){
			return BoardingAlightingPossibilityType.NEITHER_BOARD_OR_ALIGHT;
		}

		boolean pickUpOnRequest = pickUpType.equals(PickUpTypeEnum.AgencyCall) || pickUpType.equals(PickUpTypeEnum.DriverCall);
		boolean dropOffOnRequest = dropOffType.equals(DropOffTypeEnum.AgencyCall) || dropOffType.equals(DropOffTypeEnum.DriverCall);
		if(dropOffOnRequest && pickUpOnRequest){
			return BoardingAlightingPossibilityType.BOARD_AND_ALIGHT_ON_REQUEST;
		}

		if(dropOffOnRequest){
			return BoardingAlightingPossibilityType.ALIGHT_ON_REQUEST;
		}

		if(pickUpOnRequest){
			return BoardingAlightingPossibilityType.BOARD_ON_REQUEST;
		}

		return null;

	}

}
