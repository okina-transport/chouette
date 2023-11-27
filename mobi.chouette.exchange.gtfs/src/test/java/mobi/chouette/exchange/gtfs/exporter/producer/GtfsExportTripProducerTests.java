package mobi.chouette.exchange.gtfs.exporter.producer;

import java.sql.Time;

import mobi.chouette.exchange.gtfs.exporter.producer.mock.GtfsExporterMock;
import mobi.chouette.exchange.gtfs.model.GtfsStopTime;
import mobi.chouette.exchange.gtfs.model.GtfsTrip;
import mobi.chouette.exchange.gtfs.model.GtfsTrip.DirectionType;
import mobi.chouette.exchange.gtfs.model.GtfsTrip.WheelchairAccessibleType;
import mobi.chouette.exchange.gtfs.model.exporter.StopTimeExporter;
import mobi.chouette.exchange.gtfs.model.exporter.TripExporter;
import mobi.chouette.exchange.gtfs.model.importer.Context;
import mobi.chouette.model.JourneyPattern;
import mobi.chouette.model.Line;
import mobi.chouette.model.Route;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.StopPoint;
import mobi.chouette.model.VehicleJourney;
import mobi.chouette.model.VehicleJourneyAtStop;

import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.Test;


public class GtfsExportTripProducerTests 
{
   private GtfsExporterMock mock = new GtfsExporterMock();
   private GtfsTripProducer producer = new GtfsTripProducer(mock);
   private Context context = new Context();


}