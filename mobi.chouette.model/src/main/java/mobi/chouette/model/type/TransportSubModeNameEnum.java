package mobi.chouette.model.type;

import java.util.EnumSet;

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
    NightBus,
    RailReplacementBus,
    RegionalBus,
    SchoolAndPublicServiceBus,
    SchoolBus,
    ShuttleBus,
    SpecialNeedsBus,
    SightseeingBus,

    /**
     * Tram sub modes
     */
    LocalTram,
    CityTram,

    /**
     * Rail sub modes
     */
    International,
    InterregionalRail,
    Local,
    LongDistance,
    NightRail,
    RegionalRail,
    TouristRailway,
    AirportLinkRail,
    HighSpeedRail,
    SuburbanRailway,
    SleeperRailService,
    CarTransportRailService,
    RailShuttle,
    RackAndPinionRailway,

    /**
     * Metro sub modes
     */
    Metro,

    /**
     * Air sub modes
     */
    DomesticFlight,
    HelicopterService,
    InternationalFlight,

    /**
     * Water sub modes
     */
    HighSpeedPassengerService,
    HighSpeedVehicleService,
    InternationalCarFerry,
    InternationalPassengerFerry,
    LocalCarFerry,
    LocalPassengerFerry,
    NationalCarFerry,
    SightseeingService,

    /**
     * Cabelway sub modes
     */
    Telecabin,

    /**
     * Funicular sub modes
     */
    Funicular,
	
	/**
	 * Coach sub modes
	 */
	InternationalCoach,
	NationalCoach,
	TouristCoach;

    public static final EnumSet<TransportSubModeNameEnum> CABLEWAY_SUB_MODES = EnumSet.of(Telecabin);
    public static final EnumSet<TransportSubModeNameEnum> FUNICULAR_SUB_MODES = EnumSet.of(Funicular);

    public static final EnumSet<TransportSubModeNameEnum> AIR_SUB_MODES = EnumSet.of(
            DomesticFlight,
            HelicopterService,
            InternationalFlight
    );

    public static final EnumSet<TransportSubModeNameEnum> RAIL_SUB_MODES = EnumSet.of(
            International,
            InterregionalRail,
            Local,
            LongDistance,
            NightRail,
            RegionalRail,
            TouristRailway,
            AirportLinkRail,
            HighSpeedRail,
            SuburbanRailway,
            SleeperRailService,
            CarTransportRailService,
            RailShuttle,
            RackAndPinionRailway
    );

    public static final EnumSet<TransportSubModeNameEnum> FERRY_SUB_MODES = EnumSet.of(
            InternationalCarFerry,
            InternationalPassengerFerry,
            LocalCarFerry,
            LocalPassengerFerry,
            NationalCarFerry
    );

    public static final EnumSet<TransportSubModeNameEnum> WATER_SUB_MODES = EnumSet.of(
            HighSpeedPassengerService,
            HighSpeedVehicleService,
            InternationalCarFerry,
            InternationalPassengerFerry,
            LocalCarFerry,
            LocalPassengerFerry,
            NationalCarFerry,
            SightseeingService
    );

    public static final EnumSet<TransportSubModeNameEnum> BUS_SUB_MODES = EnumSet.of(
            AirportLinkBus,
            DedicatedLaneBus,
            DemandAndResponseBus,
            ExpressBus,
            HighFrequencyBus,
            LocalBus,
            MobilityBus,
            NightBus,
            RailReplacementBus,
            RegionalBus,
            SchoolAndPublicServiceBus,
            SchoolBus,
            ShuttleBus,
            SpecialNeedsBus,
            SightseeingBus
    );

    public static final EnumSet<TransportSubModeNameEnum> COACH_SUB_MODES = EnumSet.of(
            InternationalCoach,
            NationalCoach,
            TouristCoach
    );

    public static final EnumSet<TransportSubModeNameEnum> TRAM_SUB_MODES = EnumSet.of(
            LocalTram,
            CityTram
    );

}
