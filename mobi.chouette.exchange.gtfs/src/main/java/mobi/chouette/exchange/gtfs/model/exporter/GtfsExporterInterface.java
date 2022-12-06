package mobi.chouette.exchange.gtfs.model.exporter;

import mobi.chouette.exchange.gtfs.model.*;

public interface GtfsExporterInterface {
	Exporter<GtfsAgency> getAgencyExporter() throws Exception;

	Exporter<GtfsCalendarDate> getCalendarDateExporter() throws Exception;

	Exporter<GtfsCalendar> getCalendarExporter() throws Exception;

	Exporter<GtfsFeedInfo> getFeedInfoExporter() throws Exception;

	Exporter<GtfsFrequency> getFrequencyExporter() throws Exception;

	Exporter<GtfsRoute> getRouteExporter() throws Exception;

	Exporter<GtfsShape> getShapeExporter() throws Exception;

	Exporter<GtfsStop> getStopExporter() throws Exception;

	Exporter<GtfsStop> getStopExtendedExporter() throws Exception;

	Exporter<GtfsStopTime> getStopTimeExporter() throws Exception;

	Exporter<GtfsTransfer> getTransferExporter() throws Exception;

	Exporter<GtfsTrip> getTripExporter() throws Exception;

	Exporter<GtfsAttribution> getAttributionExporter() throws Exception;
}
