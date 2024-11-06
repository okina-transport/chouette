package mobi.chouette.exchange.netexprofile.exporter.producer.facilities;

import mobi.chouette.model.type.FacilityTypeEnum;

public class FacilityProducerFactory {
    private FacilityProducer instance;

    private FacilityProducerFactory(String objectType, String enumKey) {
        if (FacilityTypeEnum.SERVICE_FACILITY_SET.name().equals(objectType)) {
            switch (enumKey) {
                case "AccessibilityInfoFacilityEnumeration":
                    instance = new AccessibilityInfoFacility();
                    break;
                case "AccessibilityToolEnumeration":
                    instance = new AccessibilityTool();
                    break;
                case "AccommodationAccessEnumeration":
                    instance = new AccommodationAccess();
                    break;
                case "AccommodationFacilityEnumeration":
                    instance = new AccommodationFacility();
                    break;
                case "AssistanceFacilityEnumeration":
                    instance = new AssistanceFacility();
                    break;
                case "BoardingPermissionEnumeration":
                    instance = new BoardingPermission();
                    break;
                case "BookingProcessEnumeration":
                    instance = new BookingProcess();
                    break;
                case "CarServiceFacilityEnumeration":
                    instance = new CarServiceFacility();
                    break;
                case "CateringFacilityEnumeration":
                    instance = new CateringFacility();
                    break;
                case "CouchetteFacilityEnumeration":
                    instance = new CouchetteFacility();
                    break;
                case "FamilyFacilityEnumeration":
                    instance = new FamilyFacility();
                    break;
                case "FareClassEnumeration":
                    instance = new FareClass();
                    break;
                case "GenderLimitationEnumeration":
                    instance = new GenderLimitation();
                    break;
                case "GroupBookingEnumeration":
                    instance = new GroupBooking();
                    break;
                case "LuggageCarriageEnumeration":
                    instance = new LuggageCarriage();
                    break;
                case "MealFacilityEnumeration":
                    instance = new MealFacility();
                    break;
                case "MedicalFacilityEnumeration":
                    instance = new MedicalFacility();
                    break;
                case "MobilityFacilityEnumeration":
                    instance = new MobilityFacility();
                    break;
                case "NuisanceFacilityEnumeration":
                    instance = new NuisanceFacility();
                    break;
                case "PassengerCommsFacilityEnumeration":
                    instance = new PassengerCommsFacility();
                    break;
                case "PassengerInformationEquipmentEnumeration":
                    instance = new PassengerInformationEquipment();
                    break;
                case "PassengerInformationFacilityEnumeration":
                    instance = new PassengerInformationFacility();
                    break;
                case "RetailFacilityEnumeration":
                    instance = new RetailFacility();
                    break;
                case "SafetyFacilityEnumeration":
                    instance = new SafetyFacility();
                    break;
                case "SanitaryFacilityEnumeration":
                    instance = new SanitaryFacility();
                    break;
                case "ReservationEnumeration":
                    instance = new ServiceReservation();
                    break;
                case "TicketingFacilityEnumeration":
                    instance = new TicketingFacility();
                    break;
                case "TicketingServiceFacilityEnumeration":
                    instance = new TicketingServiceFacility();
                    break;
                case "UicProductCharacteristicEnumeration":
                    instance = new UicProductCharacteristic();
                    break;
                case "UicRateTypeEnumeration":
                    instance = new UicRateType();
                    break;
                case "VehicleAccessFacilityEnumeration":
                    instance = new VehicleAccessFacility();
                    break;
                default:
                    throw new IllegalArgumentException("Unknown facility producer key: " + enumKey);
            }
        } else if (objectType.startsWith(FacilityTypeEnum.ACCOMMODATION.name())) {
            String id = objectType.substring(FacilityTypeEnum.ACCOMMODATION.name().length()+1);
            switch (enumKey) {
                case "AccommodationFacilityEnumeration":
                    instance = new AccommodationItemFacility(id);
                    break;
                case "CouchetteFacilityEnumeration":
                    instance = new AccommodationItemCouchette(id);
                    break;
                case "GenderLimitationEnumeration":
                    instance = new AccommodationItemGender(id);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown facility producer key: " + enumKey);
            }
        } else if (objectType.startsWith(FacilityTypeEnum.ONBOARD_STAY.name())) {
            String id = objectType.substring(FacilityTypeEnum.ONBOARD_STAY.name().length()+1);
            instance = new OnboardItemFacility(id);
        }

    }


    public static FacilityProducerFactory init(String objectType, String enumKey) {
        return new FacilityProducerFactory(objectType, enumKey);
    }

    public FacilityProducer build() {
        return instance;
    }
}
