package mobi.chouette.exchange.netexprofile.exporter.producer;

import mobi.chouette.model.VehicleJourneyAtStop;
import mobi.chouette.model.type.BoardingAlightingPossibilityEnum;
import org.rutebanken.netex.model.BookingArrangementsStructure;
import org.rutebanken.netex.model.BookingMethodEnumeration;
import org.rutebanken.netex.model.RequestMethodTypeEnumeration;
import org.rutebanken.netex.model.StopPointInJourneyPattern;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.*;

public class ServiceJourneyPatternFranceProducerTest {

    private ServiceJourneyPatternFranceProducer producer;

    @BeforeTest
    public void setUp() {
        producer = new ServiceJourneyPatternFranceProducer();
    }

    @Test
    void updateBoardingAlighting_multiple_information_test() {
        StopPointInJourneyPattern stopPointInJourneyPattern = new StopPointInJourneyPattern();
        VehicleJourneyAtStop vehicleJourneyAtStop1 = new VehicleJourneyAtStop();
        vehicleJourneyAtStop1.setBoardingAlightingPossibility(BoardingAlightingPossibilityEnum.BoardAndAlight);
        VehicleJourneyAtStop vehicleJourneyAtStop2 = new VehicleJourneyAtStop();
        vehicleJourneyAtStop2.setBoardingAlightingPossibility(BoardingAlightingPossibilityEnum.BoardAndAlightOnRequest);

        List<VehicleJourneyAtStop> inputVehicleJourneysAtStop = new ArrayList<>();
        inputVehicleJourneysAtStop.add(vehicleJourneyAtStop1);
        inputVehicleJourneysAtStop.add(vehicleJourneyAtStop2);

        producer.updateBoardingAlighting(inputVehicleJourneysAtStop, stopPointInJourneyPattern);

        assertEquals(stopPointInJourneyPattern.isForAlighting(), Boolean.TRUE);
        assertEquals(stopPointInJourneyPattern.isForBoarding(), Boolean.TRUE);
        assertEquals(stopPointInJourneyPattern.isRequestStop(), Boolean.FALSE);
        assertNull(stopPointInJourneyPattern.getRequestMethod());
        BookingArrangementsStructure bookingArrangements = stopPointInJourneyPattern.getBookingArrangements();
        assertNull(bookingArrangements);
    }

    @Test
    void updateBoardingAlighting_partialBoardingAlighting_information_test() {
        StopPointInJourneyPattern stopPointInJourneyPattern = new StopPointInJourneyPattern();
        VehicleJourneyAtStop vehicleJourneyAtStop1 = new VehicleJourneyAtStop();
        vehicleJourneyAtStop1.setBoardingAlightingPossibility(BoardingAlightingPossibilityEnum.BoardAndAlightOnRequest);
        VehicleJourneyAtStop vehicleJourneyAtStop2 = new VehicleJourneyAtStop();

        List<VehicleJourneyAtStop> inputVehicleJourneysAtStop = new ArrayList<>();
        inputVehicleJourneysAtStop.add(vehicleJourneyAtStop1);
        inputVehicleJourneysAtStop.add(vehicleJourneyAtStop2);

        producer.updateBoardingAlighting(inputVehicleJourneysAtStop, stopPointInJourneyPattern);

        assertEquals(stopPointInJourneyPattern.isForAlighting(), Boolean.TRUE);
        assertEquals(stopPointInJourneyPattern.isForBoarding(), Boolean.TRUE);
        assertEquals(stopPointInJourneyPattern.isRequestStop(), Boolean.TRUE);
        assertEquals(stopPointInJourneyPattern.getRequestMethod(), RequestMethodTypeEnumeration.PHONE_CALL);
        BookingArrangementsStructure bookingArrangements = stopPointInJourneyPattern.getBookingArrangements();
        assertNotNull(bookingArrangements);
        assertNotNull(bookingArrangements.getBookingMethods());
        assertEquals(bookingArrangements.getBookingMethods().size(), 1);
        assertEquals(bookingArrangements.getBookingMethods().get(0), BookingMethodEnumeration.CALL_OFFICE);
    }

}