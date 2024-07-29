package mobi.chouette.exchange.gtfs.model.exporter;

import lombok.extern.log4j.Log4j;
import mobi.chouette.exchange.gtfs.model.*;
import mobi.chouette.exchange.gtfs.model.importer.Context;
import mobi.chouette.exchange.gtfs.model.importer.GtfsException;
import mobi.chouette.exchange.gtfs.model.importer.GtfsException.ERROR;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Log4j
public class GtfsExporter implements GtfsExporterInterface {
	public static enum EXPORTER {
		AGENCY, ATTRIBUTION, CALENDAR, CALENDAR_DATE, FEED_INFO, FREQUENCY, ROUTE, STOP, STOP_TIME, TRANSFER, TRIP, SHAPE;
	}

	private String _path;
	private Map<String, Exporter<GtfsObject>> _map = new HashMap<String, Exporter<GtfsObject>>();

	public GtfsExporter(String path) {
		_path = path;
	}

	@SuppressWarnings("rawtypes")
	public void dispose(mobi.chouette.common.Context context) {
		for (Exporter exporter : _map.values()) {
			try {
				exporter.dispose(context);
			} catch (IOException e) {
				log.error(e);
			}
		}
		_map.clear();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Exporter getExporter(String name, String path, Class clazz) {
		Exporter result = _map.get(name);

		if (result == null) {
			try {
				result = ExporterFactory.build(Paths.get(_path, path)
						.toString(), clazz.getName());
				_map.put(name, result);
			} catch (ClassNotFoundException | IOException e) {
				Context context = new Context();
				context.put(Context.PATH, _path);
				context.put(Context.ERROR, ERROR.SYSTEM);
				throw new GtfsException(context, e);
			}

		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public Exporter<GtfsAgency> getAgencyExporter() throws GtfsException {
		return getExporter(EXPORTER.AGENCY.name(), AgencyExporter.FILENAME,
				AgencyExporter.class);

	}

	@SuppressWarnings("unchecked")
	public Exporter<GtfsCalendarDate> getCalendarDateExporter()
			throws GtfsException {
		return getExporter(EXPORTER.CALENDAR_DATE.name(),
				CalendarDateExporter.FILENAME, CalendarDateExporter.class);
	}

	@SuppressWarnings("unchecked")
	public Exporter<GtfsCalendar> getCalendarExporter() throws GtfsException {
		return getExporter(EXPORTER.CALENDAR.name(), CalendarExporter.FILENAME,
				CalendarExporter.class);
	}

	@SuppressWarnings("unchecked")
	public Exporter<GtfsFeedInfo> getFeedInfoExporter() throws GtfsException {
		return getExporter(EXPORTER.FEED_INFO.name(), FeedInfoExporter.FILENAME,
				FeedInfoExporter.class);
	}

	@SuppressWarnings("unchecked")
	public Exporter<GtfsFrequency> getFrequencyExporter() throws GtfsException {
		return getExporter(EXPORTER.FREQUENCY.name(),
				FrequencyExporter.FILENAME, FrequencyExporter.class);
	}

	@SuppressWarnings("unchecked")
	public Exporter<GtfsRoute> getRouteExporter() throws GtfsException {
		return getExporter(EXPORTER.ROUTE.name(), RouteExporter.FILENAME,
				RouteExporter.class);
	}

	@SuppressWarnings("unchecked")
	public Exporter<GtfsStop> getStopExporter() throws GtfsException {
		return getExporter(EXPORTER.STOP.name(), StopExporter.FILENAME,
				StopExporter.class);
	}

	@SuppressWarnings("unchecked")
	public Exporter<GtfsStop> getStopExtendedExporter() throws GtfsException {
		return getExporter(EXPORTER.STOP.name(), StopExporter.FILENAME,
				StopExtendedExporter.class);
	}

	@SuppressWarnings("unchecked")
	public Exporter<GtfsStopTime> getStopTimeExporter() throws GtfsException {
		return getExporter(EXPORTER.STOP_TIME.name(),
				StopTimeExporter.FILENAME, StopTimeExporter.class);
	}

	@SuppressWarnings("unchecked")
	public Exporter<GtfsTransfer> getTransferExporter() throws GtfsException {
		return getExporter(EXPORTER.TRANSFER.name(), TransferExporter.FILENAME,
				TransferExporter.class);
	}

	@SuppressWarnings("unchecked")
	public Exporter<GtfsTrip> getTripExporter() throws GtfsException {
		return getExporter(EXPORTER.TRIP.name(), TripExporter.FILENAME,
				TripExporter.class);
	}

	@SuppressWarnings("unchecked")
	public Exporter<GtfsShape> getShapeExporter() throws GtfsException {
		return getExporter(EXPORTER.SHAPE.name(), ShapeExporter.FILENAME,
				ShapeExporter.class);
	}

	public Exporter<GtfsAttribution> getAttributionExporter() throws GtfsException {
		return getExporter(EXPORTER.ATTRIBUTION.name(), AttributionExporter.FILENAME, AttributionExporter.class);
	}

}
