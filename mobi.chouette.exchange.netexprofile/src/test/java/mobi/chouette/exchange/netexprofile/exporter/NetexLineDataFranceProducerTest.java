package mobi.chouette.exchange.netexprofile.exporter;


import mobi.chouette.common.Context;
import mobi.chouette.common.TimeUtil;
import mobi.chouette.exchange.netexprofile.Constant;
import mobi.chouette.exchange.netexprofile.JobDataTest;
import mobi.chouette.exchange.netexprofile.jaxb.NetexXMLProcessingHelperFactory;
import mobi.chouette.exchange.report.ActionReport;
import mobi.chouette.model.DestinationDisplay;
import mobi.chouette.model.JourneyPattern;
import mobi.chouette.model.Line;
import mobi.chouette.model.Network;
import mobi.chouette.model.Route;
import mobi.chouette.model.ScheduledStopPoint;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.Train;
import mobi.chouette.model.VehicleJourney;
import mobi.chouette.model.*;
import mobi.chouette.model.type.PTDirectionEnum;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.rutebanken.netex.model.*;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import java.io.File;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static mobi.chouette.common.Constant.*;
import static mobi.chouette.exchange.netexprofile.Constant.EXPORTABLE_NETEX_DATA;
import static mobi.chouette.exchange.netexprofile.Constant.MARSHALLER;

public class NetexLineDataFranceProducerTest {

    @Test
    public void exportOffreFrance() throws Exception {

        Context context = createContext();

        context.put(CREATION_DATE,LocalDateTime.now());

        NetexLineDataFranceProducer netexLineDataFranceProducer = new NetexLineDataFranceProducer();
        netexLineDataFranceProducer.produce(context);

        ExportableNetexData exportableNetexDataResult = (ExportableNetexData) context.get(EXPORTABLE_NETEX_DATA);

        Assert.assertEquals(exportableNetexDataResult.getRoutes().get(0).getId(), "TEST:Route:r1:LOC");
        Assert.assertEquals(exportableNetexDataResult.getRoutes().get(0).getVersion(), "any");
        Assert.assertEquals(exportableNetexDataResult.getRoutes().get(0).getLineRef().getValue().getRef(), "TEST:FlexibleLine:l1:LOC");
//        Assert.assertEquals(exportableNetexDataResult.getRoutes().get(0).getLineRef().getValue().getValue(), "version=\"any\"");
        Assert.assertEquals(exportableNetexDataResult.getRoutes().get(0).getDirectionRef().getRef(), "TEST:Direction:r1:LOC");
        Assert.assertEquals(exportableNetexDataResult.getRoutes().get(0).getDirectionRef().getVersion(), "any");

        Assert.assertEquals(exportableNetexDataResult.getDirections().get(0).getId(),"TEST:Direction:r1:LOC");
        Assert.assertEquals(exportableNetexDataResult.getDirections().get(0).getId(), exportableNetexDataResult.getRoutes().get(0).getDirectionRef().getRef());
        Assert.assertEquals(exportableNetexDataResult.getDirections().get(0).getVersion(), "any");

        Assert.assertEquals(exportableNetexDataResult.getServiceJourneyPatterns().get(0).getId(), "TEST:ServiceJourneyPattern:jp1:LOC");
        Assert.assertEquals(exportableNetexDataResult.getServiceJourneyPatterns().get(0).getVersion(), "any");
        Assert.assertEquals(exportableNetexDataResult.getServiceJourneyPatterns().get(0).getName().getValue(), "Test Journey Pattern");
        Assert.assertEquals(exportableNetexDataResult.getServiceJourneyPatterns().get(0).getRouteRef().getRef(), "TEST:Route:r1:LOC");
        Assert.assertEquals(exportableNetexDataResult.getServiceJourneyPatterns().get(0).getRouteRef().getRef(), exportableNetexDataResult.getRoutes().get(0).getId());
        Assert.assertEquals(exportableNetexDataResult.getServiceJourneyPatterns().get(0).getRouteRef().getVersion(), "any");
        Assert.assertEquals(exportableNetexDataResult.getServiceJourneyPatterns().get(0).getDestinationDisplayRef().getRef(), "TEST:DestinationDisplay:dd1:LOC");
        Assert.assertEquals(exportableNetexDataResult.getServiceJourneyPatterns().get(0).getDestinationDisplayRef().getRef(), exportableNetexDataResult.getDestinationDisplays().get("TEST:DestinationDisplay:dd1:LOC").getId());
        Assert.assertEquals(exportableNetexDataResult.getServiceJourneyPatterns().get(0).getDestinationDisplayRef().getVersion(), "any");
        Assert.assertEquals(exportableNetexDataResult.getServiceJourneyPatterns().get(0).getServiceJourneyPatternType().value(), "passenger");

        Assert.assertEquals(exportableNetexDataResult.getServiceJourneyPatterns().get(0).getPointsInSequence().getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern().get(0).getId(), "TEST:StopPointInJourneyPattern:sp1:LOC");
        Assert.assertEquals(exportableNetexDataResult.getServiceJourneyPatterns().get(0).getPointsInSequence().getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern().get(0).getOrder(), BigInteger.valueOf(1));
        Assert.assertEquals(exportableNetexDataResult.getServiceJourneyPatterns().get(0).getPointsInSequence().getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern().get(0).getVersion(), "any");

        Assert.assertEquals(exportableNetexDataResult.getServiceJourneyPatterns().get(0).getPointsInSequence().getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern().get(1).getId(), "TEST:StopPointInJourneyPattern:sp2:LOC");
        Assert.assertEquals(exportableNetexDataResult.getServiceJourneyPatterns().get(0).getPointsInSequence().getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern().get(1).getOrder(), BigInteger.valueOf(2));
        Assert.assertEquals(exportableNetexDataResult.getServiceJourneyPatterns().get(0).getPointsInSequence().getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern().get(1).getVersion(), "any");

        Assert.assertEquals(exportableNetexDataResult.getServiceJourneyPatterns().get(0).getPointsInSequence().getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern().get(2).getId(), "TEST:StopPointInJourneyPattern:sp3:LOC");
        Assert.assertEquals(exportableNetexDataResult.getServiceJourneyPatterns().get(0).getPointsInSequence().getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern().get(2).getOrder(), BigInteger.valueOf(3));
        Assert.assertEquals(exportableNetexDataResult.getServiceJourneyPatterns().get(0).getPointsInSequence().getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern().get(2).getVersion(), "any");

        Assert.assertEquals(exportableNetexDataResult.getScheduledStopPoints().get("TEST:ScheduledStopPoint:ssp1:LOC").getId(), "TEST:ScheduledStopPoint:ssp1:LOC");
        Assert.assertEquals(exportableNetexDataResult.getScheduledStopPoints().get("TEST:ScheduledStopPoint:ssp2:LOC").getId(), "TEST:ScheduledStopPoint:ssp2:LOC");
        Assert.assertEquals(exportableNetexDataResult.getScheduledStopPoints().get("TEST:ScheduledStopPoint:ssp3:LOC").getId(), "TEST:ScheduledStopPoint:ssp3:LOC");

        Assert.assertEquals(exportableNetexDataResult.getScheduledStopPoints().get("TEST:ScheduledStopPoint:ssp1:LOC").getVersion(), "any");
        Assert.assertEquals(exportableNetexDataResult.getScheduledStopPoints().get("TEST:ScheduledStopPoint:ssp2:LOC").getVersion(), "any");
        Assert.assertEquals(exportableNetexDataResult.getScheduledStopPoints().get("TEST:ScheduledStopPoint:ssp3:LOC").getVersion(), "any");

        Assert.assertEquals(exportableNetexDataResult.getStopAssignments().get("TEST:PassengerStopAssignment:ssp1:LOC").getId(), "TEST:PassengerStopAssignment:ssp1:LOC");
        Assert.assertEquals(exportableNetexDataResult.getStopAssignments().get("TEST:PassengerStopAssignment:ssp2:LOC").getId(), "TEST:PassengerStopAssignment:ssp2:LOC");
        Assert.assertEquals(exportableNetexDataResult.getStopAssignments().get("TEST:PassengerStopAssignment:ssp3:LOC").getId(), "TEST:PassengerStopAssignment:ssp3:LOC");

        Assert.assertEquals(exportableNetexDataResult.getStopAssignments().get("TEST:PassengerStopAssignment:ssp1:LOC").getVersion(), "any");
        Assert.assertEquals(exportableNetexDataResult.getStopAssignments().get("TEST:PassengerStopAssignment:ssp2:LOC").getVersion(), "any");
        Assert.assertEquals(exportableNetexDataResult.getStopAssignments().get("TEST:PassengerStopAssignment:ssp3:LOC").getVersion(), "any");

        Assert.assertEquals(exportableNetexDataResult.getStopAssignments().get("TEST:PassengerStopAssignment:ssp1:LOC").getOrder(), BigInteger.valueOf(0));
        Assert.assertEquals(exportableNetexDataResult.getStopAssignments().get("TEST:PassengerStopAssignment:ssp2:LOC").getOrder(), BigInteger.valueOf(0));
        Assert.assertEquals(exportableNetexDataResult.getStopAssignments().get("TEST:PassengerStopAssignment:ssp3:LOC").getOrder(), BigInteger.valueOf(0));

        Assert.assertEquals(exportableNetexDataResult.getStopAssignments().get("TEST:PassengerStopAssignment:ssp1:LOC").getId(), "TEST:PassengerStopAssignment:ssp1:LOC");
        Assert.assertEquals(exportableNetexDataResult.getStopAssignments().get("TEST:PassengerStopAssignment:ssp2:LOC").getId(), "TEST:PassengerStopAssignment:ssp2:LOC");
        Assert.assertEquals(exportableNetexDataResult.getStopAssignments().get("TEST:PassengerStopAssignment:ssp3:LOC").getId(), "TEST:PassengerStopAssignment:ssp3:LOC");

        Assert.assertEquals(exportableNetexDataResult.getStopAssignments().get("TEST:PassengerStopAssignment:ssp1:LOC").getScheduledStopPointRef().getValue().getRef(), "TEST:ScheduledStopPoint:ssp1:LOC");
        Assert.assertEquals(exportableNetexDataResult.getStopAssignments().get("TEST:PassengerStopAssignment:ssp2:LOC").getScheduledStopPointRef().getValue().getRef(), "TEST:ScheduledStopPoint:ssp2:LOC");
        Assert.assertEquals(exportableNetexDataResult.getStopAssignments().get("TEST:PassengerStopAssignment:ssp3:LOC").getScheduledStopPointRef().getValue().getRef(), "TEST:ScheduledStopPoint:ssp3:LOC");

        Assert.assertEquals(exportableNetexDataResult.getStopAssignments().get("TEST:PassengerStopAssignment:ssp1:LOC").getScheduledStopPointRef().getValue().getVersion(), "any");
        Assert.assertEquals(exportableNetexDataResult.getStopAssignments().get("TEST:PassengerStopAssignment:ssp2:LOC").getScheduledStopPointRef().getValue().getVersion(), "any");
        Assert.assertEquals(exportableNetexDataResult.getStopAssignments().get("TEST:PassengerStopAssignment:ssp3:LOC").getScheduledStopPointRef().getValue().getVersion(), "any");

        Assert.assertEquals(exportableNetexDataResult.getStopAssignments().get("TEST:PassengerStopAssignment:ssp1:LOC").getQuayRef().getValue().getRef(), "TEST:Quay:quay1");
        Assert.assertEquals(exportableNetexDataResult.getStopAssignments().get("TEST:PassengerStopAssignment:ssp2:LOC").getQuayRef().getValue().getRef(), "TEST:Quay:quay2");
        Assert.assertEquals(exportableNetexDataResult.getStopAssignments().get("TEST:PassengerStopAssignment:ssp3:LOC").getQuayRef().getValue().getRef(), "TEST:Quay:quay3");

        Assert.assertEquals(exportableNetexDataResult.getDestinationDisplays().get("TEST:DestinationDisplay:dd1:LOC").getId(), "TEST:DestinationDisplay:dd1:LOC");
        Assert.assertEquals(exportableNetexDataResult.getDestinationDisplays().get("TEST:DestinationDisplay:dd1:LOC").getVersion(), "any");
        Assert.assertEquals(exportableNetexDataResult.getDestinationDisplays().get("TEST:DestinationDisplay:dd1:LOC").getFrontText().getValue(), "Test Destination Display");

        Assert.assertEquals(exportableNetexDataResult.getServiceJourneys().get(0).getId(), "TEST:ServiceJourney:vj1:LOC");
        Assert.assertEquals(exportableNetexDataResult.getServiceJourneys().get(0).getVersion(), "any");
        Assert.assertEquals(exportableNetexDataResult.getServiceJourneys().get(0).getName().getValue(), "Test vehicle journey name");
        Assert.assertEquals(exportableNetexDataResult.getServiceJourneys().get(0).getJourneyPatternRef().getValue().getRef(), "TEST:ServiceJourneyPattern:jp1:LOC");
        Assert.assertEquals(exportableNetexDataResult.getServiceJourneys().get(0).getJourneyPatternRef().getValue().getVersion(), "any");
        Assert.assertEquals(exportableNetexDataResult.getServiceJourneys().get(0).getDayTypes().getDayTypeRef().get(0).getValue().getRef(), "TEST:DayType:t1:LOC");


        Assert.assertEquals(exportableNetexDataResult.getServiceJourneys().get(0).getPassingTimes().getTimetabledPassingTime().get(0).getDepartureTime(), TimeUtil.toLocalTimeFromJoda(new LocalTime(7, 0, 0)));
        Assert.assertEquals(exportableNetexDataResult.getServiceJourneys().get(0).getPassingTimes().getTimetabledPassingTime().get(0).getVersion(), "any");
        Assert.assertEquals(exportableNetexDataResult.getServiceJourneys().get(0).getPassingTimes().getTimetabledPassingTime().get(0).getArrivalTime(), TimeUtil.toLocalTimeFromJoda(new LocalTime(7, 0, 0)));

        Assert.assertEquals(exportableNetexDataResult.getServiceJourneys().get(0).getPassingTimes().getTimetabledPassingTime().get(1).getDepartureTime(), TimeUtil.toLocalTimeFromJoda(new LocalTime(7, 15, 0)));
        Assert.assertEquals(exportableNetexDataResult.getServiceJourneys().get(0).getPassingTimes().getTimetabledPassingTime().get(1).getVersion(), "any");
        Assert.assertEquals(exportableNetexDataResult.getServiceJourneys().get(0).getPassingTimes().getTimetabledPassingTime().get(1).getArrivalTime(), TimeUtil.toLocalTimeFromJoda(new LocalTime(7, 15, 0)));

        Assert.assertEquals(exportableNetexDataResult.getServiceJourneys().get(0).getPassingTimes().getTimetabledPassingTime().get(2).getDepartureTime(), TimeUtil.toLocalTimeFromJoda(new LocalTime(7, 30, 0)));
        Assert.assertEquals(exportableNetexDataResult.getServiceJourneys().get(0).getPassingTimes().getTimetabledPassingTime().get(2).getVersion(), "any");
        Assert.assertEquals(exportableNetexDataResult.getServiceJourneys().get(0).getPassingTimes().getTimetabledPassingTime().get(2).getArrivalTime(), TimeUtil.toLocalTimeFromJoda(new LocalTime(7, 30, 0)));

        Assert.assertEquals(exportableNetexDataResult.getSharedDayTypes().get("TEST:DayType:t1:LOC").getId(), "TEST:DayType:t1:LOC");
        Assert.assertEquals(exportableNetexDataResult.getSharedDayTypes().get("TEST:DayType:t1:LOC").getVersion(), "any");

        Assert.assertEquals(exportableNetexDataResult.getSharedOperatingPeriods().get("TEST:OperatingPeriod:t1-0:LOC").getId(), "TEST:OperatingPeriod:t1-0:LOC");
        Assert.assertEquals(exportableNetexDataResult.getSharedOperatingPeriods().get("TEST:OperatingPeriod:t1-0:LOC").getVersion(), "any");

        Assert.assertEquals(exportableNetexDataResult.getSharedOperatingPeriods().get("TEST:OperatingPeriod:t1-0:LOC").getFromDate(), LocalDateTime.of(2020, 1, 1, 0, 0, 0));
        Assert.assertEquals(exportableNetexDataResult.getSharedOperatingPeriods().get("TEST:OperatingPeriod:t1-0:LOC").getToDate(), LocalDateTime.of(2020, 12, 31, 0, 0, 0));

        List<JAXBElement<? extends DayTypeRefStructure>> dayTypesRef = exportableNetexDataResult.getSharedDayTypeAssignments().stream().map(DayTypeAssignment_VersionStructure::getDayTypeRef).collect(Collectors.toList());
        Assert.assertEquals(dayTypesRef.get(0).getValue().getRef(), "TEST:DayType:t1:LOC");
        Assert.assertEquals(dayTypesRef.get(0).getValue().getVersion(), "any");

        List<OperatingPeriodRefStructure> operatingPeriodsRef = exportableNetexDataResult.getSharedDayTypeAssignments().stream().map(opr -> opr.getOperatingPeriodRef().getValue()).collect(Collectors.toList());
        Assert.assertEquals(operatingPeriodsRef.get(0).getRef(), "TEST:OperatingPeriod:t1-0:LOC");
        Assert.assertEquals(operatingPeriodsRef.get(0).getVersion(), "any");

        Assert.assertEquals(exportableNetexDataResult.getSharedNotices().get("TEST:Notice:f1:LOC").getId(), "TEST:Notice:f1:LOC");
        Assert.assertEquals(exportableNetexDataResult.getSharedNotices().get("TEST:Notice:f1:LOC").getVersion(), "any");
        Assert.assertEquals(exportableNetexDataResult.getSharedNotices().get("TEST:Notice:f1:LOC").getTypeOfNoticeRef().getRef(), "ServiceJourneyNotice");
        Assert.assertEquals(exportableNetexDataResult.getSharedNotices().get("TEST:Notice:f1:LOC").getPublicCode(), "Test Code");
        Assert.assertEquals(exportableNetexDataResult.getSharedNotices().get("TEST:Notice:f1:LOC").getText().getValue(), "Test Label");

        NoticeAssignment noticeAssignment =
                (NoticeAssignment) exportableNetexDataResult.getServiceJourneys().get(0).getNoticeAssignments().getNoticeAssignment_OrNoticeAssignmentView().get(0).getValue();

        Assert.assertTrue(noticeAssignment.getId().startsWith("TEST:NoticeAssignment:"));
        Assert.assertTrue(noticeAssignment.getId().endsWith(":LOC"));
        Assert.assertEquals(noticeAssignment.getVersion(), "any");
        Assert.assertEquals(noticeAssignment.getOrder(), BigInteger.valueOf(0));
        Assert.assertEquals(noticeAssignment.getNoticeRef().getRef(), "TEST:Notice:f1:LOC");
        Assert.assertEquals(noticeAssignment.getNoticeRef().getRef(), exportableNetexDataResult.getSharedNotices().get("TEST:Notice:f1:LOC").getId());
        Assert.assertEquals(noticeAssignment.getNoticeRef().getValue(), "version=\"any\"");

        Assert.assertEquals(exportableNetexDataResult.getTrainNumbers().size(), 3, "should have 3 trains");

        TrainNumber trainNumberBordeauxMarseille = exportableNetexDataResult.getTrainNumbers().get(0);
        TrainNumber trainNumberMarseilleLyon = exportableNetexDataResult.getTrainNumbers().get(1);
        TrainNumber trainNumberLyonZurich = exportableNetexDataResult.getTrainNumbers().get(2);

        Assert.assertEquals(trainNumberBordeauxMarseille.getId(), "FR:TrainNumber:1", "wrong id");
        Assert.assertEquals(trainNumberBordeauxMarseille.getForAdvertisement(), "357224", "wrong for advertisement");
        Assert.assertEquals(trainNumberBordeauxMarseille.getDescription().getValue(), "Bordeaux - Marseille", "wrong description");
        Assert.assertEquals(trainNumberBordeauxMarseille.getVersion(), "any", "wrong version");

        Assert.assertEquals(trainNumberMarseilleLyon.getId(), "FR:TrainNumber:2", "wrong id");
        Assert.assertEquals(trainNumberMarseilleLyon.getForAdvertisement(), "11997766", "wrong for advertisement");
        Assert.assertEquals(trainNumberMarseilleLyon.getDescription().getValue(), "Marseille - Lyon", "wrong description");
        Assert.assertEquals(trainNumberMarseilleLyon.getVersion(), "1.0", "wrong version");

        Assert.assertEquals(trainNumberLyonZurich.getId(), "FR:TrainNumber:3", "wrong id");
        Assert.assertEquals(trainNumberLyonZurich.getForAdvertisement(), "123698745", "wrong for advertisement");
        Assert.assertEquals(trainNumberLyonZurich.getDescription().getValue(), "Lyon - Zurich", "wrong description");
        Assert.assertEquals(trainNumberLyonZurich.getVersion(), "any", "wrong version");

        deleteFileCreated();

    }

    private void deleteFileCreated() {
        File file = new File("src/test/data/idfm/output/offre_TestCodifligne_.xml");
        if(file.delete()){
            System.out.println("Fichier de test supprimé");
        }else{
            System.out.println("ERREUR fichier de test non supprimé");
        }
    }

    private Context createContext() throws JAXBException {

        File file = new File("src/test/data/idfm/output/offre_TestCodifligne_.xml");
        file.getParentFile().mkdirs();

        Network network = new Network();
        Company company = new Company();
        company.setCode("myComp");
        company.setEmail("email@okinatest.com");
        company.setFareUrl("www.okinatest.com");
        company.setName("Okina test");
        company.setObjectId("Test:Company:1");

        network.setCompany(company);
        network.setObjectId("Test:Network:1");


        Line line = new Line();
        line.setObjectId("TEST:Line:l1");
        line.setRegistrationNumber("l1");
        line.setCodifligne("TestCodifligne");
        line.setNetwork(network);
        line.setFlexibleService(true);

        Route route  = new Route();
        route.setObjectId("TEST:Route:r1");
        route.setName("Test Route");
        route.setLine(line);
        route.setDirection(PTDirectionEnum.A);

        DestinationDisplay destinationDisplay = new DestinationDisplay();
        destinationDisplay.setObjectId("TEST:DestinationDisplay:dd1");
        destinationDisplay.setFrontText("Test Destination Display");

        StopArea stopArea1 = new StopArea();
        stopArea1.setObjectId("TEST:Quay:quay1");


        StopArea stopArea2 = new StopArea();
        stopArea2.setObjectId("TEST:Quay:quay2");


        StopArea stopArea3 = new StopArea();
        stopArea3.setObjectId("TEST:Quay:quay3");


        ScheduledStopPoint scheduledStopPoint1 = new ScheduledStopPoint();
        scheduledStopPoint1.setObjectId("TEST:ScheduledStopPoint:ssp1");
        scheduledStopPoint1.setContainedInStopAreaRef(new SimpleObjectReference(stopArea1));

        ScheduledStopPoint scheduledStopPoint2 = new ScheduledStopPoint();
        scheduledStopPoint2.setObjectId("TEST:ScheduledStopPoint:ssp2");
        scheduledStopPoint2.setContainedInStopAreaRef(new SimpleObjectReference(stopArea2));

        ScheduledStopPoint scheduledStopPoint3 = new ScheduledStopPoint();
        scheduledStopPoint3.setObjectId("TEST:ScheduledStopPoint:ssp3");
        scheduledStopPoint3.setContainedInStopAreaRef(new SimpleObjectReference(stopArea3));


        StopPoint stopPoint1 = new StopPoint();
        stopPoint1.setObjectId("TEST:StopPoint:sp1");
        stopPoint1.setPosition(0);
        stopPoint1.setScheduledStopPoint(scheduledStopPoint1);
        stopPoint1.setRoute(route);

        StopPoint stopPoint2 = new StopPoint();
        stopPoint2.setObjectId("TEST:StopPoint:sp2");
        stopPoint2.setPosition(1);
        stopPoint2.setScheduledStopPoint(scheduledStopPoint2);
        stopPoint2.setRoute(route);

        StopPoint stopPoint3 = new StopPoint();
        stopPoint3.setObjectId("TEST:StopPoint:sp3");
        stopPoint3.setPosition(2);
        stopPoint3.setScheduledStopPoint(scheduledStopPoint3);
        stopPoint3.setDestinationDisplay(destinationDisplay);
        stopPoint3.setRoute(route);

        JourneyPattern journeyPattern = new JourneyPattern();
        journeyPattern.setObjectId("TEST:JourneyPattern:jp1");
        journeyPattern.setName("Test Journey Pattern");
        journeyPattern.setRoute(route);
        List<StopPoint> stopPoints = new ArrayList<>();
        stopPoints.add(stopPoint1);
        stopPoints.add(stopPoint2);
        stopPoints.add(stopPoint3);
        journeyPattern.setStopPoints(stopPoints);
        route.setStopPoints(stopPoints);


        Timetable timetable = new Timetable();
        timetable.setObjectId("TEST:Timetable:t1");
        Period period = new Period();
        LocalDate startLocalDate = new LocalDate("2020-01-01");
        LocalDate endLocalDate = new LocalDate("2020-12-31");
        period.setStartDate(startLocalDate);
        period.setEndDate(endLocalDate);
        ArrayList<Period> periods = new ArrayList<>();
        periods.add(period);
        timetable.setPeriods(periods);

        List<Timetable> timetables = new ArrayList<>();
        timetables.add(timetable);

        HashSet<Timetable> timetableHashSet = new HashSet<>();
        timetableHashSet.add(timetable);


        VehicleJourneyAtStop vehicleJourneyAtStop1 = new VehicleJourneyAtStop();
        vehicleJourneyAtStop1.setObjectId("TEST:TimetablePassingTime:tpt1");
        LocalTime time1 = new LocalTime(7, 0, 0);
        vehicleJourneyAtStop1.setDepartureTime(time1);
        vehicleJourneyAtStop1.setArrivalTime(time1);
        vehicleJourneyAtStop1.setStopPoint(stopPoint1);

        VehicleJourneyAtStop vehicleJourneyAtStop2 = new VehicleJourneyAtStop();
        vehicleJourneyAtStop2.setObjectId("TEST:TimetablePassingTime:tpt2");
        LocalTime time2 = new LocalTime(7, 15, 0);
        vehicleJourneyAtStop2.setDepartureTime(time2);
        vehicleJourneyAtStop2.setArrivalTime(time2);
        vehicleJourneyAtStop2.setStopPoint(stopPoint2);

        VehicleJourneyAtStop vehicleJourneyAtStop3 = new VehicleJourneyAtStop();
        vehicleJourneyAtStop3.setObjectId("TEST:TimetablePassingTime:tpt3");
        LocalTime time3 = new LocalTime(7, 30, 0);
        vehicleJourneyAtStop3.setDepartureTime(time3);
        vehicleJourneyAtStop3.setArrivalTime(time3);
        vehicleJourneyAtStop3.setStopPoint(stopPoint3);

        List<VehicleJourneyAtStop> vehicleJourneyAtStops = new ArrayList<>();
        vehicleJourneyAtStops.add(vehicleJourneyAtStop1);
        vehicleJourneyAtStops.add(vehicleJourneyAtStop2);
        vehicleJourneyAtStops.add(vehicleJourneyAtStop3);

        Train trainBordeauxMarseille = new Train();
        trainBordeauxMarseille.setObjectId("FR:TrainNumber:1");
        trainBordeauxMarseille.setVersion("any");
        trainBordeauxMarseille.setDescription("Bordeaux - Marseille");
        trainBordeauxMarseille.setPublishedName("357224");

        Train trainMarseilleLyon = new Train();
        trainMarseilleLyon.setVersion("1.0");
        trainMarseilleLyon.setObjectId("FR:TrainNumber:2");
        trainMarseilleLyon.setDescription("Marseille - Lyon");
        trainMarseilleLyon.setPublishedName("11997766");

        Train trainLyonZurich = new Train();
        trainLyonZurich.setObjectId("FR:TrainNumber:3");
        trainLyonZurich.setDescription("Lyon - Zurich");
        trainLyonZurich.setPublishedName("123698745");
        List<Train> trains = Arrays.asList(trainBordeauxMarseille, trainMarseilleLyon, trainLyonZurich);

        VehicleJourney vehicleJourney = new VehicleJourney();
        vehicleJourney.setObjectId("TEST:VehicleJourney:vj1");
        vehicleJourney.setPublishedJourneyName("Test vehicle journey name");
        vehicleJourney.setJourneyPattern(journeyPattern);
        vehicleJourney.setVehicleJourneyAtStops(vehicleJourneyAtStops);
        vehicleJourney.setTimetables(timetables);
        vehicleJourney.setRoute(route);
        vehicleJourney.setTrains(trains);

        ArrayList<Footnote> footnotes = new ArrayList<>();
        Footnote footnote = new Footnote();
        footnote.setObjectId("TEST:Footnote:f1");
        footnote.setLabel("Test Label");
        footnote.setCode("Test Code");
        footnotes.add(footnote);
        vehicleJourney.setFootnotes(footnotes);

        List<Route> routes = new ArrayList<>();
        routes.add(route);

        List<JourneyPattern> journeyPatterns = new ArrayList<>();
        journeyPatterns.add(journeyPattern);
        route.setJourneyPatterns(journeyPatterns);

        List<VehicleJourney> vehicleJourneys = new ArrayList<>();
        vehicleJourneys.add(vehicleJourney);

        Set<StopArea> stopAreas = new HashSet<>();
        stopAreas.add(stopArea1);
        stopAreas.add(stopArea2);
        stopAreas.add(stopArea3);


        ExportableData exportableData = new ExportableData();
        ExportableNetexData exportableNetexData = new ExportableNetexData();


        exportableData.setLine(line);
        exportableData.setRoutes(routes);
        exportableData.setJourneyPatterns(journeyPatterns);
        exportableData.setVehicleJourneys(vehicleJourneys);
        exportableData.setStopAreas(stopAreas);
        exportableData.setTimetables(timetableHashSet);

        Context context = new Context();
        JobDataTest jobData = new JobDataTest();
        jobData.setPathName("src/test/data/idfm");

        NetexprofileExportParameters parameters = new NetexprofileExportParameters();
        parameters.setExportStops(false);
        parameters.setAddMetadata(false);
        parameters.setDefaultCodespacePrefix("TEST");

        context.put(Constant.CONFIGURATION, parameters);
        context.put(EXPORTABLE_DATA, exportableData);
        context.put(EXPORTABLE_NETEX_DATA, exportableNetexData);
        context.put(JOB_DATA, jobData);
        context.put(REPORT, new ActionReport());
        NetexXMLProcessingHelperFactory netexXMLFactory = new NetexXMLProcessingHelperFactory();
        context.put(MARSHALLER, netexXMLFactory.createFragmentMarshaller());


        return context;
    }

    @Test
    public void testRegexSpecialCharacter(){
        String idStructureRegexpSpecialCharatec = "([^0-9A-Za-z-_])";

        String test = "-F_oire&é'au(jAçé=)àmbon&é64100.=-";

        test = test.replaceAll(idStructureRegexpSpecialCharatec, "_");

        Assert.assertEquals(test, "-F_oire___au_jA_____mbon__64100__-");

    }

}
