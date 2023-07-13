package mobi.chouette.exchange.netexprofile.parser;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.TimeUtil;
import mobi.chouette.exchange.NetexParserUtils;
import mobi.chouette.exchange.importer.Parser;
import mobi.chouette.exchange.importer.ParserFactory;
import mobi.chouette.exchange.netexprofile.Constant;
import mobi.chouette.exchange.netexprofile.importer.NetexprofileImportParameters;
import mobi.chouette.exchange.netexprofile.importer.util.NetexImportUtil;
import mobi.chouette.exchange.netexprofile.importer.util.NetexTimeConversionUtil;
import mobi.chouette.exchange.report.AnalyzeReport;
import mobi.chouette.model.*;
import mobi.chouette.model.VehicleJourney;
import mobi.chouette.model.type.JourneyCategoryEnum;
import mobi.chouette.model.type.TransportModeNameEnum;
import mobi.chouette.model.util.ObjectIdTypes;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.Referential;
import org.rutebanken.netex.model.*;
import org.rutebanken.netex.model.FlexibleServiceProperties;

import javax.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@Log4j
public class TemplateServiceJourneyParser extends NetexParser implements Parser, Constant {

    private KeyValueParser keyValueParser = new KeyValueParser();

    private ContactStructureParser contactStructureParser = new ContactStructureParser();


    @Override
    @SuppressWarnings("unchecked")
    public void parse(Context context) throws Exception {
        Referential referential = (Referential) context.get(REFERENTIAL);
        JourneysInFrame_RelStructure journeyStructs = (JourneysInFrame_RelStructure) context.get(NETEX_LINE_DATA_CONTEXT);
        NetexprofileImportParameters parameters = (NetexprofileImportParameters) context.get(CONFIGURATION);
        List<Journey_VersionStructure> serviceJourneys = journeyStructs.getVehicleJourneyOrDatedVehicleJourneyOrNormalDatedVehicleJourney();

        serviceJourneys.stream()
                .peek(templateServiceJourney -> {
                    if (!(templateServiceJourney instanceof TemplateServiceJourney)) {
                        log.debug("Ignoring non-TemplateServiceJourney journey or deadrun with id: " + templateServiceJourney.getId());
                    }
                })
                .filter(serviceJourney -> serviceJourney instanceof TemplateServiceJourney)
                .map(templateServiceJourney -> (TemplateServiceJourney)templateServiceJourney)
                .forEach(templateServiceJourney -> {
                    String serviceJourneyId = NetexImportUtil.composeObjectIdFromNetexId(context,"TemplateServiceJourney", templateServiceJourney.getId());
                    VehicleJourney vehicleJourney = ObjectFactory.getVehicleJourney(referential,serviceJourneyId);
                    if (vehicleJourney.isFilled()) {
                        VehicleJourney vehicleJourneyWithVersion = ObjectFactory.getVehicleJourney(referential,
                                templateServiceJourney.getId() + "_" + templateServiceJourney.getVersion());
                        log.warn("Already parsed " + vehicleJourney.getObjectId() + ", will use version field as part of id to separate them: "
                                + vehicleJourneyWithVersion.getObjectId());
                        vehicleJourney = vehicleJourneyWithVersion;
                    }
                    DayTypeRefs_RelStructure dayTypes = templateServiceJourney.getDayTypes();
                    if (dayTypes != null) {
                        for (JAXBElement<? extends DayTypeRefStructure> dayType : dayTypes.getDayTypeRef()) {
                            String timetableId = NetexImportUtil.composeObjectIdFromNetexId(context, "Timetable", dayType.getValue().getRef());
                            Timetable timetable = ObjectFactory.getTimetable(referential, timetableId);
                            timetable.addVehicleJourney(vehicleJourney);
                        }
                    }

                    vehicleJourney.setObjectVersion(NetexParserUtils.getVersion(templateServiceJourney));
                    vehicleJourney.setPublishedJourneyIdentifier(templateServiceJourney.getPublicCode());
                    if (templateServiceJourney.getPrivateCode() != null) {
                        vehicleJourney.setPrivateCode(templateServiceJourney.getPrivateCode().getValue());
                    }

                    if (templateServiceJourney.getJourneyPatternRef() != null) {
                        JourneyPatternRefStructure patternRefStruct = templateServiceJourney.getJourneyPatternRef().getValue();
                        String journeyPatternId = NetexImportUtil.composeObjectIdFromNetexId("JourneyPattern", parameters.getObjectIdPrefix(), patternRefStruct.getRef());

                        mobi.chouette.model.JourneyPattern journeyPattern = ObjectFactory.getJourneyPattern(referential,journeyPatternId);
                        vehicleJourney.setJourneyPattern(journeyPattern);
                    }

                    if (templateServiceJourney.getName() != null) {
                        vehicleJourney.setPublishedJourneyName(templateServiceJourney.getName().getValue());
                    } else {
                        mobi.chouette.model.JourneyPattern journeyPattern = vehicleJourney.getJourneyPattern();
                        if (journeyPattern.getDepartureStopPoint() != null) {
                            mobi.chouette.model.DestinationDisplay dd = journeyPattern.getDepartureStopPoint().getDestinationDisplay();
                            if (dd != null) {
                                vehicleJourney.setPublishedJourneyName(dd.getFrontText());
                            }
                        }
                    }
                    if (templateServiceJourney.getOperatorRef() != null) {
                        String operatorIdRef = templateServiceJourney.getOperatorRef().getRef();
                        Company company = ObjectFactory.getCompany(referential, operatorIdRef);
                        vehicleJourney.setCompany(company);
                    } else if (templateServiceJourney.getLineRef() != null) {
                        String lineIdRef = templateServiceJourney.getLineRef().getValue().getRef();
                        Company company = ObjectFactory.getLine(referential, lineIdRef).getCompany();
                        vehicleJourney.setCompany(company);
                    } else {
                        Company company = vehicleJourney.getJourneyPattern().getRoute().getLine().getCompany();
                        vehicleJourney.setCompany(company);
                    }

                    if (templateServiceJourney.getRouteRef() != null) {
                        mobi.chouette.model.Route route = ObjectFactory.getRoute(referential, templateServiceJourney.getRouteRef().getRef());
                        vehicleJourney.setRoute(route);
                    } else {
                        mobi.chouette.model.Route route = vehicleJourney.getJourneyPattern().getRoute();
                        vehicleJourney.setRoute(route);
                    }
                    if (templateServiceJourney.getTransportMode() != null) {
                        AllVehicleModesOfTransportEnumeration transportMode = templateServiceJourney.getTransportMode();
                        TransportModeNameEnum transportModeName = NetexParserUtils.toTransportModeNameEnum(transportMode.value());
                        vehicleJourney.setTransportMode(transportModeName);
                    }

                    if(templateServiceJourney.getFrequencyGroups() != null
                            && templateServiceJourney.getFrequencyGroups().getHeadwayJourneyGroupRefOrHeadwayJourneyGroupOrRhythmicalJourneyGroupRef() != null){
                        FrequencyGroups_RelStructure frequencyGroup = templateServiceJourney.getFrequencyGroups();
                        List<JourneyFrequency> journeyFrequencies = new ArrayList<>();
                        VehicleJourney finalVehicleJourney = vehicleJourney;
                        frequencyGroup.getHeadwayJourneyGroupRefOrHeadwayJourneyGroupOrRhythmicalJourneyGroupRef().stream()
                                .filter(headwayJourneyGroupRef -> headwayJourneyGroupRef instanceof HeadwayJourneyGroupRefStructure)
                                .forEach(headwayJourneyGroupRef -> {
                                    String headwayIdRef = ((HeadwayJourneyGroupRefStructure) headwayJourneyGroupRef).getRef();
                                    String serviceHeadwayJourneyId = NetexImportUtil.composeObjectIdFromNetexId(context,"HeadwayJourney", headwayIdRef);
                                    JourneyFrequency journeyFrequency = ObjectFactory.getJourneyFrequency(referential, serviceHeadwayJourneyId);
                                    journeyFrequency.setVehicleJourney(finalVehicleJourney);
                                    journeyFrequencies.add(journeyFrequency);

                        });
                        vehicleJourney.setJourneyFrequencies(journeyFrequencies);
                        vehicleJourney.setJourneyCategory(JourneyCategoryEnum.Frequency);
                    }

                    vehicleJourney.setTransportSubMode(NetexParserUtils.toTransportSubModeNameEnum(templateServiceJourney.getTransportSubmode()));

                    parseTimetabledPassingTimes(context, referential, templateServiceJourney, vehicleJourney);

                    vehicleJourney.setKeyValues(keyValueParser.parse(templateServiceJourney.getKeyList()));
                    vehicleJourney.setServiceAlteration(NetexParserUtils.toServiceAlterationEum(templateServiceJourney.getServiceAlteration()));

                    if (templateServiceJourney.getFlexibleServiceProperties() != null) {
                        vehicleJourney.setFlexibleService(true);
                        mobi.chouette.model.FlexibleServiceProperties chouetteFSP = new mobi.chouette.model.FlexibleServiceProperties();
                        FlexibleServiceProperties netexFSP = templateServiceJourney.getFlexibleServiceProperties();

                        chouetteFSP.setObjectId(netexFSP.getId());
                        chouetteFSP.setObjectVersion(NetexParserUtils.getVersion(netexFSP));

                        chouetteFSP.setChangeOfTimePossible(netexFSP.isChangeOfTimePossible());
                        chouetteFSP.setCancellationPossible(netexFSP.isCancellationPossible());
                        chouetteFSP.setFlexibleServiceType(NetexParserUtils.toFlexibleServiceType(netexFSP.getFlexibleServiceType()));

                        BookingArrangement bookingArrangement = new BookingArrangement();
                        if (netexFSP.getBookingNote() != null) {
                            bookingArrangement.setBookingNote(netexFSP.getBookingNote().getValue());
                        }
                        bookingArrangement.setBookingAccess(NetexParserUtils.toBookingAccess(netexFSP.getBookingAccess()));
                        bookingArrangement.setBookWhen(NetexParserUtils.toPurchaseWhen(netexFSP.getBookWhen()));
                        bookingArrangement.setBuyWhen(netexFSP.getBuyWhen().stream().map(NetexParserUtils::toPurchaseMoment).collect(Collectors.toList()));
                        bookingArrangement.setBookingMethods(netexFSP.getBookingMethods().stream().map(NetexParserUtils::toBookingMethod).collect(Collectors.toList()));
                        bookingArrangement.setLatestBookingTime(TimeUtil.toJodaLocalTime(netexFSP.getLatestBookingTime()));
                        bookingArrangement.setMinimumBookingPeriod(TimeUtil.toJodaDuration(netexFSP.getMinimumBookingPeriod()));

                        chouetteFSP.setBookingArrangement(bookingArrangement);
                        vehicleJourney.setFlexibleServiceProperties(chouetteFSP);
                    }
                    vehicleJourney.setFilled(true);
                });

    }


    private void parseTimetabledPassingTimes(Context context, Referential referential, TemplateServiceJourney templateServiceJourney, VehicleJourney vehicleJourney) {

        NetexprofileImportParameters configuration = (NetexprofileImportParameters) context.get(CONFIGURATION);
        String journeyPatternId = NetexImportUtil.composeObjectIdFromNetexId(context,"JourneyPattern",templateServiceJourney.getJourneyPatternRef().getValue().getRef());

        mobi.chouette.model.JourneyPattern journeyPattern = referential.getJourneyPatterns().get(journeyPatternId);

        if (templateServiceJourney.getPassingTimes() == null){
            handleEmptyPassingTimes(context, templateServiceJourney);
            return;
        }

        for (int i = 0; i < templateServiceJourney.getPassingTimes().getTimetabledPassingTime().size(); i++) {
            TimetabledPassingTime passingTime = templateServiceJourney.getPassingTimes().getTimetabledPassingTime().get(i);
            String passingTimeId = passingTime.getId();

            if (passingTimeId == null) {
                // TODO profile should prevent this from happening, creating bogus
                passingTimeId = NetexParserUtils.netexId(configuration.getObjectIdPrefix(), ObjectIdTypes.VEHICLE_JOURNEY_AT_STOP_KEY, UUID.randomUUID().toString());
            }
            VehicleJourneyAtStop vehicleJourneyAtStop = ObjectFactory.getVehicleJourneyAtStop(referential, passingTimeId);
            vehicleJourneyAtStop.setObjectVersion(NetexParserUtils.getVersion(passingTime));

            StopPoint stopPoint = journeyPattern.getStopPoints().get(i);
            vehicleJourneyAtStop.setStopPoint(stopPoint);

            parsePassingTimes(passingTime, vehicleJourneyAtStop);
            vehicleJourneyAtStop.setVehicleJourney(vehicleJourney);
        }

        vehicleJourney.getVehicleJourneyAtStops().sort(Comparator.comparingInt(o -> o.getStopPoint().getPosition()));
    }

    private void handleEmptyPassingTimes(Context context, TemplateServiceJourney templateServiceJourney) {

        String fileName = (String) context.get(FILE_NAME);
        String serviceJourneyId = templateServiceJourney.getId();

        log.error("Empty passing times in sequence in file :" + fileName + " , templateServiceJourney:" + serviceJourneyId);

        if ( context.get(ANALYSIS_REPORT) == null){
            return ;
        }

        AnalyzeReport analyzeReport = (AnalyzeReport) context.get(ANALYSIS_REPORT);
        analyzeReport.addEmptyPassingTimes(fileName, serviceJourneyId);
    }

        // TODO add support for other time zones and zone offsets, for now only handling UTC
    private void parsePassingTimes(TimetabledPassingTime timetabledPassingTime, VehicleJourneyAtStop vehicleJourneyAtStop) {

        NetexTimeConversionUtil.parsePassingTime(timetabledPassingTime, false, vehicleJourneyAtStop);
        NetexTimeConversionUtil.parsePassingTime(timetabledPassingTime, true, vehicleJourneyAtStop);

        // TODO copying missing data since Chouette pt does not properly support missing values
        if (vehicleJourneyAtStop.getArrivalTime() == null && vehicleJourneyAtStop.getDepartureTime() != null) {
            vehicleJourneyAtStop.setArrivalTime(vehicleJourneyAtStop.getDepartureTime());
            vehicleJourneyAtStop.setArrivalDayOffset(vehicleJourneyAtStop.getDepartureDayOffset());
        } else if (vehicleJourneyAtStop.getArrivalTime() != null && vehicleJourneyAtStop.getDepartureTime() == null) {
            vehicleJourneyAtStop.setDepartureTime(vehicleJourneyAtStop.getArrivalTime());
            vehicleJourneyAtStop.setDepartureDayOffset(vehicleJourneyAtStop.getArrivalDayOffset());
        }

    }

    static {
        ParserFactory.register(TemplateServiceJourneyParser.class.getName(), new ParserFactory() {
            private TemplateServiceJourneyParser instance = new TemplateServiceJourneyParser();

            @Override
            protected Parser create() {
                return instance;
            }
        });
    }
}
