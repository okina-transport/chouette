package mobi.chouette.exchange.gtfs.importer;


import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.LineDAO;
import mobi.chouette.dao.RouteDAO;
import mobi.chouette.dao.ScheduledStopPointDAO;
import mobi.chouette.dao.StopAreaDAO;
import mobi.chouette.exchange.gtfs.Constant;
import mobi.chouette.exchange.gtfs.DummyChecker;
import mobi.chouette.exchange.gtfs.GtfsTestsUtils;
import mobi.chouette.exchange.gtfs.JobDataTest;
import mobi.chouette.exchange.importer.RouteMergerCommand;
import mobi.chouette.exchange.report.ReportConstant;
import mobi.chouette.model.*;
import mobi.chouette.model.type.ChouetteAreaEnum;
import mobi.chouette.model.type.PTDirectionEnum;
import mobi.chouette.persistence.hibernate.ContextHolder;
import org.hibernate.Hibernate;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.ejb.EJB;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.File;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GtfsRouteMergerTests extends Arquillian implements Constant, ReportConstant {

    @EJB
    StopAreaDAO stopAreaDAO;


    @EJB
    ScheduledStopPointDAO scheduledStopPointDAO;

    @EJB
    RouteDAO routeDAO;

    @EJB
    LineDAO lineDAO;

    protected InitialContext initialContext;

    public void init() {
        Locale.setDefault(Locale.ENGLISH);
        if (initialContext == null) {
            try {
                initialContext = new InitialContext();
            } catch (NamingException e) {
                e.printStackTrace();
            }


        }

    }


    @Deployment
    public static EnterpriseArchive createDeployment() {

        EnterpriseArchive result;


        File[] files = Maven.resolver().loadPomFromFile("pom.xml")
                .resolve("mobi.chouette:mobi.chouette.exchange.gtfs").withTransitivity().asFile();
        List<File> jars = new ArrayList<>();
        List<JavaArchive> modules = new ArrayList<>();
        for (File file : files) {
            if (file.getName().startsWith("mobi.chouette.exchange")) {
                String name = file.getName().split("\\-")[0] + ".jar";

                JavaArchive archive = ShrinkWrap
                        .create(ZipImporter.class, name)
                        .importFrom(file)
                        .as(JavaArchive.class);
                modules.add(archive);
            } else {
                jars.add(file);
            }
        }

        File[] filesDao = Maven.resolver().loadPomFromFile("pom.xml")
                .resolve("mobi.chouette:mobi.chouette.dao").withTransitivity().asFile();
        if (filesDao.length == 0) {
            throw new NullPointerException("no dao");
        }
        for (File file : filesDao) {
            if (file.getName().startsWith("mobi.chouette.dao")) {
                String name = file.getName().split("\\-")[0] + ".jar";

                JavaArchive archive = ShrinkWrap
                        .create(ZipImporter.class, name)
                        .importFrom(file)
                        .as(JavaArchive.class);
                modules.add(archive);
                if (!modules.contains(archive))
                    modules.add(archive);
            } else {
                if (!jars.contains(file))
                    jars.add(file);
            }
        }


        final WebArchive testWar = ShrinkWrap.create(WebArchive.class, "test.war").addAsWebInfResource("postgres-ds.xml")
                .addClass(GtfsRouteMergerTests.class)
                .addClass(GtfsTestsUtils.class)
                .addClass(DummyChecker.class)
                .addClass(JobDataTest.class);

        result = ShrinkWrap.create(EnterpriseArchive.class, "test.ear")
                .addAsLibraries(jars.toArray(new File[0]))
                .addAsModules(modules.toArray(new JavaArchive[0]))
                .addAsModule(testWar)
                .addAsResource(EmptyAsset.INSTANCE, "beans.xml");
        return result;

    }


    @Test
    public void test_ABC_ACB() throws Exception {
        ContextHolder.setContext("chouette_gui"); // set tenant s
        init();

        cleanAllRoutes();

        createTripFromPattern("A-B-C", 9);
        createTripFromPattern("A-C-B", 10);
        launchMerge();

        List<Route> routes = routeDAO.findAll();
        Assert.assertEquals(routes.size(), 2, "Routes should not be merged");
    }

    @Test
    public void test_ABC_ACE() throws Exception {
        ContextHolder.setContext("chouette_gui"); // set tenant s
        init();

        cleanAllRoutes();

        createTripFromPattern("A-B-C", 9);
        createTripFromPattern("A-C-E", 10);
        launchMerge();

        List<Route> routes = routeDAO.findAll();
        Assert.assertEquals(routes.size(), 1, "Routes should be merged");
    }

    @Test
    public void test_ABCBC_ACB() throws Exception {
        ContextHolder.setContext("chouette_gui"); // set tenant s
        init();

        cleanAllRoutes();

        createTripFromPattern("A-B-C-B-C", 9);
        createTripFromPattern("A-C-B", 10);
        launchMerge();

        List<Route> routes = routeDAO.findAll();
        Assert.assertEquals(routes.size(), 1, "Routes should be merged");
    }

    @Test
    public void test_ADEFBJC_ABCJDE() throws Exception {
        ContextHolder.setContext("chouette_gui"); // set tenant s
        init();

        cleanAllRoutes();

        createTripFromPattern("A-D-E-F-B-J-C", 9);
        createTripFromPattern("A-B-C-J-D-E", 10);
        launchMerge();

        List<Route> routes = routeDAO.findAll();
        Assert.assertEquals(routes.size(), 2, "Routes should not be merged");
    }

    @Test
    public void test_ADEFBJC_ADEBDC() throws Exception {
        ContextHolder.setContext("chouette_gui"); // set tenant s
        init();

        cleanAllRoutes();

        createTripFromPattern("A-D-E-F-B-J-C", 9);
        createTripFromPattern("A-D-E-B-D-C", 10);
        launchMerge();

        List<Route> routes = routeDAO.findAll();
        Assert.assertEquals(routes.size(), 1, "Routes should be merged");
    }


    private void launchMerge() throws Exception {
        Command mergeCommand = (Command) CommandFactory.create(initialContext, RouteMergerCommand.class.getName());

        Context context = initValidatorContext();
        mergeCommand.execute(context);

    }

    protected Context initValidatorContext() {
        init();
        ContextHolder.setContext("chouette_gui"); // set tenant schema

        Context context = new Context();

        return context;

    }

    private void cleanAllRoutes() {
        routeDAO.truncate();
        scheduledStopPointDAO.truncate();
    }

    private void createTripFromPattern(String tripPattern, int startingHour) {
        String[] patternArray = tripPattern.split("-");


        Route newRoute = new Route();
        newRoute.setName(tripPattern);
        newRoute.setLine(getOrCreateDefaultLine());
        newRoute.setDirection(PTDirectionEnum.A);
        newRoute.setObjectId("MOBIITI:Route:" + tripPattern);

        JourneyPattern journeyPattern = new JourneyPattern();
        journeyPattern.setObjectId("MOBIITI:JourneyPattern:" + tripPattern);

        VehicleJourney vehicleJourney = new VehicleJourney();

        vehicleJourney.setRoute(newRoute);
        vehicleJourney.setObjectId("MOBIITI:VehicleJourney:" + tripPattern);


        int currentPosition = 0;

        for (String stopName : patternArray) {
            StopArea stopArea = getOrCreateStop(stopName);

            StopPoint stopPoint = new StopPoint();
            stopPoint.setRoute(newRoute);
            stopPoint.setPosition(currentPosition);
            stopPoint.setObjectId("MOBIITI:StopPoint:" + tripPattern + "_" + stopName + "_" + currentPosition);


            ScheduledStopPoint scheduledStopPoint = new ScheduledStopPoint();
            scheduledStopPoint.setObjectId("MOBIITI:Scheduled:" + tripPattern + "_" + currentPosition);
            scheduledStopPoint.setContainedInStopAreaRef(new SimpleObjectReference(stopArea));

            stopPoint.setScheduledStopPoint(scheduledStopPoint);

            journeyPattern.getStopPoints().add(stopPoint);


            VehicleJourneyAtStop vjas = new VehicleJourneyAtStop();
            vjas.setObjectId("MOBIITI:VJAS:" + tripPattern + "_" + stopName + "_" + currentPosition);
            vjas.setStopPoint(stopPoint);
            vjas.setDepartureTime(LocalTime.of(startingHour, currentPosition, 0));
            vjas.setArrivalTime(LocalTime.of(startingHour, currentPosition, 0));

            vehicleJourney.getVehicleJourneyAtStops().add(vjas);

            currentPosition++;
        }

        journeyPattern.setRoute(newRoute);
        journeyPattern.getVehicleJourneys().add(vehicleJourney);
        journeyPattern.setDepartureStopPoint(journeyPattern.getStopPoints().get(0));
        journeyPattern.setArrivalStopPoint(journeyPattern.getStopPoints().get(journeyPattern.getStopPoints().size() - 1));

        routeDAO.create(newRoute);
    }

    private Line getOrCreateDefaultLine() {
        Line line = lineDAO.findByObjectIdAndInitialize("MOBIITI:Line:1");

        if (line != null) {
            Hibernate.initialize(line.getRoutes());
            return line;
        }

        Line newLine = new Line();
        newLine.setObjectId("MOBIITI:Line:1");
        lineDAO.create(newLine);
        return newLine;
    }

    private StopArea getOrCreateStop(String stopName) {
        StopArea foundStopArea = stopAreaDAO.findByObjectId("MOBIITI:StopArea:" + stopName);

        if (foundStopArea != null) {
            return foundStopArea;
        }

        StopArea newStopArea = new StopArea();
        newStopArea.setObjectId("MOBIITI:StopArea:" + stopName);
        newStopArea.setName(stopName);
        newStopArea.setOriginalStopId(stopName);
        newStopArea.setAreaType(ChouetteAreaEnum.BoardingPosition);
        stopAreaDAO.create(newStopArea);
        return newStopArea;
    }

}
