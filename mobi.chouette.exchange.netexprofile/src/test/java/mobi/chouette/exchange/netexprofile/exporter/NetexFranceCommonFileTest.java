package mobi.chouette.exchange.netexprofile.exporter;


import mobi.chouette.common.Context;
import mobi.chouette.exchange.netexprofile.Constant;
import mobi.chouette.exchange.netexprofile.JobDataTest;
import mobi.chouette.exchange.netexprofile.jaxb.NetexXMLProcessingHelperFactory;
import mobi.chouette.exchange.report.ActionReport;
import mobi.chouette.model.Company;
import mobi.chouette.model.ConnectionLink;
import mobi.chouette.model.DestinationDisplay;
import mobi.chouette.model.Footnote;
import mobi.chouette.model.JourneyPattern;
import mobi.chouette.model.Line;
import mobi.chouette.model.Network;
import mobi.chouette.model.OkinaAccessibilityAssessment;
import mobi.chouette.model.OkinaAccessibilityLimitation;
import mobi.chouette.model.Period;
import mobi.chouette.model.Route;
import mobi.chouette.model.ScheduledStopPoint;
import mobi.chouette.model.SimpleObjectReference;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.StopPoint;
import mobi.chouette.model.Timetable;
import mobi.chouette.model.VehicleJourney;
import mobi.chouette.model.VehicleJourneyAtStop;
import mobi.chouette.model.type.ChouetteAreaEnum;
import mobi.chouette.model.type.PTDirectionEnum;
import mobi.chouette.model.type.TransportModeNameEnum;
import mobi.chouette.model.util.Referential;
import org.joda.time.Duration;
import org.joda.time.LocalDate;
import org.rutebanken.netex.model.AccessibilityAssessment;
import org.rutebanken.netex.model.AccessibilityLimitation;
import org.rutebanken.netex.model.AccessibilityLimitations_RelStructure;
import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration;
import org.rutebanken.netex.model.GeneralFrame;
import org.rutebanken.netex.model.General_VersionFrameStructure;
import org.rutebanken.netex.model.LimitationStatusEnumeration;
import org.rutebanken.netex.model.PublicationDeliveryStructure;
import org.rutebanken.netex.model.SiteConnection;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;

import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static mobi.chouette.common.Constant.*;
import static mobi.chouette.exchange.netexprofile.Constant.EXPORTABLE_NETEX_DATA;
import static mobi.chouette.exchange.netexprofile.Constant.MARSHALLER;

public class NetexFranceCommonFileTest {

    private static String codifLigne = "TestNetexFranceProfile";
    private static String testPath = "src/test/data/netexFranceProfile";
    private static String generatedFilePath = testPath+"/output/TEST_commun.xml";


    private File generatedFile = new File(generatedFilePath);

    private NetexXMLProcessingHelperFactory importer = new NetexXMLProcessingHelperFactory();




    @Test
    public void exportAndCheckCommonFile() throws Exception {
        deleteFileCreated();

        Context context = createContext();

        context.put(CREATION_DATE,LocalDateTime.now());

        NetexLineDataIDFMProducer producer = new NetexLineDataIDFMProducer();
        producer.produce(context);

        NetexCommonDataProducerCommand commonPoducer = new NetexCommonDataProducerCommand();
        commonPoducer.execute(context);



        checkGeneratedFile();



      //  deleteFileCreated();

    }

    private void checkGeneratedFile(){

        PublicationDeliveryStructure lineDeliveryStructure;
        try {
            lineDeliveryStructure = importer.unmarshal(generatedFile,new HashSet<>());
        } catch (JAXBException|XMLStreamException|IOException|SAXException e) {
            Assert.fail("Unable to unmarshal generated file");
            System.out.println(e);
            return;
        }
        lineDeliveryStructure.getDataObjects().getCompositeFrameOrCommonFrame().get(0).getValue();
        Assert.assertEquals(lineDeliveryStructure.getVersion(), "1.1:FR-NETEX_COMMUN-2.2", "wrong version");
        Assert.assertEquals(lineDeliveryStructure.getParticipantRef(), "TEST", "wrong participant REF");
        List<GeneralFrame> compositeFrames = getGeneralFrames(lineDeliveryStructure);
        Assert.assertFalse(compositeFrames.isEmpty(),"GeneralFrame should have been generated");
        GeneralFrame firstFrame = compositeFrames.get(0);
        List<SiteConnection> siteConnections = getSiteConnections(firstFrame.getMembers());
        Assert.assertFalse(siteConnections.isEmpty(),"SitecConnections should be there");
        SiteConnection firstSiteConnection = siteConnections.get(0);
        Assert.assertEquals(firstSiteConnection.getName().getValue(),"nameC1");
        Assert.assertEquals(firstSiteConnection.getId(),"TEST:ConnectionLink:uffcarr_uffcarr2");
        Assert.assertEquals(firstSiteConnection.getFrom().getQuayRef().getRef(),"TEST:Quay:quay1");
        Assert.assertEquals(firstSiteConnection.getTo().getQuayRef().getRef(),"TEST:Quay:quay2");
        Assert.assertEquals(firstSiteConnection.getTo().getTransportMode(), AllVehicleModesOfTransportEnumeration.BUS);
        Assert.assertEquals(firstSiteConnection.getFrom().getTransportMode(),AllVehicleModesOfTransportEnumeration.BUS);

        Assert.assertEquals(firstSiteConnection.getFrom().getStopPlaceRef().getRef(),"TEST:StopPlace:SP1");
        Assert.assertEquals(firstSiteConnection.getTo().getStopPlaceRef().getRef(),"TEST:StopPlace:SP2");
        Assert.assertEquals(firstSiteConnection.getDistance(),new BigDecimal(5));
        java.time.Duration expectedDuration = java.time.Duration.parse(Duration.parse("PT76400S").toString());
        Assert.assertEquals(firstSiteConnection.getWalkTransferDuration().getDefaultDuration(),expectedDuration);


        List<org.rutebanken.netex.model.Line> lines = getLines(firstFrame.getMembers());
        Assert.assertFalse(lines.isEmpty(),"lines should be there");
        org.rutebanken.netex.model.Line firstLine = lines.get(0);
        Assert.assertEquals(firstLine.getName().getValue(),"TestLineName");
        Assert.assertEquals(firstLine.getShortName().getValue(),"testPublishedName");
        Assert.assertEquals(firstLine.getTransportMode(),AllVehicleModesOfTransportEnumeration.BUS);

        AccessibilityAssessment firstLineAssessment = firstLine.getAccessibilityAssessment();
        Assert.assertNotNull(firstLineAssessment);

        Assert.assertEquals(firstLineAssessment.getMobilityImpairedAccess(),LimitationStatusEnumeration.PARTIAL);

        AccessibilityLimitation limitations = firstLineAssessment.getLimitations().getAccessibilityLimitation();
        Assert.assertNotNull(limitations);
        Assert.assertNull(limitations.getWheelchairAccess());
        Assert.assertEquals(limitations.getStepFreeAccess(),LimitationStatusEnumeration.FALSE);
        Assert.assertEquals(limitations.getEscalatorFreeAccess(),LimitationStatusEnumeration.PARTIAL);
        Assert.assertEquals(limitations.getLiftFreeAccess(),LimitationStatusEnumeration.TRUE);
        Assert.assertEquals(limitations.getAudibleSignalsAvailable(),LimitationStatusEnumeration.UNKNOWN);



    }

    private List<org.rutebanken.netex.model.SiteConnection> getSiteConnections(General_VersionFrameStructure.Members members){
        return members.getGeneralFrameMemberOrDataManagedObjectOrEntity_Entity().stream()
                .map(JAXBElement::getValue)
                .filter(member -> member instanceof org.rutebanken.netex.model.SiteConnection)
                .map(member -> (org.rutebanken.netex.model.SiteConnection)member )
                .collect(Collectors.toList());

    }


    private List<org.rutebanken.netex.model.Line> getLines(General_VersionFrameStructure.Members members){
        return members.getGeneralFrameMemberOrDataManagedObjectOrEntity_Entity().stream()
                .map(JAXBElement::getValue)
                .filter(member -> member instanceof org.rutebanken.netex.model.Line)
                .map(member -> (org.rutebanken.netex.model.Line)member )
                .collect(Collectors.toList());

    }



    private List<GeneralFrame> getGeneralFrames(PublicationDeliveryStructure pubDelivery){
        return pubDelivery.getDataObjects().getCompositeFrameOrCommonFrame().stream()
                                            .map(frame ->{
                                                if (frame.getValue() instanceof GeneralFrame){
                                                    return (GeneralFrame)frame.getValue();
                                                }else{
                                                    return null;
                                                }
                                            })
                                        .filter(frame -> frame != null)
                                        .collect(Collectors.toList());
    }




    private void deleteFileCreated() {
        File file = new File(generatedFilePath);
        if(file.delete()){
            System.out.println("Fichier de test supprimé");
        }else{
            System.out.println("ERREUR fichier de test non supprimé");
        }
    }

    private Context createContext() throws JAXBException {

        File file = new File(generatedFilePath);
        file.getParentFile().mkdirs();


        Line line = new Line();
        line.setObjectId("TEST:Line:l1");
        line.setRegistrationNumber("l1");
        line.setCodifligne(codifLigne);
        line.setName("TestLineName");
        line.setPublishedName("testPublishedName");

        OkinaAccessibilityAssessment accessibilityAssessment = new OkinaAccessibilityAssessment();
        accessibilityAssessment.setId(3l);
        accessibilityAssessment.setMobilityImpairedAccess(LimitationStatusEnumeration.PARTIAL);
        OkinaAccessibilityLimitation limitations = new OkinaAccessibilityLimitation();
        limitations.setStepFreeAccess(LimitationStatusEnumeration.FALSE);
        limitations.setLiftFreeAccess(LimitationStatusEnumeration.TRUE);
        limitations.setEscalatorFreeAccess(LimitationStatusEnumeration.PARTIAL);
        limitations.setAudibleSignalsAvailable(LimitationStatusEnumeration.UNKNOWN);
        limitations.setId(4l);


        accessibilityAssessment.setLimitations(limitations);


        line.setAccessibilityAssessment(accessibilityAssessment);

        Network network = new Network();
        Company company = new Company();
        company.setCode("myComp");
        company.setEmail("email@okinatest.com");
        company.setFareUrl("www.okinatest.com");
        company.setName("Okina test");
        company.setObjectId("Test:Company:1");

        network.setCompany(company);
        network.setObjectId("Test:Network:1");
        line.setNetwork(network);

        Route route  = new Route();
        route.setObjectId("TEST:Route:r1");
        route.setName("Test Route");
        route.setLine(line);
        route.setDirection(PTDirectionEnum.A);
        route.setPublishedName("publishedNameRoute");


        DestinationDisplay destinationDisplay = new DestinationDisplay();
        destinationDisplay.setObjectId("TEST:DestinationDisplay:dd1");
        destinationDisplay.setFrontText("Test Destination Display");

        StopArea stopArea1 = new StopArea();
        stopArea1.setObjectId("TEST:Quay:quay1");
        stopArea1.setAreaType(ChouetteAreaEnum.Quay);
        stopArea1.setLatitude(new BigDecimal("4.4444"));
        stopArea1.setLongitude(new BigDecimal("5.5555"));
        stopArea1.setName("quay1");

        StopArea parentOfStop1 = new StopArea();
        parentOfStop1.setObjectId("TEST:StopPlace:SP1");
        parentOfStop1.setTransportModeName(TransportModeNameEnum.Bus);
        stopArea1.setParent(parentOfStop1);

        StopArea stopArea2 = new StopArea();
        stopArea2.setObjectId("TEST:Quay:quay2");

        stopArea2.setAreaType(ChouetteAreaEnum.Quay);
        stopArea2.setLatitude(new BigDecimal("1.1111"));
        stopArea2.setLongitude(new BigDecimal("2.2222"));
        stopArea2.setName("quay2");

        StopArea parentOfStop2 = new StopArea();
        parentOfStop2.setTransportModeName(TransportModeNameEnum.Bus);
        parentOfStop2.setObjectId("TEST:StopPlace:SP2");
        stopArea2.setParent(parentOfStop2);


        StopArea stopArea3 = new StopArea();
        stopArea3.setObjectId("TEST:Quay:quay3");
        stopArea3.setAreaType(ChouetteAreaEnum.Quay);
        stopArea3.setLatitude(new BigDecimal("1.12345"));
        stopArea3.setLongitude(new BigDecimal("2.12345"));
        stopArea3.setName("quay3");


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
        org.joda.time.LocalTime time1 = new org.joda.time.LocalTime(7, 0, 0);
        vehicleJourneyAtStop1.setDepartureTime(time1);
        vehicleJourneyAtStop1.setArrivalTime(time1);
        vehicleJourneyAtStop1.setStopPoint(stopPoint1);

        VehicleJourneyAtStop vehicleJourneyAtStop2 = new VehicleJourneyAtStop();
        vehicleJourneyAtStop2.setObjectId("TEST:TimetablePassingTime:tpt2");
        org.joda.time.LocalTime time2 = new org.joda.time.LocalTime(7, 15, 0);
        vehicleJourneyAtStop2.setDepartureTime(time2);
        vehicleJourneyAtStop2.setArrivalTime(time2);
        vehicleJourneyAtStop2.setStopPoint(stopPoint2);

        VehicleJourneyAtStop vehicleJourneyAtStop3 = new VehicleJourneyAtStop();
        vehicleJourneyAtStop3.setObjectId("TEST:TimetablePassingTime:tpt3");
        org.joda.time.LocalTime time3 = new org.joda.time.LocalTime(7, 30, 0);
        vehicleJourneyAtStop3.setDepartureTime(time3);
        vehicleJourneyAtStop3.setArrivalTime(time3);
        vehicleJourneyAtStop3.setStopPoint(stopPoint3);

        List<VehicleJourneyAtStop> vehicleJourneyAtStops = new ArrayList<>();
        vehicleJourneyAtStops.add(vehicleJourneyAtStop1);
        vehicleJourneyAtStops.add(vehicleJourneyAtStop2);
        vehicleJourneyAtStops.add(vehicleJourneyAtStop3);

        VehicleJourney vehicleJourney = new VehicleJourney();
        vehicleJourney.setObjectId("TEST:VehicleJourney:vj1");
        vehicleJourney.setPublishedJourneyName("Test vehicle journey name");
        vehicleJourney.setJourneyPattern(journeyPattern);
        vehicleJourney.setVehicleJourneyAtStops(vehicleJourneyAtStops);
        vehicleJourney.setTimetables(timetables);
        vehicleJourney.setRoute(route);

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


        Context context = new Context();
        JobDataTest jobData = new JobDataTest();
        jobData.setPathName(testPath);

        NetexprofileExportParameters parameters = new NetexprofileExportParameters();
        parameters.setExportStops(false);
        parameters.setAddMetadata(false);
        parameters.setDefaultCodespacePrefix("TEST");

        context.put(Constant.CONFIGURATION, parameters);
        context.put(JOB_DATA, jobData);
        context.put(REPORT, new ActionReport());
        NetexXMLProcessingHelperFactory netexXMLFactory = new NetexXMLProcessingHelperFactory();
        context.put(MARSHALLER, netexXMLFactory.createFragmentMarshaller());
        context.put(LINE,line);
        context.put(REFERENTIAL, new Referential());
        ExportableNetexData exportableNetexData = new ExportableNetexData();

        ConnectionLink c1 = new ConnectionLink();
        c1.setName("nameC1");
        c1.setLinkDistance(new BigDecimal(5));
        c1.setObjectId("TEST:ConnectionLink:uffcarr_uffcarr2");
        c1.setStartOfLink(stopArea1);
        c1.setEndOfLink(stopArea2);
        c1.setDefaultDuration(Duration.parse("PT76400S"));

        exportableNetexData.getConnectionLinks().add(c1);
        context.put(EXPORTABLE_NETEX_DATA, exportableNetexData);

        ExportableData exportableData = new ExportableData();
        exportableData.setLine(line);

        context.put(EXPORTABLE_DATA, exportableData);


        return context;
    }


}
