package mobi.chouette.exchange.gtfs.exporter.producer;

import mobi.chouette.exchange.gtfs.exporter.producer.mock.GtfsExporterMock;
import mobi.chouette.exchange.gtfs.model.GtfsStopTime;
import mobi.chouette.exchange.gtfs.model.GtfsTrip;
import mobi.chouette.exchange.gtfs.model.GtfsTrip.DirectionType;
import mobi.chouette.exchange.gtfs.model.GtfsTrip.WheelchairAccessibleType;
import mobi.chouette.exchange.gtfs.model.exporter.StopTimeExporter;
import mobi.chouette.exchange.gtfs.model.exporter.TripExporter;
import mobi.chouette.exchange.gtfs.model.importer.Context;
import mobi.chouette.exchange.gtfs.parameters.IdFormat;
import mobi.chouette.exchange.gtfs.parameters.IdParameters;
import mobi.chouette.model.AccessibilityAssessment;
import mobi.chouette.model.AccessibilityLimitation;
import mobi.chouette.model.DestinationDisplay;
import mobi.chouette.model.JourneyPattern;
import mobi.chouette.model.Line;
import mobi.chouette.model.Route;
import mobi.chouette.model.ScheduledStopPoint;
import mobi.chouette.model.SimpleObjectReference;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.StopPoint;
import mobi.chouette.model.VehicleJourney;
import mobi.chouette.model.VehicleJourneyAtStop;
import mobi.chouette.model.type.ChouetteAreaEnum;
import mobi.chouette.model.type.LimitationStatusEnum;
import mobi.chouette.model.util.ObjectIdTypes;

import org.checkerframework.checker.units.qual.A;
import org.joda.time.LocalTime;
import org.rutebanken.netex.model.LimitationStatusEnumeration;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.Test;

import static mobi.chouette.exchange.gtfs.parameters.IdFormat.SOURCE;


public class GtfsExportTripProducerTests 
{
   private GtfsExporterMock mock = new GtfsExporterMock();
   private GtfsTripProducer producer = new GtfsTripProducer(mock);
   private Context context = new Context();

   @Test(groups = { "Producers" }, description = "test trip with full data")
   public void verifyTripProducerWithFullData() {
      
      mock.reset();

      VehicleJourney neptuneObject = buildNeptuneObject(true);

      producer.save(neptuneObject, "tm_01", "GTFS", false, new IdParameters(), false);
      Reporter.log("verifyTripProducerWithFullData");

      Assert.assertEquals(mock.getExportedTrips().size(), 1, "Trip should be returned");
      Assert.assertEquals(mock.getExportedStopTimes().size(), 4, "StopTimes should be returned");
      GtfsTrip gtfsObject = mock.getExportedTrips().get(0);
      Reporter.log(TripExporter.CONVERTER.to(context,gtfsObject));

      Assert.assertEquals(gtfsObject.getTripId(), "4321", "TripId must be correctly set");
      Assert.assertEquals(gtfsObject.getServiceId(), "tm_01", "ServiceId must be correctly set");
      Assert.assertEquals(gtfsObject.getRouteId(), "0123", "RouteId must be correctly set");
      Assert.assertEquals(gtfsObject.getTripShortName(), "456", "TripShortName must be correctly set");
      Assert.assertEquals(gtfsObject.getDirectionId(), DirectionType.Outbound, "DirectionId must be correctly set");
      Assert.assertEquals(gtfsObject.getTripHeadSign(), neptuneObject.getJourneyPattern().getPublishedName(), "TripHeadSign must be correctly set");
      Assert.assertNull(gtfsObject.getBlockId(), "BlockId must not be set");
      Assert.assertNull(gtfsObject.getShapeId(), "ShapeId must not be set");
      Assert.assertEquals(gtfsObject.getWheelchairAccessible(), WheelchairAccessibleType.Allowed, "WheelchairAccessible must be correctly set");
      Assert.assertEquals(gtfsObject.getBikesAllowed(), GtfsTrip.BikesAllowedType.NoInformation, "BikesAllowed must not be set");
      
      int i = 0;
      
      /**
       * Check that
       */
      for (GtfsStopTime gtfsStopTime : mock.getExportedStopTimes())
      {
         Reporter.log(StopTimeExporter.CONVERTER.to(context,gtfsStopTime));
         Assert.assertEquals(gtfsStopTime.getTripId(), "4321", "TripId must be correctly set");
         Assert.assertEquals(gtfsStopTime.getStopId(), "GTFS:StopPoint:SA"+i, "StopId must be correctly set");
         Assert.assertEquals(gtfsStopTime.getStopSequence(), Integer.valueOf(i*2), "StopSequence must be correctly set");
         if (i == 0) 
         {
            Assert.assertEquals(gtfsStopTime.getArrivalTime().getDay(), Integer.valueOf(0), "ArrivalTime must be today");
            Assert.assertEquals(gtfsStopTime.getDepartureTime().getDay(), Integer.valueOf(1), "DepartureTime must be today");
         }
         
         else if (i == 1) 
         {
             Assert.assertEquals(gtfsStopTime.getArrivalTime().getDay(), Integer.valueOf(1), "ArrivalTime must be today");
             Assert.assertEquals(gtfsStopTime.getDepartureTime().getDay(), Integer.valueOf(1), "DepartureTime must be today");
          }
         else if (i == 2)
         {
            Assert.assertEquals(gtfsStopTime.getArrivalTime().getDay(), Integer.valueOf(1), "ArrivalTime must be today");
            Assert.assertEquals(gtfsStopTime.getDepartureTime().getDay(), Integer.valueOf(1), "DepartureTime must be tomorrow");           
         }
         else
         {
            Assert.assertEquals(gtfsStopTime.getArrivalTime().getDay(), Integer.valueOf(2), "ArrivalTime must be tomorrow");
            Assert.assertEquals(gtfsStopTime.getDepartureTime().getDay(), Integer.valueOf(2), "DepartureTime must be tomorrow");           
         }
         
            
         i++;
      }

   }



   @Test(groups = { "Producers" }, description = "test on routeID")
   public void verifyTripProducerRouteId() {

      mock.reset();

      VehicleJourney neptuneObject = buildNeptuneObject(true);

      producer.save(neptuneObject, "tm_01", "GTFS",false, new IdParameters("PREFIXSTOP",null,null,"PREFIX","COMPREFIX"), false);
      Reporter.log("verifyTripProducerRouteId");

      Assert.assertEquals(mock.getExportedTrips().size(), 1, "Trip should be returned");
      Assert.assertEquals(mock.getExportedStopTimes().size(), 4, "StopTimes should be returned");
      GtfsTrip gtfsObject = mock.getExportedTrips().get(0);
      Reporter.log(TripExporter.CONVERTER.to(context,gtfsObject));

      Assert.assertEquals(gtfsObject.getRouteId(), "PREFIX0123", "RouteID must be correctly set");

   }


   @Test(groups = { "Producers" }, description = "test on routeID")
   public void verifyTripProducerRouteIdWithSuffix() {

      mock.reset();

      VehicleJourney neptuneObject = buildNeptuneObject(true);

      producer.save(neptuneObject, "tm_01", "GTFS",false, new IdParameters("PREFIXSTOP",null,"SUFFIX",null,"COMPREFIX"), false);
      Reporter.log("verifyTripProducerRouteIdWithSuffix");

      Assert.assertEquals(mock.getExportedTrips().size(), 1, "Trip should be returned");
      Assert.assertEquals(mock.getExportedStopTimes().size(), 4, "StopTimes should be returned");
      GtfsTrip gtfsObject = mock.getExportedTrips().get(0);
      Reporter.log(TripExporter.CONVERTER.to(context,gtfsObject));

      Assert.assertEquals(gtfsObject.getRouteId(), "0123SUFFIX", "RouteID must be correctly set");

   }

   @Test(groups = { "Producers" }, description = "test on routeID")
   public void verifyTripProducerRouteIdTrident() {

      mock.reset();

      VehicleJourney neptuneObject = buildNeptuneObject(true);

      producer.save(neptuneObject, "tm_01", "GTFS",false, new IdParameters("PREFIXSTOP",IdFormat.TRIDENT,null,"PREFIX","COMPREFIX"), true);
      Reporter.log("verifyTripProducerRouteIdTrident");

      Assert.assertEquals(mock.getExportedTrips().size(), 1, "Trip should be returned");
      Assert.assertEquals(mock.getExportedStopTimes().size(), 4, "StopTimes should be returned");
      GtfsTrip gtfsObject = mock.getExportedTrips().get(0);
      Reporter.log(TripExporter.CONVERTER.to(context,gtfsObject));

      Assert.assertEquals(gtfsObject.getRouteId(), "PREFIX:Line:0123", "RouteID must be correctly set");

   }


   @Test(groups = { "Producers" }, description = "test on routeID")
   public void verifyTripProducerRouteIdTridentWithSuffix() {

      mock.reset();

      VehicleJourney neptuneObject = buildNeptuneObject(true);

      producer.save(neptuneObject, "tm_01", "GTFS",false, new IdParameters("PREFIXSTOP",IdFormat.TRIDENT,"SUFFIX","PREFIX","COMPREFIX"), false);
      Reporter.log("verifyTripProducerRouteIdTridentWithSuffix");

      Assert.assertEquals(mock.getExportedTrips().size(), 1, "Trip should be returned");
      Assert.assertEquals(mock.getExportedStopTimes().size(), 4, "StopTimes should be returned");
      GtfsTrip gtfsObject = mock.getExportedTrips().get(0);
      Reporter.log(TripExporter.CONVERTER.to(context,gtfsObject));

      Assert.assertEquals(gtfsObject.getRouteId(), "PREFIX:Line:0123SUFFIX", "RouteID must be correctly set");

   }



   @Test(groups = { "Producers" }, description = "test Stop Id generation")
   public void verifyStopIds() {

      mock.reset();

      VehicleJourney neptuneObject = buildNeptuneObjectWithOriginalStopIds();

      producer.save(neptuneObject, "tm_01", "GTFS",false, new IdParameters("PREFIX",SOURCE,null,"PREFIXROUTE","COMPREFIX"), false);
      Reporter.log("verifyStopIds");

      Assert.assertEquals(mock.getExportedTrips().size(), 1, "Trip should be returned");
      Assert.assertEquals(mock.getExportedStopTimes().size(), 4, "StopTimes should be returned");
      GtfsTrip gtfsObject = mock.getExportedTrips().get(0);
      Reporter.log(TripExporter.CONVERTER.to(context,gtfsObject));

      int i = 0;

      /**
       * Check that
       */
      for (GtfsStopTime gtfsStopTime : mock.getExportedStopTimes())
      {
         Reporter.log(StopTimeExporter.CONVERTER.to(context,gtfsStopTime));
         Assert.assertEquals(gtfsStopTime.getStopId(), "PREFIXorigSA"+i, "StopId must be correctly set");
         i++;
      }

   }

   @Test(groups = { "Producers" }, description = "test commercial point Stop Id generation")
   public void verifyCommercialPointStopIds() {

      mock.reset();

      VehicleJourney neptuneObject = buildNeptuneObjectWithCommercialPointsIds(true);

      producer.save(neptuneObject, "tm_01", "GTFS",false, new IdParameters("PREFIX",SOURCE,null,"PREFIXROUTE","COMPREFIX"), false);
      Reporter.log("verifyStopIds");

      Assert.assertEquals(mock.getExportedTrips().size(), 1, "Trip should be returned");
      Assert.assertEquals(mock.getExportedStopTimes().size(), 4, "StopTimes should be returned");
      GtfsTrip gtfsObject = mock.getExportedTrips().get(0);
      Reporter.log(TripExporter.CONVERTER.to(context,gtfsObject));

      int i = 0;

      /**
       * Check that
       */
      for (GtfsStopTime gtfsStopTime : mock.getExportedStopTimes())
      {
         Reporter.log(StopTimeExporter.CONVERTER.to(context,gtfsStopTime));
         Assert.assertEquals(gtfsStopTime.getStopId(), "COMPREFIXorigSA"+i, "StopId must be correctly set");
         i++;
      }

   }

   @Test(groups = { "Producers" }, description = "test Stop Id generation")
   public void verifyStopIdsTrident() {

      mock.reset();

      VehicleJourney neptuneObject = buildNeptuneObjectWithOriginalStopIds();

      producer.save(neptuneObject, "tm_01", "GTFS",false, new IdParameters("PREFIX", IdFormat.TRIDENT,null,"PREFIXLINE","COMPREFIX"), false);
      Reporter.log("verifyStopIds");

      Assert.assertEquals(mock.getExportedTrips().size(), 1, "Trip should be returned");
      Assert.assertEquals(mock.getExportedStopTimes().size(), 4, "StopTimes should be returned");
      GtfsTrip gtfsObject = mock.getExportedTrips().get(0);
      Reporter.log(TripExporter.CONVERTER.to(context,gtfsObject));

      int i = 0;

      /**
       * Check that
       */
      for (GtfsStopTime gtfsStopTime : mock.getExportedStopTimes())
      {
         Reporter.log(StopTimeExporter.CONVERTER.to(context,gtfsStopTime));
         Assert.assertEquals(gtfsStopTime.getStopId(), "PREFIX:StopPlace:origSA"+i, "StopId must be correctly set");
         i++;
      }

   }

   @Test(groups = { "Producers" }, description = "test trip with less data")
   public void verifyTripProducerWithLessData() {
      
      mock.reset();

      VehicleJourney neptuneObject = buildNeptuneObject(false);

      producer.save(neptuneObject, "tm_01", "GTFS",false, new IdParameters(), false);
      Reporter.log("verifyTripProducerWithLessData");

      Assert.assertEquals(mock.getExportedTrips().size(), 1, "Trip should be returned");
      Assert.assertEquals(mock.getExportedStopTimes().size(), 4, "StopTimes should be returned");
      GtfsTrip gtfsObject = mock.getExportedTrips().get(0);
      Reporter.log(TripExporter.CONVERTER.to(context,gtfsObject));

      Assert.assertEquals(gtfsObject.getTripId(), "4321", "TripId must be correctly set");
      Assert.assertEquals(gtfsObject.getServiceId(), "tm_01", "ServiceId must be correctly set");
      Assert.assertEquals(gtfsObject.getRouteId(), "0123", "RouteId must be correctly set");
      Assert.assertNull(gtfsObject.getTripShortName(),  "TripShortName must not be set");
      Assert.assertEquals(gtfsObject.getDirectionId(), DirectionType.Outbound, "DirectionId must be correctly set");
      Assert.assertNull(gtfsObject.getTripHeadSign(),  "TripHeadSign must not be set");
      Assert.assertNull(gtfsObject.getBlockId(), "BlockId must not be set");
      Assert.assertNull(gtfsObject.getShapeId(), "ShapeId must not be set");
      Assert.assertEquals(gtfsObject.getWheelchairAccessible(), WheelchairAccessibleType.NoInformation, "WheelchairAccessible must be correctly set");
      Assert.assertEquals(gtfsObject.getBikesAllowed(), GtfsTrip.BikesAllowedType.NoInformation, "BikesAllowed must not be set");
      
      int i = 0;
      
      /**
       * Check offset value on journeys during more than one day
       */
      for (GtfsStopTime gtfsStopTime : mock.getExportedStopTimes())
      {
         Reporter.log(StopTimeExporter.CONVERTER.to(context,gtfsStopTime));
         Assert.assertEquals(gtfsStopTime.getTripId(), "4321", "TripId must be correctly set");
         Assert.assertEquals(gtfsStopTime.getStopId(), "GTFS:StopPoint:SA"+i, "StopId must be correctly set");
         Assert.assertEquals(gtfsStopTime.getStopSequence(), Integer.valueOf(i*2), "StopSequence must be correctly set");
         if (i == 0) 
         {
            Assert.assertEquals(gtfsStopTime.getArrivalTime().getDay(), Integer.valueOf(0), "ArrivalTime must be today");
            Assert.assertEquals(gtfsStopTime.getDepartureTime().getDay(), Integer.valueOf(1), "DepartureTime must be tomorrow");
            
         }
         
         else if (i == 1)
         {
            Assert.assertEquals(gtfsStopTime.getArrivalTime().getDay(), Integer.valueOf(1), "ArrivalTime must be tomorrow");
            Assert.assertEquals(gtfsStopTime.getDepartureTime().getDay(), Integer.valueOf(1), "DepartureTime must be tomorrow");           
         }
         else if (i == 2)
         {
        	 Assert.assertEquals(gtfsStopTime.getArrivalTime().getDay(), Integer.valueOf(1), "ArrivalTime must be tomorrow");
             Assert.assertEquals(gtfsStopTime.getDepartureTime().getDay(), Integer.valueOf(1), "DepartureTime must be tomorrow");
         }
         else
         {
            Assert.assertEquals(gtfsStopTime.getArrivalTime().getDay(), Integer.valueOf(2), "ArrivalTime must be after tomorrow");
            Assert.assertEquals(gtfsStopTime.getDepartureTime().getDay(), Integer.valueOf(2), "DepartureTime must be after tomorrow");           
         }
         
            
         i++;
      }
   }

   @Test(groups = { "Producers" }, description = "test trip wheelChair mapping")
   public void verifyTripProducerForWheelChairMapping() {
      mock.reset();
      Reporter.log("verifyTripProducerForWheelChairMapping");

      VehicleJourney neptuneObject = buildNeptuneObject(true);
      neptuneObject.getAccessibilityAssessment().setMobilityImpairedAccess(LimitationStatusEnum.TRUE);

      producer.save(neptuneObject, "tm_01",  "GTFS",false, new IdParameters(), false);

      Assert.assertEquals(mock.getExportedTrips().size(), 1, "Trip should be returned");
      GtfsTrip gtfsObject = mock.getExportedTrips().get(0);
      Reporter.log(TripExporter.CONVERTER.to(context,gtfsObject));
      Assert.assertEquals(gtfsObject.getWheelchairAccessible(), WheelchairAccessibleType.Allowed, "WheelchairAccessible must be correctly set");

      mock.reset();
      neptuneObject.getAccessibilityAssessment().getAccessibilityLimitation().setWheelchairAccess(LimitationStatusEnumeration.FALSE);

      producer.save(neptuneObject, "tm_01", "GTFS",false, new IdParameters(), false);
      Assert.assertEquals(mock.getExportedTrips().size(), 1, "Trip should be returned");
      gtfsObject = mock.getExportedTrips().get(0);
      Reporter.log(TripExporter.CONVERTER.to(context,gtfsObject));
      Assert.assertEquals(gtfsObject.getWheelchairAccessible(), WheelchairAccessibleType.NoAllowed, "WheelchairAccessible must be correctly set");

      mock.reset();
      neptuneObject.getAccessibilityAssessment().getAccessibilityLimitation().setWheelchairAccess(LimitationStatusEnumeration.UNKNOWN);

      producer.save(neptuneObject, "tm_01",  "GTFS",false, new IdParameters(), false);
      Assert.assertEquals(mock.getExportedTrips().size(), 1, "Trip should be returned");
      gtfsObject = mock.getExportedTrips().get(0);
      Reporter.log(TripExporter.CONVERTER.to(context,gtfsObject));
      Assert.assertEquals(gtfsObject.getWheelchairAccessible(), WheelchairAccessibleType.NoInformation, "WheelchairAccessible must be correctly set");
   }

  
   @Test(groups = { "Producers" }, description = "test trip Direction mapping")
   public void verifyTripProducerForDirectionMapping() {
      mock.reset();
      Reporter.log("verifyTripProducerForDirectionMapping");

      VehicleJourney neptuneObject = buildNeptuneObject(true);
      Route r = neptuneObject.getRoute();
      r.setWayBack("A");
      producer.save(neptuneObject, "tm_01", "GTFS",false, new IdParameters(), false);

      Assert.assertEquals(mock.getExportedTrips().size(), 1, "Trip should be returned");
      GtfsTrip gtfsObject = mock.getExportedTrips().get(0);
      Assert.assertEquals(gtfsObject.getDirectionId(), DirectionType.Outbound, "DirectionId must be correctly set");

      mock.reset();
      r.setWayBack("R");
      producer.save(neptuneObject, "tm_01",  "GTFS",false, new IdParameters(), false);
      Assert.assertEquals(mock.getExportedTrips().size(), 1, "Trip should be returned");
      gtfsObject = mock.getExportedTrips().get(0);
      Assert.assertEquals(gtfsObject.getDirectionId(), DirectionType.Inbound, "DirectionId must be correctly set");

      mock.reset();
      r.setWayBack(null);
      producer.save(neptuneObject, "tm_01",  "GTFS",false, new IdParameters(), false);
      Assert.assertEquals(mock.getExportedTrips().size(), 1, "Trip should be returned");
      gtfsObject = mock.getExportedTrips().get(0);
      Assert.assertEquals(gtfsObject.getDirectionId(), DirectionType.Outbound, "DirectionId must be correctly set");
   }

   @Test(groups = { "Producers" }, description = "test trip headsign mapping")
   public void verifyTripProducerForTripAndTripHeadSignWithDestinationDisplayOnFirst() {
      mock.reset();

      VehicleJourney neptuneObject = buildNeptuneObject(true);
      DestinationDisplay destinationDisplay = new DestinationDisplay();
      destinationDisplay.setFrontText("ShouldBeTripHeadSign");
      neptuneObject.getVehicleJourneyAtStops().get(0).getStopPoint().setDestinationDisplay(destinationDisplay );
      producer.save(neptuneObject, "tm_01", "GTFS",false, new IdParameters(), false);

      Assert.assertEquals(mock.getExportedTrips().size(), 1, "Trip should be returned");
      GtfsTrip gtfsObject = mock.getExportedTrips().get(0);
      Assert.assertEquals(gtfsObject.getTripHeadSign(),"ShouldBeTripHeadSign" , "Trip headsign must be set based on first destination display");
   }

   @Test(groups = { "Producers" }, description = "test trip headsign mapping")
   public void verifyTripProducerForTripAndTripHeadSignWithDestinationDisplayOnFirstWithVias() {
      mock.reset();

      VehicleJourney neptuneObject = buildNeptuneObject(true);
      DestinationDisplay destinationDisplay = new DestinationDisplay();
      destinationDisplay.setFrontText("MainDestination");
      
      DestinationDisplay via = new DestinationDisplay();
      via.setFrontText("ViaDestination");
      destinationDisplay.getVias().add(via);
      
      neptuneObject.getVehicleJourneyAtStops().get(0).getStopPoint().setDestinationDisplay(destinationDisplay );
      producer.save(neptuneObject, "tm_01", "GTFS",false, new IdParameters(), false);

      Assert.assertEquals(mock.getExportedTrips().size(), 1, "Trip should be returned");
      GtfsTrip gtfsObject = mock.getExportedTrips().get(0);
      Assert.assertEquals(gtfsObject.getTripHeadSign(),"MainDestination via ViaDestination" , "Trip headsign must be set based on first destination display");
   }

   @Test(groups = { "Producers" }, description = "test trip headsign mapping")
   public void verifyTripProducerForTripAndStopHeadSignWithDestinationDisplay() {
      mock.reset();

      
      VehicleJourney journey1 = buildNeptuneObject(true);
      DestinationDisplay journey1FirstDisplay = new DestinationDisplay();
      journey1FirstDisplay.setFrontText("MainDestination1");
      
      DestinationDisplay journey1SecondDisplay = new DestinationDisplay();
      journey1SecondDisplay.setFrontText("UpdatedDestination1");
      
      journey1.getVehicleJourneyAtStops().get(0).getStopPoint().setDestinationDisplay(journey1FirstDisplay );
      journey1.getVehicleJourneyAtStops().get(2).getStopPoint().setDestinationDisplay(journey1SecondDisplay );
      producer.save(journey1, "tm_01", "GTFS",false, new IdParameters(), false);

      
      VehicleJourney journey2 = buildNeptuneObject(true);
      DestinationDisplay journey2FirstDisplay = new DestinationDisplay();
      journey2FirstDisplay.setFrontText("MainDestination");
      
      DestinationDisplay journey2SecondDisplay = new DestinationDisplay();
      journey2SecondDisplay.setFrontText("UpdatedDestination");
      
      journey2.getVehicleJourneyAtStops().get(0).getStopPoint().setDestinationDisplay(journey2FirstDisplay );
      journey2.getVehicleJourneyAtStops().get(2).getStopPoint().setDestinationDisplay(journey2SecondDisplay );
      producer.save(journey2, "tm_01", "GTFS",false, new IdParameters(), false);

      
      Assert.assertEquals(mock.getExportedTrips().size(), 2, "Trip should be returned");
      GtfsTrip gtfsObject = mock.getExportedTrips().get(1);
      Assert.assertEquals(gtfsObject.getTripHeadSign(),"MainDestination" , "Trip headsign must be set based on first destination display");
      
      Assert.assertEquals(mock.getExportedStopTimes().size(), 8);
      Assert.assertEquals(mock.getExportedStopTimes().get(0).getStopHeadsign(), null);
      Assert.assertEquals(mock.getExportedStopTimes().get(1).getStopHeadsign(), null);
      Assert.assertEquals(mock.getExportedStopTimes().get(2).getStopHeadsign(), "UpdatedDestination1");
      Assert.assertEquals(mock.getExportedStopTimes().get(3).getStopHeadsign(), "UpdatedDestination1");
      Assert.assertEquals(mock.getExportedStopTimes().get(4).getStopHeadsign(), null);
      Assert.assertEquals(mock.getExportedStopTimes().get(5).getStopHeadsign(), null);
      Assert.assertEquals(mock.getExportedStopTimes().get(6).getStopHeadsign(), "UpdatedDestination");
      Assert.assertEquals(mock.getExportedStopTimes().get(7).getStopHeadsign(), "UpdatedDestination");
      
   }

   /**
    * @return
    */
   @SuppressWarnings("deprecation")
private VehicleJourney buildNeptuneObject(boolean full)
   {
      VehicleJourney neptuneObject = new VehicleJourney();
      neptuneObject.setObjectId("GTFS:VehicleJourney:4321");
      // if (full) neptuneObject.setName("name");
      if (full) neptuneObject.setNumber(Long.valueOf(456));
      if (full) neptuneObject.setPublishedJourneyIdentifier("456");

      AccessibilityAssessment accessibilityAssessment = new AccessibilityAssessment();
      AccessibilityLimitation accessibilityLimitation = new AccessibilityLimitation();
      accessibilityAssessment.setAccessibilityLimitation(accessibilityLimitation);
      neptuneObject.setAccessibilityAssessment(accessibilityAssessment);
      if (full) neptuneObject.getAccessibilityAssessment().getAccessibilityLimitation().setWheelchairAccess(LimitationStatusEnumeration.TRUE);

      JourneyPattern jp = new JourneyPattern();
      neptuneObject.setJourneyPattern(jp);
      Route route = new Route();
      neptuneObject.setRoute(route);
      if (full) jp.setPublishedName("jp name");
      Line line = new Line();
      line.setObjectId("GTFS:Line:0123");

      route.setLine(line);
      if (full) route.setWayBack("A");
      
      
      int h = 23;
      int m = 59;
      int current_departure_offset = 0;
      int current_arrival_offset = 0;
      VehicleJourneyAtStop previous_vjas = null;
      
      /**
       * Mocking journey during more than one day
       */
      for (int i = 0; i < 4; i++)
      {
         StopPoint sp = new StopPoint();
         sp.setPosition(i*2);
         StopArea sa = new StopArea();
         sp.setObjectId("GTFS:StopPoint:SP"+i);
         sa.setObjectId("GTFS:StopPoint:SA"+i);

         ScheduledStopPoint scheduledStopPoint = new ScheduledStopPoint();
         scheduledStopPoint.setObjectId("GTFS:" + ObjectIdTypes.SCHEDULED_STOP_POINT_KEY + ":SSP" + i);
         scheduledStopPoint.setContainedInStopAreaRef(new SimpleObjectReference<>(sa));
         sp.setScheduledStopPoint(scheduledStopPoint);
         VehicleJourneyAtStop vjas = new VehicleJourneyAtStop();
         vjas.setStopPoint(sp);
         vjas.setArrivalDayOffset(current_arrival_offset);
         vjas.setDepartureDayOffset(current_departure_offset);
         vjas.setArrivalTime(new LocalTime(h,m,0));
         
         h = h + 1;
         if (h > 23)
         {
            h -= 24;
            
         }
      
         vjas.setDepartureTime(new LocalTime(h,m,0));
         
         if(previous_vjas == null) {
        	 if(vjas.getDepartureTime().isBefore(vjas.getArrivalTime())) {
        		 current_departure_offset = current_departure_offset + 1;
                 vjas.setDepartureDayOffset(current_departure_offset);
        	 }
         } else {
        	 if(vjas.getArrivalTime().isBefore(previous_vjas.getArrivalTime())) {
        		 current_arrival_offset = current_arrival_offset + 1;
                 vjas.setArrivalDayOffset(current_arrival_offset);
        	 }
        	 
        	 if(vjas.getDepartureTime().isBefore(previous_vjas.getDepartureTime())) {
        		 current_departure_offset = current_departure_offset + 1;
                 vjas.setDepartureDayOffset(current_departure_offset);
        	 }
         }
         h = h + 8;
         if (h > 23)
         {
            h -= 24;
         }
         
         Reporter.log("Current arrival offset : " + vjas.getArrivalDayOffset(), true);
         Reporter.log("Current departure offset : " + vjas.getDepartureDayOffset(), true);
         vjas.setVehicleJourney(neptuneObject);
         
         previous_vjas = vjas;
      }
      
      
      
      return neptuneObject;
   }


   private VehicleJourney buildNeptuneObjectWithOriginalStopIds()
   {
      VehicleJourney neptuneObject = new VehicleJourney();
      neptuneObject.setObjectId("GTFS:VehicleJourney:4321");
      neptuneObject.setNumber(456L);
      AccessibilityAssessment accessibilityAssessment = new AccessibilityAssessment();
      AccessibilityLimitation accessibilityLimitation = new AccessibilityLimitation();
      accessibilityLimitation.setWheelchairAccess(LimitationStatusEnumeration.TRUE);
      accessibilityAssessment.setAccessibilityLimitation(accessibilityLimitation);
      neptuneObject.setAccessibilityAssessment(accessibilityAssessment);
      JourneyPattern jp = new JourneyPattern();
      neptuneObject.setJourneyPattern(jp);
      Route route = new Route();
      neptuneObject.setRoute(route);
      jp.setPublishedName("jp name");
      Line line = new Line();
      line.setObjectId("GTFS:Line:0123");

      route.setLine(line);
      route.setWayBack("A");


      int h = 23;
      int m = 59;
      int current_departure_offset = 0;
      int current_arrival_offset = 0;
      VehicleJourneyAtStop previous_vjas = null;

      /**
       * Mocking journey during more than one day
       */
      for (int i = 0; i < 4; i++)
      {
         StopPoint sp = new StopPoint();
         sp.setPosition(i*2);
         StopArea sa = new StopArea();
         sp.setObjectId("GTFS:StopPoint:SP"+i);
         sa.setObjectId("GTFS:StopPoint:SA"+i);
         sa.setOriginalStopId("origSA"+i);
         sa.setAreaType(ChouetteAreaEnum.BoardingPosition);

         ScheduledStopPoint scheduledStopPoint = new ScheduledStopPoint();
         scheduledStopPoint.setObjectId("GTFS:" + ObjectIdTypes.SCHEDULED_STOP_POINT_KEY + ":SSP" + i);
         scheduledStopPoint.setContainedInStopAreaRef(new SimpleObjectReference<>(sa));
         sp.setScheduledStopPoint(scheduledStopPoint);
         VehicleJourneyAtStop vjas = new VehicleJourneyAtStop();
         vjas.setStopPoint(sp);
         vjas.setArrivalDayOffset(current_arrival_offset);
         vjas.setDepartureDayOffset(current_departure_offset);
         vjas.setArrivalTime(new LocalTime(h,m,0));

         h = h + 1;
         if (h > 23)
         {
            h -= 24;

         }

         vjas.setDepartureTime(new LocalTime(h,m,0));

         if(previous_vjas == null) {
            if(vjas.getDepartureTime().isBefore(vjas.getArrivalTime())) {
               current_departure_offset = current_departure_offset + 1;
               vjas.setDepartureDayOffset(current_departure_offset);
            }
         } else {
            if(vjas.getArrivalTime().isBefore(previous_vjas.getArrivalTime())) {
               current_arrival_offset = current_arrival_offset + 1;
               vjas.setArrivalDayOffset(current_arrival_offset);
            }

            if(vjas.getDepartureTime().isBefore(previous_vjas.getDepartureTime())) {
               current_departure_offset = current_departure_offset + 1;
               vjas.setDepartureDayOffset(current_departure_offset);
            }
         }
         h = h + 8;
         if (h > 23)
         {
            h -= 24;
         }

         Reporter.log("Current arrival offset : " + vjas.getArrivalDayOffset(), true);
         Reporter.log("Current departure offset : " + vjas.getDepartureDayOffset(), true);
         vjas.setVehicleJourney(neptuneObject);

         previous_vjas = vjas;
      }



      return neptuneObject;
   }

   private VehicleJourney buildNeptuneObjectWithCommercialPointsIds(boolean full)
   {
      VehicleJourney neptuneObject = new VehicleJourney();
      neptuneObject.setObjectId("GTFS:VehicleJourney:4321");
      // if (full) neptuneObject.setName("name");
      if (full) neptuneObject.setNumber(Long.valueOf(456));
      AccessibilityAssessment accessibilityAssessment = new AccessibilityAssessment();
      AccessibilityLimitation accessibilityLimitation = new AccessibilityLimitation();
      accessibilityAssessment.setAccessibilityLimitation(accessibilityLimitation);
      neptuneObject.setAccessibilityAssessment(accessibilityAssessment);
      if (full) neptuneObject.getAccessibilityAssessment().getAccessibilityLimitation().setWheelchairAccess(LimitationStatusEnumeration.TRUE);
      JourneyPattern jp = new JourneyPattern();
      neptuneObject.setJourneyPattern(jp);
      Route route = new Route();
      neptuneObject.setRoute(route);
      if (full) jp.setPublishedName("jp name");
      Line line = new Line();
      line.setObjectId("GTFS:Line:0123");

      route.setLine(line);
      if (full) route.setWayBack("A");


      int h = 23;
      int m = 59;
      int current_departure_offset = 0;
      int current_arrival_offset = 0;
      VehicleJourneyAtStop previous_vjas = null;

      /**
       * Mocking journey during more than one day
       */
      for (int i = 0; i < 4; i++)
      {
         StopPoint sp = new StopPoint();
         sp.setPosition(i*2);
         StopArea sa = new StopArea();
         sp.setObjectId("GTFS:StopPoint:SP"+i);
         sa.setObjectId("GTFS:StopPoint:SA"+i);
         sa.setOriginalStopId("origSA"+i);
         sa.setAreaType(ChouetteAreaEnum.CommercialStopPoint);

         ScheduledStopPoint scheduledStopPoint = new ScheduledStopPoint();
         scheduledStopPoint.setObjectId("GTFS:" + ObjectIdTypes.SCHEDULED_STOP_POINT_KEY + ":SSP" + i);
         scheduledStopPoint.setContainedInStopAreaRef(new SimpleObjectReference<>(sa));
         sp.setScheduledStopPoint(scheduledStopPoint);
         VehicleJourneyAtStop vjas = new VehicleJourneyAtStop();
         vjas.setStopPoint(sp);
         vjas.setArrivalDayOffset(current_arrival_offset);
         vjas.setDepartureDayOffset(current_departure_offset);
         vjas.setArrivalTime(new LocalTime(h,m,0));

         h = h + 1;
         if (h > 23)
         {
            h -= 24;

         }

         vjas.setDepartureTime(new LocalTime(h,m,0));

         if(previous_vjas == null) {
            if(vjas.getDepartureTime().isBefore(vjas.getArrivalTime())) {
               current_departure_offset = current_departure_offset + 1;
               vjas.setDepartureDayOffset(current_departure_offset);
            }
         } else {
            if(vjas.getArrivalTime().isBefore(previous_vjas.getArrivalTime())) {
               current_arrival_offset = current_arrival_offset + 1;
               vjas.setArrivalDayOffset(current_arrival_offset);
            }

            if(vjas.getDepartureTime().isBefore(previous_vjas.getDepartureTime())) {
               current_departure_offset = current_departure_offset + 1;
               vjas.setDepartureDayOffset(current_departure_offset);
            }
         }
         h = h + 8;
         if (h > 23)
         {
            h -= 24;
         }

         Reporter.log("Current arrival offset : " + vjas.getArrivalDayOffset(), true);
         Reporter.log("Current departure offset : " + vjas.getDepartureDayOffset(), true);
         vjas.setVehicleJourney(neptuneObject);

         previous_vjas = vjas;
      }



      return neptuneObject;
   }

}