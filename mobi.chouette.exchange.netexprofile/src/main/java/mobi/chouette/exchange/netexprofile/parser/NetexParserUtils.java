package mobi.chouette.exchange.netexprofile.parser;

import lombok.extern.log4j.Log4j;
import mobi.chouette.exchange.importer.ParserUtils;
import mobi.chouette.model.type.*;
import org.rutebanken.netex.model.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

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
				AirSubmodeEnumeration mode = subModeStructure.getAirSubmode();
				switch (mode) {
					case DOMESTIC_FLIGHT:
						return TransportSubModeNameEnum.DomesticFlight;
					case HELICOPTER_SERVICE:
						return TransportSubModeNameEnum.HelicopterService;
					case INTERNATIONAL_FLIGHT:
						return TransportSubModeNameEnum.InternationalFlight;
					default:
						log.warn("Unsupported air sub mode " + mode);
				}
			} else if (subModeStructure.getBusSubmode() != null) {
				BusSubmodeEnumeration mode = subModeStructure.getBusSubmode();
				switch (mode) {
					case AIRPORT_LINK_BUS:
						return TransportSubModeNameEnum.AirportLinkBus;
					case EXPRESS_BUS:
						return TransportSubModeNameEnum.ExpressBus;
					case LOCAL_BUS:
						return TransportSubModeNameEnum.LocalBus;
					case NIGHT_BUS:
						return TransportSubModeNameEnum.NightBus;
					case RAIL_REPLACEMENT_BUS:
						return TransportSubModeNameEnum.RailReplacementBus;
					case REGIONAL_BUS:
						return TransportSubModeNameEnum.RegionalBus;
					case SCHOOL_BUS:
						return TransportSubModeNameEnum.SchoolBus;
					case SHUTTLE_BUS:
						return TransportSubModeNameEnum.ShuttleBus;
					case SIGHTSEEING_BUS:
						return TransportSubModeNameEnum.SightseeingBus;
					case UNKNOWN:
						return TransportSubModeNameEnum.Unknown;
					default:
						log.warn("Unsupported bus sub mode " + mode);
				}
			} else if (subModeStructure.getCoachSubmode() != null) {
				CoachSubmodeEnumeration mode = subModeStructure.getCoachSubmode();
				switch (mode) {
					case TOURIST_COACH:
						return TransportSubModeNameEnum.TouristCoach;
					case INTERNATIONAL_COACH:
						return TransportSubModeNameEnum.InternationalCoach;
					case NATIONAL_COACH:
						return TransportSubModeNameEnum.NationalCoach;
					default:
						log.warn("Unsupported coach sub mode " + mode);
				}
			} else if (subModeStructure.getFunicularSubmode() != null) {
				FunicularSubmodeEnumeration mode = subModeStructure.getFunicularSubmode();
				switch (mode) {
					case FUNICULAR:
						return TransportSubModeNameEnum.Funicular;
					default:
						log.warn("Unsupported funicular sub mode " + mode);
				}
			} else if (subModeStructure.getMetroSubmode() != null) {
				MetroSubmodeEnumeration mode = subModeStructure.getMetroSubmode();
				switch (mode) {
					case METRO:
						return TransportSubModeNameEnum.Metro;
					default:
						log.warn("Unsupported metro sub mode " + mode);
				}
			} else if (subModeStructure.getRailSubmode() != null) {
				RailSubmodeEnumeration mode = subModeStructure.getRailSubmode();
				switch (mode) {
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
					case REGIONAL_RAIL:
						return TransportSubModeNameEnum.RegionalRail;
					case TOURIST_RAILWAY:
						return TransportSubModeNameEnum.TouristRailway;
					case AIRPORT_LINK_RAIL:
						return TransportSubModeNameEnum.AirportLinkRail;
					default:
						log.warn("Unsupported rail sub mode " + mode);
				}
			} else if (subModeStructure.getTelecabinSubmode() != null) {
				TelecabinSubmodeEnumeration mode = subModeStructure.getTelecabinSubmode();
				switch (mode) {
					case TELECABIN:
						return TransportSubModeNameEnum.Telecabin;
					default:
						log.warn("Unsupported telecabin sub mode " + mode);
				}
			} else if (subModeStructure.getTramSubmode() != null) {
				TramSubmodeEnumeration mode = subModeStructure.getTramSubmode();
				switch (mode) {
					case LOCAL_TRAM:
						return TransportSubModeNameEnum.LocalTram;
					case CITY_TRAM:
						return TransportSubModeNameEnum.CityTram;
					default:
						log.warn("Unsupported tram sub mode " + mode);
				}
			} else if (subModeStructure.getWaterSubmode() != null) {
				WaterSubmodeEnumeration mode = subModeStructure.getWaterSubmode();
				switch (mode) {
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
					case SIGHTSEEING_SERVICE:
						return TransportSubModeNameEnum.SightseeingService;
					default:
						log.warn("Unsupported water sub mode " + mode);
				}
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
			case REPLACED:
				return ServiceAlterationEnum.Replaced;
			default:
				log.warn("Unsupported NeTEx ServiceAlteration value: " + netexValue);
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

	/**
	 * Convert the NeTEx object version to an Integer.
	 * Version "any" and non-integer versions are mapped to 0.
	 *
	 * @param obj a versioned NeTEx object.
	 * @return the NeTEx version as an Integer.
	 */
	public static Long getVersion(EntityInVersionStructure obj) {
		String netexObjectVersion = obj.getVersion();
		if ("any".equals(netexObjectVersion)) {
			return 0L;
		} else {
			try {
				return Long.parseLong(netexObjectVersion);
			} catch (NumberFormatException e) {
				if (log.isTraceEnabled()) {
					log.trace("Unable to parse version " + netexObjectVersion + " for Entity " + obj.getId() + " to Integer as supported by Neptune, returning 0");
				}
				return 0L;
			}
		}
	}

	public static Long getVersion(String version) {
		try {
			return Long.parseLong(version);
		} catch (NumberFormatException e) {
			return 0L;
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

	// TODO : Check merge entur : La partie Publication n'est pas présente dans le model NETEX-FR Okina. Donc le lien ici ne peut pas fonctionner. A voir la récup du Netex Entur dans le Netex-FR.
	/*public static PublicationEnum toPublicationEnum(PublicationEnumeration netexType) {
		if (netexType == null) {
			return null;
		}

		switch (netexType) {
			case PUBLIC:
				return PublicationEnum.Public;
			case AUTHORISED:
				return PublicationEnum.Authorised;
			case CONFIDENTIAL:
				return PublicationEnum.Confidential;
			case PRIVATE:
				return PublicationEnum.Private;
			case RESTRICTED:
				return PublicationEnum.Restricted;
			case TEST:
				return PublicationEnum.Test;
		}
		return null;
	}*/

}
