package mobi.chouette.exchange.netexprofile;

import lombok.extern.log4j.Log4j;
import mobi.chouette.model.type.*;
import org.apache.commons.lang.StringUtils;
import org.rutebanken.netex.model.*;

import java.math.BigInteger;
import java.time.*;
import java.util.ArrayList;
import java.util.List;

import static mobi.chouette.common.TimeUtil.toLocalTimeFromJoda;

@Log4j
public class ConversionUtil {

	private ConversionUtil() {
		throw new IllegalStateException("Utility class");
	}

	public static MultilingualString getMultiLingualString(String v) {
		if (v == null) {
			return null;
		} else {
			return new MultilingualString().withValue(v);
		}
	}

	public static Integer asInteger(BigInteger v) {
		if (v == null) {
			return null;
		} else {
			return v.intValue();
		}
	}

	public static BigInteger asBigInteger(Integer v) {
		if (v == null) {
			return null;
		} else {
			return BigInteger.valueOf(v.longValue());
		}
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

	public static AllVehicleModesOfTransportEnumeration toVehicleModeOfTransportEnum(TransportModeNameEnum value) {
		if (value == null)
			return null;
		switch (value) {
			case Air:
				return AllVehicleModesOfTransportEnumeration.AIR;
			case Bus:
				return AllVehicleModesOfTransportEnumeration.BUS;
			case Coach:
				return AllVehicleModesOfTransportEnumeration.COACH;
			case Metro:
				return AllVehicleModesOfTransportEnumeration.METRO;
			case Rail:
				return AllVehicleModesOfTransportEnumeration.RAIL;
			case TrolleyBus:
				return AllVehicleModesOfTransportEnumeration.TROLLEY_BUS;
			case Tram:
				return AllVehicleModesOfTransportEnumeration.TRAM;
			case Water:
			case Ferry:
				return AllVehicleModesOfTransportEnumeration.WATER;
			case Lift:
			case Cableway:
				return AllVehicleModesOfTransportEnumeration.CABLEWAY;
			case Funicular:
				return AllVehicleModesOfTransportEnumeration.FUNICULAR;
			case Taxi:
				return AllVehicleModesOfTransportEnumeration.TAXI;

			case Bicycle:
			case Other:
			default:
				return AllVehicleModesOfTransportEnumeration.UNKNOWN;

		}

	}

	public static TransportSubmodeStructure toTransportSubmodeStructure(TransportSubModeNameEnum transportSubMode) {
		if (transportSubMode == null) {
			return null;
		} else {
			switch (transportSubMode) {

				/**
				 * Bus sub modes
				 */
				case AirportLinkBus:
					return new TransportSubmodeStructure().withBusSubmode(BusSubmodeEnumeration.AIRPORT_LINK_BUS);
				case DedicatedLaneBus:
					return new TransportSubmodeStructure().withBusSubmode(BusSubmodeEnumeration.DEDICATED_LANE_BUS);
				case DemandAndResponseBus:
					return new TransportSubmodeStructure().withBusSubmode(BusSubmodeEnumeration.DEMAND_AND_RESPONSE_BUS);
				case ExpressBus:
					return new TransportSubmodeStructure().withBusSubmode(BusSubmodeEnumeration.EXPRESS_BUS);
				case HighFrequencyBus:
					return new TransportSubmodeStructure().withBusSubmode(BusSubmodeEnumeration.HIGH_FREQUENCY_BUS);
				case LocalBus:
					return new TransportSubmodeStructure().withBusSubmode(BusSubmodeEnumeration.LOCAL_BUS);
				case MobilityBus:
					return new TransportSubmodeStructure().withBusSubmode(BusSubmodeEnumeration.MOBILITY_BUS);
				case MobilityBusForRegisteredDisabled:
					return new TransportSubmodeStructure().withBusSubmode(BusSubmodeEnumeration.MOBILITY_BUS_FOR_REGISTERED_DISABLED);
				case NightBus:
					return new TransportSubmodeStructure().withBusSubmode(BusSubmodeEnumeration.NIGHT_BUS);
				case PostBus:
					return new TransportSubmodeStructure().withBusSubmode(BusSubmodeEnumeration.POST_BUS);
				case RailReplacementBus:
					return new TransportSubmodeStructure().withBusSubmode(BusSubmodeEnumeration.RAIL_REPLACEMENT_BUS);
				case RegionalBus:
					return new TransportSubmodeStructure().withBusSubmode(BusSubmodeEnumeration.REGIONAL_BUS);
				case SchoolAndPublicServiceBus:
					return new TransportSubmodeStructure().withBusSubmode(BusSubmodeEnumeration.SCHOOL_AND_PUBLIC_SERVICE_BUS);
				case SchoolBus:
					return new TransportSubmodeStructure().withBusSubmode(BusSubmodeEnumeration.SCHOOL_BUS);
				case ShuttleBus:
					return new TransportSubmodeStructure().withBusSubmode(BusSubmodeEnumeration.SHUTTLE_BUS);
				case SpecialNeedsBus:
					return new TransportSubmodeStructure().withBusSubmode(BusSubmodeEnumeration.SPECIAL_NEEDS_BUS);
				case SightseeingBus:
					return new TransportSubmodeStructure().withBusSubmode(BusSubmodeEnumeration.SIGHTSEEING_BUS);


				/**
				 * Coach sub modes
				 */
				case CommuterCoach:
					return new TransportSubmodeStructure().withCoachSubmode(CoachSubmodeEnumeration.COMMUTER_COACH);
				case InternationalCoach:
					return new TransportSubmodeStructure().withCoachSubmode(CoachSubmodeEnumeration.INTERNATIONAL_COACH);
				case NationalCoach:
					return new TransportSubmodeStructure().withCoachSubmode(CoachSubmodeEnumeration.NATIONAL_COACH);
				case RegionalCoach:
					return new TransportSubmodeStructure().withCoachSubmode(CoachSubmodeEnumeration.REGIONAL_COACH);
				case SchoolCoach:
					return new TransportSubmodeStructure().withCoachSubmode(CoachSubmodeEnumeration.SCHOOL_COACH);
				case ShuttleCoach:
					return new TransportSubmodeStructure().withCoachSubmode(CoachSubmodeEnumeration.SHUTTLE_COACH);
				case SightseeingCoach:
					return new TransportSubmodeStructure().withCoachSubmode(CoachSubmodeEnumeration.SIGHTSEEING_COACH);
				case SpecialCoach:
					return new TransportSubmodeStructure().withCoachSubmode(CoachSubmodeEnumeration.SPECIAL_COACH);
				case TouristCoach:
					return new TransportSubmodeStructure().withCoachSubmode(CoachSubmodeEnumeration.TOURIST_COACH);
				/**
				 * Tram sub modes
				 */
				case LocalTram:
					return new TransportSubmodeStructure().withTramSubmode(TramSubmodeEnumeration.LOCAL_TRAM);
				case CityTram:
					return new TransportSubmodeStructure().withTramSubmode(TramSubmodeEnumeration.CITY_TRAM);
				case RegionalTram:
					return new TransportSubmodeStructure().withTramSubmode(TramSubmodeEnumeration.REGIONAL_TRAM);
				case ShuttleTram:
					return new TransportSubmodeStructure().withTramSubmode(TramSubmodeEnumeration.SHUTTLE_TRAM);
				case SightseeingTram:
					return new TransportSubmodeStructure().withTramSubmode(TramSubmodeEnumeration.SIGHTSEEING_TRAM);
				case TrainTram:
					return new TransportSubmodeStructure().withTramSubmode(TramSubmodeEnumeration.TRAIN_TRAM);

				/**
				 * Rail sub modes
				 */
				case AirportLinkRail:
					return new TransportSubmodeStructure().withRailSubmode(RailSubmodeEnumeration.AIRPORT_LINK_RAIL);
				case CarTransportRailService:
					return new TransportSubmodeStructure().withRailSubmode(RailSubmodeEnumeration.CAR_TRANSPORT_RAIL_SERVICE);
				case CrossCountryRail:
					return new TransportSubmodeStructure().withRailSubmode(RailSubmodeEnumeration.CROSS_COUNTRY_RAIL);
				case HighSpeedRail:
					return new TransportSubmodeStructure().withRailSubmode(RailSubmodeEnumeration.HIGH_SPEED_RAIL);
				case International:
					return new TransportSubmodeStructure().withRailSubmode(RailSubmodeEnumeration.INTERNATIONAL);
				case InterregionalRail:
					return new TransportSubmodeStructure().withRailSubmode(RailSubmodeEnumeration.INTERREGIONAL_RAIL);
				case Local:
					return new TransportSubmodeStructure().withRailSubmode(RailSubmodeEnumeration.LOCAL);
				case LongDistance:
					return new TransportSubmodeStructure().withRailSubmode(RailSubmodeEnumeration.LONG_DISTANCE);
				case NightRail:
					return new TransportSubmodeStructure().withRailSubmode(RailSubmodeEnumeration.NIGHT_RAIL);
				case RackAndPinionRailway:
					return new TransportSubmodeStructure().withRailSubmode(RailSubmodeEnumeration.RACK_AND_PINION_RAILWAY);
				case RailShuttle:
					return new TransportSubmodeStructure().withRailSubmode(RailSubmodeEnumeration.RAIL_SHUTTLE);
				case RegionalRail:
					return new TransportSubmodeStructure().withRailSubmode(RailSubmodeEnumeration.REGIONAL_RAIL);
				case ReplacementRailService:
					return new TransportSubmodeStructure().withRailSubmode(RailSubmodeEnumeration.REPLACEMENT_RAIL_SERVICE);
				case SleeperRailService:
					return new TransportSubmodeStructure().withRailSubmode(RailSubmodeEnumeration.SLEEPER_RAIL_SERVICE);
				case SpecialTrain:
					return new TransportSubmodeStructure().withRailSubmode(RailSubmodeEnumeration.SPECIAL_TRAIN);
				case SuburbanRailway:
					return new TransportSubmodeStructure().withRailSubmode(RailSubmodeEnumeration.SUBURBAN_RAILWAY);
				case TouristRailway:
					return new TransportSubmodeStructure().withRailSubmode(RailSubmodeEnumeration.TOURIST_RAILWAY);

				/**
				 * Metro sub modes
				 */
				case Metro:
					return new TransportSubmodeStructure().withMetroSubmode(MetroSubmodeEnumeration.METRO);
				case Tube:
					return new TransportSubmodeStructure().withMetroSubmode(MetroSubmodeEnumeration.TUBE);
				case UrbanRailway:
					return new TransportSubmodeStructure().withMetroSubmode(MetroSubmodeEnumeration.URBAN_RAILWAY);

				/**
				 * Air sub modes
				 */
				case AirshipService:
					return new TransportSubmodeStructure().withAirSubmode(AirSubmodeEnumeration.AIRSHIP_SERVICE);
				case DomesticCharterFlight:
					return new TransportSubmodeStructure().withAirSubmode(AirSubmodeEnumeration.DOMESTIC_CHARTER_FLIGHT);
				case DomesticFlight:
					return new TransportSubmodeStructure().withAirSubmode(AirSubmodeEnumeration.DOMESTIC_FLIGHT);
				case DomesticScheduledFlight:
					return new TransportSubmodeStructure().withAirSubmode(AirSubmodeEnumeration.DOMESTIC_SCHEDULED_FLIGHT);
				case HelicopterService:
					return new TransportSubmodeStructure().withAirSubmode(AirSubmodeEnumeration.HELICOPTER_SERVICE);
				case IntercontinentalCharterFlight:
					return new TransportSubmodeStructure().withAirSubmode(AirSubmodeEnumeration.INTERCONTINENTAL_CHARTER_FLIGHT);
				case IntercontinentalFlight:
					return new TransportSubmodeStructure().withAirSubmode(AirSubmodeEnumeration.INTERCONTINENTAL_FLIGHT);
				case InternationalCharterFligth:
					return new TransportSubmodeStructure().withAirSubmode(AirSubmodeEnumeration.INTERNATIONAL_CHARTER_FLIGHT);
				case InternationalFlight:
					return new TransportSubmodeStructure().withAirSubmode(AirSubmodeEnumeration.INTERNATIONAL_FLIGHT);
				case RoundTripCharterFlight:
					return new TransportSubmodeStructure().withAirSubmode(AirSubmodeEnumeration.ROUND_TRIP_CHARTER_FLIGHT);
				case SchengenAreaFlight:
					return new TransportSubmodeStructure().withAirSubmode(AirSubmodeEnumeration.SCHENGEN_AREA_FLIGHT);
				case ShortHaulInternationalFlight:
					return new TransportSubmodeStructure().withAirSubmode(AirSubmodeEnumeration.SHORT_HAUL_INTERNATIONAL_FLIGHT);
				case ShuttleFlight:
					return new TransportSubmodeStructure().withAirSubmode(AirSubmodeEnumeration.SHUTTLE_FLIGHT);
				case SightseeingFlight:
					return new TransportSubmodeStructure().withAirSubmode(AirSubmodeEnumeration.SIGHTSEEING_FLIGHT);

				/**
				 * Water sub modes
				 */
				case AirportBoatLink:
					return new TransportSubmodeStructure().withWaterSubmode(WaterSubmodeEnumeration.AIRPORT_BOAT_LINK);
				case CableFerry:
					return new TransportSubmodeStructure().withWaterSubmode(WaterSubmodeEnumeration.CABLE_FERRY);
				case CanalBarge:
					return new TransportSubmodeStructure().withWaterSubmode(WaterSubmodeEnumeration.CANAL_BARGE);
				case HighSpeedPassengerService:
					return new TransportSubmodeStructure().withWaterSubmode(WaterSubmodeEnumeration.HIGH_SPEED_PASSENGER_SERVICE);
				case HighSpeedVehicleService:
					return new TransportSubmodeStructure().withWaterSubmode(WaterSubmodeEnumeration.HIGH_SPEED_VEHICLE_SERVICE);
				case InternationalCarFerry:
					return new TransportSubmodeStructure().withWaterSubmode(WaterSubmodeEnumeration.INTERNATIONAL_CAR_FERRY);
				case InternationalPassengerFerry:
					return new TransportSubmodeStructure().withWaterSubmode(WaterSubmodeEnumeration.INTERNATIONAL_PASSENGER_FERRY);
				case LocalCarFerry:
					return new TransportSubmodeStructure().withWaterSubmode(WaterSubmodeEnumeration.LOCAL_CAR_FERRY);
				case LocalPassengerFerry:
					return new TransportSubmodeStructure().withWaterSubmode(WaterSubmodeEnumeration.LOCAL_PASSENGER_FERRY);
				case NationalCarFerry:
					return new TransportSubmodeStructure().withWaterSubmode(WaterSubmodeEnumeration.NATIONAL_CAR_FERRY);
				case NationalPassengerFerry:
					return new TransportSubmodeStructure().withWaterSubmode(WaterSubmodeEnumeration.NATIONAL_PASSENGER_FERRY);
				case PostBoat:
					return new TransportSubmodeStructure().withWaterSubmode(WaterSubmodeEnumeration.POST_BOAT);
				case RegionalCarFerry:
					return new TransportSubmodeStructure().withWaterSubmode(WaterSubmodeEnumeration.REGIONAL_CAR_FERRY);
				case RegionalPassengerFerry:
					return new TransportSubmodeStructure().withWaterSubmode(WaterSubmodeEnumeration.REGIONAL_PASSENGER_FERRY);
				case RiverBus:
					return new TransportSubmodeStructure().withWaterSubmode(WaterSubmodeEnumeration.RIVER_BUS);
				case RoadFerryLink:
					return new TransportSubmodeStructure().withWaterSubmode(WaterSubmodeEnumeration.ROAD_FERRY_LINK);
				case ScheduledFerry:
					return new TransportSubmodeStructure().withWaterSubmode(WaterSubmodeEnumeration.SCHEDULED_FERRY);
				case SchoolBoat:
					return new TransportSubmodeStructure().withWaterSubmode(WaterSubmodeEnumeration.SCHOOL_BOAT);
				case ShuttleFerryService:
					return new TransportSubmodeStructure().withWaterSubmode(WaterSubmodeEnumeration.SHUTTLE_FERRY_SERVICE);
				case SightseeingService:
					return new TransportSubmodeStructure().withWaterSubmode(WaterSubmodeEnumeration.SIGHTSEEING_SERVICE);
				case TrainFerry:
					return new TransportSubmodeStructure().withWaterSubmode(WaterSubmodeEnumeration.TRAIN_FERRY);

				/**
				 * Cabelway sub modes
				 */
				case CableCar:
					return new TransportSubmodeStructure().withTelecabinSubmode(TelecabinSubmodeEnumeration.CABLE_CAR);
				case ChairLift:
					return new TransportSubmodeStructure().withTelecabinSubmode(TelecabinSubmodeEnumeration.CHAIR_LIFT);
				case DragLift:
					return new TransportSubmodeStructure().withTelecabinSubmode(TelecabinSubmodeEnumeration.DRAG_LIFT);
				case Lift:
					return new TransportSubmodeStructure().withTelecabinSubmode(TelecabinSubmodeEnumeration.LIFT);
				case Telecabin:
					return new TransportSubmodeStructure().withTelecabinSubmode(TelecabinSubmodeEnumeration.TELECABIN);
				case TelecabinLink:
					return new TransportSubmodeStructure().withTelecabinSubmode(TelecabinSubmodeEnumeration.TELECABIN_LINK);

				/**
				 * Funicular sub modes
				 */
				case AllFunicularServices:
					return new TransportSubmodeStructure().withFunicularSubmode(FunicularSubmodeEnumeration.ALL_FUNICULAR_SERVICES);
				case Funicular:
					return new TransportSubmodeStructure().withFunicularSubmode(FunicularSubmodeEnumeration.FUNICULAR);
				case StreetCableCar:
					return new TransportSubmodeStructure().withFunicularSubmode(FunicularSubmodeEnumeration.STREET_CABLE_CAR);

				default:
					// Fall through
			}
		}

		return null;
	}

	public static ServiceAlterationEnumeration toServiceAlterationEnumeration(ServiceAlterationEnum chouetteValue) {
		if (chouetteValue == null) {
			return null;
		}
		switch (chouetteValue) {
			case Planned:
				return ServiceAlterationEnumeration.PLANNED;
			case Cancellation:
				return ServiceAlterationEnumeration.CANCELLATION;
			case ExtraJourney:
				return ServiceAlterationEnumeration.EXTRA_JOURNEY;
			default:
				log.error("Unsupported Chouette ServiceAlteration value: " + chouetteValue);
		}

		return null;
	}

	public static String getValue(MultilingualString m) {
		String v = null;
		if (m != null) {
			v = StringUtils.trimToNull(m.getValue());
		}

		return v;

	}

	public static OffsetTime toOffsetTimeUtc(org.joda.time.LocalTime time) {
		return time == null ? null
				: toLocalTimeFromJoda(time).atOffset(ConversionUtil.getZoneOffset(ConversionUtil.LOCAL_ZONE_ID)).withOffsetSameInstant(ZoneOffset.UTC);
	}

	public static final ZoneId LOCAL_ZONE_ID = ZoneId.of("Europe/Paris");

	public static ZoneOffset getZoneOffset(ZoneId zoneId) {
		return zoneId == null ? null : zoneId.getRules().getOffset(Instant.now(Clock.system(zoneId)));
	}

	public static FlexibleLineTypeEnumeration toFlexibleLineType(FlexibleLineTypeEnum chouetteType) {
		if (chouetteType == null) {
			return null;
		}

		switch (chouetteType) {
			case corridorService:
				return FlexibleLineTypeEnumeration.CORRIDOR_SERVICE;
			case mainRouteWithFlexibleEnds:
				return FlexibleLineTypeEnumeration.MAIN_ROUTE_WITH_FLEXIBLE_ENDS;
			case flexibleAreasOnly:
				return FlexibleLineTypeEnumeration.FLEXIBLE_AREAS_ONLY;
			case hailAndRideSections:
				return FlexibleLineTypeEnumeration.HAIL_AND_RIDE_SECTIONS;
			case fixedStopAreaWide:
				return FlexibleLineTypeEnumeration.FIXED_STOP_AREA_WIDE;
			case freeAreaAreaWide:
				return FlexibleLineTypeEnumeration.FREE_AREA_AREA_WIDE;
			case mixedFlexible:
				return FlexibleLineTypeEnumeration.MIXED_FLEXIBLE;
			case mixedFlexibleAndFixed:
				return FlexibleLineTypeEnumeration.MIXED_FLEXIBLE_AND_FIXED;
			case fixed:
				return FlexibleLineTypeEnumeration.FIXED;
			case other:
				return FlexibleLineTypeEnumeration.OTHER;

		}
		return null;
	}

	public static BookingAccessEnumeration toBookingAccess(BookingAccessEnum chouetteType) {
		if (chouetteType == null) {
			return null;
		}

		switch (chouetteType) {
			case publicAccess:
				return BookingAccessEnumeration.PUBLIC;
			case authorisedPublic:
				return BookingAccessEnumeration.AUTHORISED_PUBLIC;
			case staff:
				return BookingAccessEnumeration.STAFF;
			case other:
				return BookingAccessEnumeration.OTHER;
		}
		return null;
	}

	public static BookingMethodEnumeration toBookingMethod(BookingMethodEnum chouetteType) {
		if (chouetteType == null) {
			return null;
		}

		switch (chouetteType) {
			case callDriver:
				return BookingMethodEnumeration.CALL_DRIVER;
			case callOffice:
				return BookingMethodEnumeration.CALL_OFFICE;
			case online:
				return BookingMethodEnumeration.ONLINE;
			case other:
				return BookingMethodEnumeration.OTHER;
			case phoneAtStop:
				return BookingMethodEnumeration.PHONE_AT_STOP;
			case text:
				return BookingMethodEnumeration.TEXT;
			case none:
				return BookingMethodEnumeration.NONE;
		}
		return null;
	}

	public static PurchaseWhenEnumeration toPurchaseWhen(PurchaseWhenEnum chouetteType) {
		if (chouetteType == null) {
			return null;
		}

		switch (chouetteType) {
			case timeOfTravelOnly:
				return PurchaseWhenEnumeration.TIME_OF_TRAVEL_ONLY;
			case dayOfTravelOnly:
				return PurchaseWhenEnumeration.DAY_OF_TRAVEL_ONLY;
			case untilPreviousDay:
				return PurchaseWhenEnumeration.UNTIL_PREVIOUS_DAY;
			case advanceOnly:
				return PurchaseWhenEnumeration.ADVANCE_ONLY;
			case advanceAndDayOfTravel:
				return PurchaseWhenEnumeration.ADVANCE_AND_DAY_OF_TRAVEL;
			case other:
				return PurchaseWhenEnumeration.OTHER;
		}
		return null;
	}


	public static FlexibleServiceEnumeration toFlexibleServiceType(FlexibleServiceTypeEnum chouetteType) {
		if (chouetteType == null) {
			return null;
		}

		switch (chouetteType) {
			case dynamicPassingTimes:
				return FlexibleServiceEnumeration.DYNAMIC_PASSING_TIMES;
			case fixedPassingTimes:
				return FlexibleServiceEnumeration.FIXED_PASSING_TIMES;
			case fixedHeadwayFrequency:
				return FlexibleServiceEnumeration.FIXED_HEADWAY_FREQUENCY;
			case notFlexible:
				return FlexibleServiceEnumeration.NOT_FLEXIBLE;
			case other:
				return FlexibleServiceEnumeration.OTHER;
		}
		return null;
	}

	public static PurchaseMomentEnumeration toPurchaseMoment(PurchaseMomentEnum chouetteType) {
		if (chouetteType == null) {
			return null;
		}

		switch (chouetteType) {
			case onReservation:
				return PurchaseMomentEnumeration.ON_RESERVATION;
			case beforeBoarding:
				return PurchaseMomentEnumeration.BEFORE_BOARDING;
			case onBoarding:
				return PurchaseMomentEnumeration.ON_BOARDING;
			case afterBoarding:
				return PurchaseMomentEnumeration.AFTER_BOARDING;
			case onCheckOut:
				return PurchaseMomentEnumeration.ON_CHECK_OUT;
			case other:
				return PurchaseMomentEnumeration.OTHER;
		}
		return null;
	}

}
