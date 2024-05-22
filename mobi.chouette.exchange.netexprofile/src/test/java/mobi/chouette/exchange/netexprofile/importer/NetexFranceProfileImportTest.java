package mobi.chouette.exchange.netexprofile.importer;

import mobi.chouette.common.Constant;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.exchange.netexprofile.jaxb.NetexXMLProcessingHelperFactory;
import mobi.chouette.exchange.netexprofile.util.NetexReferential;
import mobi.chouette.exchange.report.ActionReport;
import mobi.chouette.exchange.validation.report.ValidationReport;
import mobi.chouette.model.*;
import mobi.chouette.model.type.ChouetteAreaEnum;
import mobi.chouette.model.type.PTDirectionEnum;
import mobi.chouette.model.type.TransportModeNameEnum;
import mobi.chouette.model.util.Referential;
import mobi.chouette.persistence.hibernate.ContextHolder;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.rutebanken.netex.model.PublicationDeliveryStructure;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

import static mobi.chouette.common.Constant.*;
import static mobi.chouette.exchange.netexprofile.Constant.*;

public class NetexFranceProfileImportTest {

    private static InitialContext initialContext;

    private String rootDir = "src/test/data/netexFranceProfile/";


    @Test(groups = { "parseStopFile" }, description = "Stop file should be parsed")
    public void verifyStopFileParsing() throws Exception {

        // INITIALISATION
        String testFileName = "stopFileParsingTest.xml";
        Context importContext = initContext();

        // PARSING
        launchParsing(importContext, testFileName);


        // CHECK RESULTS
        Referential referential = (Referential) importContext.get(REFERENTIAL);
        Map<String, StopArea> parsedStopAreas = referential.getSharedStopAreas();
        Assert.assertEquals(parsedStopAreas.size(),96, "some stopAreas were not parsed correctly");

        StopArea someArea = parsedStopAreas.get("TESTORGANISATION:StopPlace:PROD3##3A##StopPlace##3A##20");
        Assert.assertNotNull(someArea);
        Assert.assertEquals(someArea.getName(),"Eglise", "name was not parsed correctly");
        Assert.assertEquals(someArea.getAreaType(), ChouetteAreaEnum.CommercialStopPoint, "type was not parsed correctly");
        Assert.assertEquals(someArea.getTransportModeName(), TransportModeNameEnum.Bus, "transport mode name was not parsed correctly");
        Assert.assertEquals(someArea.getContainedStopAreas().size(),1, "contained Area was not parsed correctly");
        Assert.assertEquals(someArea.getLongitude(),new BigDecimal("1.885458"), "longitude was not parsed correctly");
        Assert.assertEquals(someArea.getLatitude(),new BigDecimal("48.702522"), "latitude was not parsed correctly");

    }

    @Test(groups = { "parseCalendarFile" }, description = "calendar file should be parsed")
    public void verifyCalendarFileParsing() throws Exception {

        // INITIALISATION
        String testFileName = "calendarParsingTest.xml";
        Context importContext = initContext();

        // PARSING
        launchParsing(importContext, testFileName);


        // CHECK RESULTS
        Referential referential = (Referential) importContext.get(REFERENTIAL);
        Map<String, Timetable> timetables = referential.getSharedTimetables();
        Assert.assertEquals(timetables.size(),5, "some timetables were not parsed correctly");

        Timetable someCalendar = timetables.get("TESTORGANISATION:Timetable:PERH18-3615-Semaine-00-1101100");
        Assert.assertNotNull(someCalendar);

        Assert.assertEquals(someCalendar.getIntDayTypes(),new Integer(108));

        Assert.assertEquals(someCalendar.getStartOfPeriod(), new LocalDate(2019,12,30));
        Assert.assertEquals(someCalendar.getEndOfPeriod(),new LocalDate(2022,12,31));

        Assert.assertEquals(someCalendar.getCalendarDays().size(),20);
        CalendarDay someCalendarDay = someCalendar.getCalendarDays().get(0);
        Assert.assertNotNull(someCalendarDay);

        Assert.assertEquals(someCalendarDay.getDate(),new LocalDate(2020,02,21));
        Assert.assertEquals(someCalendarDay.getIncluded(),new Boolean(false));

        List<Period> periods = someCalendar.getPeriods();
        Assert.assertEquals(periods.size(),1, "some periods were not parsed correctly");
        Period somePeriod = periods.get(0);
        Assert.assertEquals(somePeriod.getStartDate(), new LocalDate(2020,01,06));
        Assert.assertEquals(somePeriod.getEndDate(), new LocalDate(2022,07,03));

    }

    @Test(groups = { "parseCommonFile" }, description = "common file should be parsed")
    public void verifyCommonFileParsing() throws Exception {

        // INITIALISATION
        String testFileName = "commonFileParsingTest.xml";
        Context importContext = initContext();

        // PARSING
        launchParsing(importContext, testFileName);


        // CHECK RESULTS
        Referential referential = (Referential) importContext.get(REFERENTIAL);


        // networks
        Map<String, Network> networks = referential.getSharedPTNetworks();
        Assert.assertEquals(networks.size(),2, "some networks were not parsed correctly");
        Network someNetwork = networks.get("TESTORGANISATION:Network:perrier");
        Assert.assertEquals(someNetwork.getName(),"perrier", "network name was not parsed correctly");
        Assert.assertEquals(someNetwork.getLines().size(),1, "network line was not parsed correctly");


        // companies
        Map<String, Company> companies = referential.getSharedCompanies();
        Assert.assertEquals(companies.size(),4, "some companies were not parsed correctly");
        Company someCompany =companies.get("TESTORGANISATION:Authority:perrier");
        Assert.assertNotNull(someCompany);
        Assert.assertEquals(someCompany.getName(),"perrier");
        Assert.assertEquals(someCompany.getRegistrationNumber(),"perrier");
        Assert.assertEquals(someCompany.getUrl(),"http://www.giro.ca");

        // lines
        Map<String, Line> lines = referential.getSharedLines();
        Assert.assertEquals(lines.size(),4, "some lines were not parsed correctly");
        Line someLine = lines.get("TESTORGANISATION:Line:142");
        Assert.assertEquals(someLine.getName(),"Ligne Express A14 Les Mureaux - La Défense");
        Assert.assertEquals(someLine.getNumber(),"142");
        Assert.assertEquals(someLine.getPublishedName(),"Ligne Express A14 Les Mureaux - La Défense");
        Assert.assertEquals(someLine.getRegistrationNumber(),"142");
        Assert.assertEquals(someLine.getTransportModeName(),TransportModeNameEnum.Bus);
        Assert.assertEquals(someLine.getColor(),null);
        Assert.assertEquals(someLine.getTextColor(),null);
        Assert.assertEquals(someLine.getNetwork().getObjectId(),"TESTORGANISATION:Network:default", "Problem between line-network link");
        Assert.assertEquals(someLine.getCompany().getObjectId(),"TESTORGANISATION:Operator:ag1o", "Problem between line-company link");

    }

    @Test(groups = { "parseLineFile" }, description = "line file should be parsed")
    public void verifyLineFileParsing() throws Exception {

        // INITIALISATION
        String testFileName = "lineFileParsingTest.xml";
        Context importContext = initContext();
        importContext.put(NETEX_WITH_COMMON_DATA, Boolean.FALSE);

        // PARSING
        File inputFile = new File(rootDir + testFileName);
        setUpInputFileInContext(importContext,inputFile);

        // stop file need to be parsed before line file (to collect stop info geolocatlisation, stop names,etc)
        launchParsing(importContext, "stopFileParsingTest.xml");

        // then, line parsing can be launched
        setUpInputFileInContext(importContext,inputFile);
        NetexLineParserCommand parser = (NetexLineParserCommand) CommandFactory.create(initialContext, NetexLineParserCommand.class.getName());
        parser.setPath(inputFile.toPath());
        parser.execute(importContext);

        // CHECK RESULTS
        Referential referential = (Referential) importContext.get(REFERENTIAL);
        Assert.assertEquals(referential.getLines().size(),1, "some lines were not parsed correctly");

        Line someLine = referential.getLines().get("TESTORGANISATION:Line:100");
        Assert.assertNotNull(someLine);


        Assert.assertEquals(someLine.getRoutes().size(),1);
        Route someRoute = someLine.getRoutes().get(0);
        Assert.assertEquals(someRoute.getName(),"Gare Routière -> St-Quentin Gare");
        Assert.assertEquals(someRoute.getPublishedName(),"Gare Routière -> St-Quentin Gare");
        Assert.assertEquals(someRoute.getDirection(), PTDirectionEnum.A);
        Assert.assertEquals(someRoute.getStopPoints().size(),16);
        Assert.assertEquals(someRoute.getJourneyPatterns().size(),1);

        JourneyPattern someJourneyPattern = someRoute.getJourneyPatterns().get(0);
        String journeyName = "ELANCOURT De Lattre de Tassigny";

        Assert.assertEquals(someJourneyPattern.getName(),journeyName);

        Assert.assertEquals(someJourneyPattern.getName(),journeyName);
        Assert.assertEquals(someJourneyPattern.getStopPoints().size(),16);
        String departureObjectId = "TESTORGANISATION:StopPoint:100_1000107_8F4F0374F12AB161169F15B6A486D12E_lmurgar1_travagu_16almurgar1";
        String arrivalObjectId = "TESTORGANISATION:StopPoint:100_1000107_8F4F0374F12AB161169F15B6A486D12E_lmurgar1_travagu_16ambga11";
        Assert.assertEquals(someJourneyPattern.getStopPoints().get(0).getObjectId(),departureObjectId);
        Assert.assertEquals(someJourneyPattern.getStopPoints().get(15).getObjectId(),arrivalObjectId);
        Assert.assertEquals(someJourneyPattern.getDepartureStopPoint().getObjectId(),departureObjectId);
        Assert.assertEquals(someJourneyPattern.getArrivalStopPoint().getObjectId(),arrivalObjectId);

        Assert.assertEquals(someJourneyPattern.getVehicleJourneys().size(),1);
        Assert.assertEquals(someJourneyPattern.getRouteSections().size(),0);

        if(!someJourneyPattern.getRouteSections().isEmpty()) {
            RouteSection someRouteSection = someJourneyPattern.getRouteSections().get(0);
            Assert.assertEquals(someRouteSection.getDistance(), new BigDecimal("1060.528029730735"));
            Assert.assertEquals(someRouteSection.getFromScheduledStopPoint().getObjectId(), "TESTORGANISATION:ScheduledStopPoint:100_1000107_8F4F0374F12AB161169F15B6A486D12E_lmurgar1_travagu_16almurgar1");
            Assert.assertEquals(someRouteSection.getToScheduledStopPoint().getObjectId(), "TESTORGANISATION:ScheduledStopPoint:100_1000107_8F4F0374F12AB161169F15B6A486D12E_lmurgar1_travagu_16almubougi");
        }
        VehicleJourney someVehicleJourney = someJourneyPattern.getVehicleJourneys().get(0);
        Assert.assertEquals(someVehicleJourney.getPublishedJourneyName(),journeyName);

        Assert.assertEquals(someVehicleJourney.getTimetables().size(),1);

        Assert.assertEquals(someVehicleJourney.getTimetables().get(0).getObjectId(),"TESTORGANISATION:Timetable:STILE_19_1-Jan2020-Semaine-02");
        Assert.assertEquals(someVehicleJourney.getVehicleJourneyAtStops().size(),16);
        Assert.assertEquals(someVehicleJourney.getVehicleJourneyAtStops().get(0).getArrivalTime(),new LocalTime(5,15,0));
        Assert.assertEquals(someVehicleJourney.getVehicleJourneyAtStops().get(0).getDepartureTime(),new LocalTime(5,15,0));
        Assert.assertEquals(someVehicleJourney.getVehicleJourneyAtStops().get(15).getArrivalTime(),new LocalTime(6,15,0));
        Assert.assertEquals(someVehicleJourney.getVehicleJourneyAtStops().get(15).getDepartureTime(),new LocalTime(6,15,0));

        Assert.assertEquals(someVehicleJourney.getTrains().size(), 3);

        Train train112358 = someVehicleJourney.getTrains().get(0);
        Train train987654321 = someVehicleJourney.getTrains().get(1);
        Train train666 = someVehicleJourney.getTrains().get(2);

        Assert.assertEquals(train112358.getObjectId(), "FR:TrainNumber:112358");
        Assert.assertEquals(train112358.getPublishedName(), "112358");
        Assert.assertEquals(train112358.getDescription(), "Paris - Roubaix");

        Assert.assertEquals(train987654321.getObjectId(), "FR:TrainNumber:987654321");
        Assert.assertEquals(train987654321.getPublishedName(), "987654321");

        Assert.assertEquals(train666.getObjectId(), "FR:TrainNumber:666");
        Assert.assertEquals(train666.getPublishedName(), "666");
        Assert.assertEquals(train666.getDescription(), "Nantes - Clisson");
    }

    private void launchParsing(Context context, String fileName) throws Exception {

        File inputFile = new File(rootDir + fileName);
        setUpInputFileInContext(context,inputFile);
        NetexCommonFilesParserCommand commonFilesParser = (NetexCommonFilesParserCommand) CommandFactory.create(initialContext, NetexCommonFilesParserCommand.class.getName());
        commonFilesParser.execute(context);

    }


    /**
     * Unmarshall input xml and set all variables in context
     */
    private void setUpInputFileInContext(Context context, File file) throws IOException, XMLStreamException, SAXException, JAXBException {
        Set<QName> elementsToSkip = new HashSet<>();


        NetexXMLProcessingHelperFactory importer = new NetexXMLProcessingHelperFactory();

        PublicationDeliveryStructure netexJava = null;
        try {
            netexJava = importer.unmarshal(file,elementsToSkip,context);
        } catch (JAXBException | XMLStreamException | IOException | SAXException e) {
            e.printStackTrace();
            throw e;
        }

        context.put(NETEX_DATA_JAVA,netexJava);
        context.put(FILE_NAME,file.getName());
    }


    private Context initContext() {
        Locale.setDefault(Locale.ENGLISH);

        if (initialContext == null) {
            try {
                initialContext = new InitialContext();
            } catch (NamingException e) {
                e.printStackTrace();
            }
        }

        ContextHolder.setContext("chouette_gui");
        Context context = new Context();
        context.put(INITIAL_CONTEXT, initialContext);

        NetexprofileImportParameters configuration = new NetexprofileImportParameters();
        context.put(CONFIGURATION, configuration);
        context.put(Constant.VALIDATION_REPORT, new ValidationReport());
        context.put(Constant.REPORT, new ActionReport());

        Referential referential = new Referential();
        context.put(Constant.REFERENTIAL,referential);

        NetexprofileImportParameters parameters = new NetexprofileImportParameters();
        parameters.setParseSiteFrames(true);
        parameters.setObjectIdPrefix("TESTORGANISATION");

        context.put(CONFIGURATION,parameters);
        context.put(NETEX_REFERENTIAL, new NetexReferential());
        context.put(INCOMING_LINE_LIST, new ArrayList());
        context.put(STREAM_TO_CLOSE, new ArrayList<>());

        return context;
    }


}
