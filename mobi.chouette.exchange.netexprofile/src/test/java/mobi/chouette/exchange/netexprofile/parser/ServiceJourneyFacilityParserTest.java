package mobi.chouette.exchange.netexprofile.parser;

import mobi.chouette.common.Context;
import mobi.chouette.exchange.netexprofile.importer.NetexprofileImportParameters;
import mobi.chouette.model.KeyValue;
import mobi.chouette.model.VehicleJourney;
import mobi.chouette.model.VehicleJourneyFacility;
import mobi.chouette.model.util.Referential;
import org.rutebanken.netex.model.*;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

import static mobi.chouette.common.Constant.CONFIGURATION;
import static mobi.chouette.common.Constant.REFERENTIAL;
import static mobi.chouette.model.type.FacilityTypeEnum.*;
import static mobi.chouette.model.type.VehicleJourneyFacilityEnum.*;
import static org.rutebanken.netex.model.BoardingPermissionEnumeration.EARLY_BOARDING_POSSIBLE_BEFORE_DEPARTURE;
import static org.rutebanken.netex.model.BoardingPermissionEnumeration.OVERNIGHT_STAY_ONBOARD_ALLOWED;
import static org.rutebanken.netex.model.MealFacilityEnumeration.DINNER;
import static org.rutebanken.netex.model.NuisanceFacilityEnumeration.ANIMALS_ALLOWED;
import static org.rutebanken.netex.model.NuisanceFacilityEnumeration.CHILDFREE_AREA;
import static org.rutebanken.netex.model.VehicleAccessFacilityEnumeration.AUTOMATIC_RAMP;
import static org.rutebanken.netex.model.VehicleAccessFacilityEnumeration.NARROW_ENTRANCE;

public class ServiceJourneyFacilityParserTest {

    private static final String IDENTIFIER_1 = "id1";
    private static final String IDENTIFIER_2 = "id2";
    private static final String DESCRIPTION = "description";
    private static final String MOBI_ITI = "MOBI-ITI";

    private ServiceJourneyFacilityParser serviceJourneyFacilityParser;

    private Context context;

    private Referential referential;

    private VehicleJourney vehicleJourney;

    @BeforeMethod
    public void setUp() {
        context = getContext();
        serviceJourneyFacilityParser = new ServiceJourneyFacilityParser();
        referential = new Referential();
        vehicleJourney = new VehicleJourney();
    }

    @Test
    public void test_accommodationParsing() {
        ServiceJourney serviceJourney = new ServiceJourney();

        ServiceFacilitySet serviceFacilitySet = new ServiceFacilitySet();
        serviceFacilitySet.setDescription(buildMultilingualString(DESCRIPTION));
        serviceFacilitySet.setId("sfsIdentifier");
        ServiceFacilitySets_RelStructure facilitySetContainer = new ServiceFacilitySets_RelStructure();

        Accommodation accommodation = buildAccommodation(IDENTIFIER_1);
        accommodation.setGenderLimitation(GenderLimitationEnumeration.FEMALE_ONLY);

        Accommodation accommodation2 = buildAccommodation(IDENTIFIER_2);
        accommodation2.setCouchetteFacility(CouchetteFacilityEnumeration.C_2);

        Accommodations_RelStructure accommodationContainer = new Accommodations_RelStructure();
        accommodationContainer.withAccommodationRefOrAccommodation(accommodation, accommodation2);
        serviceFacilitySet.withAccommodations(accommodationContainer);
        facilitySetContainer.getServiceFacilitySetRefOrServiceFacilitySet().add(serviceFacilitySet);
        serviceJourney.withFacilities(facilitySetContainer);

        serviceJourneyFacilityParser.parseFacilities(context, referential, serviceJourney, vehicleJourney);

        VehicleJourneyFacility vehicleJourneyFacility = referential.getFacilities().get("referential:ServiceFacilitySet:sfsIdentifier");
        Assert.assertNotNull(vehicleJourneyFacility);
        Assert.assertEquals(vehicleJourneyFacility.getDescription(), DESCRIPTION);
        Assert.assertEquals(vehicleJourneyFacility.getVehicleJourney(), vehicleJourney);
        Assert.assertEquals(vehicleJourneyFacility.getObjectVersion().intValue(), 0);
        Assert.assertEquals(vehicleJourneyFacility.getCreatorId(), MOBI_ITI);
        List<KeyValue> keyValues = vehicleJourneyFacility.getKeyValues();
        Assert.assertNotNull(keyValues);
        Assert.assertEquals(keyValues.size(), 2);

        checkKeyValue(keyValues.get(0),
                ACCOMMODATION + ";" + IDENTIFIER_1,
                GENDER_LIMITATION_ENUMERATION.getKey(),
                GenderLimitationEnumeration.FEMALE_ONLY.name()
        );

        checkKeyValue(keyValues.get(1),
                ACCOMMODATION + ";" + IDENTIFIER_2,
                COUCHETTE_FACILITY_ENUMERATION.getKey(),
                CouchetteFacilityEnumeration.C_2.name()
        );

    }

    @Test
    public void test_onBoardStayParsing() {
        ServiceJourney serviceJourney = new ServiceJourney();

        ServiceFacilitySet serviceFacilitySet = new ServiceFacilitySet();
        serviceFacilitySet.setDescription(buildMultilingualString(DESCRIPTION));
        serviceFacilitySet.setId("sfsIdentifier");
        ServiceFacilitySets_RelStructure facilitySetContainer = new ServiceFacilitySets_RelStructure();

        OnboardStay onboardStay = buildOnboardStay(IDENTIFIER_1);
        onboardStay.setBoardingPermission(EARLY_BOARDING_POSSIBLE_BEFORE_DEPARTURE);
        OnboardStay onboardStay2 = buildOnboardStay(IDENTIFIER_2);
        onboardStay2.setBoardingPermission(OVERNIGHT_STAY_ONBOARD_ALLOWED);

        OnboardStays_RelStructure onBoardStayContainer = new OnboardStays_RelStructure();
        onBoardStayContainer.withOnboardStay(onboardStay, onboardStay2);
        serviceFacilitySet.withOnboardStays(onBoardStayContainer);
        facilitySetContainer.getServiceFacilitySetRefOrServiceFacilitySet().add(serviceFacilitySet);
        serviceJourney.withFacilities(facilitySetContainer);

        serviceJourneyFacilityParser.parseFacilities(context, referential, serviceJourney, vehicleJourney);

        VehicleJourneyFacility vehicleJourneyFacility = referential.getFacilities().get("referential:ServiceFacilitySet:sfsIdentifier");
        Assert.assertNotNull(vehicleJourneyFacility);
        Assert.assertEquals(vehicleJourneyFacility.getDescription(), DESCRIPTION);
        Assert.assertEquals(vehicleJourneyFacility.getVehicleJourney(), vehicleJourney);
        Assert.assertEquals(vehicleJourneyFacility.getObjectVersion().intValue(), 0);
        Assert.assertEquals(vehicleJourneyFacility.getCreatorId(), MOBI_ITI);
        List<KeyValue> keyValues = vehicleJourneyFacility.getKeyValues();
        Assert.assertNotNull(keyValues);
        Assert.assertEquals(keyValues.size(), 2);

        checkKeyValue(keyValues.get(0),
                ONBOARD_STAY + ";" + IDENTIFIER_1,
                BOARDING_PERMISSION_ENUMERATION.getKey(),
                EARLY_BOARDING_POSSIBLE_BEFORE_DEPARTURE.name()
        );

        checkKeyValue(keyValues.get(1),
                ONBOARD_STAY + ";" + IDENTIFIER_2,
                BOARDING_PERMISSION_ENUMERATION.getKey(),
                OVERNIGHT_STAY_ONBOARD_ALLOWED.name()
        );
    }

    @Test
    public void test_facilitiesParsing() {
        ServiceJourney serviceJourney = new ServiceJourney();

        ServiceFacilitySet serviceFacilitySet = new ServiceFacilitySet();
        serviceFacilitySet.setDescription(buildMultilingualString(DESCRIPTION));
        serviceFacilitySet.setId("sfsIdentifier");
        serviceFacilitySet.withVehicleAccessFacilityList(AUTOMATIC_RAMP, NARROW_ENTRANCE);
        serviceFacilitySet.withNuisanceFacilityList(ANIMALS_ALLOWED, CHILDFREE_AREA);
        serviceFacilitySet.withMealFacilityList(DINNER);

        ServiceFacilitySets_RelStructure facilitySetContainer = new ServiceFacilitySets_RelStructure();
        facilitySetContainer.getServiceFacilitySetRefOrServiceFacilitySet().add(serviceFacilitySet);
        serviceJourney.withFacilities(facilitySetContainer);

        serviceJourneyFacilityParser.parseFacilities(context, referential, serviceJourney, vehicleJourney);

        VehicleJourneyFacility vehicleJourneyFacility = referential.getFacilities().get("referential:ServiceFacilitySet:sfsIdentifier");
        Assert.assertNotNull(vehicleJourneyFacility);
        Assert.assertEquals(vehicleJourneyFacility.getDescription(), DESCRIPTION);
        Assert.assertEquals(vehicleJourneyFacility.getVehicleJourney(), vehicleJourney);
        Assert.assertEquals(vehicleJourneyFacility.getObjectVersion().intValue(), 0);
        Assert.assertEquals(vehicleJourneyFacility.getCreatorId(), MOBI_ITI);
        List<KeyValue> keyValues = vehicleJourneyFacility.getKeyValues();
        Assert.assertNotNull(keyValues);
        Assert.assertEquals(keyValues.size(), 3);

        checkKeyValue(keyValues.get(0),
                SERVICE_FACILITY_SET.name(),
                MEAL_FACILITY_ENUMERATION.getKey(),
                DINNER.name()
        );

        checkKeyValue(keyValues.get(1),
                SERVICE_FACILITY_SET.name(),
                NUISANCE_FACILITY_ENUMERATION.getKey(),
                ANIMALS_ALLOWED.name() +";"+ CHILDFREE_AREA.name()
        );

        checkKeyValue(keyValues.get(2),
                SERVICE_FACILITY_SET.name(),
                VEHICLE_ACCESS_FACILITY_ENUMERATION.getKey(),
                AUTOMATIC_RAMP.name() +";"+ NARROW_ENTRANCE.name()
        );
    }

    private static Context getContext() {
        Context context = new Context();
        NetexprofileImportParameters parameters = new NetexprofileImportParameters();
        parameters.setObjectIdPrefix(REFERENTIAL);
        context.put(CONFIGURATION, parameters);
        return context;
    }

    private static Accommodation buildAccommodation(String identifier) {
        Accommodation accommodation = new Accommodation();
        accommodation.setVersion("any");
        accommodation.setId(identifier);
        return accommodation;
    }

    private static OnboardStay buildOnboardStay(String identifier) {
        OnboardStay onboardStay = new OnboardStay();
        onboardStay.setVersion("any");
        onboardStay.setId(identifier);
        return onboardStay;
    }

    private static MultilingualString buildMultilingualString(String inputValue) {
        MultilingualString multilingualString = new MultilingualString();
        multilingualString.setValue(inputValue);
        return multilingualString;
    }

    private void checkKeyValue(KeyValue keyValue, String expectedTypeOfKey, String expectedKey, String expectedValue) {
        Assert.assertEquals(keyValue.getTypeOfKey(), expectedTypeOfKey);
        Assert.assertEquals(keyValue.getKey(), expectedKey);
        Assert.assertEquals(keyValue.getValue(),expectedValue);
    }
}