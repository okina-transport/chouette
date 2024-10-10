package mobi.chouette.model.type;

import java.util.EnumSet;
import java.util.Set;

public enum TransportSubModeNameEnum {

    /**
     * Bus sub modes
     */
    AirportLinkBus,
    DedicatedLaneBus,
    DemandAndResponseBus,
    ExpressBus,
    HighFrequencyBus,
    LocalBus,
    MobilityBus,
    MobilityBusForRegisteredDisabled,
    NightBus,
    PostBus,
    RailReplacementBus,
    RegionalBus,
    SchoolAndPublicServiceBus,
    SchoolBus,
    ShuttleBus,
    SightseeingBus,
    SpecialNeedsBus,

    /**
     * Tram sub modes
     */
    LocalTram,
    CityTram,
    RegionalTram,
    ShuttleTram,
    SightseeingTram,
    TrainTram,

    /**
     * Rail sub modes
     */
    AirportLinkRail,
    CarTransportRailService,
    CrossCountryRail,
    HighSpeedRail,
    International,
    InterregionalRail,
    Local,
    LongDistance,
    NightRail,
    RackAndPinionRailway,
    RailShuttle,
    RegionalRail,
    ReplacementRailService,
    SleeperRailService,
    SpecialTrain,
    SuburbanRailway,
    TouristRailway,

    /**
     * Metro sub modes
     */
    Metro,
    Tube,
    UrbanRailway,

    /**
     * Air sub modes
     */
    AirshipService,
    DomesticCharterFlight,
    DomesticFlight,
    DomesticScheduledFlight,
    HelicopterService,
    IntercontinentalCharterFlight,
    IntercontinentalFlight,
    InternationalCharterFligth,
    InternationalFlight,
    RoundTripCharterFlight,
    SchengenAreaFlight,
    ShortHaulInternationalFlight,
    ShuttleFlight,
    SightseeingFlight,

    /**
     * Water sub modes
     */
    AirportBoatLink,
    CableFerry,
    CanalBarge,
    HighSpeedPassengerService,
    HighSpeedVehicleService,
    InternationalCarFerry,
    InternationalPassengerFerry,
    LocalCarFerry,
    LocalPassengerFerry,
    NationalCarFerry,
    NationalPassengerFerry,
    PostBoat,
    RegionalCarFerry,
    RegionalPassengerFerry,
    RiverBus,
    RoadFerryLink,
    ScheduledFerry,
    SchoolBoat,
    ShuttleFerryService,
    SightseeingService,
    TrainFerry,

    /**
     * Cabelway sub modes
     */
    CableCar,
    ChairLift,
    DragLift,
    Lift,
    Telecabin,
    TelecabinLink,

    /**
     * Funicular sub modes
     */
    AllFunicularServices,
    Funicular,
    StreetCableCar,

	/**
	 * Coach sub modes
	 */
    CommuterCoach,
	InternationalCoach,
	NationalCoach,
    RegionalCoach,
    SchoolCoach,
    ShuttleCoach,
    SightseeingCoach,
    SpecialCoach,
	TouristCoach;

    public static final Set<TransportSubModeNameEnum> CABLEWAY_SUB_MODES = EnumSet.of(Telecabin);
    public static final Set<TransportSubModeNameEnum> FUNICULAR_SUB_MODES = EnumSet.of(Funicular);

    public static final Set<TransportSubModeNameEnum> AIR_SUB_MODES = EnumSet.of(
            AirshipService,
            DomesticCharterFlight,
            DomesticFlight,
            DomesticScheduledFlight,
            HelicopterService,
            IntercontinentalCharterFlight,
            IntercontinentalFlight,
            InternationalCharterFligth,
            InternationalFlight,
            RoundTripCharterFlight,
            SchengenAreaFlight,
            ShortHaulInternationalFlight,
            ShuttleFlight,
            SightseeingFlight
    );

    public static final Set<TransportSubModeNameEnum> RAIL_SUB_MODES = EnumSet.of(
            AirportLinkRail,
            CarTransportRailService,
            CrossCountryRail,
            HighSpeedRail,
            International,
            InterregionalRail,
            Local,
            LongDistance,
            NightRail,
            RackAndPinionRailway,
            RailShuttle,
            RegionalRail,
            ReplacementRailService,
            SleeperRailService,
            SpecialTrain,
            SuburbanRailway,
            TouristRailway
    );

    public static final Set<TransportSubModeNameEnum> FERRY_SUB_MODES = EnumSet.of(
            InternationalCarFerry,
            InternationalPassengerFerry,
            LocalCarFerry,
            LocalPassengerFerry,
            NationalCarFerry
    );

    public static final Set<TransportSubModeNameEnum> WATER_SUB_MODES = EnumSet.of(
            AirportBoatLink,
            CableFerry,
            CanalBarge,
            HighSpeedPassengerService,
            HighSpeedVehicleService,
            InternationalCarFerry,
            InternationalPassengerFerry,
            LocalCarFerry,
            LocalPassengerFerry,
            NationalCarFerry,
            NationalPassengerFerry,
            PostBoat,
            RegionalCarFerry,
            RegionalPassengerFerry,
            RiverBus,
            RoadFerryLink,
            ScheduledFerry,
            SchoolBoat,
            ShuttleFerryService,
            SightseeingService,
            TrainFerry
    );

    public static final Set<TransportSubModeNameEnum> BUS_SUB_MODES = EnumSet.of(
            AirportLinkBus,
            DedicatedLaneBus,
            DemandAndResponseBus,
            ExpressBus,
            HighFrequencyBus,
            LocalBus,
            MobilityBus,
            MobilityBusForRegisteredDisabled,
            NightBus,
            PostBus,
            RailReplacementBus,
            RegionalBus,
            SchoolAndPublicServiceBus,
            SchoolBus,
            ShuttleBus,
            SightseeingBus,
            SpecialNeedsBus
    );

    public static final Set<TransportSubModeNameEnum> COACH_SUB_MODES = EnumSet.of(
            InternationalCoach,
            NationalCoach,
            TouristCoach
    );

    public static final Set<TransportSubModeNameEnum> TRAM_SUB_MODES = EnumSet.of(
            LocalTram,
            CityTram,
            RegionalTram,
            ShuttleTram,
            SightseeingTram,
            TrainTram
    );

}
