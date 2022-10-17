/**
 * Projet CHOUETTE
 *
 * ce projet est sous license libre
 * voir LICENSE.txt pour plus de details
 *
 */

package mobi.chouette.exchange.gtfs.exporter.producer;

import lombok.extern.log4j.Log4j;
import mobi.chouette.exchange.gtfs.exporter.GtfsStopUtils;
import mobi.chouette.exchange.gtfs.model.GtfsFrequency;
import mobi.chouette.exchange.gtfs.model.GtfsShape;
import mobi.chouette.exchange.gtfs.model.GtfsStopTime;
import mobi.chouette.exchange.gtfs.model.GtfsTime;
import mobi.chouette.exchange.gtfs.model.GtfsTrip;
import mobi.chouette.exchange.gtfs.model.exporter.GtfsExporterInterface;
import mobi.chouette.exchange.gtfs.parameters.IdParameters;
import mobi.chouette.model.DestinationDisplay;
import mobi.chouette.model.JourneyFrequency;
import mobi.chouette.model.JourneyPattern;
import mobi.chouette.model.Line;
import mobi.chouette.model.Route;
import mobi.chouette.model.RouteSection;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.VehicleJourney;
import mobi.chouette.model.VehicleJourneyAtStop;
import mobi.chouette.model.type.AlightingPossibilityEnum;
import mobi.chouette.model.type.BoardingPossibilityEnum;
import mobi.chouette.model.type.DropOffTypeEnum;
import mobi.chouette.model.type.JourneyCategoryEnum;
import mobi.chouette.model.type.PTDirectionEnum;
import mobi.chouette.model.type.PickUpTypeEnum;
import mobi.chouette.model.type.SectionStatusEnum;
import java.time.LocalTime;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * produce Trips and stop_times for vehicleJourney
 * <p>
 * when vehicleJourney is on multiple timetables, it will be cloned for each
 *
 * @ TODO : refactor to produce one calendar for each timetable groups
 */
@Log4j
public class GtfsTripProducer extends AbstractProducer {

	GtfsTrip trip = new GtfsTrip();
	GtfsStopTime time = new GtfsStopTime();
	GtfsFrequency frequency = new GtfsFrequency();
	GtfsShape shape = new GtfsShape();

	public GtfsTripProducer(GtfsExporterInterface exporter) {
		super(exporter);
	}

	/**
	 * produce stoptimes for vehiclejourneyatstops @ TODO see how to manage ITL
	 *
	 * @param vj
	 * @param prefix
	 * @param keepOriginalId
	 * @param changesDestinationDisplay
	 * @param lvjas
	 * @pram idParams
	 * @return list of stoptimes
	 */
	private boolean saveTimes(VehicleJourney vj, String prefix, boolean keepOriginalId, boolean changesDestinationDisplay, List<VehicleJourneyAtStop> lvjas, IdParameters idParams) {
		if (vj.getVehicleJourneyAtStops().isEmpty())
			return false;
		Line l = vj.getRoute().getLine();

		/**
		 * GJT : Attributes used to handle times after midnight
		 */
		int departureOffset = 0;
		int arrivalOffset = 0;

		String tripId = toGtfsId(vj.getObjectId(), prefix, keepOriginalId);
		time.setTripId(tripId);
		float distance = (float) 0.0;
		List<RouteSection> routeSections = vj.getJourneyPattern().getRouteSections();
		int index = 0;
		for (VehicleJourneyAtStop vjas : lvjas) {
			StopArea stopArea = vjas.getStopPoint().getScheduledStopPoint().getContainedInStopAreaRef().getObject();
			if (stopArea != null) {
				time.setStopId(GtfsStopUtils.getNewStopId(stopArea, idParams, keepOriginalId, prefix));
			}

			LocalTime arrival = vjas.getArrivalTime();
			arrivalOffset = vjas.getArrivalDayOffset(); /** GJT */

			if (arrival == null) {
				arrival = vjas.getDepartureTime();
				arrivalOffset = vjas.getDepartureDayOffset(); /** GJT */
			}
			time.setArrivalTime(new GtfsTime(arrival, arrivalOffset)); /** GJT */

			LocalTime departure = vjas.getDepartureTime();
			departureOffset = vjas.getDepartureDayOffset(); /** GJT */
			if (departure == null) {
				departure = vjas.getArrivalTime();
				departureOffset = vjas.getArrivalDayOffset(); /** GJT */
			}
			time.setDepartureTime(new GtfsTime(departure, departureOffset)); /** GJT */

			time.setStopSequence(vjas.getStopPoint().getPosition());

			if(changesDestinationDisplay && vjas.getStopPoint().getDestinationDisplay() != null) {
				String stopHeadSign = vjas.getStopPoint().getDestinationDisplay().getFrontTextWithComputedVias();
				if(trip.getTripHeadSign() != null) {
					// Skip if equal to tripHeadSign
					if(!trip.getTripHeadSign().equals(stopHeadSign)) {
						time.setStopHeadsign(stopHeadSign);
					}
				} else {
					// Always set if tripheadSign is null
					time.setStopHeadsign(stopHeadSign);
				}
			}
			addDropOffAndPickUpType(time, l, vj, vjas);

			if (vj.getJourneyPattern().hasCompleteValidRouteSections()) {
				Float shapeDistTraveled = Float.valueOf((float) distance);
				time.setShapeDistTraveled(shapeDistTraveled);
				while (index < routeSections.size() && routeSections.get(index) == null) {
					index++;
				}
				if (index < routeSections.size()) {
					distance += computeDistance(routeSections.get(index));
				}
				index++;
			}
			else
			{
			   time.setShapeDistTraveled(null);
			}

			try {
				getExporter().getStopTimeExporter().export(time);
			} catch (Exception e) {
		          log.error("fail to produce stoptime "+e.getClass().getName()+" "+e.getMessage());
				return false;
			}

		}
		return true;
	}

	private double computeDistance(RouteSection section)
	{
		if (isTrue(section.getNoProcessing()) || section.getProcessedGeometry() == null)
		{
			double distance = section.getInputGeometry().getLength();
			distance *= (Math.PI / 180) * 6378137;
			return distance;
		}
		else
		{
			double distance = section.getProcessedGeometry().getLength();
			distance *= (Math.PI / 180) * 6378137;
			return distance;
		}
	}

	private void addDropOffAndPickUpType(GtfsStopTime time, Line l, VehicleJourney vj, VehicleJourneyAtStop vjas) {

		time.setPickupType(null);
		time.setDropOffType(null);
		boolean routeOnDemand = isTrue(l.getFlexibleService());
		boolean tripOnDemand;
		if (routeOnDemand) {
			// line is on demand, check if trip is not explicitly regular
			tripOnDemand = vj.getFlexibleService() == null || vj.getFlexibleService();
		} else {
			// line is regular or undefined , check if trip is explicitly on
			// demand
			tripOnDemand = isTrue(vj.getFlexibleService());
		}
		if (tripOnDemand) {
			time.setPickupType(PickUpTypeEnum.AgencyCall);
			time.setDropOffType(DropOffTypeEnum.AgencyCall);
		} else if (routeOnDemand) {
			time.setPickupType(PickUpTypeEnum.Scheduled);
			time.setDropOffType(DropOffTypeEnum.Scheduled);
		}

		if(vjas.getBoardingAlightingPossibility() != null){
			time.setPickupType(vjas.getBoardingAlightingPossibility().getPickUpType());
			time.setDropOffType(vjas.getBoardingAlightingPossibility().getDropOffType());
		}

	}

	private DropOffTypeEnum toDropOffType(AlightingPossibilityEnum forAlighting, DropOffTypeEnum defaultValue) {
		if(forAlighting == null) {
			// If not set on StopPoint return defaultValue (that is, the previous value) or if not set; Scheduled
			return defaultValue == null ? DropOffTypeEnum.Scheduled : defaultValue;
		}

		switch (forAlighting) {
		case normal:
			return DropOffTypeEnum.Scheduled;
		case forbidden:
			return DropOffTypeEnum.NoAvailable;
		case is_flexible:
			return DropOffTypeEnum.AgencyCall;
		case request_stop:
			return DropOffTypeEnum.DriverCall;
		}
		return defaultValue;
	}

	private PickUpTypeEnum toPickUpType(BoardingPossibilityEnum forBoarding, PickUpTypeEnum defaultValue) {
		if(forBoarding == null) {
			// If not set on StopPoint return defaultValue (that is, the previous value) or if not set; Scheduled
			return defaultValue == null ? PickUpTypeEnum.Scheduled : defaultValue;
		}

		switch (forBoarding) {
		case normal:
			return PickUpTypeEnum.Scheduled;
		case forbidden:
			return PickUpTypeEnum.NoAvailable;
		case is_flexible:
			return PickUpTypeEnum.AgencyCall;
		case request_stop:
			return PickUpTypeEnum.DriverCall;
		}
		return defaultValue;
	}






	/**
	 * convert vehicle journey to trip for a specific timetable
	 *
	 * @param vj
	 *            vehicle journey
	 * @param serviceId
	 * @param schemaPrefix
	 * 			Prefix of the database schema
	 * @param keepOriginalId
	 * @param idParams
	 * 			Parameters about IDs:
	 * 		    -	Format for Ids : TRIDENT (e.g: PREFIX:StopPlace:10545) or identical to source (e.g:10545)
	 * 		    -   prefix
	 * 		    - suffix
	 * @return gtfs trip
	 */
	public boolean save(VehicleJourney vj, String serviceId, String schemaPrefix, boolean keepOriginalId, IdParameters idParams) {

		time.setStopHeadsign(null); // Clear between each journey

		String tripId = toGtfsId(vj.getObjectId(), schemaPrefix, keepOriginalId);

		trip.setTripId(tripId);

		JourneyPattern jp = vj.getJourneyPattern();
		if (jp.getSectionStatus() == SectionStatusEnum.Completed && jp.hasCompleteValidRouteSections()) {
			String shapeId = toGtfsId(jp.getObjectId(), schemaPrefix, keepOriginalId);
			trip.setShapeId(shapeId);
		}
		else
		{
			trip.setShapeId(null);
		}
		Route route = vj.getRoute();
		Line line = route.getLine();
		trip.setRouteId(generateCustomRouteId(toGtfsId(line.getObjectId(), schemaPrefix, keepOriginalId),idParams));
		if ("R".equals(route.getWayBack()) || PTDirectionEnum.R.equals(route.getDirection())) {
			trip.setDirectionId(GtfsTrip.DirectionType.Inbound);
		} else {
			trip.setDirectionId(GtfsTrip.DirectionType.Outbound);
		}

		trip.setServiceId(serviceId);

		if (vj.getNumber() != null && !vj.getNumber().equals(0L)) {
			trip.setTripShortName(vj.getNumber().toString());
		}
		else {
			trip.setTripShortName(null);
		}
		List<VehicleJourneyAtStop> lvjas = new ArrayList<>(vj.getVehicleJourneyAtStops());
		lvjas.sort(Comparator.comparing(o -> o.getStopPoint().getPosition()));

		List<DestinationDisplay> allDestinationDisplays = new ArrayList<>();
		for(VehicleJourneyAtStop vjas : lvjas) {
			if(vjas.getStopPoint().getDestinationDisplay() != null) {
				allDestinationDisplays.add(vjas.getStopPoint().getDestinationDisplay());
			}
		}
		DestinationDisplay startDestinationDisplay = lvjas.get(0).getStopPoint().getDestinationDisplay();
		boolean changesDestinationDisplay = allDestinationDisplays.size() > 1;

		if(!isEmpty(vj.getPublishedJourneyName())) {
			trip.setTripHeadSign(vj.getPublishedJourneyName());
		} else if(startDestinationDisplay != null) {
			trip.setTripHeadSign(startDestinationDisplay.getFrontTextWithComputedVias());
		} else if (!isEmpty(jp.getPublishedName())) {
			trip.setTripHeadSign(jp.getPublishedName());
		} else
			trip.setTripHeadSign(null);

		if (vj.getMobilityRestrictedSuitability() != null)
			trip.setWheelchairAccessible(vj.getMobilityRestrictedSuitability() ? GtfsTrip.WheelchairAccessibleType.Allowed
					: GtfsTrip.WheelchairAccessibleType.NoAllowed);
		else
			trip.setWheelchairAccessible(GtfsTrip.WheelchairAccessibleType.NoInformation);

		if (vj.getBikesAllowed() != null)
			trip.setBikesAllowed(vj.getBikesAllowed() ? GtfsTrip.BikesAllowedType.Allowed
					: GtfsTrip.BikesAllowedType.NoAllowed);
		else
			trip.setBikesAllowed(GtfsTrip.BikesAllowedType.NoInformation);

		// add StopTimes
		if (saveTimes(vj, schemaPrefix, keepOriginalId, changesDestinationDisplay, lvjas, idParams)) {
			try {
				getExporter().getTripExporter().export(trip);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
				return false;
			}
		}

		// add frequencies
		if (JourneyCategoryEnum.Frequency == vj.getJourneyCategory()) {
			for (JourneyFrequency journeyFrequency : vj.getJourneyFrequencies()) { // Don't care about Timebands !
				frequency.setTripId(tripId);
				frequency.setExactTimes(journeyFrequency.getExactTime());
				frequency.setStartTime(new GtfsTime(journeyFrequency.getFirstDepartureTime(), 0));
				if (!journeyFrequency.getFirstDepartureTime().isAfter(journeyFrequency.getLastDepartureTime()))
					frequency.setEndTime(new GtfsTime(journeyFrequency.getLastDepartureTime(), 0));
				else
					frequency.setEndTime(new GtfsTime(journeyFrequency.getLastDepartureTime(), 1));
				frequency.setHeadwaySecs((int) journeyFrequency.getScheduledHeadwayInterval().getSeconds());
				try {
					getExporter().getFrequencyExporter().export(frequency);
				} catch (Exception e) {
					log.error(e.getMessage(), e);
					return false;
				}
			}
		}

		return true;
	}

}
