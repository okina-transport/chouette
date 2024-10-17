package mobi.chouette.exchange;

import mobi.chouette.model.type.TransportModeNameEnum;
import mobi.chouette.model.type.TransportSubModeNameEnum;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.rutebanken.netex.model.*;

import static mobi.chouette.model.type.TransportModeNameEnum.*;
import static mobi.chouette.model.type.TransportSubModeNameEnum.*;
import static org.testng.Assert.assertEquals;

import java.util.Arrays;

@RunWith(Parameterized.class)
public class NetexParserUtilsTest {

    @Parameter()
    public TransportModeNameEnum transportModeNameEnum;
    @Parameter(1)
    public String inputTransportSubModeName;
    @Parameter(2)
    public TransportSubModeNameEnum expectedTransportSubMode;

    @Parameters()
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { Air, "airshipService", AirshipService },
                { Air,"domesticCharterFlight", DomesticCharterFlight },
                { Air,"domesticFlight", DomesticFlight },
                { Air,"domesticScheduledFlight", DomesticScheduledFlight },
                { Air,"helicopterService", HelicopterService },
                { Air,"intercontinentalCharterFlight", IntercontinentalCharterFlight },
                { Air,"intercontinentalFlight", IntercontinentalFlight },
                { Air,"internationalCharterFlight", InternationalCharterFligth },
                { Air,"internationalFlight", InternationalFlight },
                { Air,"roundTripCharterFlight", RoundTripCharterFlight },
                { Air,"SchengenAreaFlight", SchengenAreaFlight },
                { Air,"shortHaulInternationalFlight", ShortHaulInternationalFlight },
                { Air,"shuttleFlight", ShuttleFlight },
                { Air,"sightseeingFlight", SightseeingFlight },
                { Air,"unknown", null },

                { Bus, "airportLinkBus", AirportLinkBus },
                { Bus, "dedicatedLaneBus", DedicatedLaneBus },
                { Bus, "demandAndResponseBus", DemandAndResponseBus },
                { Bus, "expressBus", ExpressBus },
                { Bus, "highFrequencyBus", HighFrequencyBus },
                { Bus, "localBus", LocalBus },
                { Bus, "mobilityBus", MobilityBus },
                { Bus, "mobilityBusForRegisteredDisabled", MobilityBusForRegisteredDisabled },
                { Bus, "nightBus", NightBus },
                { Bus, "postBus", PostBus },
                { Bus, "railReplacementBus", RailReplacementBus },
                { Bus, "regionalBus", RegionalBus },
                { Bus, "schoolAndPublicServiceBus", SchoolAndPublicServiceBus },
                { Bus, "schoolBus", SchoolBus },
                { Bus, "shuttleBus", ShuttleBus },
                { Bus, "specialNeedsBus", SpecialNeedsBus },
                { Bus, "unknown", null },

                { Coach, "commuterCoach", CommuterCoach },
                { Coach, "internationalCoach", InternationalCoach },
                { Coach, "nationalCoach", NationalCoach },
                { Coach, "regionalCoach", RegionalCoach },
                { Coach, "schoolCoach", SchoolCoach },
                { Coach, "shuttleCoach", ShuttleCoach },
                { Coach, "sightseeingCoach", SightseeingCoach },
                { Coach, "specialCoach", SpecialCoach },
                { Coach, "touristCoach", TouristCoach },

                { Rail, "airportLinkRail", AirportLinkRail },
                { Rail, "carTransportRailService", CarTransportRailService },
                { Rail, "crossCountryRail", CrossCountryRail },
                { Rail, "highSpeedRail", HighSpeedRail },
                { Rail, "international", International },
                { Rail, "interregionalRail", InterregionalRail },
                { Rail, "local", Local },
                { Rail, "longDistance", LongDistance },
                { Rail, "nightRail", NightRail },
                { Rail, "rackAndPinionRailway", RackAndPinionRailway },
                { Rail, "railShuttle", RailShuttle },
                { Rail, "regionalRail", RegionalRail },
                { Rail, "replacementRailService", ReplacementRailService},
                { Rail, "sleeperRailService", SleeperRailService },
                { Rail, "specialTrain", SpecialTrain },
                { Rail, "suburbanRailway", SuburbanRailway },
                { Rail, "touristRailway", TouristRailway },
        });
    }

    @Test
    public void toTransportSubModeNameEnum_airSubmodeEnumerationValues_shouldReturnCorrectEnumMapping_test(){
        TransportSubmodeStructure input = setInputTransportSubMode();

        TransportSubModeNameEnum transportSubMode = NetexParserUtils.toTransportSubModeNameEnum(input);

        assertEquals(expectedTransportSubMode, transportSubMode);
    }

    protected TransportSubmodeStructure setInputTransportSubMode() {
        TransportSubmodeStructure input = new TransportSubmodeStructure();
        switch (transportModeNameEnum) {
            case Air:
                AirSubmodeEnumeration inputAirSubMode = AirSubmodeEnumeration.fromValue(inputTransportSubModeName);
                input.setAirSubmode(inputAirSubMode);
                break;
            case Bus:
                BusSubmodeEnumeration busSubmodeEnumeration = BusSubmodeEnumeration.fromValue(inputTransportSubModeName);
                input.setBusSubmode(busSubmodeEnumeration);
                break;
            case Coach:
                CoachSubmodeEnumeration coachSubmodeEnumeration = CoachSubmodeEnumeration.fromValue(inputTransportSubModeName);
                input.setCoachSubmode(coachSubmodeEnumeration);
                break;
            case Rail:
                RailSubmodeEnumeration railSubmodeEnumeration = RailSubmodeEnumeration.fromValue(inputTransportSubModeName);
                input.setRailSubmode(railSubmodeEnumeration);
                break;
            default:
                break;

        }
        return input;

    }
}