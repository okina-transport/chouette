package mobi.chouette.exchange.gtfs.model;

import lombok.Getter;
import lombok.extern.log4j.Log4j;
import mobi.chouette.model.type.TransportModeNameEnum;
import mobi.chouette.model.type.TransportSubModeNameEnum;

import java.util.EnumSet;
import java.util.Optional;

@Log4j
@Getter
public enum RouteTypeEnum {

    // TODO this mapping is adapted to submodes valid in Norway.
    // TransportSubModeEnum must be extended to account for all possible types
    Tram(0, TransportModeNameEnum.Tram),
    Subway(1, TransportModeNameEnum.Metro),
    Rail(2, TransportModeNameEnum.Rail),
    Bus(3, TransportModeNameEnum.Bus),
    Ferry(4, TransportModeNameEnum.Ferry),
    CableTram(5, TransportModeNameEnum.Funicular),
    AerialLift(6, TransportModeNameEnum.Cableway),
    Funicular(7, TransportModeNameEnum.Funicular),
    Trolleybus(11, TransportModeNameEnum.TrolleyBus),
    // extension Rail
    RailwayService(100, TransportModeNameEnum.Rail),
    HighSpeedRailService(101, TransportModeNameEnum.Rail, TransportSubModeNameEnum.AirportLinkRail),
    LongDistanceTrains(102, TransportModeNameEnum.Rail, TransportSubModeNameEnum.LongDistance),
    InterRegionalRailService(103, TransportModeNameEnum.Rail, TransportSubModeNameEnum.InterregionalRail),
    CarTransportRailService(104, TransportModeNameEnum.Rail),
    SleeperRailService(105, TransportModeNameEnum.Rail, TransportSubModeNameEnum.NightRail),
    RegionalRailService(106, TransportModeNameEnum.Rail, TransportSubModeNameEnum.RegionalRail),
    TouristRailwayService(107, TransportModeNameEnum.Rail, TransportSubModeNameEnum.TouristRailway),
    RailShuttleWithinComplex(108, TransportModeNameEnum.Rail, TransportSubModeNameEnum.Local),
    SuburbanRailway(109, TransportModeNameEnum.Rail, TransportSubModeNameEnum.Local),
    ReplacementRailService(110, TransportModeNameEnum.Rail),
    SpecialRailService(111, TransportModeNameEnum.Rail),
    LorryTransportRailService(112, TransportModeNameEnum.Rail),
    AllRailServices(113, TransportModeNameEnum.Rail),
    CrossCountryRailService(114, TransportModeNameEnum.Rail, TransportSubModeNameEnum.International),
    VehicleTransportRailService(115, TransportModeNameEnum.Rail),
    RackandPinionRailway(116, TransportModeNameEnum.Rail),
    AdditionalRailService(117, TransportModeNameEnum.Rail),
    // extension Coach
    CoachService(200, TransportModeNameEnum.Coach),
    InternationalCoachService(201, TransportModeNameEnum.Coach, TransportSubModeNameEnum.InternationalCoach),
    NationalCoachService(202, TransportModeNameEnum.Coach, TransportSubModeNameEnum.NationalCoach),
    ShuttleCoachService(203, TransportModeNameEnum.Coach),
    RegionalCoachService(204, TransportModeNameEnum.Coach),
    SpecialCoachService(205, TransportModeNameEnum.Coach),
    SightseeingCoachService(206, TransportModeNameEnum.Coach),
    TouristCoachService(207, TransportModeNameEnum.Coach, TransportSubModeNameEnum.TouristCoach),
    CommuterCoachService(208, TransportModeNameEnum.Coach),
    AllCoachServices(209, TransportModeNameEnum.Coach),
    // extension
    SuburbanRailwayService(300, TransportModeNameEnum.Rail, TransportSubModeNameEnum.Local),
    // extension
    UrbanRailwayService(400, TransportModeNameEnum.Metro),
    MetroService(401, TransportModeNameEnum.Metro),
    UndergroundService(402, TransportModeNameEnum.Metro),
    UrbanRailwayService2(403, TransportModeNameEnum.Metro),
    AllUrbanRailwayServices(404, TransportModeNameEnum.Metro),
    Monorail(405, TransportModeNameEnum.Metro),
    // extension
    MetroService2(500, TransportModeNameEnum.Metro),
    // extension
    UndergroundService2(600, TransportModeNameEnum.Metro),
    // extension
    BusService(700, TransportModeNameEnum.Bus),
    RegionalBusService(701, TransportModeNameEnum.Bus, TransportSubModeNameEnum.RegionalBus),
    ExpressBusService(702, TransportModeNameEnum.Bus, TransportSubModeNameEnum.ExpressBus),
    StoppingBusService(703, TransportModeNameEnum.Bus),
    LocalBusService(704, TransportModeNameEnum.Bus, TransportSubModeNameEnum.LocalBus),
    NightBusService(705, TransportModeNameEnum.Bus, TransportSubModeNameEnum.NightBus),
    PostBusService(706, TransportModeNameEnum.Bus),
    SpecialNeedsBus(707, TransportModeNameEnum.Bus, TransportSubModeNameEnum.SpecialNeedsBus),
    MobilityBusService(708, TransportModeNameEnum.Bus, TransportSubModeNameEnum.MobilityBus),
    MobilityBusforRegisteredDisabled(709, TransportModeNameEnum.Bus),
    SightseeingBus(710, TransportModeNameEnum.Bus, TransportSubModeNameEnum.SightseeingBus),
    ShuttleBus(711, TransportModeNameEnum.Bus, TransportSubModeNameEnum.ShuttleBus),
    SchoolBus(712, TransportModeNameEnum.Bus, TransportSubModeNameEnum.SchoolBus),
    SchoolandPublicServiceBus(713, TransportModeNameEnum.Bus, TransportSubModeNameEnum.SchoolAndPublicServiceBus),
    RailReplacementBusService(714, TransportModeNameEnum.Bus, TransportSubModeNameEnum.RailReplacementBus),
    DemandandResponseBusService(715, TransportModeNameEnum.Bus, TransportSubModeNameEnum.DemandAndResponseBus),
    AllBusServices(716, TransportModeNameEnum.Bus),
    // extension
    TrolleybusService(800, TransportModeNameEnum.TrolleyBus),
    // extension
    TramService(900, TransportModeNameEnum.Tram),
    CityTramService(901, TransportModeNameEnum.Tram),
    LocalTramService(902, TransportModeNameEnum.Tram),
    RegionalTramService(903, TransportModeNameEnum.Tram),
    SightseeingTramService(904, TransportModeNameEnum.Tram),
    ShuttleTramService(905, TransportModeNameEnum.Tram),
    AllTramServices(906, TransportModeNameEnum.Tram),
    // extension
    WaterTransportService(1000, TransportModeNameEnum.Water),
    InternationalCarFerryService(1001, TransportModeNameEnum.Water, TransportSubModeNameEnum.InternationalCarFerry),
    NationalCarFerryService(1002, TransportModeNameEnum.Water, TransportSubModeNameEnum.NationalCarFerry),
    RegionalCarFerryService(1003, TransportModeNameEnum.Water, TransportSubModeNameEnum.LocalCarFerry),
    LocalCarFerryService(1004, TransportModeNameEnum.Water, TransportSubModeNameEnum.LocalCarFerry),
    InternationalPassengerFerryService(1005, TransportModeNameEnum.Water, TransportSubModeNameEnum.InternationalPassengerFerry),
    NationalPassengerFerryService(1006, TransportModeNameEnum.Water, TransportSubModeNameEnum.NationalCarFerry),
    RegionalPassengerFerryService(1007, TransportModeNameEnum.Water, TransportSubModeNameEnum.LocalPassengerFerry),
    LocalPassengerFerryService(1008, TransportModeNameEnum.Water, TransportSubModeNameEnum.LocalPassengerFerry),
    PostBoatService(1009, TransportModeNameEnum.Water),
    TrainFerryService(1010, TransportModeNameEnum.Water),
    RoadLinkFerryService(1011, TransportModeNameEnum.Water, TransportSubModeNameEnum.LocalCarFerry),
    AirportLinkFerryService(1012, TransportModeNameEnum.Water, TransportSubModeNameEnum.LocalPassengerFerry),
    CarHighSpeedFerryService(1013, TransportModeNameEnum.Water, TransportSubModeNameEnum.HighSpeedVehicleService),
    PassengerHighSpeedFerryService(1014, TransportModeNameEnum.Water, TransportSubModeNameEnum.HighSpeedPassengerService),
    SightseeingBoatService(1015, TransportModeNameEnum.Water, TransportSubModeNameEnum.SightseeingService),
    SchoolBoat(1016, TransportModeNameEnum.Water, TransportSubModeNameEnum.LocalPassengerFerry),
    CableDrawnBoatService(1017, TransportModeNameEnum.Water),
    RiverBusService(1018, TransportModeNameEnum.Water, TransportSubModeNameEnum.LocalPassengerFerry),
    ScheduledFerryService(1019, TransportModeNameEnum.Water),
    ShuttleFerryService(1020, TransportModeNameEnum.Water),
    AllWaterTransportServices(1021, TransportModeNameEnum.Water),
    // extension
    AirService(1100, TransportModeNameEnum.Air),
    InternationalAirService(1101, TransportModeNameEnum.Air, TransportSubModeNameEnum.InternationalFlight),
    DomesticAirService(1102, TransportModeNameEnum.Air, TransportSubModeNameEnum.DomesticFlight),
    IntercontinentalAirService(1103, TransportModeNameEnum.Air, TransportSubModeNameEnum.InternationalFlight),
    DomesticScheduledAirService(1104, TransportModeNameEnum.Air, TransportSubModeNameEnum.DomesticFlight),
    ShuttleAirService(1105, TransportModeNameEnum.Air),
    IntercontinentalCharterAirService(1106, TransportModeNameEnum.Air, TransportSubModeNameEnum.InternationalFlight),
    InternationalCharterAirService(1107, TransportModeNameEnum.Air, TransportSubModeNameEnum.InternationalFlight),
    RoundTripCharterAirService(1108, TransportModeNameEnum.Air),
    SightseeingAirService(1109, TransportModeNameEnum.Air),
    HelicopterAirService(1110, TransportModeNameEnum.Air, TransportSubModeNameEnum.HelicopterService),
    DomesticCharterAirService(1111, TransportModeNameEnum.Air),
    SchengenAreaAirService(1112, TransportModeNameEnum.Air, TransportSubModeNameEnum.InternationalFlight),
    AirshipService(1113, TransportModeNameEnum.Air),
    AllAirServices(1114, TransportModeNameEnum.Air),
    // extension
    FerryService(1200, TransportModeNameEnum.Ferry),
    // extension
    TelecabinService(1300, TransportModeNameEnum.Cableway),
    TelecabinService2(1301, TransportModeNameEnum.Cableway),
    CableCarService(1302, TransportModeNameEnum.Funicular),
    ElevatorService(1303, TransportModeNameEnum.Cableway),
    ChairLiftService(1304, TransportModeNameEnum.Cableway),
    DragLiftService(1305, TransportModeNameEnum.Cableway),
    SmallTelecabinService(1306, TransportModeNameEnum.Cableway),
    AllTelecabinServices(1307, TransportModeNameEnum.Cableway),
    // extension
    FunicularService(1400, TransportModeNameEnum.Funicular),
    FunicularService2(1401, TransportModeNameEnum.Funicular),
    AllFunicularService(1402, TransportModeNameEnum.Funicular),
    // extension
    TaxiService(1500, TransportModeNameEnum.Taxi),
    CommunalTaxiService(1501, TransportModeNameEnum.Taxi),
    WaterTaxiService(1502, TransportModeNameEnum.Water),
    RailTaxiService(1503, TransportModeNameEnum.Rail),
    BikeTaxiService(1504, TransportModeNameEnum.Bicycle),
    LicensedTaxiService(1505, TransportModeNameEnum.Taxi),
    PrivateHireServiceVehicle(1506, TransportModeNameEnum.Taxi),
    AllTaxiServices(1507, TransportModeNameEnum.Taxi),
    // extension
    SelfDrive(1600),
    HireCar(1601),
    HireVan(1602),
    HireMotorbike(1603),
    HireCycle(1604),
    // extension
    MiscellaneousService(1700),
    CableCar(1701, TransportModeNameEnum.Funicular),
    HorseDrawnCarriage(1702);
    
    public static final EnumSet<RouteTypeEnum> BASE_ROUTE_TYPES = EnumSet.of(
            Tram,
            Subway,
            Rail,
            Bus,
            Ferry,
            CableTram,
            AerialLift,
            Funicular,
            Trolleybus
    );
    
    private final int value;
    private TransportModeNameEnum transportMode;
    private TransportSubModeNameEnum subMode;

    RouteTypeEnum(final int value, TransportModeNameEnum transportMode, TransportSubModeNameEnum subMode) {
        this.value = value;
        this.transportMode = transportMode;
        this.subMode = subMode;
    }

    RouteTypeEnum(final int value, TransportModeNameEnum transportMode) {
        this.value = value;
        this.transportMode = transportMode;
    }

    RouteTypeEnum(final int value) {
        this.value = value;
    }

    public static RouteTypeEnum fromValue(final int value) {
        for (RouteTypeEnum c : RouteTypeEnum.values()) {
            if (c.value == value) {
                return c;
            }
        }
        throw new IllegalArgumentException(Integer.toString(value));
    }
    
    public static RouteTypeEnum fromTransportMode(TransportModeNameEnum transportMode) {
        if (transportMode == TransportModeNameEnum.Water) {
            // required because it would return Bus otherwise
            return Ferry;
        }
        if (transportMode == TransportModeNameEnum.Funicular) {
            // required because it would return CableTram otherwise
            return Funicular;
        }
        Optional<RouteTypeEnum> routeType = BASE_ROUTE_TYPES.stream().filter(rte -> rte.transportMode == transportMode).findFirst();
        if (!routeType.isPresent()) {
            log.warn(String.format("No routeType found for transportMode %s, return bus routeType by default", transportMode));
        }
       return routeType.orElse(Bus);
    }

    public static RouteTypeEnum fromTransportModeAndSubMode(TransportModeNameEnum transportMode, TransportSubModeNameEnum subMode) {
        if (transportMode != null && subMode != null) {
            for (RouteTypeEnum rte : RouteTypeEnum.values()) {
                if (rte.transportMode == transportMode && rte.subMode == subMode) {
                    return rte;
                }
            }
            log.warn(String.format("No routeType found for transportMode %s and subMode %s", transportMode, subMode));
        }
        return fromTransportMode(transportMode);
    }

}
