package mobi.chouette.exchange.gtfs.parser;

import mobi.chouette.exchange.gtfs.model.GtfsShape;
import mobi.chouette.model.*;
import mobi.chouette.model.type.TransportModeNameEnum;
import mobi.chouette.model.type.TransportSubModeNameEnum;
import mobi.chouette.model.util.Referential;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static mobi.chouette.exchange.gtfs.parser.GtfsTripUtils.*;

public class GtfsTripParserTest {

    public GtfsTripParser gtfsTripParser;

    @BeforeMethod
    public void beforeEachTest() {
        gtfsTripParser = new GtfsTripParser();
    }

    // TER : 6 chiffres commençant par 8
    @Test
    public void testTerCode() {
        // nominal
        String tripHeadSign = "888888";
        Assert.assertEquals(gtfsTripParser.getSubModeFromTripHeadSign(tripHeadSign), TransportSubModeNameEnum.RegionalRail);

        // ne commence pas par 8
        tripHeadSign = "777777";
        Assert.assertNotEquals(gtfsTripParser.getSubModeFromTripHeadSign(tripHeadSign), TransportSubModeNameEnum.RegionalRail);

        // pas 8 chiffres
        tripHeadSign = "46513";
        Assert.assertNotEquals(gtfsTripParser.getSubModeFromTripHeadSign(tripHeadSign), TransportSubModeNameEnum.RegionalRail);

        // vide
        tripHeadSign = "";
        Assert.assertNotEquals(gtfsTripParser.getSubModeFromTripHeadSign(tripHeadSign), TransportSubModeNameEnum.RegionalRail);

        // null
        tripHeadSign = null;
        Assert.assertNotEquals(gtfsTripParser.getSubModeFromTripHeadSign(tripHeadSign), TransportSubModeNameEnum.RegionalRail);
    }

    // TGV : [4 chiffres OU BIEN 4 chiffres "slash" 2 chiffres OU BIEN 4 chiffres "slash" 4 chiffres] LE TOUT devant commencer par 8, 5 ou 7
    @Test
    public void testTGVCode() {
        // nominal 4 chiffres par 8
        String tripHeadSign = "8888";
        Assert.assertEquals(gtfsTripParser.getSubModeFromTripHeadSign(tripHeadSign), TransportSubModeNameEnum.LongDistance);

        // nominal 4 chiffres / 2 chiffres par 8
        tripHeadSign = "8888/88";
        Assert.assertEquals(gtfsTripParser.getSubModeFromTripHeadSign(tripHeadSign), TransportSubModeNameEnum.LongDistance);

        // nominal 4 chiffres / 4 chiffres par 8
        tripHeadSign = "8888/8888";
        Assert.assertEquals(gtfsTripParser.getSubModeFromTripHeadSign(tripHeadSign), TransportSubModeNameEnum.LongDistance);

        // nominal 4 chiffres par 5
        tripHeadSign = "5555";
        Assert.assertEquals(gtfsTripParser.getSubModeFromTripHeadSign(tripHeadSign), TransportSubModeNameEnum.LongDistance);

        // nominal 4 chiffres / 2 chiffres par 5
        tripHeadSign = "5555/55";
        Assert.assertEquals(gtfsTripParser.getSubModeFromTripHeadSign(tripHeadSign), TransportSubModeNameEnum.LongDistance);

        // nominal 4 chiffres / 4 chiffres par 5
        tripHeadSign = "5555/5555";
        Assert.assertEquals(gtfsTripParser.getSubModeFromTripHeadSign(tripHeadSign), TransportSubModeNameEnum.LongDistance);

        // nominal 4 chiffres par 7
        tripHeadSign = "7777";
        Assert.assertEquals(gtfsTripParser.getSubModeFromTripHeadSign(tripHeadSign), TransportSubModeNameEnum.LongDistance);

        // nominal 4 chiffres / 2 chiffres par 7
        tripHeadSign = "7777/77";
        Assert.assertEquals(gtfsTripParser.getSubModeFromTripHeadSign(tripHeadSign), TransportSubModeNameEnum.LongDistance);

        // nominal 4 chiffres / 2 chiffres par 7
        tripHeadSign = "7777/7777";
        Assert.assertEquals(gtfsTripParser.getSubModeFromTripHeadSign(tripHeadSign), TransportSubModeNameEnum.LongDistance);

        // pas le bon nombre de chiffres
        tripHeadSign = "333";
        Assert.assertNotEquals(gtfsTripParser.getSubModeFromTripHeadSign(tripHeadSign), TransportSubModeNameEnum.LongDistance);

        // vide
        tripHeadSign = "";
        Assert.assertNotEquals(gtfsTripParser.getSubModeFromTripHeadSign(tripHeadSign), TransportSubModeNameEnum.LongDistance);

        // null
        tripHeadSign = null;
        Assert.assertNotEquals(gtfsTripParser.getSubModeFromTripHeadSign(tripHeadSign), TransportSubModeNameEnum.LongDistance);
    }

    // IC : 4 chiffres commençant par 4 (Bordeaux-Marseille) ou 3 (Bordeaux-Nantes) ou 6 chiffres commençant par 1.
    @Test
    public void testICCode() {
        // nominal 4 chiffres par 4
        String tripHeadSign = "4444";
        Assert.assertEquals(gtfsTripParser.getSubModeFromTripHeadSign(tripHeadSign), TransportSubModeNameEnum.InterregionalRail);

        // nominal 4 chiffres par 3
        tripHeadSign = "3333";
        Assert.assertEquals(gtfsTripParser.getSubModeFromTripHeadSign(tripHeadSign), TransportSubModeNameEnum.InterregionalRail);

        // nominal 6 chiffres par 1
        tripHeadSign = "111111";
        Assert.assertEquals(gtfsTripParser.getSubModeFromTripHeadSign(tripHeadSign), TransportSubModeNameEnum.InterregionalRail);

        // 4 chiffres pas par 4 ou 3
        tripHeadSign = "1111";
        Assert.assertNotEquals(gtfsTripParser.getSubModeFromTripHeadSign(tripHeadSign), TransportSubModeNameEnum.InterregionalRail);

        // 6 chiffres pas par 1
        tripHeadSign = "222222";
        Assert.assertNotEquals(gtfsTripParser.getSubModeFromTripHeadSign(tripHeadSign), TransportSubModeNameEnum.InterregionalRail);

        // vide
        tripHeadSign = "";
        Assert.assertNotEquals(gtfsTripParser.getSubModeFromTripHeadSign(tripHeadSign), TransportSubModeNameEnum.InterregionalRail);

        // null
        tripHeadSign = null;
        Assert.assertNotEquals(gtfsTripParser.getSubModeFromTripHeadSign(tripHeadSign), TransportSubModeNameEnum.InterregionalRail);

    }

    @Test
    public void createRouteSectionsLoopStardEndTest() {
        Referential referential = new Referential();
        Route route = new Route();
        Line line = new Line();
        line.setTransportModeName(TransportModeNameEnum.Bus);
        route.setLine(line);
        JourneyPattern journeyPattern = new JourneyPattern();
        journeyPattern.setRoute(route);
        journeyPattern.setObjectId("TEST:JourneyPattern:JP1");
        journeyPattern.getStopPoints().add(initStopPoint("Q1","47.586373","1.324129"));
        journeyPattern.getStopPoints().add(initStopPoint("Q2","47.578662","1.322183"));
        journeyPattern.getStopPoints().add(initStopPoint("Q3","47.581562","1.329217"));
        journeyPattern.getStopPoints().add(initStopPoint("Q4","47.585385","1.335180"));
        journeyPattern.getStopPoints().add(initStopPoint("Q5","47.586531","1.335416"));
        journeyPattern.getStopPoints().add(initStopPoint("Q6","47.587546","1.333394"));
        journeyPattern.getStopPoints().add(initStopPoint("Q7","47.587552","1.328727"));
        journeyPattern.getStopPoints().add(initStopPoint("Q1","47.586373","1.324129"));

        List<RouteSection> routeSections = gtfsTripParser.createRouteSections(referential, journeyPattern, GtfsTripUtils.getGtfsTripLoopSameStartEnd());

        Assert.assertEquals(routeSections.size(), 7);
        RouteSection firstSegment = routeSections.get(0);
        Assert.assertEquals(firstSegment.getFromScheduledStopPoint().getContainedInStopAreaRef().getObjectId(), "TEST:StopArea:Q1");
        Assert.assertEquals(firstSegment.getToScheduledStopPoint().getContainedInStopAreaRef().getObjectId(), "TEST:StopArea:Q2");

        RouteSection lastSegment = routeSections.get(6);
        Assert.assertEquals(lastSegment.getFromScheduledStopPoint().getContainedInStopAreaRef().getObjectId(), "TEST:StopArea:Q7");
        Assert.assertEquals(lastSegment.getToScheduledStopPoint().getContainedInStopAreaRef().getObjectId(), "TEST:StopArea:Q1");

        for (RouteSection section : routeSections) {
            Assert.assertTrue(section.getDistance().signum() > 0);
        }
    }

    @Test
    public void createRouteSectionsNotEnoughCoordinatesTest() {
        Referential referential = new Referential();
        Route route = new Route();
        Line line = new Line();
        line.setTransportModeName(TransportModeNameEnum.Bus);
        route.setLine(line);
        JourneyPattern journeyPattern = new JourneyPattern();
        journeyPattern.setRoute(route);
        journeyPattern.setObjectId("TEST:JourneyPattern:JP1");
        journeyPattern.getStopPoints().add(initStopPoint("BLGAROU","47.586373","1.324129"));
        journeyPattern.getStopPoints().add(initStopPoint("BLFOIXA","47.578662","1.322183"));

        List<GtfsShape> gtfsShapes = new ArrayList<>();
        gtfsShapes.add(initGtfsShape("210", 0, "47.586372", "1.324129"));
        gtfsShapes.add(initGtfsShape("210", 1, "47.58637201", "1.324129"));
        gtfsShapes.add(initGtfsShape("210", 2, "47.58637202", "1.324129"));
        gtfsShapes.add(initGtfsShape("210", 3, "47.58637203", "1.324129"));

        List<RouteSection> routeSections = gtfsTripParser.createRouteSections(referential, journeyPattern, gtfsShapes);

        Assert.assertEquals(routeSections.size(), 0);
    }

    @Test
    public void createRouteSectionsLoopInsideTravelTest() {
        Referential referential = new Referential();
        Route route = new Route();
        Line line = new Line();
        line.setTransportModeName(TransportModeNameEnum.Bus);
        route.setLine(line);
        JourneyPattern journeyPattern = new JourneyPattern();
        journeyPattern.setRoute(route);
        journeyPattern.setObjectId("TEST:JourneyPattern:JP1");
        journeyPattern.getStopPoints().add(initStopPoint("Q1","44.856941","0.488340"));
        journeyPattern.getStopPoints().add(initStopPoint("Q2","44.853291","0.489525"));
        journeyPattern.getStopPoints().add(initStopPoint("Q3","44.851246","0.496818"));
        journeyPattern.getStopPoints().add(initStopPoint("Q4","44.851017","0.540536"));
        journeyPattern.getStopPoints().add(initStopPoint("Q5","44.851887","0.532041"));
        journeyPattern.getStopPoints().add(initStopPoint("Q6","44.847427","0.510430"));
        journeyPattern.getStopPoints().add(initStopPoint("Q7","44.853355","0.508444"));
        journeyPattern.getStopPoints().add(initStopPoint("Q8","44.851246","0.496818"));
        journeyPattern.getStopPoints().add(initStopPoint("Q9","44.854328","0.494515"));
        journeyPattern.getStopPoints().add(initStopPoint("Q10","44.856934","0.488536"));

        List<RouteSection> routeSections = gtfsTripParser.createRouteSections(referential, journeyPattern, getGtfsTripLoopInsideTravel());

        Assert.assertEquals(routeSections.size(), 9);
        for (int i = 0; i < routeSections.size(); i++) {
            RouteSection segment = routeSections.get(i);
            int indexStart = i+1;
            int indexEnd = i+2;
            Assert.assertEquals(segment.getFromScheduledStopPoint().getContainedInStopAreaRef().getObjectId(), "TEST:StopArea:Q"+ indexStart);
            Assert.assertEquals(segment.getToScheduledStopPoint().getContainedInStopAreaRef().getObjectId(), "TEST:StopArea:Q" +indexEnd);
            Assert.assertTrue(segment.getDistance().signum() > 0);
        }

    }

    @Test
    public void createRouteSectionsMultipleLoopsInsideTravelTest() {
        Referential referential = new Referential();
        Route route = new Route();
        Line line = new Line();
        line.setTransportModeName(TransportModeNameEnum.Bus);
        route.setLine(line);
        JourneyPattern journeyPattern = new JourneyPattern();
        journeyPattern.setRoute(route);
        journeyPattern.setObjectId("TEST:JourneyPattern:JP1");
        journeyPattern.getStopPoints().add(initStopPoint("Q1","44.856941","0.488340"));
        journeyPattern.getStopPoints().add(initStopPoint("Q2","44.852818","0.487270"));
        journeyPattern.getStopPoints().add(initStopPoint("Q3","44.853470","0.483342"));
        journeyPattern.getStopPoints().add(initStopPoint("Q4","44.850552","0.481649"));
        journeyPattern.getStopPoints().add(initStopPoint("Q5","44.846207","0.481422"));
        journeyPattern.getStopPoints().add(initStopPoint("Q6","44.845146","0.480677"));
        journeyPattern.getStopPoints().add(initStopPoint("Q7","44.844116","0.470519"));
        journeyPattern.getStopPoints().add(initStopPoint("Q8","44.842514","0.466968"));
        journeyPattern.getStopPoints().add(initStopPoint("Q9","44.839153","0.459263"));
        journeyPattern.getStopPoints().add(initStopPoint("Q10","44.836578","0.453396"));
        journeyPattern.getStopPoints().add(initStopPoint("Q11","44.836105","0.445569"));
        journeyPattern.getStopPoints().add(initStopPoint("Q12","44.836105","0.445569"));
        journeyPattern.getStopPoints().add(initStopPoint("Q13","44.836445","0.453571"));
        journeyPattern.getStopPoints().add(initStopPoint("Q14","44.839054","0.459378"));
        journeyPattern.getStopPoints().add(initStopPoint("Q15","44.842445","0.467001"));
        journeyPattern.getStopPoints().add(initStopPoint("Q16","44.844044","0.470637"));
        journeyPattern.getStopPoints().add(initStopPoint("Q17","44.845142","0.480471"));
        journeyPattern.getStopPoints().add(initStopPoint("Q18","44.846554","0.482177"));
        journeyPattern.getStopPoints().add(initStopPoint("Q19","44.849735","0.484596"));
        journeyPattern.getStopPoints().add(initStopPoint("Q20","44.853470","0.483342"));
        journeyPattern.getStopPoints().add(initStopPoint("Q21","44.855637","0.484144"));
        journeyPattern.getStopPoints().add(initStopPoint("Q22","44.856941","0.488340"));

        List<RouteSection> routeSections = gtfsTripParser.createRouteSections(referential, journeyPattern, getGtfsTripLoopSameMultipleLoops());

        Assert.assertEquals(routeSections.size(), 21);
        for (int i = 0; i < routeSections.size(); i++) {
            RouteSection segment = routeSections.get(i);
            int indexStart = i+1;
            int indexEnd = i+2;
            Assert.assertEquals(segment.getFromScheduledStopPoint().getContainedInStopAreaRef().getObjectId(), "TEST:StopArea:Q"+ indexStart);
            Assert.assertEquals(segment.getToScheduledStopPoint().getContainedInStopAreaRef().getObjectId(), "TEST:StopArea:Q" +indexEnd);
        }

    }

    private static StopPoint initStopPoint(String id, String latitude, String longitude) {
        StopArea stopArea = new StopArea();
        stopArea.setLatitude(new BigDecimal(latitude));
        stopArea.setLongitude(new BigDecimal(longitude));
        stopArea.setObjectId("TEST:StopArea:"+id);
        StopPoint stopPoint = new StopPoint();
        ScheduledStopPoint scheduledStopPoint = new ScheduledStopPoint();
        scheduledStopPoint.setContainedInStopAreaRef(new SimpleObjectReference<>(stopArea));
        stopPoint.setScheduledStopPoint(scheduledStopPoint);
        return stopPoint;
    }



}