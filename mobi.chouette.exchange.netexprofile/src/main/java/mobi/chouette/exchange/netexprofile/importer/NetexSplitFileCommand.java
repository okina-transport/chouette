package mobi.chouette.exchange.netexprofile.importer;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Color;
import mobi.chouette.common.Context;
import mobi.chouette.common.FileUtil;
import mobi.chouette.common.JobData;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.exchange.netexprofile.Constant;
import mobi.chouette.exchange.netexprofile.jaxb.NetexXMLProcessingHelperFactory;
import mobi.chouette.exchange.netexprofile.util.NetexObjectUtil;
import org.rutebanken.netex.model.*;

import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static javax.xml.bind.JAXBContext.newInstance;

@Log4j
@Stateless(name = NetexSplitFileCommand.COMMAND)
public class NetexSplitFileCommand implements Command, Constant {

    public static final String COMMAND = "NetexSplitFileCommand";

    private ObjectFactory objectFact = new ObjectFactory();

    private static Set<String> scheduledStopPointsToCopy = new HashSet<>();
    private static Set<String> journeyPatternsToCopy = new HashSet<>();
    private static Set<String> trainNumbersToCopy = new HashSet<>();
    private static Set<String> journeyPartsCoupleToCopy = new HashSet<>();
    private static Set<String> journeyPartsToCopy = new HashSet<>();
    private static Set<String> routePointsToCopy = new HashSet<>();
    private static Set<String> destinationDisplayToCopy = new HashSet<>();
    private static Set<String> dayTypesToCopy = new HashSet<>();
    private static Set<DayTypeAssignment> dayTypeAssignmentsToCopy = new HashSet<>();
    private static Set<String> operatingPeriodToCopy = new HashSet<>();


    private static final JAXBContext publicationDeliveryContext = createContext(PublicationDeliveryStructure.class);

    @Override
    @SuppressWarnings("unchecked")
    public boolean execute(Context context) throws Exception {
        boolean result = SUCCESS;
        Monitor monitor = MonitorFactory.start(COMMAND);
        NetexXMLProcessingHelperFactory importer = (NetexXMLProcessingHelperFactory) context.get(IMPORTER);
        JobData jobData = (JobData) context.get(JOB_DATA);
        Path path = Paths.get(jobData.getPathName(), INPUT);

        List<Path> allFilePaths = FileUtil.listFiles(path, "*.xml", ".*.xml");
        File originalFile = new File(allFilePaths.get(0).toAbsolutePath().toString());

        try {

            PublicationDeliveryStructure netexJava = importer.unmarshal(new File(allFilePaths.get(0).toAbsolutePath().toString()), new HashSet<>(), context);

            List<Line> lines = extractLines(netexJava);
            Marshaller marshaller = createMarshaller();

            for (Line line : lines) {
                clearAllData();
                String fileName = "Line_" + line.getId().replace(":", "") + ".xml";
                PublicationDeliveryStructure newLinePubDelivery = generateNewLineFile(netexJava, line);
                File newLineFile = new File(path + "/" + fileName);

                OutputStream outputStream = new FileOutputStream(newLineFile);
                marshaller.marshal(objectFact.createPublicationDelivery(newLinePubDelivery), outputStream);
            }

            PublicationDeliveryStructure commonFilePub = generateCommonFile(netexJava);
            originalFile.delete();
            File commonFile = new File(path + "/commun.xml");
            OutputStream commonOutputStream = new FileOutputStream(commonFile);
            marshaller.marshal(objectFact.createPublicationDelivery(commonFilePub), commonOutputStream);

        } catch (Exception e) {
            log.error("Netex split file failed ", e);
            throw e;
        } finally {
            log.info(Color.MAGENTA + monitor.stop() + Color.NORMAL);
        }

        return result;
    }

    private void clearAllData() {
        scheduledStopPointsToCopy.clear();
        journeyPatternsToCopy.clear();
        journeyPartsCoupleToCopy.clear();
        trainNumbersToCopy.clear();
        journeyPartsToCopy.clear();
        routePointsToCopy.clear();
        destinationDisplayToCopy.clear();
        dayTypesToCopy.clear();
        dayTypeAssignmentsToCopy.clear();
        operatingPeriodToCopy.clear();
    }

    private static JAXBContext createContext(Class clazz) {
        try {
            JAXBContext jaxbContext = newInstance(clazz);

            return jaxbContext;
        } catch (JAXBException e) {
            String message = "Could not create instance of jaxb context for class " + clazz;
            log.warn(message, e);
            throw new RuntimeException("Could not create instance of jaxb context for class " + clazz, e);
        }
    }

    private Marshaller createMarshaller() throws JAXBException, IOException {
        Marshaller marshaller = publicationDeliveryContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, "");
        marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        marshaller.setProperty("com.sun.xml.bind.xmlDeclaration", Boolean.FALSE);
        marshaller.setProperty("com.sun.xml.bind.xmlHeaders", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        return marshaller;
    }


    /**
     * Generate a common file that contains common data relative to multiple lines
     *
     * @param netexJava the complete netex object that represent the original file
     * @return a new publication delivery structure that contains common data
     */
    private PublicationDeliveryStructure generateCommonFile(PublicationDeliveryStructure netexJava) {

        PublicationDeliveryStructure pubDelivery = new PublicationDeliveryStructure();

        ServiceFrame serviceFrame = new ServiceFrame();
        serviceFrame.setId(UUID.randomUUID().toString());
        serviceFrame.setVersion("any");
        List<JAXBElement<? extends Common_VersionFrameStructure>> frameList = new ArrayList<>();

        PublicationDeliveryStructure.DataObjects dataObjects = netexJava.getDataObjects();
        List<JAXBElement<? extends Common_VersionFrameStructure>> dataObjectFrames = dataObjects.getCompositeFrameOrCommonFrame();
        List<CompositeFrame> compositeFrames = NetexObjectUtil.getFrames(CompositeFrame.class, dataObjectFrames);
        for (CompositeFrame compositeFrame : compositeFrames) {
            for (JAXBElement<? extends Common_VersionFrameStructure> jaxbFrame : compositeFrame.getFrames().getCommonFrame()) {
                if (jaxbFrame.getValue() instanceof ServiceFrame) {
                    ServiceFrame originalServiceFrame = (ServiceFrame) jaxbFrame.getValue();
                    serviceFrame.setAdditionalNetworks(originalServiceFrame.getAdditionalNetworks());
                    serviceFrame.setRoutePoints(originalServiceFrame.getRoutePoints());
                    serviceFrame.setRouteLinks(originalServiceFrame.getRouteLinks());
                }

                if (jaxbFrame.getValue() instanceof SiteFrame) {
                    SiteFrame originalSiteFrame = (SiteFrame) jaxbFrame.getValue();
                    frameList.add(objectFact.createSiteFrame(originalSiteFrame));
                }

                if (jaxbFrame.getValue() instanceof ResourceFrame) {
                    ResourceFrame originalResourceFrame = (ResourceFrame) jaxbFrame.getValue();
                    frameList.add(objectFact.createResourceFrame(originalResourceFrame));
                }
            }
        }

        frameList.add(objectFact.createServiceFrame(serviceFrame));

        PublicationDeliveryStructure.DataObjects dataObj = new PublicationDeliveryStructure.DataObjects();
        dataObj.withCompositeFrameOrCommonFrame(frameList);
        dataObj.getCompositeFrameOrCommonFrame().get(0).getValue().setId(UUID.randomUUID().toString());
        dataObj.getCompositeFrameOrCommonFrame().get(0).getValue().setVersion("any");

        pubDelivery.setDataObjects(dataObj);
        pubDelivery.setPublicationTimestamp(LocalDateTime.now());
        pubDelivery.setParticipantRef(NETEX_VALID_PREFIX);

        return pubDelivery;
    }


    /**
     * Generate a new publication delivery that represent a line + all its related data
     *
     * @param netexJava the complete netex object that represent the original file
     * @param line      the line for which a new file must be created
     * @return the new publication delivery that contains the line data
     */
    private PublicationDeliveryStructure generateNewLineFile(PublicationDeliveryStructure netexJava, Line line) {

        PublicationDeliveryStructure pubDelivery = new PublicationDeliveryStructure();
        List<JAXBElement<? extends Common_VersionFrameStructure>> frameList = new ArrayList<>();

        ServiceFrame serviceFrame = new ServiceFrame();
        serviceFrame.setId(UUID.randomUUID().toString());
        serviceFrame.setVersion("any");
        LinesInFrame_RelStructure lineStruct = new LinesInFrame_RelStructure();
        lineStruct.getLine_().add(objectFact.createLine(line));
        serviceFrame.setLines(lineStruct);
        addRoutesToServiceFrame(netexJava, serviceFrame);

        if (!scheduledStopPointsToCopy.isEmpty()) {
            copyScheduledStopPoints(netexJava, serviceFrame);
            copyPassengerAssignments(netexJava, serviceFrame);
        }

        if (!journeyPatternsToCopy.isEmpty()) {
            TimetableFrame timetableFrame = new TimetableFrame();
            timetableFrame.setId(UUID.randomUUID().toString());
            timetableFrame.setVersion("any");
            copyServiceJourney(netexJava, timetableFrame);
            frameList.add(objectFact.createTimetableFrame(timetableFrame));

            if (!trainNumbersToCopy.isEmpty()) {
                copyTrainNumbers(netexJava, timetableFrame);
            }

            if (!journeyPartsCoupleToCopy.isEmpty()) {
                copyJourneyPartsCouple(netexJava, timetableFrame);
            }

            if (!dayTypesToCopy.isEmpty()) {
                ServiceCalendarFrame serviceCalendarFrame = new ServiceCalendarFrame();
                serviceCalendarFrame.setId(UUID.randomUUID().toString());
                serviceCalendarFrame.setVersion("any");
                copyDayTypes(netexJava, serviceCalendarFrame);
                frameList.add(objectFact.createServiceCalendarFrame(serviceCalendarFrame));
            }
        }

        if (!destinationDisplayToCopy.isEmpty()) {
            copyDestinationDisplays(netexJava, serviceFrame);
        }


        PublicationDeliveryStructure.DataObjects dataObj = new PublicationDeliveryStructure.DataObjects();
        frameList.add(objectFact.createServiceFrame(serviceFrame));

        dataObj.withCompositeFrameOrCommonFrame(frameList);
        dataObj.getCompositeFrameOrCommonFrame().get(0).getValue().setId(UUID.randomUUID().toString());
        dataObj.getCompositeFrameOrCommonFrame().get(0).getValue().setVersion("any");


        pubDelivery.setDataObjects(dataObj);
        pubDelivery.setPublicationTimestamp(LocalDateTime.now());
        pubDelivery.setParticipantRef("MOBIITI");
        return pubDelivery;
    }


    /**
     * Copy day types into a service calendar frame
     *
     * @param completeOriginalNetex      the complete netex object that represent the original file
     * @param serviceCalendarFrameToFeed the service calendar frame in which we must add day types
     */
    private void copyDayTypes(PublicationDeliveryStructure completeOriginalNetex, ServiceCalendarFrame serviceCalendarFrameToFeed) {


        List<ServiceCalendarFrame> serviceCalendarFrames = extractServiceCalendarFramesFromPublicationDelivery(completeOriginalNetex);
        if (serviceCalendarFrames.isEmpty()){
            return;
        }

        List<DayType> dayTypes = new ArrayList<>();
        for (ServiceCalendarFrame serviceCalendarFrame : serviceCalendarFrames) {
            for (JAXBElement<? extends DataManagedObjectStructure> jaxbDayType : serviceCalendarFrame.getDayTypes().getDayType_()) {
                if (jaxbDayType.getValue() instanceof DayType && dayTypesToCopy.contains(((DayType) jaxbDayType.getValue()).getId())) {
                    dayTypes.add((DayType) jaxbDayType.getValue());
                }
            }

            // Read all dayTypes assignments and search for assignments related to dayTypes that are being copied
            if (serviceCalendarFrame.getDayTypeAssignments() != null) {
                for (DayTypeAssignment dayTypeAssignment : serviceCalendarFrame.getDayTypeAssignments().getDayTypeAssignment()) {
                    if (dayTypeAssignment.getDayTypeRef() != null && dayTypesToCopy.contains(((DayTypeRefStructure) dayTypeAssignment.getDayTypeRef().getValue()).getRef())) {
                        dayTypeAssignmentsToCopy.add(dayTypeAssignment);
                    }
                }
            }
        }

        if (serviceCalendarFrameToFeed.getDayTypes() == null) {
            serviceCalendarFrameToFeed.setDayTypes(new DayTypesInFrame_RelStructure());
        }

        List<JAXBElement<DayType>> jaxbDayTypes = dayTypes.stream()
                .map(dayType -> objectFact.createDayType(dayType))
                .collect(Collectors.toList());

        serviceCalendarFrameToFeed.getDayTypes().getDayType_().addAll(jaxbDayTypes);

        if (serviceCalendarFrameToFeed.getDayTypeAssignments() == null) {
            serviceCalendarFrameToFeed.setDayTypeAssignments(new DayTypeAssignmentsInFrame_RelStructure());
        }

        serviceCalendarFrameToFeed.getDayTypeAssignments().getDayTypeAssignment().addAll(dayTypeAssignmentsToCopy);

        for (DayTypeAssignment dayTypeAssignment : dayTypeAssignmentsToCopy) {
            if (dayTypeAssignment.getOperatingPeriodRef() != null) {
                operatingPeriodToCopy.add(dayTypeAssignment.getOperatingPeriodRef().getValue().getRef());
            }
        }

        copyOperatingPeriods(completeOriginalNetex, serviceCalendarFrameToFeed);
    }

    private List<ServiceCalendarFrame> extractServiceCalendarFramesFromPublicationDelivery(PublicationDeliveryStructure completeOriginalNetex){
        List<ServiceCalendarFrame> result = new ArrayList<>();
        PublicationDeliveryStructure.DataObjects dataObjects = completeOriginalNetex.getDataObjects();
        List<JAXBElement<? extends Common_VersionFrameStructure>> dataObjectFrames = dataObjects.getCompositeFrameOrCommonFrame();
        List<CompositeFrame> compositeFrames = NetexObjectUtil.getFrames(CompositeFrame.class, dataObjectFrames);
        List<OperatingPeriod_VersionStructure> operatingPeriods = new ArrayList<>();

        for (CompositeFrame compositeFrame : compositeFrames) {
            for (JAXBElement<? extends Common_VersionFrameStructure> jaxbFrame : compositeFrame.getFrames().getCommonFrame()) {
                if (jaxbFrame.getValue() instanceof ServiceCalendarFrame) {
                    result.add((ServiceCalendarFrame) jaxbFrame.getValue());
                }
            }
        }

        return result;
    }

    /**
     * Copy operating periods into a service calendar frame
     *
     * @param completeOriginalNetex      the complete netex object that represent the original file
     * @param serviceCalendarFrameToFeed the service calendar in which operating periods must be copied
     */
    private void copyOperatingPeriods(PublicationDeliveryStructure completeOriginalNetex, ServiceCalendarFrame serviceCalendarFrameToFeed) {

        List<ServiceCalendarFrame> serviceCalendarFrames = extractServiceCalendarFramesFromPublicationDelivery(completeOriginalNetex);
        if (serviceCalendarFrames.isEmpty()){
            return;
        }

        List<OperatingPeriod_VersionStructure> operatingPeriods = new ArrayList<>();
        for (ServiceCalendarFrame serviceCalendarFrame : serviceCalendarFrames) {
            for (OperatingPeriod_VersionStructure operatingPeriod : serviceCalendarFrame.getOperatingPeriods().getOperatingPeriodOrUicOperatingPeriod()) {
                if (operatingPeriodToCopy.contains(operatingPeriod.getId())) {
                    operatingPeriods.add(operatingPeriod);
                }
            }
        }

        if (serviceCalendarFrameToFeed.getOperatingPeriods() == null) {
            serviceCalendarFrameToFeed.setOperatingPeriods(new OperatingPeriodsInFrame_RelStructure());
        }

        serviceCalendarFrameToFeed.getOperatingPeriods().getOperatingPeriodOrUicOperatingPeriod().addAll(operatingPeriods);

    }

    /**
     * Copy destination displays into a service Frame
     *
     * @param completeOriginalNetex the complete netex object that represent the original file
     * @param serviceFrameToFeed    the service frame in which destination display must be added
     */
    private void copyDestinationDisplays(PublicationDeliveryStructure completeOriginalNetex, ServiceFrame serviceFrameToFeed) {

        List<ServiceFrame> serviceFrames = extractServiceFramesFromPublicationDelivery(completeOriginalNetex);
        if (serviceFrames.isEmpty()){
            return;
        }

        List<DestinationDisplay> destinationDisplays = new ArrayList<>();

        for (ServiceFrame completeServiceFrame : serviceFrames) {
            for (DestinationDisplay destDisplay : completeServiceFrame.getDestinationDisplays().getDestinationDisplay()) {
                if (destinationDisplayToCopy.contains(destDisplay.getId())) {
                    destinationDisplays.add(destDisplay);
                }
            }
        }

        if (serviceFrameToFeed.getDestinationDisplays() == null) {
            serviceFrameToFeed.setDestinationDisplays(new DestinationDisplaysInFrame_RelStructure());
        }

        serviceFrameToFeed.getDestinationDisplays().getDestinationDisplay().addAll(destinationDisplays);
    }


    /**
     * Copy journey parts couple into a timetable frame
     *
     * @param completeOriginalNetex the complete netex object that represent the original file
     * @param timetableFrame        the timetable frame in which journey parts couple must be added
     */
    private void copyJourneyPartsCouple(PublicationDeliveryStructure completeOriginalNetex, TimetableFrame timetableFrame) {

        List<JourneyPartCouple> journeyPartCouples = new ArrayList<>();
        List<TimetableFrame> timetableFrames = extractTimetableFramesFromPublicationDelivery(completeOriginalNetex);

        if (timetableFrames.isEmpty()) {
            return;
        }

        for (TimetableFrame completeTimetableFrame : timetableFrames) {
            for (JourneyPartCouple journeyPartCouple : completeTimetableFrame.getJourneyPartCouples().getJourneyPartCouple()) {

                if (journeyPartsCoupleToCopy.contains(journeyPartCouple.getId())) {
                    journeyPartCouples.add(journeyPartCouple);
                    if (journeyPartCouple.getMainPartRef() != null) {
                        journeyPartsToCopy.add(journeyPartCouple.getMainPartRef().getRef());
                    }
                    if (journeyPartCouple.getJourneyParts() != null) {

                        for (JourneyPartRefStructure journeyPartRefStructure : journeyPartCouple.getJourneyParts().getJourneyPartRef()) {
                            journeyPartsToCopy.add(journeyPartRefStructure.getRef());
                        }
                    }
                }
            }
        }

        if (timetableFrame.getJourneyPartCouples() == null) {
            timetableFrame.setJourneyPartCouples(new JourneyPartCouplesInFrame_RelStructure());
        }
        timetableFrame.getJourneyPartCouples().getJourneyPartCouple().addAll(journeyPartCouples);

    }

    /**
     * Copy train numbers into a timetable frame
     *
     * @param completeOriginalNetex the complete netex object that represent the original file
     * @param timetableFrame        the timetable frame in which train numbers must be added
     */
    private void copyTrainNumbers(PublicationDeliveryStructure completeOriginalNetex, TimetableFrame timetableFrame) {

        List<TrainNumber> trainNumbers = new ArrayList<>();
        List<TimetableFrame> timetableFrames = extractTimetableFramesFromPublicationDelivery(completeOriginalNetex);

        if (timetableFrames.isEmpty()) {
            return;
        }

        for (TimetableFrame completeTimetableFrame : timetableFrames) {
            for (Object trainNumberObj : completeTimetableFrame.getTrainNumbers().getTrainNumberOrTrainNumberRef()) {
                if (trainNumberObj instanceof TrainNumber) {
                    TrainNumber trainNumber = (TrainNumber) trainNumberObj;
                    if (trainNumbersToCopy.contains(trainNumber.getId())) {
                        trainNumbers.add(trainNumber);
                    }
                }
            }
        }

        if (timetableFrame.getTrainNumbers() == null) {
            timetableFrame.setTrainNumbers(new TrainNumbersInFrame_RelStructure());
        }
        timetableFrame.getTrainNumbers().getTrainNumberOrTrainNumberRef().addAll(trainNumbers);

    }

    /**
     * Read a publication delivery and extract timetables
     *
     * @param completeOriginalNetex publication delivery that contains all data of the original file
     * @return a list of extracted timetables
     */
    private List<TimetableFrame> extractTimetableFramesFromPublicationDelivery(PublicationDeliveryStructure completeOriginalNetex) {
        List<TimetableFrame> results = new ArrayList<>();

        PublicationDeliveryStructure.DataObjects dataObjects = completeOriginalNetex.getDataObjects();
        List<JAXBElement<? extends Common_VersionFrameStructure>> dataObjectFrames = dataObjects.getCompositeFrameOrCommonFrame();
        List<CompositeFrame> compositeFrames = NetexObjectUtil.getFrames(CompositeFrame.class, dataObjectFrames);


        for (CompositeFrame compositeFrame : compositeFrames) {
            for (JAXBElement<? extends Common_VersionFrameStructure> jaxbFrame : compositeFrame.getFrames().getCommonFrame()) {
                if (jaxbFrame.getValue() instanceof TimetableFrame) {
                    results.add((TimetableFrame) jaxbFrame.getValue());
                }
            }
        }

        return results;
    }


    /**
     * Copy service journeys to a timetableFrame
     *
     * @param completeOriginalNetex the publication delivery that contains all data of the original file
     * @param timeTableFrame        the timetable frame that must be fed with serviceJourneys
     */
    private void copyServiceJourney(PublicationDeliveryStructure completeOriginalNetex, TimetableFrame timeTableFrame) {
        List<ServiceJourney> serviceJourneysToCopy = new ArrayList<>();
        List<TimetableFrame> timetableFrames = extractTimetableFramesFromPublicationDelivery(completeOriginalNetex);

        if (timetableFrames.isEmpty()) {
            return;
        }

        for (TimetableFrame completeTimetableFrame : timetableFrames) {
            for (Journey_VersionStructure journeyVersionStructure : completeTimetableFrame.getVehicleJourneys().getVehicleJourneyOrDatedVehicleJourneyOrNormalDatedVehicleJourney()) {
                if (journeyVersionStructure instanceof ServiceJourney) {
                    ServiceJourney serviceJourney = (ServiceJourney) journeyVersionStructure;
                    if (serviceJourney.getJourneyPatternRef() != null && serviceJourney.getJourneyPatternRef().getValue() != null
                            && journeyPatternsToCopy.contains(((JourneyPatternRefStructure) serviceJourney.getJourneyPatternRef().getValue()).getRef())) {

                        serviceJourney.setParts(null);
                        serviceJourneysToCopy.add(serviceJourney);
                    }
                }
            }
        }


        if (timeTableFrame.getVehicleJourneys() == null) {
            timeTableFrame.setVehicleJourneys(new JourneysInFrame_RelStructure());
        }

        for (ServiceJourney serviceJourney : serviceJourneysToCopy) {
            if (serviceJourney.getTrainNumbers() != null) {
                for (TrainNumberRefStructure trainNumberRefStructure : serviceJourney.getTrainNumbers().getTrainNumberRef()) {
                    trainNumbersToCopy.add(trainNumberRefStructure.getRef());
                }
            }

            if (serviceJourney.getParts() != null) {
                for (Object partObj : serviceJourney.getParts().getJourneyPartRefOrJourneyPart()) {
                    if (partObj instanceof JourneyPart) {
                        JourneyPart journeyPart = (JourneyPart) partObj;
                        if (journeyPart.getJourneyPartCoupleRef() != null) {
                            journeyPartsCoupleToCopy.add(journeyPart.getJourneyPartCoupleRef().getRef());
                        }
                    }
                }
            }

            if (serviceJourney.getDayTypes() != null && serviceJourney.getDayTypes().getDayTypeRef() != null) {
                for (JAXBElement<? extends DayTypeRefStructure> jaxbDayType : serviceJourney.getDayTypes().getDayTypeRef()) {
                    if (jaxbDayType.getValue() instanceof DayTypeRefStructure) {
                        DayTypeRefStructure dayTypeRef = (DayTypeRefStructure) jaxbDayType.getValue();
                        dayTypesToCopy.add(dayTypeRef.getRef());
                    }
                }
            }
        }

        timeTableFrame.getVehicleJourneys().getVehicleJourneyOrDatedVehicleJourneyOrNormalDatedVehicleJourney().addAll(serviceJourneysToCopy);
    }

    /**
     * Copy passenger assignments to a service frame
     *
     * @param completeOriginalNetex publication delivery that contains all data from the original file
     * @param serviceFrameToFeed    the service frame to feed with passenger assignments
     */
    private void copyPassengerAssignments(PublicationDeliveryStructure completeOriginalNetex, ServiceFrame serviceFrameToFeed) {

        List<ServiceFrame> serviceFrames = extractServiceFramesFromPublicationDelivery(completeOriginalNetex);
        if (serviceFrames.isEmpty()){
            return;
        }

        List<PassengerStopAssignment> passengerStopAssignments = new ArrayList<>();
        for (ServiceFrame completeServiceFrame : serviceFrames) {
            for (JAXBElement<? extends StopAssignment_VersionStructure> jaxbStopAssignment : completeServiceFrame.getStopAssignments().getStopAssignment()) {
                if (jaxbStopAssignment.getValue() instanceof PassengerStopAssignment) {

                    PassengerStopAssignment passengerStopAssignment = (PassengerStopAssignment) jaxbStopAssignment.getValue();
                    if (passengerStopAssignment.getScheduledStopPointRef() != null && scheduledStopPointsToCopy.contains(passengerStopAssignment.getScheduledStopPointRef().getValue().getRef())) {
                        passengerStopAssignments.add(passengerStopAssignment);
                    }
                }
            }
        }

        if (serviceFrameToFeed.getStopAssignments() == null) {
            serviceFrameToFeed.setStopAssignments(new StopAssignmentsInFrame_RelStructure());
        }

        for (PassengerStopAssignment passengerStopAssignment : passengerStopAssignments) {
            if (passengerStopAssignment.getQuayRef() != null && passengerStopAssignment.getQuayRef().getValue() != null) {
                QuayRefStructure quayRef = passengerStopAssignment.getQuayRef().getValue();
                quayRef.setVersionRef(quayRef.getVersion());
                quayRef.setVersion(null);
            }

        }

        List<JAXBElement<PassengerStopAssignment>> jaxbAssignmentList = passengerStopAssignments.stream()
                .map(passAssignement -> objectFact.createPassengerStopAssignment(passAssignement))
                .collect(Collectors.toList());
        serviceFrameToFeed.getStopAssignments().getStopAssignment().addAll(jaxbAssignmentList);

    }

    /**
     * Read a service frame to identify a line and then, copy all routes related to this line in the service frame
     *
     * @param completeOriginalNetex the publication delivery that contains all data from the original file
     * @param serviceFrame          the service frame that must be fed with routes
     */
    private void addRoutesToServiceFrame(PublicationDeliveryStructure completeOriginalNetex, ServiceFrame serviceFrame) {
        LinesInFrame_RelStructure lineStruct = serviceFrame.getLines();
        if (lineStruct == null || lineStruct.getLine_() == null || lineStruct.getLine_().size() == 0) {
            return;
        }

        Line line = (Line) lineStruct.getLine_().get(0).getValue();

        if (line.getRoutes() == null) {
            return;
        }

        for (RouteRefStructure routeRefStructure : line.getRoutes().getRouteRef()) {
            String routeRef = routeRefStructure.getRef();
            addRouteToServiceFrame(completeOriginalNetex, serviceFrame, routeRef);
            addServicePatternsToServiceFrame(completeOriginalNetex, serviceFrame, routeRef);
        }
    }


    /**
     * Copy all service patterns related to a route into a service frame
     *
     * @param completeOriginalNetex the publication delivery that contains all data of the original file
     * @param serviceFrameToFeed    the service frame to feed with service patterns
     * @param routeRef              the route for which service patterns must be added
     */
    private void addServicePatternsToServiceFrame(PublicationDeliveryStructure completeOriginalNetex, ServiceFrame serviceFrameToFeed, String routeRef) {


        List<JourneyPattern> journeyPatterns = new ArrayList<>();
        List<ServiceJourneyPattern> serviceJourneyPatterns = new ArrayList<>();

        List<ServiceFrame> serviceFrames = extractServiceFramesFromPublicationDelivery(completeOriginalNetex);
        if (serviceFrames.isEmpty()){
            return;
        }


        for (ServiceFrame completeServiceFrame : serviceFrames) {
            for (JAXBElement<?> journeyPatternObj : completeServiceFrame.getJourneyPatterns().getJourneyPattern_OrJourneyPatternView()) {

                if (journeyPatternObj.getValue() instanceof JourneyPattern) {
                    JourneyPattern journeyPattern = (JourneyPattern) journeyPatternObj.getValue();

                    if (journeyPattern.getRouteRef() != null && routeRef.equals(journeyPattern.getRouteRef().getValue())) {
                        journeyPatterns.add(journeyPattern);
                        journeyPatternsToCopy.add(journeyPattern.getId());
                    }
                }

                if (journeyPatternObj.getValue() instanceof ServiceJourneyPattern) {
                    ServiceJourneyPattern serviceJourneyPattern = (ServiceJourneyPattern) journeyPatternObj.getValue();

                    if (serviceJourneyPattern.getRouteRef() != null && routeRef.equals(serviceJourneyPattern.getRouteRef().getRef())) {
                        serviceJourneyPatterns.add(serviceJourneyPattern);
                        journeyPatternsToCopy.add(serviceJourneyPattern.getId());
                    }
                }
            }
        }


        if (journeyPatterns.size() == 0 && serviceJourneyPatterns.size() == 0) {
            return;
        }

        if (serviceFrameToFeed.getJourneyPatterns() == null) {
            serviceFrameToFeed.setJourneyPatterns(new JourneyPatternsInFrame_RelStructure());
        }

        if (journeyPatterns.size() > 0) {
            for (JourneyPattern journeyPattern : journeyPatterns) {
                if (journeyPattern.getDestinationDisplayRef() != null) {
                    destinationDisplayToCopy.add(journeyPattern.getDestinationDisplayRef().getRef());
                }
            }


            List<JAXBElement<JourneyPattern>> jaxbJourneyPatternList = journeyPatterns.stream()
                    .map(jp -> objectFact.createJourneyPattern(jp))
                    .collect(Collectors.toList());

            serviceFrameToFeed.getJourneyPatterns().getJourneyPattern_OrJourneyPatternView().addAll(jaxbJourneyPatternList);
            collectScheduledStopPointsFromJourneyPatterns(journeyPatterns);
        }


        if (serviceJourneyPatterns.size() > 0) {

            for (ServiceJourneyPattern serviceJourneyPattern : serviceJourneyPatterns) {
                if (serviceJourneyPattern.getDestinationDisplayRef() != null) {
                    destinationDisplayToCopy.add(serviceJourneyPattern.getDestinationDisplayRef().getRef());
                }
            }
            List<JAXBElement<ServiceJourneyPattern>> jaxbServiceJourneyPatternList = serviceJourneyPatterns.stream()
                    .map(jp -> objectFact.createServiceJourneyPattern(jp))
                    .collect(Collectors.toList());

            serviceFrameToFeed.getJourneyPatterns().getJourneyPattern_OrJourneyPatternView().addAll(jaxbServiceJourneyPatternList);
            collectScheduledStopPointsFromServiceJourneyPatterns(serviceJourneyPatterns);
        }
    }

    /**
     * Copy scheduled stop points to a service frame
     *
     * @param completeOriginalNetex the publication delivery that contains all data of the original file
     * @param serviceFrameToFeed    the service frame to feed with scheduled stop points
     */
    private void copyScheduledStopPoints(PublicationDeliveryStructure completeOriginalNetex, ServiceFrame serviceFrameToFeed) {
        List<ScheduledStopPoint> scheduledStopPoints = new ArrayList<>();

        List<ServiceFrame> serviceFrames = extractServiceFramesFromPublicationDelivery(completeOriginalNetex);
        if (serviceFrames.isEmpty()){
            return;
        }

        for (ServiceFrame completeServiceFrame : serviceFrames) {
            for (ScheduledStopPoint scheduledStopPoint : completeServiceFrame.getScheduledStopPoints().getScheduledStopPoint()) {
                if (scheduledStopPointsToCopy.contains(scheduledStopPoint.getId())) {
                    scheduledStopPoints.add(scheduledStopPoint);
                }
            }
        }

        if (serviceFrameToFeed.getScheduledStopPoints() == null) {
            serviceFrameToFeed.setScheduledStopPoints(new ScheduledStopPointsInFrame_RelStructure());
        }
        serviceFrameToFeed.getScheduledStopPoints().getScheduledStopPoint().addAll(scheduledStopPoints);

    }

    /**
     * Read a list of service journey patterns and collect scheduled stop points related to these service journey patterns
     *
     * @param serviceJourneyPatterns a list of service journey patterns that must be read
     */
    private static void collectScheduledStopPointsFromServiceJourneyPatterns(List<ServiceJourneyPattern> serviceJourneyPatterns) {
        for (ServiceJourneyPattern journeyPattern : serviceJourneyPatterns) {
            for (PointInLinkSequence_VersionedChildStructure pointInLinkSequenceVersionedChildStructure : journeyPattern.getPointsInSequence().getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern()) {
                if (pointInLinkSequenceVersionedChildStructure instanceof StopPointInJourneyPattern) {
                    JAXBElement<? extends ScheduledStopPointRefStructure> scheduledStopPointRef = ((StopPointInJourneyPattern) pointInLinkSequenceVersionedChildStructure).getScheduledStopPointRef();
                    if (scheduledStopPointRef != null && scheduledStopPointRef.getValue() != null) {
                        scheduledStopPointsToCopy.add(scheduledStopPointRef.getValue().getRef());
                    }
                }
            }

        }
    }

    /**
     * Read a list of journey patterns and collect scheduled stop points related to these journey pattens
     *
     * @param journeyPatterns a list of journey patterns to read
     */
    private static void collectScheduledStopPointsFromJourneyPatterns(List<JourneyPattern> journeyPatterns) {
        for (JourneyPattern journeyPattern : journeyPatterns) {
            for (PointInLinkSequence_VersionedChildStructure pointInLinkSequenceVersionedChildStructure : journeyPattern.getPointsInSequence().getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern()) {
                if (pointInLinkSequenceVersionedChildStructure instanceof StopPointInJourneyPattern) {
                    JAXBElement<? extends ScheduledStopPointRefStructure> scheduledStopPointRef = ((StopPointInJourneyPattern) pointInLinkSequenceVersionedChildStructure).getScheduledStopPointRef();
                    if (scheduledStopPointRef != null && scheduledStopPointRef.getValue() != null) {
                        scheduledStopPointsToCopy.add(scheduledStopPointRef.getValue().getRef());
                    }
                }
            }
        }
    }

    /**
     * Add route to a service frame
     *
     * @param completeOriginalNetex the publication delivery that contains all data of the original file
     * @param serviceFrameToFeed    the service frame to feed with the route
     * @param routeRef              the route ref to copy
     */
    private void addRouteToServiceFrame(PublicationDeliveryStructure completeOriginalNetex, ServiceFrame serviceFrameToFeed, String routeRef) {


        List<Route> routeList = new ArrayList<>();

        List<ServiceFrame> serviceFrame = extractServiceFramesFromPublicationDelivery(completeOriginalNetex);
        if (serviceFrame.isEmpty()){
            return;
        }

        for (ServiceFrame completeServiceFrame : serviceFrame) {
            for (JAXBElement<? extends DataManagedObjectStructure> jaxbElement : completeServiceFrame.getRoutes().getRoute_()) {
                if (jaxbElement.getValue() instanceof Route) {
                    Route route = (Route) jaxbElement.getValue();
                    if (routeRef.equals(route.getId())) {
                        routeList.add((Route) jaxbElement.getValue());
                        for (PointOnRoute pointOnRoute : route.getPointsInSequence().getPointOnRoute()) {
                            PointRefStructure pointref = pointOnRoute.getPointRef().getValue();
                            routePointsToCopy.add(pointref.getRef());
                        }
                    }
                }
            }
        }

        if (routeList.size() == 0) {
            return;
        }

        RoutesInFrame_RelStructure routStruct = serviceFrameToFeed.getRoutes() != null ? serviceFrameToFeed.getRoutes() : new RoutesInFrame_RelStructure();
        List<JAXBElement<Route>> jaxbRouteList = routeList.stream()
                .map(route -> objectFact.createRoute(route))
                .collect(Collectors.toList());
        routStruct.getRoute_().addAll(jaxbRouteList);
        serviceFrameToFeed.setRoutes(routStruct);
    }


    private List<ServiceFrame> extractServiceFramesFromPublicationDelivery(PublicationDeliveryStructure publicationDelivery) {
        List<ServiceFrame> results = new ArrayList<>();
        PublicationDeliveryStructure.DataObjects dataObjects = publicationDelivery.getDataObjects();
        List<JAXBElement<? extends Common_VersionFrameStructure>> dataObjectFrames = dataObjects.getCompositeFrameOrCommonFrame();
        List<CompositeFrame> compositeFrames = NetexObjectUtil.getFrames(CompositeFrame.class, dataObjectFrames);

        for (CompositeFrame compositeFrame : compositeFrames) {
            for (JAXBElement<? extends Common_VersionFrameStructure> jaxbFrame : compositeFrame.getFrames().getCommonFrame()) {
                if (jaxbFrame.getValue() instanceof ServiceFrame) {
                    results.add((ServiceFrame) jaxbFrame.getValue());
                }
            }
        }
        return results;
    }

    /**
     * Read a publication delivery and extract a list of line
     *
     * @param publicationDelivery the publication delivery that contains all data of the original file
     * @return a list of extracted lines
     */
    private List<Line> extractLines(PublicationDeliveryStructure publicationDelivery) {

        List<Line> lineList = new ArrayList<>();
        List<ServiceFrame> serviceFrames = extractServiceFramesFromPublicationDelivery(publicationDelivery);
        if (serviceFrames.isEmpty()) {
            return lineList;
        }

        for (ServiceFrame serviceFrame : serviceFrames) {
            for (JAXBElement<? extends DataManagedObjectStructure> jaxbElement : serviceFrame.getLines().getLine_()) {
                if (jaxbElement.getValue() instanceof Line) {
                    lineList.add((Line) jaxbElement.getValue());
                }
            }
        }
        return lineList;
    }


    public static class DefaultCommandFactory extends CommandFactory {

        @Override
        protected Command create(InitialContext context) throws IOException {
            Command result = new NetexSplitFileCommand();
            return result;
        }
    }

    static {
        CommandFactory.factories.put(NetexSplitFileCommand.class.getName(), new DefaultCommandFactory());
    }
}
