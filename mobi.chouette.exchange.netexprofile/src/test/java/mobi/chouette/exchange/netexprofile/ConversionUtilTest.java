package mobi.chouette.exchange.netexprofile;

import mobi.chouette.model.type.TransportSubModeNameEnum;
import org.rutebanken.netex.model.TransportSubmodeStructure;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static mobi.chouette.model.type.TransportSubModeNameEnum.*;
import static org.rutebanken.netex.model.BusSubmodeEnumeration.*;
import static org.rutebanken.netex.model.CoachSubmodeEnumeration.*;
import static org.rutebanken.netex.model.FunicularSubmodeEnumeration.*;
import static org.rutebanken.netex.model.MetroSubmodeEnumeration.*;
import static org.rutebanken.netex.model.RailSubmodeEnumeration.*;
import static org.rutebanken.netex.model.TelecabinSubmodeEnumeration.*;
import static org.rutebanken.netex.model.TramSubmodeEnumeration.*;
import static org.rutebanken.netex.model.WaterSubmodeEnumeration.*;
import static org.testng.Assert.assertEquals;

public class ConversionUtilTest {
    @DataProvider(name = "testData")
    public Object[][] data() {
        return new Object[][]{
                {AirportLinkBus, new TransportSubmodeStructure().withBusSubmode(AIRPORT_LINK_BUS) },
                {DedicatedLaneBus, new TransportSubmodeStructure().withBusSubmode(DEDICATED_LANE_BUS)},
                {DemandAndResponseBus, new TransportSubmodeStructure().withBusSubmode(DEMAND_AND_RESPONSE_BUS)},
                {ExpressBus, new TransportSubmodeStructure().withBusSubmode(EXPRESS_BUS)},
                {HighFrequencyBus, new TransportSubmodeStructure().withBusSubmode(HIGH_FREQUENCY_BUS)},
                {LocalBus, new TransportSubmodeStructure().withBusSubmode(LOCAL_BUS)},
                {MobilityBus, new TransportSubmodeStructure().withBusSubmode(MOBILITY_BUS)},
                {MobilityBusForRegisteredDisabled, new TransportSubmodeStructure().withBusSubmode(MOBILITY_BUS_FOR_REGISTERED_DISABLED)},
                {NightBus, new TransportSubmodeStructure().withBusSubmode(NIGHT_BUS)},
                {PostBus, new TransportSubmodeStructure().withBusSubmode(POST_BUS)},
                {RailReplacementBus, new TransportSubmodeStructure().withBusSubmode(RAIL_REPLACEMENT_BUS)},
                {RegionalBus, new TransportSubmodeStructure().withBusSubmode(REGIONAL_BUS)},
                {SchoolAndPublicServiceBus, new TransportSubmodeStructure().withBusSubmode(SCHOOL_AND_PUBLIC_SERVICE_BUS)},
                {SchoolBus, new TransportSubmodeStructure().withBusSubmode(SCHOOL_BUS)},
                {ShuttleBus, new TransportSubmodeStructure().withBusSubmode(SHUTTLE_BUS)},
                {SightseeingBus, new TransportSubmodeStructure().withBusSubmode(SIGHTSEEING_BUS)},
                {SpecialNeedsBus, new TransportSubmodeStructure().withBusSubmode(SPECIAL_NEEDS_BUS)},

                {CommuterCoach, new TransportSubmodeStructure().withCoachSubmode(COMMUTER_COACH)},
                {InternationalCoach, new TransportSubmodeStructure().withCoachSubmode(INTERNATIONAL_COACH)},
                {NationalCoach, new TransportSubmodeStructure().withCoachSubmode(NATIONAL_COACH)},
                {RegionalCoach, new TransportSubmodeStructure().withCoachSubmode(REGIONAL_COACH)},
                {SchoolCoach, new TransportSubmodeStructure().withCoachSubmode(SCHOOL_COACH)},
                {ShuttleCoach, new TransportSubmodeStructure().withCoachSubmode(SHUTTLE_COACH)},
                {SightseeingCoach, new TransportSubmodeStructure().withCoachSubmode(SIGHTSEEING_COACH)},
                {SpecialCoach, new TransportSubmodeStructure().withCoachSubmode(SPECIAL_COACH)},
                {TouristCoach, new TransportSubmodeStructure().withCoachSubmode(TOURIST_COACH)},

                {AllFunicularServices, new TransportSubmodeStructure().withFunicularSubmode(ALL_FUNICULAR_SERVICES)},
                {Funicular, new TransportSubmodeStructure().withFunicularSubmode(FUNICULAR)},
                {StreetCableCar, new TransportSubmodeStructure().withFunicularSubmode(STREET_CABLE_CAR)},

                {Metro, new TransportSubmodeStructure().withMetroSubmode(METRO)},
                {Tube, new TransportSubmodeStructure().withMetroSubmode(TUBE)},
                {UrbanRailway, new TransportSubmodeStructure().withMetroSubmode(URBAN_RAILWAY)},

                {AirportLinkRail, new TransportSubmodeStructure().withRailSubmode(AIRPORT_LINK_RAIL)},
                {CarTransportRailService, new TransportSubmodeStructure().withRailSubmode(CAR_TRANSPORT_RAIL_SERVICE)},
                {CrossCountryRail, new TransportSubmodeStructure().withRailSubmode(CROSS_COUNTRY_RAIL)},
                {HighSpeedRail, new TransportSubmodeStructure().withRailSubmode(HIGH_SPEED_RAIL)},
                {International, new TransportSubmodeStructure().withRailSubmode(INTERNATIONAL)},
                {InterregionalRail, new TransportSubmodeStructure().withRailSubmode(INTERREGIONAL_RAIL)},
                {Local, new TransportSubmodeStructure().withRailSubmode(LOCAL)},
                {LongDistance, new TransportSubmodeStructure().withRailSubmode(LONG_DISTANCE)},
                {NightRail, new TransportSubmodeStructure().withRailSubmode(NIGHT_RAIL)},
                {RackAndPinionRailway, new TransportSubmodeStructure().withRailSubmode(RACK_AND_PINION_RAILWAY)},
                {RailShuttle, new TransportSubmodeStructure().withRailSubmode(RAIL_SHUTTLE)},
                {RegionalRail, new TransportSubmodeStructure().withRailSubmode(REGIONAL_RAIL)},
                {ReplacementRailService, new TransportSubmodeStructure().withRailSubmode(REPLACEMENT_RAIL_SERVICE)},
                {SleeperRailService, new TransportSubmodeStructure().withRailSubmode(SLEEPER_RAIL_SERVICE)},
                {SpecialTrain, new TransportSubmodeStructure().withRailSubmode(SPECIAL_TRAIN)},
                {SuburbanRailway, new TransportSubmodeStructure().withRailSubmode(SUBURBAN_RAILWAY)},
                {TouristRailway, new TransportSubmodeStructure().withRailSubmode(TOURIST_RAILWAY)},

                {CableCar, new TransportSubmodeStructure().withTelecabinSubmode(CABLE_CAR)},
                {ChairLift, new TransportSubmodeStructure().withTelecabinSubmode(CHAIR_LIFT)},
                {DragLift, new TransportSubmodeStructure().withTelecabinSubmode(DRAG_LIFT)},
                {Lift, new TransportSubmodeStructure().withTelecabinSubmode(LIFT)},
                {Telecabin, new TransportSubmodeStructure().withTelecabinSubmode(TELECABIN)},
                {TelecabinLink, new TransportSubmodeStructure().withTelecabinSubmode(TELECABIN_LINK)},

                {CityTram, new TransportSubmodeStructure().withTramSubmode(CITY_TRAM)},
                {LocalTram, new TransportSubmodeStructure().withTramSubmode(LOCAL_TRAM)},
                {RegionalTram, new TransportSubmodeStructure().withTramSubmode(REGIONAL_TRAM)},
                {ShuttleTram, new TransportSubmodeStructure().withTramSubmode(SHUTTLE_TRAM)},
                {SightseeingTram, new TransportSubmodeStructure().withTramSubmode(SIGHTSEEING_TRAM)},
                {TrainTram, new TransportSubmodeStructure().withTramSubmode(TRAIN_TRAM)},

                {AirportBoatLink, new TransportSubmodeStructure().withWaterSubmode(AIRPORT_BOAT_LINK)},
                {CableFerry, new TransportSubmodeStructure().withWaterSubmode(CABLE_FERRY)},
                {CanalBarge, new TransportSubmodeStructure().withWaterSubmode(CANAL_BARGE)},
                {HighSpeedPassengerService, new TransportSubmodeStructure().withWaterSubmode(HIGH_SPEED_PASSENGER_SERVICE)},
                {HighSpeedVehicleService, new TransportSubmodeStructure().withWaterSubmode(HIGH_SPEED_VEHICLE_SERVICE)},
                {InternationalCarFerry, new TransportSubmodeStructure().withWaterSubmode(INTERNATIONAL_CAR_FERRY)},
                {InternationalPassengerFerry, new TransportSubmodeStructure().withWaterSubmode(INTERNATIONAL_PASSENGER_FERRY)},
                {LocalCarFerry, new TransportSubmodeStructure().withWaterSubmode(LOCAL_CAR_FERRY)},
                {LocalPassengerFerry, new TransportSubmodeStructure().withWaterSubmode(LOCAL_PASSENGER_FERRY)},
                {NationalCarFerry, new TransportSubmodeStructure().withWaterSubmode(NATIONAL_CAR_FERRY)},
                {NationalPassengerFerry, new TransportSubmodeStructure().withWaterSubmode(NATIONAL_PASSENGER_FERRY)},
                {PostBoat, new TransportSubmodeStructure().withWaterSubmode(POST_BOAT)},
                {RegionalCarFerry, new TransportSubmodeStructure().withWaterSubmode(REGIONAL_CAR_FERRY)},
                {RegionalPassengerFerry, new TransportSubmodeStructure().withWaterSubmode(REGIONAL_PASSENGER_FERRY)},
                {RiverBus, new TransportSubmodeStructure().withWaterSubmode(RIVER_BUS)},
                {RoadFerryLink, new TransportSubmodeStructure().withWaterSubmode(ROAD_FERRY_LINK)},
                {ScheduledFerry, new TransportSubmodeStructure().withWaterSubmode(SCHEDULED_FERRY)},
                {SchoolBoat, new TransportSubmodeStructure().withWaterSubmode(SCHOOL_BOAT)},
                {ShuttleFerryService, new TransportSubmodeStructure().withWaterSubmode(SHUTTLE_FERRY_SERVICE)},
                {SightseeingService, new TransportSubmodeStructure().withWaterSubmode(SIGHTSEEING_SERVICE)},
                {TrainFerry, new TransportSubmodeStructure().withWaterSubmode(TRAIN_FERRY)},
        };
    }

    @Test(dataProvider = "testData")
    public void toTransportSubmodeStructure_shouldReturnNeTExTransportSubModeNameEnum_test(TransportSubModeNameEnum inputEnum, TransportSubmodeStructure expectedResult) {
        TransportSubmodeStructure computedTransportSubmodeStructure = ConversionUtil.toTransportSubmodeStructure(inputEnum);

        checkTransportSubMode(computedTransportSubmodeStructure, expectedResult);
    }

    private static void checkTransportSubMode(TransportSubmodeStructure computedTransportSubmodeStructure, TransportSubmodeStructure expectedResult) {
        if (expectedResult.getAirSubmode() != null) {
            assertEquals(computedTransportSubmodeStructure.getAirSubmode(), expectedResult.getAirSubmode());
        }
        if (expectedResult.getBusSubmode() != null) {
            assertEquals(computedTransportSubmodeStructure.getBusSubmode(), expectedResult.getBusSubmode());
        }
        if (expectedResult.getCoachSubmode() != null) {
            assertEquals(computedTransportSubmodeStructure.getCoachSubmode(), expectedResult.getCoachSubmode());
        }
        if (expectedResult.getFunicularSubmode() != null) {
            assertEquals(computedTransportSubmodeStructure.getFunicularSubmode(), expectedResult.getFunicularSubmode());
        }
        if (expectedResult.getMetroSubmode() != null) {
            assertEquals(computedTransportSubmodeStructure.getMetroSubmode(), expectedResult.getMetroSubmode());
        }
        if (expectedResult.getRailSubmode() != null) {
            assertEquals(computedTransportSubmodeStructure.getRailSubmode(), expectedResult.getRailSubmode());
        }
        if (expectedResult.getTelecabinSubmode() != null) {
            assertEquals(computedTransportSubmodeStructure.getTelecabinSubmode(), expectedResult.getTelecabinSubmode());
        }
        if (expectedResult.getTramSubmode() != null) {
            assertEquals(computedTransportSubmodeStructure.getTramSubmode(), expectedResult.getTramSubmode());
        }
        if (expectedResult.getWaterSubmode() != null) {
            assertEquals(computedTransportSubmodeStructure.getWaterSubmode(), expectedResult.getWaterSubmode());
        }
    }

}