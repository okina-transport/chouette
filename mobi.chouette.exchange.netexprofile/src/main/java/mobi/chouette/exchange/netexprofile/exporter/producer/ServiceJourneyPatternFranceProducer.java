package mobi.chouette.exchange.netexprofile.exporter.producer;

import mobi.chouette.model.JourneyPattern;
import mobi.chouette.model.StopPoint;
import mobi.chouette.model.VehicleJourneyAtStop;
import mobi.chouette.model.type.BoardingAlightingPossibilityEnum;
import mobi.chouette.model.type.DropOffTypeEnum;
import mobi.chouette.model.type.PickUpTypeEnum;
import org.rutebanken.netex.model.BookingArrangementsStructure;
import org.rutebanken.netex.model.BookingMethodEnumeration;
import org.rutebanken.netex.model.DestinationDisplayRefStructure;
import org.rutebanken.netex.model.MultilingualString;
import org.rutebanken.netex.model.PointInLinkSequence_VersionedChildStructure;
import org.rutebanken.netex.model.PointsInJourneyPattern_RelStructure;
import org.rutebanken.netex.model.RequestMethodTypeEnumeration;
import org.rutebanken.netex.model.RouteRefStructure;
import org.rutebanken.netex.model.ScheduledStopPointRefStructure;
import org.rutebanken.netex.model.ServiceJourneyPatternTypeEnumeration;
import org.rutebanken.netex.model.StopPointInJourneyPattern;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class ServiceJourneyPatternFranceProducer extends NetexProducer {

    private static List<DropOffTypeEnum> requestDropOffTypes = Arrays.asList(DropOffTypeEnum.AgencyCall, DropOffTypeEnum.DriverCall);
    private static List<PickUpTypeEnum> requestPickUpTypes = Arrays.asList(PickUpTypeEnum.AgencyCall, PickUpTypeEnum.DriverCall);
    private static KeyListStructureProducer keyListStructureProducer = new KeyListStructureProducer();


    public org.rutebanken.netex.model.ServiceJourneyPattern produce(JourneyPattern journeyPattern) {
        org.rutebanken.netex.model.ServiceJourneyPattern netexServiceJourneyPattern = netexFactory.createServiceJourneyPattern();

        NetexProducerUtils.populateIdAndVersionIDFM(journeyPattern, netexServiceJourneyPattern);

        MultilingualString serviceJourneyPatternName = new MultilingualString();
        serviceJourneyPatternName.setValue(journeyPattern.getName());
        netexServiceJourneyPattern.setName(serviceJourneyPatternName);

        RouteRefStructure routeRefStructure = new RouteRefStructure();
        NetexProducerUtils.populateReferenceIDFM(journeyPattern.getRoute(), routeRefStructure);
        netexServiceJourneyPattern.setRouteRef(routeRefStructure);

        mobi.chouette.model.DestinationDisplay dd = null;

        if (journeyPattern.getDestinationDisplay() != null) {
            dd = journeyPattern.getDestinationDisplay();
        } else {
            for (StopPoint stopPoint : journeyPattern.getStopPoints()) {
                if (stopPoint != null) {
                    dd = stopPoint.getDestinationDisplay();
                }
            }
        }

        if (dd != null && dd.getFrontText() != null && !dd.getFrontText().isEmpty()) {
            DestinationDisplayRefStructure destinationDisplayRefStructure = new DestinationDisplayRefStructure();
            destinationDisplayRefStructure.setRef(dd.getObjectId());
            if(!destinationDisplayRefStructure.getRef().endsWith(":LOC")){
                destinationDisplayRefStructure.setRef(destinationDisplayRefStructure.getRef() + ":LOC");
            }
            destinationDisplayRefStructure.setVersion("any");
            netexServiceJourneyPattern.setDestinationDisplayRef(destinationDisplayRefStructure);
        }


        PointsInJourneyPattern_RelStructure pointsInJourneyPattern_relStructure = new PointsInJourneyPattern_RelStructure();
        Collection<PointInLinkSequence_VersionedChildStructure> pointInLinkSequence_versionedChildStructures = new ArrayList<>();

        for (StopPoint stopPoint : journeyPattern.getStopPoints()) {
            StopPointInJourneyPattern stopPointInJourneyPattern = new StopPointInJourneyPattern();
            NetexProducerUtils.populateIdAndVersionIDFM(stopPoint, stopPointInJourneyPattern);

            stopPointInJourneyPattern.setOrder(BigInteger.valueOf(stopPoint.getPosition() + 1));

            ScheduledStopPointRefStructure scheduledStopPointRefStructure = netexFactory.createScheduledStopPointRefStructure();
            NetexProducerUtils.populateReferenceIDFM(stopPoint.getScheduledStopPoint(), scheduledStopPointRefStructure);
            stopPointInJourneyPattern.setScheduledStopPointRef(netexFactory.createScheduledStopPointRef(scheduledStopPointRefStructure));

            pointInLinkSequence_versionedChildStructures.add(stopPointInJourneyPattern);

            // On récupère l'ensemble des vehicleJourneyAtStops
            List<VehicleJourneyAtStop> vehicleJourneyAtStops =
                    journeyPattern.getRoute().getJourneyPatterns()
                            .stream()
                            .flatMap(journeyPattern1 -> journeyPattern1.getVehicleJourneys()
                                    .stream()
                                    .flatMap(vehicleJourney -> vehicleJourney.getVehicleJourneyAtStops().stream()
                                            .filter(vehicleJourneyAtStop -> stopPoint.getObjectId().equals(vehicleJourneyAtStop.getStopPoint().getObjectId()))))
                            .collect(Collectors.toList());

            // On ne récupère que les vehicleJourneyAtStop qui sont liés à un même stoppoint et qui ont un boardingAlighting identique
            for (VehicleJourneyAtStop vehicleJourneyAtStop : vehicleJourneyAtStops) {
                boolean getVehicleJourneyAtStopWithBoardingAlighting = false;

                for (VehicleJourneyAtStop vehicleJourneyAtStop1 : vehicleJourneyAtStops) {
                    if (vehicleJourneyAtStop.getBoardingAlightingPossibility() != null && vehicleJourneyAtStop1.getBoardingAlightingPossibility() != null) {
                        getVehicleJourneyAtStopWithBoardingAlighting = vehicleJourneyAtStop.getBoardingAlightingPossibility().equals(vehicleJourneyAtStop1.getBoardingAlightingPossibility());
                    } else {
                        getVehicleJourneyAtStopWithBoardingAlighting = false;
                    }
                    if (!getVehicleJourneyAtStopWithBoardingAlighting)
                        break;

                }
                if (getVehicleJourneyAtStopWithBoardingAlighting && vehicleJourneyAtStop.getBoardingAlightingPossibility() != null) {
                    setBoardingAlighting(stopPointInJourneyPattern, vehicleJourneyAtStop);
                }
            }
        }

        pointsInJourneyPattern_relStructure.withPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern(pointInLinkSequence_versionedChildStructures);
        netexServiceJourneyPattern.setPointsInSequence(pointsInJourneyPattern_relStructure);

        netexServiceJourneyPattern.setServiceJourneyPatternType(ServiceJourneyPatternTypeEnumeration.PASSENGER);

        netexServiceJourneyPattern.setKeyList(keyListStructureProducer.produce(journeyPattern.getKeyValues()));

        return netexServiceJourneyPattern;
    }


    private void setBoardingAlighting(StopPointInJourneyPattern stopPointInJourneyPattern, VehicleJourneyAtStop vehicleJourneyAtStop) {


        BoardingAlightingPossibilityEnum boardingAlightingPossibility = vehicleJourneyAtStop.getBoardingAlightingPossibility();

        if (boardingAlightingPossibility == null)
            return;

        DropOffTypeEnum dropOffType = boardingAlightingPossibility.getDropOffType();
        PickUpTypeEnum pickUpType = boardingAlightingPossibility.getPickUpType();

        boolean canBoard = !pickUpType.equals(PickUpTypeEnum.NoAvailable);
        stopPointInJourneyPattern.setForBoarding(canBoard);

        boolean canAlight = !dropOffType.equals(DropOffTypeEnum.NoAvailable);
        stopPointInJourneyPattern.setForAlighting(canAlight);


        boolean requestStop = requestDropOffTypes.contains(dropOffType);
        boolean requestPickup = requestPickUpTypes.contains(pickUpType);

        stopPointInJourneyPattern.setRequestStop(requestStop);


        if (requestPickup || requestStop) {
            stopPointInJourneyPattern.setRequestMethod(RequestMethodTypeEnumeration.PHONE_CALL);
            BookingArrangementsStructure bookingArrangements = new BookingArrangementsStructure();

            BookingMethodEnumeration bookingMethod;

            if (dropOffType.equals(DropOffTypeEnum.DriverCall) && !pickUpType.equals(PickUpTypeEnum.AgencyCall)
                    || !dropOffType.equals(DropOffTypeEnum.AgencyCall) && pickUpType.equals(PickUpTypeEnum.DriverCall)) {
                bookingMethod = BookingMethodEnumeration.CALL_DRIVER;
            } else {
                bookingMethod = BookingMethodEnumeration.CALL_OFFICE;
            }

            bookingArrangements.withBookingMethods(bookingMethod);
            stopPointInJourneyPattern.setBookingArrangements(bookingArrangements);

        }
    }

}
