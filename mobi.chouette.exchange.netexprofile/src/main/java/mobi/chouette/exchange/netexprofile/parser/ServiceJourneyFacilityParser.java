package mobi.chouette.exchange.netexprofile.parser;

import mobi.chouette.common.Context;
import mobi.chouette.exchange.NetexParserUtils;
import mobi.chouette.exchange.netexprofile.importer.util.NetexImportUtil;
import mobi.chouette.model.KeyValue;
import mobi.chouette.model.VehicleJourney;
import mobi.chouette.model.VehicleJourneyFacility;
import mobi.chouette.model.type.FacilityTypeEnum;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.Referential;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.LocalDateTime;
import org.rutebanken.netex.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static mobi.chouette.model.constants.VehicleJourneyFacilityConstants.SERVICE_FACILITY_SET_OBJECT_ID_KEY;
import static mobi.chouette.model.type.FacilityTypeEnum.ACCOMMODATION;
import static mobi.chouette.model.type.FacilityTypeEnum.ONBOARD_STAY;
import static mobi.chouette.model.type.VehicleJourneyFacilityEnum.*;

public class ServiceJourneyFacilityParser {

    private static final String SEPARATOR = ";";
    private static final String ORIGINE = "MOBI-ITI";

    public void parseFacilities(Context context, Referential referential, ServiceJourney serviceJourney, VehicleJourney vehicleJourney) {
        ServiceFacilitySets_RelStructure facilities = serviceJourney.getFacilities();
        if (facilities != null) {
            List<Object> rawFacilities = facilities.getServiceFacilitySetRefOrServiceFacilitySet();
            if (CollectionUtils.isNotEmpty(rawFacilities)) {
                parseVehicleJourneyFacilities(context, referential, vehicleJourney, rawFacilities);
            }
        }
    }

    private void parseVehicleJourneyFacilities(Context context, Referential referential, VehicleJourney vehicleJourney, List<Object> rawFacilities) {
        for (Object rawFacility : rawFacilities) {
            if (rawFacility instanceof ServiceFacilitySet) {
                ServiceFacilitySet serviceFacilitySet = (ServiceFacilitySet) rawFacility;
                VehicleJourneyFacility vehicleJourneyFacilitySet = mapServiceFacilitySet(serviceFacilitySet);
                if (CollectionUtils.isNotEmpty(vehicleJourneyFacilitySet.getKeyValues())) {
                    VehicleJourneyFacility inputFacility = ObjectFactory.getVehicleJourneyFacility(referential, NetexImportUtil.composeObjectIdFromNetexId(context, SERVICE_FACILITY_SET_OBJECT_ID_KEY, serviceFacilitySet.getId()));

                    inputFacility.setCreatorId(ORIGINE);
                    inputFacility.setCreationTime(LocalDateTime.now());
                    inputFacility.setKeyValues(vehicleJourneyFacilitySet.getKeyValues());
                    inputFacility.setDescription(vehicleJourneyFacilitySet.getDescription());
                    inputFacility.setProvider(vehicleJourneyFacilitySet.getProvider());
                    inputFacility.setObjectVersion(NetexParserUtils.getVersion(((ServiceFacilitySet) rawFacility).getVersion()));
                    inputFacility.setObjectId(NetexImportUtil.composeObjectIdFromNetexId(context, SERVICE_FACILITY_SET_OBJECT_ID_KEY, serviceFacilitySet.getId()));
                    vehicleJourney.addVehicleJourneyFacility(inputFacility);
                }
            }
        }

    }

    private VehicleJourneyFacility mapServiceFacilitySet(ServiceFacilitySet rawFacility) {
        VehicleJourneyFacility vehicleJourneyFacility = new VehicleJourneyFacility();
        if (rawFacility.getDescription() != null && StringUtils.isNotBlank(rawFacility.getDescription().getValue())) {
            vehicleJourneyFacility.setDescription(rawFacility.getDescription().getValue());
        }
        if (rawFacility.getProvidedByRef() != null && StringUtils.isNotBlank(rawFacility.getProvidedByRef().getValue())) {
            vehicleJourneyFacility.setProvider(rawFacility.getProvidedByRef().getValue());
        }
        ArrayList<KeyValue> facilitiesKeyValue = new ArrayList<>();
        mapEnumListToKeyValue(ACCESSIBILITY_INFO_FACILITY_ENUMERATION.getKey(), facilitiesKeyValue, rawFacility.getAccessibilityInfoFacilityList());
        mapEnumListToKeyValue(ACCESSIBILITY_TOOL_ENUMERATION.getKey(), facilitiesKeyValue, rawFacility.getAccessibilityToolList());
        mapEnumListToKeyValue(ACCOMMODATION_ACCESS_ENUMERATION.getKey(), facilitiesKeyValue, rawFacility.getAccommodationAccessList());
        mapEnumListToKeyValue(ACCOMMODATION_FACILITY_ENUMERATION.getKey(), facilitiesKeyValue, rawFacility.getAccommodationFacilityList());
        mapEnumListToKeyValue(ASSISTANCE_FACILITY_ENUMERATION.getKey(), facilitiesKeyValue, rawFacility.getAssistanceFacilityList());
        mapEnumToKeyValue(FacilityTypeEnum.SERVICE_FACILITY_SET.name(),BOARDING_PERMISSION_ENUMERATION.getKey(), facilitiesKeyValue, rawFacility.getBoardingPermission());
        mapEnumListToKeyValue(BOOKING_PROCESS_ENUMERATION.getKey(), facilitiesKeyValue, rawFacility.getBookingProcessFacilityList());
        mapEnumListToKeyValue(CAR_SERVICE_FACILITY_ENUMERATION.getKey(), facilitiesKeyValue, rawFacility.getCarServiceFacilityList());
        mapEnumListToKeyValue(CATERING_FACILITY_ENUMERATION.getKey(), facilitiesKeyValue, rawFacility.getCateringFacilityList());
        mapEnumListToKeyValue(COUCHETTE_FACILITY_ENUMERATION.getKey(), facilitiesKeyValue, rawFacility.getCouchetteFacilityList());
        mapEnumListToKeyValue(FAMILY_FACILITY_ENUMERATION.getKey(), facilitiesKeyValue, rawFacility.getFamilyFacilityList());
        mapEnumListToKeyValue(FARE_CLASS_ENUMERATION.getKey(), facilitiesKeyValue, rawFacility.getFareClasses());
        mapEnumToKeyValue(FacilityTypeEnum.SERVICE_FACILITY_SET.name(),GENDER_LIMITATION_ENUMERATION.getKey(), facilitiesKeyValue, rawFacility.getGenderLimitation());
        mapEnumToKeyValue(FacilityTypeEnum.SERVICE_FACILITY_SET.name(),GROUP_BOOKING_ENUMERATION.getKey(), facilitiesKeyValue, rawFacility.getGroupBookingFacility());
        mapEnumListToKeyValue(LUGGAGE_CARRIAGE_ENUMERATION.getKey(), facilitiesKeyValue, rawFacility.getLuggageCarriageFacilityList());
        mapEnumListToKeyValue(MEAL_FACILITY_ENUMERATION.getKey(), facilitiesKeyValue, rawFacility.getMealFacilityList());
        mapEnumListToKeyValue(MEDICAL_FACILITY_ENUMERATION.getKey(), facilitiesKeyValue, rawFacility.getMedicalFacilityList());
        mapEnumListToKeyValue(MOBILITY_FACILITY_ENUMERATION.getKey(), facilitiesKeyValue, rawFacility.getMobilityFacilityList());
        mapEnumListToKeyValue(NUISANCE_FACILITY_ENUMERATION.getKey(), facilitiesKeyValue, rawFacility.getNuisanceFacilityList());
        mapEnumListToKeyValue(PASSENGER_COMMS_FACILITY_ENUMERATION.getKey(), facilitiesKeyValue, rawFacility.getPassengerCommsFacilityList());
        mapEnumToKeyValue(FacilityTypeEnum.SERVICE_FACILITY_SET.name(),PASSENGER_INFORMATION_EQUIPMENT_ENUMERATION.getKey(), facilitiesKeyValue, rawFacility.getPassengerInformationEquipmentList());
        mapEnumListToKeyValue(PASSENGER_INFORMATION_FACILITY_ENUMERATION.getKey(), facilitiesKeyValue, rawFacility.getPassengerInformationFacilityList());
        mapEnumListToKeyValue(RETAIL_FACILITY_ENUMERATION.getKey(), facilitiesKeyValue, rawFacility.getRetailFacilityList());
        mapEnumListToKeyValue(SAFETY_FACILITY_ENUMERATION.getKey(), facilitiesKeyValue, rawFacility.getSafetyFacilityList());
        mapEnumListToKeyValue(SANITARY_FACILITY_ENUMERATION.getKey(), facilitiesKeyValue, rawFacility.getSanitaryFacilityList());
        mapEnumListToKeyValue(RESERVATION_ENUMERATION.getKey(), facilitiesKeyValue, rawFacility.getServiceReservationFacilityList());
        mapEnumListToKeyValue(TICKETING_FACILITY_ENUMERATION.getKey(), facilitiesKeyValue, rawFacility.getTicketingFacilityList());
        mapEnumListToKeyValue(TICKETING_SERVICE_FACILITY_ENUMERATION.getKey(), facilitiesKeyValue, rawFacility.getTicketingServiceFacilityList());
        mapEnumListToKeyValue(UIC_PRODUCT_CHARACTERISTIC_ENUMERATION.getKey(), facilitiesKeyValue, rawFacility.getUicProductCharacteristicList());
        mapEnumToKeyValue(FacilityTypeEnum.SERVICE_FACILITY_SET.name(), UIC_RATE_TYPE_ENUMERATION.getKey(), facilitiesKeyValue, rawFacility.getUicTrainRate());
        mapEnumListToKeyValue(VEHICLE_ACCESS_FACILITY_ENUMERATION.getKey(), facilitiesKeyValue, rawFacility.getVehicleAccessFacilityList());

        mapAccommodations(rawFacility, facilitiesKeyValue);

        mapOnboardStays(rawFacility, facilitiesKeyValue);

        if (CollectionUtils.isNotEmpty(facilitiesKeyValue)) {
            vehicleJourneyFacility.setKeyValues(facilitiesKeyValue);
        }
        return vehicleJourneyFacility;
    }

    private void mapAccommodations(ServiceFacilitySet rawFacility, ArrayList<KeyValue> facilitiesKeyValue) {
        Accommodations_RelStructure accommodations = rawFacility.getAccommodations();
        if (accommodations != null && CollectionUtils.isNotEmpty(accommodations.getAccommodationRefOrAccommodation())) {
            List<Object> accommodationRefOrAccommodation = accommodations.getAccommodationRefOrAccommodation();
            for (Object accommodationDetail : accommodationRefOrAccommodation) {
                if (accommodationDetail instanceof Accommodation) {
                    Accommodation accommodation = (Accommodation) accommodationDetail;
                    String identifier = ACCOMMODATION.name() + SEPARATOR + accommodation.getId();
                    mapEnumToKeyValue(identifier, ACCOMMODATION_FACILITY_ENUMERATION.getKey(), facilitiesKeyValue, accommodation.getAccommodationFacility());
                    mapEnumToKeyValue(identifier, COUCHETTE_FACILITY_ENUMERATION.getKey(), facilitiesKeyValue, accommodation.getCouchetteFacility());
                    mapEnumToKeyValue(identifier, GENDER_LIMITATION_ENUMERATION.getKey(), facilitiesKeyValue, accommodation.getGenderLimitation());
                }
            }
        }
    }

    private void mapOnboardStays(ServiceFacilitySet rawFacility, ArrayList<KeyValue> facilitiesKeyValue) {
        OnboardStays_RelStructure onboardStays = rawFacility.getOnboardStays();
        if (onboardStays != null && CollectionUtils.isNotEmpty(onboardStays.getOnboardStay())) {
            List<OnboardStay> onboardStay = onboardStays.getOnboardStay();
            for (OnboardStay onboardStayDetail : onboardStay) {
                String identifier = ONBOARD_STAY.name() + SEPARATOR + onboardStayDetail.getId();
                mapEnumToKeyValue(identifier,BOARDING_PERMISSION_ENUMERATION.getKey(), facilitiesKeyValue, onboardStayDetail.getBoardingPermission());
            }
        }

    }

    private <T extends Enum<T>> void mapEnumListToKeyValue(String keyType, ArrayList<KeyValue> facilitiesKeyValue, List<T> enumList) {
        if (CollectionUtils.isNotEmpty(enumList)) {
            String enumValues = enumList.stream().map(Enum::name).collect(Collectors.joining(SEPARATOR));
            KeyValue keyValue = new KeyValue();
            keyValue.setTypeOfKey(FacilityTypeEnum.SERVICE_FACILITY_SET.name());
            keyValue.setKey(keyType);
            keyValue.setValue(enumValues);
            facilitiesKeyValue.add(keyValue);
        }
    }

    private <T extends Enum<T>> void mapEnumToKeyValue(String objectType, String keyType, List<KeyValue> facilitiesKeyValue, T inputEnum) {
        if (inputEnum != null) {
            KeyValue keyValue = new KeyValue();
            keyValue.setKey(keyType);
            keyValue.setTypeOfKey(objectType);
            keyValue.setValue(inputEnum.name());
            facilitiesKeyValue.add(keyValue);
        }
    }

}

