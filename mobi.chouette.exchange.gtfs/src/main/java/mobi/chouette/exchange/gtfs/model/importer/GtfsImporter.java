package mobi.chouette.exchange.gtfs.model.importer;

import mobi.chouette.exchange.gtfs.model.*;
import mobi.chouette.exchange.gtfs.model.fares.GtfsFareV2File;
import mobi.chouette.exchange.gtfs.model.importer.GtfsException.ERROR;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class GtfsImporter {
	private final String _path;
	private final FactoryParameters _factoryParameters;
	private Map<String, Index<GtfsObject>> _map = new HashMap<String, Index<GtfsObject>>();

	public GtfsImporter(String path) {
		this(path, null);
	}

	public GtfsImporter(String path, FactoryParameters factoryParameters) {
		_path = path;
		_factoryParameters = factoryParameters;
	}

	@SuppressWarnings("rawtypes")
	public void dispose() {
		for (Index importer : _map.values()) {
			importer.dispose();
		}
		_map.clear();
		_map = null;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public Index getImporter(String name, String path, Class clazz) {
		return getImporter(name, path, clazz, null);
	}

	public Index getImporter(String name, String path, Class clazz, FactoryParameters factoryParameters) {
		Index importer = _map.get(name);

		if (importer == null) {
			try {
				if (factoryParameters == null) {
					importer = IndexFactory.build(Paths.get(_path, path).toString(), clazz.getName());
				} else {
					importer = IndexFactory.build(Paths.get(_path, path).toString(), clazz.getName(), factoryParameters);
				}
				_map.put(name, importer);
			} catch (ClassNotFoundException | IOException e) {
				Context context = new Context();
				context.put(Context.PATH, _path);
				context.put(Context.ERROR, ERROR.SYSTEM);
				throw new GtfsException(context, e);
			}

		}
		return importer;
	}

	public boolean hasAgencyImporter() {
		return hasImporter(AgencyById.FILENAME);
	}

	public boolean hasFareProductsImporter() {
		return hasImporter(GtfsFareV2File.FARE_PRODUCTS.getFilename());
	}

	public boolean hasFareMediaImporter() {
		return hasImporter(GtfsFareV2File.FARE_MEDIA.getFilename());
	}

	public boolean hasFareRiderCategoriesImporter() {
		return hasImporter(GtfsFareV2File.RIDER_CATEGORIES.getFilename());
	}

	public boolean hasFareLegRulesImporter() {
		return hasImporter(GtfsFareV2File.FARE_LEG_RULES.getFilename());
	}

	public boolean hasTimeBasedFaresFileImporter() {
		return hasImporter(GtfsFareV2File.TIME_BASED_FARES.getFilename());
	}

	public boolean hasCalendarImporter() {
		return hasImporter(CalendarByService.FILENAME);
	}

	public boolean hasCalendarDateImporter() {
		return hasImporter(CalendarDateByService.FILENAME);
	}

	public boolean hasFrequencyImporter() {
		return hasImporter(FrequencyByTrip.FILENAME);
	}

	public boolean hasRouteImporter() {
		return hasImporter(RouteById.FILENAME);
	}

	public boolean hasStopImporter() {
		return hasImporter(StopById.FILENAME);
	}

	public boolean hasStopTimeImporter() {
		return hasImporter(StopTimeByTrip.FILENAME);
	}

	public boolean hasTransferImporter() {
		return hasImporter(TransferByFromStop.FILENAME);
	}

	public boolean hasTripImporter() {
		return hasImporter(TripById.FILENAME);
	}

	public boolean hasShapeImporter() {
		return hasImporter(ShapeById.FILENAME);
	}

	public boolean hasFareAttributeImporter() {
		return hasImporter(FareAttributeById.FILENAME);
	}

	public boolean hasFareRuleImporter() {
		return hasImporter(FareRuleById.FILENAME);
	}

	public boolean hasTranslationImporter() {
		return hasImporter(TranslationIndex.FILENAME);
	}

	private boolean hasImporter(String filename) {
		File f = new File(_path, filename);
		return f.exists();
	}

	@SuppressWarnings("unchecked")
	public Index<GtfsAgency> getAgencyById() {
		return getImporter(INDEX.AGENCY_BY_ID.name(), AgencyById.FILENAME, AgencyById.class);
	}

	@SuppressWarnings("unchecked")
	public Index<GtfsCalendar> getCalendarByService() {
		return getImporter(INDEX.CALENDAR_BY_SERVICE.name(), CalendarByService.FILENAME, CalendarByService.class);
	}

	@SuppressWarnings("unchecked")
	public Index<GtfsCalendarDate> getCalendarDateByService() {
		return getImporter(INDEX.CALENDAR_DATE_BY_SERVICE.name(), CalendarDateByService.FILENAME, CalendarDateByService.class);
	}

	@SuppressWarnings("unchecked")
	public Index<GtfsFrequency> getFrequencyByTrip() {
		return getImporter(INDEX.FREQUENCY_BY_TRIP.name(), FrequencyByTrip.FILENAME, FrequencyByTrip.class);
	}

	@SuppressWarnings("unchecked")
	public Index<GtfsRoute> getRouteById() {
		if (StringUtils.isEmpty(_factoryParameters.getSplitCharacter())) {
			return getImporter(INDEX.ROUTE_BY_ID.name(), RouteById.FILENAME, RouteById.class, _factoryParameters);
		} else {
			return getImporter(INDEX.ROUTE_BY_ID.name(), RouteById.FILENAME, RouteByIdWithMergedIndex.class, _factoryParameters);
		}
	}

	@SuppressWarnings("unchecked")
	public Index<GtfsStop> getStopById() {
		if (StringUtils.isEmpty(_factoryParameters.getCommercialPointIdPrefixToRemove())) {
			return getImporter(INDEX.STOP_BY_ID.name(), StopById.FILENAME, StopById.class);
		} else {
			return getImporter(INDEX.STOP_BY_ID.name(), StopById.FILENAME, StopById.class, _factoryParameters);
		}
	}

	@SuppressWarnings("unchecked")
	public FactoryParameters getFactoryParameters() {
		return _factoryParameters;
	}

	@SuppressWarnings("unchecked")
	public Index<GtfsStopTime> getStopTimeByTrip() {
		return getImporter(INDEX.STOP_TIME_BY_TRIP.name(), StopTimeByTrip.FILENAME, StopTimeByTrip.class);
	}

	@SuppressWarnings("unchecked")
	public Index<GtfsTransfer> getTransferByFromStop() {
		return getImporter(INDEX.TRANSFER_BY_FROM_STOP.name(), TransferByFromStop.FILENAME, TransferByFromStop.class);
	}

	@SuppressWarnings("unchecked")
	public Index<GtfsTransfer> getTransferByToTrip() {
		return getImporter(INDEX.TRANSFER_BY_TO_TRIP.name(), TransferByFromStop.FILENAME, TransferByToTrip.class);
	}

	@SuppressWarnings("unchecked")
	public Index<GtfsTransfer> getTransferByFromTrip() {
		return getImporter(INDEX.TRANSFER_BY_FROM_TRIP.name(), TransferByFromStop.FILENAME, TransferByFromTrip.class);
	}

	@SuppressWarnings("unchecked")
	public Index<GtfsShape> getShapeById() {
		return getImporter(INDEX.SHAPE_BY_ID.name(), ShapeById.FILENAME, ShapeById.class);
	}

	@SuppressWarnings("unchecked")
	public Index<GtfsTrip> getTripById() {
		if (StringUtils.isEmpty(_factoryParameters.getSplitCharacter()) && StringUtils.isEmpty(_factoryParameters.getLinePrefixToRemove())) {
			return getImporter(INDEX.TRIP_BY_ID.name(), TripById.FILENAME, TripById.class);
		} else {
			return getImporter(INDEX.TRIP_BY_ID.name(), TripById.FILENAME, TripById.class, _factoryParameters);
		}
	}

	@SuppressWarnings("unchecked")
	public Index<GtfsTrip> getTripByRoute() {
		if (StringUtils.isEmpty(_factoryParameters.getSplitCharacter()) && StringUtils.isEmpty(_factoryParameters.getLinePrefixToRemove())) {
			return getImporter(INDEX.TRIP_BY_ROUTE.name(), TripById.FILENAME, TripByRoute.class);
		} else {
			return getImporter(INDEX.TRIP_BY_ROUTE.name(), TripById.FILENAME, TripByRoute.class, _factoryParameters);
		}


	}

	@SuppressWarnings("unchecked")
	public Index<GtfsTrip> getTripByService() {
		return getImporter(INDEX.TRIP_BY_SERVICE.name(), TripById.FILENAME, TripByRoute.class);
	}

	@SuppressWarnings("unchecked")
	public Index<GtfsFareAttribute> getFareAttributeById() {
		return getImporter(INDEX.FARE_ID_BY_ATTRIBUTE.name(), FareAttributeById.FILENAME, FareAttributeById.class);
	}

	@SuppressWarnings("unchecked")
	public Index<GtfsFareRule> getFareRuleById() {
		return getImporter(INDEX.FARE_ID_BY_RULE.name(), FareRuleById.FILENAME, FareRuleById.class);
	}

	public Index<GtfsFareMedia> getFareMediaIndex() {
		return getImporter(INDEX.FARE_MEDIA_TYPE.name(), FareMediaIndex.FILENAME, FareMediaIndex.class);
	}

	public Index<GtfsRiderCategories> getFareRiderCategoriesIndex() {
		return getImporter(INDEX.FARE_RIDER_CATEGORIES.name(), FareRiderCategoriesIndex.FILENAME, FareRiderCategoriesIndex.class);
	}

	public Index<GtfsFareProducts> getFareProductsIndex() {
		return getImporter(INDEX.FARE_PRODUCTS.name(), FareProductsIndex.FILENAME, FareProductsIndex.class);
	}

	public Index<GtfsFareLegRules> getFareLegRulesIndex() {
		return getImporter(INDEX.FARE_LEG_RULES.name(), FareLegRulesIndex.FILENAME, FareLegRulesIndex.class);
	}

	public Index<GtfsTimeframe> getFareTimeframeIndex() {
		return getImporter(INDEX.FARE_TIMEFRAME.name(), TimeframeIndex.FILENAME, TimeframeIndex.class);
	}

	public Index<GtfsTranslation> getTranslationIndex() {
		return getImporter(INDEX.TRANSLATION.name(), TranslationIndex.FILENAME, TranslationIndex.class);
	}

	public enum INDEX {
		AGENCY_BY_ID, CALENDAR_BY_SERVICE, CALENDAR_DATE_BY_SERVICE, FREQUENCY_BY_TRIP, ROUTE_BY_ID, STOP_BY_ID, STOP_TIME_BY_TRIP,
		TRANSFER_BY_FROM_STOP, TRANSFER_BY_FROM_TRIP, TRANSFER_BY_TO_TRIP, SHAPE_BY_ID, TRIP_BY_ID, TRIP_BY_ROUTE, TRIP_BY_SERVICE,
		FARE_ID_BY_ATTRIBUTE, FARE_ID_BY_RULE, FARE_MEDIA_TYPE, FARE_RIDER_CATEGORIES, FARE_LEG_RULES, FARE_PRODUCTS, FARE_TIMEFRAME, TRANSLATION
	}

}
