package mobi.chouette.model.type;

import lombok.Getter;

@Getter
public enum VehicleJourneyFacilityEnum {
    ACCESSIBILITY_INFO_FACILITY_ENUMERATION("AccessibilityInfoFacilityEnumeration"),
    ACCESSIBILITY_TOOL_ENUMERATION("AccessibilityToolEnumeration"),
    ACCOMMODATION_ACCESS_ENUMERATION("AccommodationAccessEnumeration"),
    ACCOMMODATION_FACILITY_ENUMERATION("AccommodationFacilityEnumeration"),
    ASSISTANCE_FACILITY_ENUMERATION("AssistanceFacilityEnumeration"),
    BOARDING_PERMISSION_ENUMERATION("BoardingPermissionEnumeration"),
    BOOKING_PROCESS_ENUMERATION("BookingProcessEnumeration"),
    CAR_SERVICE_FACILITY_ENUMERATION("CarServiceFacilityEnumeration"),
    CATERING_FACILITY_ENUMERATION("CateringFacilityEnumeration"),
    COUCHETTE_FACILITY_ENUMERATION("CouchetteFacilityEnumeration"),
    FAMILY_FACILITY_ENUMERATION("FamilyFacilityEnumeration"),
    FARE_CLASS_ENUMERATION("FareClassEnumeration"),
    GENDER_LIMITATION_ENUMERATION("GenderLimitationEnumeration"),
    GROUP_BOOKING_ENUMERATION("GroupBookingEnumeration"),
    LUGGAGE_CARRIAGE_ENUMERATION("LuggageCarriageEnumeration"),
    MEAL_FACILITY_ENUMERATION("MealFacilityEnumeration"),
    MEDICAL_FACILITY_ENUMERATION("MedicalFacilityEnumeration"),
    MOBILITY_FACILITY_ENUMERATION("MobilityFacilityEnumeration"),
    NUISANCE_FACILITY_ENUMERATION("NuisanceFacilityEnumeration"),
    PASSENGER_COMMS_FACILITY_ENUMERATION("PassengerCommsFacilityEnumeration"),
    PASSENGER_INFORMATION_EQUIPMENT_ENUMERATION("PassengerInformationEquipmentEnumeration"),
    PASSENGER_INFORMATION_FACILITY_ENUMERATION("PassengerInformationFacilityEnumeration"),
    RETAIL_FACILITY_ENUMERATION("RetailFacilityEnumeration"),
    SAFETY_FACILITY_ENUMERATION("SafetyFacilityEnumeration"),
    SANITARY_FACILITY_ENUMERATION("SanitaryFacilityEnumeration"),
    RESERVATION_ENUMERATION("ReservationEnumeration"),
    TICKETING_FACILITY_ENUMERATION("TicketingFacilityEnumeration"),
    TICKETING_SERVICE_FACILITY_ENUMERATION("TicketingServiceFacilityEnumeration"),
    UIC_PRODUCT_CHARACTERISTIC_ENUMERATION("UicProductCharacteristicEnumeration"),
    UIC_RATE_TYPE_ENUMERATION("UicRateTypeEnumeration"),
    VEHICLE_ACCESS_FACILITY_ENUMERATION("VehicleAccessFacilityEnumeration");

    VehicleJourneyFacilityEnum(String key) {
        this.key = key;
    }

    private final String key;
}