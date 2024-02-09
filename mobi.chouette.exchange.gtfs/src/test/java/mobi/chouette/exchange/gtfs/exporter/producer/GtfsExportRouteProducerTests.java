package mobi.chouette.exchange.gtfs.exporter.producer;

import mobi.chouette.core.ChouetteException;
import mobi.chouette.exchange.gtfs.exporter.producer.mock.GtfsExporterMock;
import mobi.chouette.exchange.gtfs.model.GtfsRoute;
import mobi.chouette.exchange.gtfs.model.RouteTypeEnum;
import mobi.chouette.exchange.gtfs.model.exporter.RouteExporter;
import mobi.chouette.exchange.gtfs.model.importer.Context;
import mobi.chouette.exchange.gtfs.parameters.IdFormat;
import mobi.chouette.exchange.gtfs.parameters.IdParameters;
import mobi.chouette.model.Company;
import mobi.chouette.model.Line;

import mobi.chouette.model.Network;
import mobi.chouette.model.type.TransportModeNameEnum;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.Test;


public class GtfsExportRouteProducerTests 
{

   private GtfsExporterMock mock = new GtfsExporterMock();
   private GtfsRouteProducer producer = new GtfsRouteProducer(mock);
   private Context context = new Context();

   @Test(groups = { "Producers" }, description = "test route with both short and long name")
   public void verifyRouteProducerWithShortAndLongName() throws ChouetteException
   {
      mock.reset();

      Line neptuneObject = buildStandardLine();
      String expectedId = "4321";

      producer.save(neptuneObject, "GTFS",false,false,new IdParameters());
      Reporter.log("verifyRouteProducerWithShortAndLongName");
      Assert.assertEquals(mock.getExportedRoutes().size(), 1, "Route should be returned");
      GtfsRoute gtfsObject = mock.getExportedRoutes().get(0);
      Reporter.log(RouteExporter.CONVERTER.to(context, gtfsObject));

      Assert.assertEquals(gtfsObject.getRouteShortName(), neptuneObject.getNumber(), "RouteShortName must be line Number");
      Assert.assertEquals(gtfsObject.getRouteLongName(), neptuneObject.getPublishedName(), "RouteLongName must be correctly set");
      Assert.assertEquals(gtfsObject.getRouteUrl().toString(), neptuneObject.getUrl(), "RouteUrl must be correctly set");
      Assert.assertEquals(gtfsObject.getRouteColor().getRed(), Integer.parseInt("00",16), "RouteColor must be correctly set");
      Assert.assertEquals(gtfsObject.getRouteColor().getGreen(), Integer.parseInt("00",16), "RouteColor must be correctly set");
      Assert.assertEquals(gtfsObject.getRouteColor().getBlue(), Integer.parseInt("FF",16), "RouteColor must be correctly set");
      Assert.assertEquals(gtfsObject.getRouteTextColor().getRed(), Integer.parseInt("00",16), "RouteTextColor must be correctly set");
      Assert.assertEquals(gtfsObject.getRouteTextColor().getGreen(), Integer.parseInt("FF",16), "RouteTextColor must be correctly set");
      Assert.assertEquals(gtfsObject.getRouteTextColor().getBlue(), Integer.parseInt("00",16), "RouteTextColor must be correctly set");
      Assert.assertEquals(gtfsObject.getRouteId(), expectedId, "RouteId not generated correctly");
   }


   private static Line buildStandardLine(){
      Line neptuneObject = new Line();
      neptuneObject.setObjectId("GTFS:Line:4321");
      neptuneObject.setName("lineName");
      neptuneObject.setNumber("lineNumber");
      neptuneObject.setPublishedName("publishedLineName");
      neptuneObject.setUrl("http://www.line.fr");
      neptuneObject.setColor("0000FF");
      neptuneObject.setTextColor("00FF00");
      Company company = new Company();
      company.setObjectId("GTFS:Company:1234");
      company.setName("name");
      neptuneObject.setCompany(company);
      Network lineNetwork = new Network();
      neptuneObject.setNetwork(lineNetwork);
      return neptuneObject;
   }


   @Test(groups = { "Producers" }, description = "test route with both short and long name")
   public void verifyRouteID() throws ChouetteException
   {
      mock.reset();

      Line neptuneObject = buildStandardLine();
      String expectedId = "PREFIX4321";

      IdParameters idParams = new IdParameters("PREFIXSTOP",null,null,"PREFIX","");

      producer.save(neptuneObject, "GTFS",false,false,idParams);
      Reporter.log("verifyRouteID");
      Assert.assertEquals(mock.getExportedRoutes().size(), 1, "Route should be returned");
      GtfsRoute gtfsObject = mock.getExportedRoutes().get(0);
      Reporter.log(RouteExporter.CONVERTER.to(context, gtfsObject));
      Assert.assertEquals(gtfsObject.getRouteId(), expectedId, "RouteId not generated correctly");
   }

   @Test(groups = { "Producers" }, description = "test route with both short and long name")
   public void verifyRouteIDSuffix() throws ChouetteException
   {
      mock.reset();

      Line neptuneObject = buildStandardLine();
      String expectedId = "4321SUFFIX";

      IdParameters idParams = new IdParameters("PREFIXSTOP",null,"SUFFIX","","");

      producer.save(neptuneObject, "GTFS",false,false,idParams);
      Reporter.log("verifyRouteIDSuffix");
      Assert.assertEquals(mock.getExportedRoutes().size(), 1, "Route should be returned");
      GtfsRoute gtfsObject = mock.getExportedRoutes().get(0);
      Reporter.log(RouteExporter.CONVERTER.to(context, gtfsObject));
      Assert.assertEquals(gtfsObject.getRouteId(), expectedId, "RouteId not generated correctly");
   }

   @Test(groups = { "Producers" }, description = "test route with both short and long name")
   public void verifyRouteIDWithTridentFormatAndSuffix() throws ChouetteException
   {
      mock.reset();

      Line neptuneObject = buildStandardLine();
      String expectedId = "PREFIX:FlexibleLine:4321SUFFIX";

      IdParameters idParams = new IdParameters("PREFIXSTOP", IdFormat.TRIDENT,"SUFFIX","PREFIX","");
      producer.save(neptuneObject, "GTFS",false,false,idParams);
      Reporter.log("verifyRouteIDWithTridentFormatAndSuffix");
      Assert.assertEquals(mock.getExportedRoutes().size(), 1, "Route should be returned");
      GtfsRoute gtfsObject = mock.getExportedRoutes().get(0);
      Reporter.log(RouteExporter.CONVERTER.to(context, gtfsObject));

      Assert.assertEquals(gtfsObject.getRouteId(), expectedId, "Trident RouteId not generated correctly");
   }

   @Test(groups = { "Producers" }, description = "test route with both short and long name")
   public void verifyRouteIDWithTridentFormat() throws ChouetteException
   {
      mock.reset();

      Line neptuneObject = buildStandardLine();
      String expectedId = "PREFIX:FlexibleLine:4321";

      IdParameters idParams = new IdParameters("PREFIXSTOP", IdFormat.TRIDENT,null,"PREFIX","");
      producer.save(neptuneObject, "GTFS",false,false,idParams);
      Reporter.log("verifyRouteIDWithTridentFormat");
      Assert.assertEquals(mock.getExportedRoutes().size(), 1, "Route should be returned");
      GtfsRoute gtfsObject = mock.getExportedRoutes().get(0);
      Reporter.log(RouteExporter.CONVERTER.to(context, gtfsObject));

      Assert.assertEquals(gtfsObject.getRouteId(), expectedId, "Trident RouteId not generated correctly");
   }

   @Test(groups = { "Producers" }, description = "test route with no short name")
   public void verifyRouteProducerWithNoShortName() throws ChouetteException
   {
      mock.reset();

      Line neptuneObject = new Line();
      neptuneObject.setObjectId("GTFS:Line:4321");
      neptuneObject.setName("lineNname");
      neptuneObject.setPublishedName("publishedLineName");
      Company company = new Company();
      company.setObjectId("GTFS:Company:1234");
      company.setName("name");
      neptuneObject.setCompany(company);
      neptuneObject.setNetwork(new Network());
      producer.save(neptuneObject,"GTFS",false,false,new IdParameters());
      Reporter.log("verifyRouteProducerWithNoShortName");
      Assert.assertEquals(mock.getExportedRoutes().size(), 1, "Route should be returned");
      GtfsRoute gtfsObject = mock.getExportedRoutes().get(0);
      Reporter.log(RouteExporter.CONVERTER.to(context, gtfsObject));

      Assert.assertNull(gtfsObject.getRouteShortName(), "RouteShortName must null");
      Assert.assertEquals(gtfsObject.getRouteLongName(), neptuneObject.getPublishedName(), "RouteLongName must be correctly set");
      Assert.assertNull(gtfsObject.getRouteUrl(),  "RouteUrl must not be set");
      Assert.assertNull(gtfsObject.getRouteColor(),  "RouteColor must not be set");
      Assert.assertNull(gtfsObject.getRouteTextColor(),  "RouteTextColor must not be set");

   }

   @Test(groups = { "Producers" }, description = "test route with no long name")
   public void verifyRouteProducerWithNoLongName() throws ChouetteException
   {
      mock.reset();

      Line neptuneObject = new Line();
      neptuneObject.setObjectId("GTFS:Line:4321");
      neptuneObject.setNumber("lineNumber");
      Company company = new Company();
      company.setObjectId("GTFS:Company:1234");
      company.setName("name");
      Network lineNetwork = new Network();
      neptuneObject.setNetwork(lineNetwork);
      neptuneObject.setCompany(company);
      producer.save(neptuneObject,  "GTFS",false,false,new IdParameters());
      Reporter.log("verifyRouteProducerWithNoLongName");
      Assert.assertEquals(mock.getExportedRoutes().size(), 1, "Route should be returned");
      GtfsRoute gtfsObject = mock.getExportedRoutes().get(0);
      Reporter.log(RouteExporter.CONVERTER.to(context, gtfsObject));

      Assert.assertEquals(gtfsObject.getRouteShortName(), neptuneObject.getNumber(), "RouteShortName must be line Number");
      Assert.assertNull(gtfsObject.getRouteLongName(), "RouteLongName must be empty");

   }

   @Test(groups = { "Producers" }, description = "test route with no name")
   public void verifyRouteProducerWithNoName() throws ChouetteException
   {
      mock.reset();

      Line neptuneObject = new Line();
      neptuneObject.setObjectId("GTFS:Line:4321");
      Company company = new Company();
      company.setObjectId("GTFS:Company:1234");
      company.setName("name");
      neptuneObject.setCompany(company);
      boolean state = producer.save(neptuneObject, "GTFS",false,false,new IdParameters());
      Reporter.log("verifyRouteProducerWithNoName");
      Assert.assertFalse(state, "GTFS Route must not be produced");

   }

   @Test(groups = { "Producers" }, description = "test route with no name")
   public void verifyRouteProducerWithTransportTypeAir() throws Exception {
      mock.reset();

      GtfsRouteProducer routeProducer = new GtfsRouteProducer(mock);
      Line line = new Line();
      line.setObjectId("GTFS:Line:4321");
      line.setName("short name");

      Company company = new Company();
      company.setObjectId("TFS:Company:1234");
      line.setCompany(company);
      line.setTransportModeName(TransportModeNameEnum.Air);
      line.setNetwork(new Network());

      boolean result = routeProducer.save(line, "prefix", false,true,new IdParameters());
      Assert.assertTrue(result);
      Assert.assertEquals(mock.getExportedRoutes().size(), 1);
      Assert.assertEquals(mock.getExportedRoutes().get(0).getRouteType(), RouteTypeEnum.AirService);
   }
}
