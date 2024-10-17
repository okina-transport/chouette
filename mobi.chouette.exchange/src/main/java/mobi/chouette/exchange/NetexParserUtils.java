package mobi.chouette.exchange;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import lombok.extern.log4j.Log4j;
import mobi.chouette.exchange.importer.ParserUtils;
import mobi.chouette.model.type.AlightingPossibilityEnum;
import mobi.chouette.model.type.BoardingAlightingPossibilityEnum;
import mobi.chouette.model.type.BoardingPossibilityEnum;
import mobi.chouette.model.type.BookingAccessEnum;
import mobi.chouette.model.type.BookingMethodEnum;
import mobi.chouette.model.type.DayTypeEnum;
import mobi.chouette.model.type.FlexibleLineTypeEnum;
import mobi.chouette.model.type.FlexibleServiceTypeEnum;
import mobi.chouette.model.type.OrganisationTypeEnum;
import mobi.chouette.model.type.PurchaseMomentEnum;
import mobi.chouette.model.type.PurchaseWhenEnum;
import mobi.chouette.model.type.ServiceAlterationEnum;
import mobi.chouette.model.type.TransportModeNameEnum;
import mobi.chouette.model.type.TransportSubModeNameEnum;

import org.rutebanken.netex.model.AirSubmodeEnumeration;
import org.rutebanken.netex.model.BookingAccessEnumeration;
import org.rutebanken.netex.model.BookingMethodEnumeration;
import org.rutebanken.netex.model.BusSubmodeEnumeration;
import org.rutebanken.netex.model.CoachSubmodeEnumeration;
import org.rutebanken.netex.model.DayOfWeekEnumeration;
import org.rutebanken.netex.model.EntityInVersionStructure;
import org.rutebanken.netex.model.FlexibleLineTypeEnumeration;
import org.rutebanken.netex.model.FlexibleServiceEnumeration;
import org.rutebanken.netex.model.FunicularSubmodeEnumeration;
import org.rutebanken.netex.model.MetroSubmodeEnumeration;
import org.rutebanken.netex.model.OrganisationTypeEnumeration;
import org.rutebanken.netex.model.PurchaseMomentEnumeration;
import org.rutebanken.netex.model.PurchaseWhenEnumeration;
import org.rutebanken.netex.model.RailSubmodeEnumeration;
import org.rutebanken.netex.model.ServiceAlterationEnumeration;
import org.rutebanken.netex.model.TelecabinSubmodeEnumeration;
import org.rutebanken.netex.model.TramSubmodeEnumeration;
import org.rutebanken.netex.model.TransportSubmodeStructure;
import org.rutebanken.netex.model.WaterSubmodeEnumeration;

@Log4j
public class NetexParserUtils extends ParserUtils {

    public static TransportModeNameEnum toTransportModeNameEnum(String value) {
        if (value == null)
            return null;
        else if (value.equals("air"))
            return TransportModeNameEnum.Air;
        else if (value.equals("rail"))
            return TransportModeNameEnum.Rail;
        else if (value.equals("metro"))
            return TransportModeNameEnum.Metro;
        else if (value.equals("tram"))
            return TransportModeNameEnum.Tram;
        else if (value.equals("coach"))
            return TransportModeNameEnum.Coach;
        else if (value.equals("bus"))
            return TransportModeNameEnum.Bus;
        else if (value.equals("water"))
            return TransportModeNameEnum.Water;
        else if (value.equals("ferry"))
            return TransportModeNameEnum.Ferry;
        else if (value.equals("trolleyBus"))
            return TransportModeNameEnum.TrolleyBus;
        else if (value.equals("taxi"))
            return TransportModeNameEnum.Taxi;
        else if (value.equals("cableway"))
            return TransportModeNameEnum.Cableway;
        else if (value.equals("funicular"))
            return TransportModeNameEnum.Funicular;
        else if (value.equals("lift"))
            return TransportModeNameEnum.Lift;
        else if (value.equals("unknown"))
            return TransportModeNameEnum.Other;
        else if (value.equals("bicycle"))
            return TransportModeNameEnum.Bicycle;
        else
            return TransportModeNameEnum.Other;
    }

    public static TransportSubModeNameEnum toTransportSubModeNameEnum(TransportSubmodeStructure subModeStructure) {
        if (subModeStructure != null) {
            if (subModeStructure.getAirSubmode() != null) {
                return getAirSubModeNameEnum(subModeStructure);
            } else if (subModeStructure.getBusSubmode() != null) {
                return getBusSubModeNameEnum(subModeStructure);
            } else if (subModeStructure.getCoachSubmode() != null) {
                return getCoachSubModeNameEnum(subModeStructure);
            } else if (subModeStructure.getFunicularSubmode() != null) {
                return getFunicularSubModeNameEnum(subModeStructure);
            } else if (subModeStructure.getMetroSubmode() != null) {
                return getMetroSubModeNameEnum(subModeStructure);
            } else if (subModeStructure.getRailSubmode() != null) {
                return getRailSubModeNameEnum(subModeStructure);
            } else if (subModeStructure.getTelecabinSubmode() != null) {
                return getTelecabinSubModeNameEnum(subModeStructure);
            } else if (subModeStructure.getTramSubmode() != null) {
                return getTramSubModeNameEnum(subModeStructure);
            } else if (subModeStructure.getWaterSubmode() != null) {
                return getWaterSubModeNameEnum(subModeStructure);
            }

        }

        return null;
    }

    public static ServiceAlterationEnum toServiceAlterationEum(ServiceAlterationEnumeration netexValue) {
        if (netexValue == null) {
            return null;
        }

        switch (netexValue) {
            case PLANNED:
                return ServiceAlterationEnum.Planned;
            case CANCELLATION:
                return ServiceAlterationEnum.Cancellation;
            case EXTRA_JOURNEY:
                return ServiceAlterationEnum.ExtraJourney;
            default:
                log.error("Unsupported NeTEx ServiceAlteration value: " + netexValue);
        }

        return null;
    }

    public static ZoneOffset getZoneOffset(ZoneId zoneId) {
        if (zoneId == null) {
            return null;
        }
        return zoneId.getRules().getOffset(Instant.now(Clock.system(zoneId)));
    }

    public static List<DayTypeEnum> convertDayOfWeek(DayOfWeekEnumeration dayOfWeek) {
        List<DayTypeEnum> days = new ArrayList<>();

        switch (dayOfWeek) {
            case MONDAY:
                days.add(DayTypeEnum.Monday);
                break;
            case TUESDAY:
                days.add(DayTypeEnum.Tuesday);
                break;
            case WEDNESDAY:
                days.add(DayTypeEnum.Wednesday);
                break;
            case THURSDAY:
                days.add(DayTypeEnum.Thursday);
                break;
            case FRIDAY:
                days.add(DayTypeEnum.Friday);
                break;
            case SATURDAY:
                days.add(DayTypeEnum.Saturday);
                break;
            case SUNDAY:
                days.add(DayTypeEnum.Sunday);
                break;
            case EVERYDAY:
                days.add(DayTypeEnum.Monday);
                days.add(DayTypeEnum.Tuesday);
                days.add(DayTypeEnum.Wednesday);
                days.add(DayTypeEnum.Thursday);
                days.add(DayTypeEnum.Friday);
                days.add(DayTypeEnum.Saturday);
                days.add(DayTypeEnum.Sunday);
                break;
            case WEEKDAYS:
                days.add(DayTypeEnum.Monday);
                days.add(DayTypeEnum.Tuesday);
                days.add(DayTypeEnum.Wednesday);
                days.add(DayTypeEnum.Thursday);
                days.add(DayTypeEnum.Friday);
                break;
            case WEEKEND:
                days.add(DayTypeEnum.Saturday);
                days.add(DayTypeEnum.Sunday);
                break;
            case NONE:
                // None
                break;
        }
        return days;
    }

    public static AlightingPossibilityEnum getForAlighting(BoardingAlightingPossibilityEnum boardingAlightingPossibility) {
        if (boardingAlightingPossibility == null)
            return AlightingPossibilityEnum.normal;
        switch (boardingAlightingPossibility) {
            case BoardAndAlight:
                return AlightingPossibilityEnum.normal;
            case AlightOnly:
                return AlightingPossibilityEnum.normal;
            case BoardOnly:
                return AlightingPossibilityEnum.forbidden;
            case NeitherBoardOrAlight:
                return AlightingPossibilityEnum.forbidden;
            case BoardAndAlightOnRequest:
                return AlightingPossibilityEnum.request_stop;
            case AlightOnRequest:
                return AlightingPossibilityEnum.request_stop;
            case BoardOnRequest:
                return AlightingPossibilityEnum.normal;
        }
        return null;
    }

    public static BoardingPossibilityEnum getForBoarding(BoardingAlightingPossibilityEnum boardingAlightingPossibility) {
        if (boardingAlightingPossibility == null)
            return BoardingPossibilityEnum.normal;
        switch (boardingAlightingPossibility) {
            case BoardAndAlight:
                return BoardingPossibilityEnum.normal;
            case AlightOnly:
                return BoardingPossibilityEnum.forbidden;
            case BoardOnly:
                return BoardingPossibilityEnum.normal;
            case NeitherBoardOrAlight:
                return BoardingPossibilityEnum.forbidden;
            case BoardAndAlightOnRequest:
                return BoardingPossibilityEnum.request_stop;
            case AlightOnRequest:
                return BoardingPossibilityEnum.normal;
            case BoardOnRequest:
                return BoardingPossibilityEnum.request_stop;
        }
        return null;
    }

    public static OrganisationTypeEnum getOrganisationType(OrganisationTypeEnumeration organisationTypeEnumeration) {
        if (organisationTypeEnumeration == null)
            return null;
        switch (organisationTypeEnumeration) {
            case AUTHORITY:
                return OrganisationTypeEnum.Authority;
            case OPERATOR:
                return OrganisationTypeEnum.Operator;
            default:
                // Passthrough
        }
        return null;
    }

    public static Integer getVersion(EntityInVersionStructure obj) {
        Integer version = 0;
        try {
            version = Integer.parseInt(obj.getVersion());
        } catch (NumberFormatException e) {
            log.debug("Unable to parse " + obj.getVersion() + " to Integer as supported by Neptune, returning 0");
        }
        return version;
    }

    public static Integer getVersion(String version) {
        try {
            return Integer.parseInt(version);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static String netexId(String objectIdPrefix, String elementName, String objectIdSuffix) {
        return objectIdPrefix + ":" + elementName + ":" + objectIdSuffix;
    }

    public static FlexibleLineTypeEnum toFlexibleLineType(FlexibleLineTypeEnumeration netexType) {
        if (netexType == null) {
            return null;
        }

        switch (netexType) {
            case CORRIDOR_SERVICE:
                return FlexibleLineTypeEnum.corridorService;
            case MAIN_ROUTE_WITH_FLEXIBLE_ENDS:
                return FlexibleLineTypeEnum.mainRouteWithFlexibleEnds;
            case FLEXIBLE_AREAS_ONLY:
                return FlexibleLineTypeEnum.flexibleAreasOnly;
            case HAIL_AND_RIDE_SECTIONS:
                return FlexibleLineTypeEnum.hailAndRideSections;
            case FIXED_STOP_AREA_WIDE:
                return FlexibleLineTypeEnum.fixedStopAreaWide;
            case FREE_AREA_AREA_WIDE:
                return FlexibleLineTypeEnum.freeAreaAreaWide;
            case MIXED_FLEXIBLE:
                return FlexibleLineTypeEnum.mixedFlexible;
            case MIXED_FLEXIBLE_AND_FIXED:
                return FlexibleLineTypeEnum.mixedFlexibleAndFixed;
            case FIXED:
                return FlexibleLineTypeEnum.fixed;
            case OTHER:
                return FlexibleLineTypeEnum.other;

        }
        return null;
    }

    public static BookingAccessEnum toBookingAccess(BookingAccessEnumeration netexType) {
        if (netexType == null) {
            return null;
        }

        switch (netexType) {
            case PUBLIC:
                return BookingAccessEnum.publicAccess;
            case AUTHORISED_PUBLIC:
                return BookingAccessEnum.authorisedPublic;
            case STAFF:
                return BookingAccessEnum.staff;
            case OTHER:
                return BookingAccessEnum.other;
        }
        return null;
    }

    public static BookingMethodEnum toBookingMethod(BookingMethodEnumeration netexType) {
        if (netexType == null) {
            return null;
        }

        switch (netexType) {
            case CALL_DRIVER:
                return BookingMethodEnum.callDriver;
            case CALL_OFFICE:
                return BookingMethodEnum.callOffice;
            case ONLINE:
                return BookingMethodEnum.online;
            case OTHER:
                return BookingMethodEnum.other;
            case PHONE_AT_STOP:
                return BookingMethodEnum.phoneAtStop;
            case TEXT:
                return BookingMethodEnum.text;
            case NONE:
                return BookingMethodEnum.none;
        }
        return null;
    }

    public static PurchaseWhenEnum toPurchaseWhen(PurchaseWhenEnumeration netexType) {
        if (netexType == null) {
            return null;
        }

        switch (netexType) {
            case TIME_OF_TRAVEL_ONLY:
                return PurchaseWhenEnum.timeOfTravelOnly;
            case DAY_OF_TRAVEL_ONLY:
                return PurchaseWhenEnum.dayOfTravelOnly;
            case UNTIL_PREVIOUS_DAY:
                return PurchaseWhenEnum.untilPreviousDay;
            case ADVANCE_ONLY:
                return PurchaseWhenEnum.advanceOnly;
            case ADVANCE_AND_DAY_OF_TRAVEL:
                return PurchaseWhenEnum.advanceAndDayOfTravel;
            case OTHER:
                return PurchaseWhenEnum.other;
        }
        return null;
    }


    public static PurchaseMomentEnum toPurchaseMoment(PurchaseMomentEnumeration netexType) {
        if (netexType == null) {
            return null;
        }

        switch (netexType) {
            case ON_RESERVATION:
                return PurchaseMomentEnum.onReservation;
            case BEFORE_BOARDING:
                return PurchaseMomentEnum.beforeBoarding;
            case ON_BOARDING:
                return PurchaseMomentEnum.onBoarding;
            case AFTER_BOARDING:
                return PurchaseMomentEnum.afterBoarding;
            case ON_CHECK_OUT:
                return PurchaseMomentEnum.onCheckOut;
            case OTHER:
                return PurchaseMomentEnum.other;
        }
        return null;
    }

    public static FlexibleServiceTypeEnum toFlexibleServiceType(FlexibleServiceEnumeration netexType) {
        if (netexType == null) {
            return null;
        }

        switch (netexType) {
            case DYNAMIC_PASSING_TIMES:
                return FlexibleServiceTypeEnum.dynamicPassingTimes;
            case FIXED_HEADWAY_FREQUENCY:
                return FlexibleServiceTypeEnum.fixedHeadwayFrequency;
            case FIXED_PASSING_TIMES:
                return FlexibleServiceTypeEnum.fixedPassingTimes;
            case NOT_FLEXIBLE:
                return FlexibleServiceTypeEnum.notFlexible;
            case OTHER:
                return FlexibleServiceTypeEnum.other;
        }
        return null;
    }

    protected static TransportSubModeNameEnum getWaterSubModeNameEnum(TransportSubmodeStructure subModeStructure) {
        WaterSubmodeEnumeration mode = subModeStructure.getWaterSubmode();
        switch (mode) {
            case AIRPORT_BOAT_LINK:
                return TransportSubModeNameEnum.AirportBoatLink;
            case CABLE_FERRY:
                return TransportSubModeNameEnum.CableFerry;
            case CANAL_BARGE:
                return TransportSubModeNameEnum.CanalBarge;
            case HIGH_SPEED_PASSENGER_SERVICE:
                return TransportSubModeNameEnum.HighSpeedPassengerService;
            case HIGH_SPEED_VEHICLE_SERVICE:
                return TransportSubModeNameEnum.HighSpeedVehicleService;
            case INTERNATIONAL_CAR_FERRY:
                return TransportSubModeNameEnum.InternationalCarFerry;
            case INTERNATIONAL_PASSENGER_FERRY:
                return TransportSubModeNameEnum.InternationalPassengerFerry;
            case LOCAL_CAR_FERRY:
                return TransportSubModeNameEnum.LocalCarFerry;
            case LOCAL_PASSENGER_FERRY:
                return TransportSubModeNameEnum.LocalPassengerFerry;
            case NATIONAL_CAR_FERRY:
                return TransportSubModeNameEnum.NationalCarFerry;
            case NATIONAL_PASSENGER_FERRY:
                return TransportSubModeNameEnum.NationalPassengerFerry;
            case POST_BOAT:
                return TransportSubModeNameEnum.PostBoat;
            case REGIONAL_CAR_FERRY:
                return TransportSubModeNameEnum.RegionalCarFerry;
            case REGIONAL_PASSENGER_FERRY:
                return TransportSubModeNameEnum.RegionalPassengerFerry;
            case RIVER_BUS:
                return TransportSubModeNameEnum.RiverBus;
            case ROAD_FERRY_LINK:
                return TransportSubModeNameEnum.RoadFerryLink;
            case SCHEDULED_FERRY:
                return TransportSubModeNameEnum.ScheduledFerry;
            case SCHOOL_BOAT:
                return TransportSubModeNameEnum.SchoolBoat;
            case SHUTTLE_FERRY_SERVICE:
                return TransportSubModeNameEnum.ShuttleFerryService;
            case SIGHTSEEING_SERVICE:
                return TransportSubModeNameEnum.SightseeingService;
            case TRAIN_FERRY:
                return TransportSubModeNameEnum.TrainFerry;
            default:
                log.error("Unsupported water sub mode " + mode);
        }
        return null;
    }

    protected static TransportSubModeNameEnum getTramSubModeNameEnum(TransportSubmodeStructure subModeStructure) {
        TramSubmodeEnumeration mode = subModeStructure.getTramSubmode();
        switch (mode) {
            case CITY_TRAM:
                return TransportSubModeNameEnum.CityTram;
            case LOCAL_TRAM:
                return TransportSubModeNameEnum.LocalTram;
            case REGIONAL_TRAM:
                return TransportSubModeNameEnum.RegionalTram;
            case SHUTTLE_TRAM:
                return TransportSubModeNameEnum.ShuttleTram;
            case SIGHTSEEING_TRAM:
                return TransportSubModeNameEnum.SightseeingTram;
            case TRAIN_TRAM:
                return TransportSubModeNameEnum.TrainTram;
            default:
                log.error("Unsupported tram sub mode " + mode);
        }
        return null;
    }

    protected static TransportSubModeNameEnum getTelecabinSubModeNameEnum(TransportSubmodeStructure subModeStructure) {
        TelecabinSubmodeEnumeration mode = subModeStructure.getTelecabinSubmode();
        switch (mode) {
            case CABLE_CAR:
                return TransportSubModeNameEnum.CableCar;
            case CHAIR_LIFT:
                return TransportSubModeNameEnum.ChairLift;
            case DRAG_LIFT:
                return TransportSubModeNameEnum.DragLift;
            case LIFT:
                return TransportSubModeNameEnum.Lift;
            case TELECABIN:
                return TransportSubModeNameEnum.Telecabin;
            case TELECABIN_LINK:
                return TransportSubModeNameEnum.TelecabinLink;
            default:
                log.error("Unsupported telecabin sub mode " + mode);
        }
        return null;
    }

    protected static TransportSubModeNameEnum getRailSubModeNameEnum(TransportSubmodeStructure subModeStructure) {
        RailSubmodeEnumeration mode = subModeStructure.getRailSubmode();
        switch (mode) {
            case AIRPORT_LINK_RAIL:
                return TransportSubModeNameEnum.AirportLinkRail;
            case CAR_TRANSPORT_RAIL_SERVICE:
                return TransportSubModeNameEnum.CarTransportRailService;
            case CROSS_COUNTRY_RAIL:
                return TransportSubModeNameEnum.CrossCountryRail;
            case HIGH_SPEED_RAIL:
                return TransportSubModeNameEnum.HighSpeedRail;
            case INTERNATIONAL:
                return TransportSubModeNameEnum.International;
            case INTERREGIONAL_RAIL:
                return TransportSubModeNameEnum.InterregionalRail;
            case LOCAL:
                return TransportSubModeNameEnum.Local;
            case LONG_DISTANCE:
                return TransportSubModeNameEnum.LongDistance;
            case NIGHT_RAIL:
                return TransportSubModeNameEnum.NightRail;
            case RACK_AND_PINION_RAILWAY:
                return TransportSubModeNameEnum.RackAndPinionRailway;
            case RAIL_SHUTTLE:
                return TransportSubModeNameEnum.RailShuttle;
            case REGIONAL_RAIL:
                return TransportSubModeNameEnum.RegionalRail;
            case REPLACEMENT_RAIL_SERVICE:
                return TransportSubModeNameEnum.ReplacementRailService;
            case SLEEPER_RAIL_SERVICE:
                return TransportSubModeNameEnum.SleeperRailService;
            case SPECIAL_TRAIN:
                return TransportSubModeNameEnum.SpecialTrain;
            case SUBURBAN_RAILWAY:
                return TransportSubModeNameEnum.SuburbanRailway;
            case TOURIST_RAILWAY:
                return TransportSubModeNameEnum.TouristRailway;
            default:
                log.error("Unsupported rail sub mode " + mode);
        }
        return null;
    }

    protected static TransportSubModeNameEnum getMetroSubModeNameEnum(TransportSubmodeStructure subModeStructure) {
        MetroSubmodeEnumeration mode = subModeStructure.getMetroSubmode();
        switch (mode) {
            case METRO:
                return TransportSubModeNameEnum.Metro;
            case TUBE:
                return TransportSubModeNameEnum.Tube;
            case URBAN_RAILWAY:
                return TransportSubModeNameEnum.UrbanRailway;
            default:
                log.error("Unsupported metro sub mode " + mode);
        }
        return null;
    }

    protected static TransportSubModeNameEnum getFunicularSubModeNameEnum(TransportSubmodeStructure subModeStructure) {
        FunicularSubmodeEnumeration mode = subModeStructure.getFunicularSubmode();
        switch (mode) {
            case ALL_FUNICULAR_SERVICES:
                return TransportSubModeNameEnum.AllFunicularServices;
            case FUNICULAR:
                return TransportSubModeNameEnum.Funicular;
            case STREET_CABLE_CAR:
                return TransportSubModeNameEnum.StreetCableCar;
            default:
                log.error("Unsupported funicular sub mode " + mode);
        }
        return null;
    }

    protected static TransportSubModeNameEnum getCoachSubModeNameEnum(TransportSubmodeStructure subModeStructure) {
        CoachSubmodeEnumeration mode = subModeStructure.getCoachSubmode();
        switch (mode) {
            case COMMUTER_COACH:
                return TransportSubModeNameEnum.CommuterCoach;
            case INTERNATIONAL_COACH:
                return TransportSubModeNameEnum.InternationalCoach;
            case NATIONAL_COACH:
                return TransportSubModeNameEnum.NationalCoach;
            case REGIONAL_COACH:
                return TransportSubModeNameEnum.RegionalCoach;
            case SCHOOL_COACH:
                return TransportSubModeNameEnum.SchoolCoach;
            case SHUTTLE_COACH:
                return TransportSubModeNameEnum.ShuttleCoach;
            case SIGHTSEEING_COACH:
                return TransportSubModeNameEnum.SightseeingCoach;
            case SPECIAL_COACH:
                return TransportSubModeNameEnum.SpecialCoach;
            case TOURIST_COACH:
                return TransportSubModeNameEnum.TouristCoach;
            default:
                log.error("Unsupported coach sub mode " + mode);
        }
        return null;
    }

    protected static TransportSubModeNameEnum getBusSubModeNameEnum(TransportSubmodeStructure subModeStructure) {
        BusSubmodeEnumeration mode = subModeStructure.getBusSubmode();
        switch (mode) {
            case AIRPORT_LINK_BUS:
                return TransportSubModeNameEnum.AirportLinkBus;
            case DEDICATED_LANE_BUS:
                return TransportSubModeNameEnum.DedicatedLaneBus;
            case DEMAND_AND_RESPONSE_BUS:
                return TransportSubModeNameEnum.DemandAndResponseBus;
            case EXPRESS_BUS:
                return TransportSubModeNameEnum.ExpressBus;
            case HIGH_FREQUENCY_BUS:
                return TransportSubModeNameEnum.HighFrequencyBus;
            case LOCAL_BUS:
                return TransportSubModeNameEnum.LocalBus;
            case MOBILITY_BUS:
                return TransportSubModeNameEnum.MobilityBus;
            case MOBILITY_BUS_FOR_REGISTERED_DISABLED:
                return TransportSubModeNameEnum.MobilityBusForRegisteredDisabled;
            case NIGHT_BUS:
                return TransportSubModeNameEnum.NightBus;
            case POST_BUS:
                return TransportSubModeNameEnum.PostBus;
            case RAIL_REPLACEMENT_BUS:
                return TransportSubModeNameEnum.RailReplacementBus;
            case REGIONAL_BUS:
                return TransportSubModeNameEnum.RegionalBus;
            case SCHOOL_BUS:
                return TransportSubModeNameEnum.SchoolBus;
            case SCHOOL_AND_PUBLIC_SERVICE_BUS:
                return TransportSubModeNameEnum.SchoolAndPublicServiceBus;
            case SHUTTLE_BUS:
                return TransportSubModeNameEnum.ShuttleBus;
            case SIGHTSEEING_BUS:
                return TransportSubModeNameEnum.SightseeingBus;
            case SPECIAL_NEEDS_BUS:
                return TransportSubModeNameEnum.SpecialNeedsBus;
            default:
                log.error("Unsupported bus sub mode " + mode);
        }
        return null;
    }

    protected static TransportSubModeNameEnum getAirSubModeNameEnum(TransportSubmodeStructure subModeStructure) {
        AirSubmodeEnumeration mode = subModeStructure.getAirSubmode();
        switch (mode) {
            case AIRSHIP_SERVICE:
                return TransportSubModeNameEnum.AirshipService;
            case DOMESTIC_CHARTER_FLIGHT:
                return TransportSubModeNameEnum.DomesticCharterFlight;
            case DOMESTIC_FLIGHT:
                return TransportSubModeNameEnum.DomesticFlight;
            case DOMESTIC_SCHEDULED_FLIGHT:
                return TransportSubModeNameEnum.DomesticScheduledFlight;
            case HELICOPTER_SERVICE:
                return TransportSubModeNameEnum.HelicopterService;
            case INTERCONTINENTAL_CHARTER_FLIGHT:
                return TransportSubModeNameEnum.IntercontinentalCharterFlight;
            case INTERCONTINENTAL_FLIGHT:
                return TransportSubModeNameEnum.IntercontinentalFlight;
            case INTERNATIONAL_CHARTER_FLIGHT:
                return TransportSubModeNameEnum.InternationalCharterFligth;
            case INTERNATIONAL_FLIGHT:
                return TransportSubModeNameEnum.InternationalFlight;
            case ROUND_TRIP_CHARTER_FLIGHT:
                return TransportSubModeNameEnum.RoundTripCharterFlight;
            case SCHENGEN_AREA_FLIGHT:
                return TransportSubModeNameEnum.SchengenAreaFlight;
            case SHORT_HAUL_INTERNATIONAL_FLIGHT:
                return TransportSubModeNameEnum.ShortHaulInternationalFlight;
            case SHUTTLE_FLIGHT:
                return TransportSubModeNameEnum.ShuttleFlight;
            case SIGHTSEEING_FLIGHT:
                return TransportSubModeNameEnum.SightseeingFlight;
            default:
                log.error("Unsupported air sub mode " + mode);
        }
        return null;
    }
}
