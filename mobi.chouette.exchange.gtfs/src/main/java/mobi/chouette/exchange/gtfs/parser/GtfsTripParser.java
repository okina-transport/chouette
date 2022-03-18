package mobi.chouette.exchange.gtfs.parser;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.PrecisionModel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.TimeUtil;
import mobi.chouette.exchange.gtfs.importer.GtfsImportParameters;
import mobi.chouette.exchange.gtfs.model.GtfsFrequency;
import mobi.chouette.exchange.gtfs.model.GtfsRoute;
import mobi.chouette.exchange.gtfs.model.GtfsShape;
import mobi.chouette.exchange.gtfs.model.GtfsStop;
import mobi.chouette.exchange.gtfs.model.GtfsStop.LocationType;
import mobi.chouette.exchange.gtfs.model.GtfsStopTime;
import mobi.chouette.exchange.gtfs.model.GtfsStopTime.DropOffType;
import mobi.chouette.exchange.gtfs.model.GtfsStopTime.PickupType;
import mobi.chouette.exchange.gtfs.model.GtfsTransfer;
import mobi.chouette.exchange.gtfs.model.GtfsTransfer.TransferType;
import mobi.chouette.exchange.gtfs.model.GtfsTrip;
import mobi.chouette.exchange.gtfs.model.GtfsTrip.DirectionType;
import mobi.chouette.exchange.gtfs.model.importer.GtfsException;
import mobi.chouette.exchange.gtfs.model.importer.GtfsImporter;
import mobi.chouette.exchange.gtfs.model.importer.Index;
import mobi.chouette.exchange.gtfs.model.importer.RouteById;
import mobi.chouette.exchange.gtfs.model.importer.ShapeById;
import mobi.chouette.exchange.gtfs.model.importer.StopById;
import mobi.chouette.exchange.gtfs.model.importer.StopTimeByTrip;
import mobi.chouette.exchange.gtfs.validation.Constant;
import mobi.chouette.exchange.gtfs.validation.GtfsValidationReporter;
import mobi.chouette.exchange.importer.Parser;
import mobi.chouette.exchange.importer.ParserFactory;
import mobi.chouette.exchange.importer.Validator;
import mobi.chouette.model.DestinationDisplay;
import mobi.chouette.model.Interchange;
import mobi.chouette.model.JourneyFrequency;
import mobi.chouette.model.JourneyPattern;
import mobi.chouette.model.Line;
import mobi.chouette.model.Route;
import mobi.chouette.model.RoutePoint;
import mobi.chouette.model.RouteSection;
import mobi.chouette.model.ScheduledStopPoint;
import mobi.chouette.model.SimpleObjectReference;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.StopPoint;
import mobi.chouette.model.Timeband;
import mobi.chouette.model.Timetable;
import mobi.chouette.model.VehicleJourney;
import mobi.chouette.model.VehicleJourneyAtStop;
import mobi.chouette.model.type.AlightingPossibilityEnum;
import mobi.chouette.model.type.BoardingAlightingPossibilityEnum;
import mobi.chouette.model.type.BoardingPossibilityEnum;
import mobi.chouette.model.type.JourneyCategoryEnum;
import mobi.chouette.model.type.PTDirectionEnum;
import mobi.chouette.model.type.SectionStatusEnum;
import mobi.chouette.model.type.TransportSubModeNameEnum;
import mobi.chouette.model.util.NeptuneUtil;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.ObjectIdTypes;
import mobi.chouette.model.util.Referential;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.joda.time.Duration;
import org.joda.time.LocalTime;

import java.math.BigDecimal;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j
public class GtfsTripParser implements Parser, Validator, Constant {

    private static final Comparator<OrderedCoordinate> COORDINATE_SORTER = new OrderedCoordinateComparator();

    @Getter
    @Setter
    private String gtfsRouteId;


    //key = "stopAreaId"_"occurence"
    private Map<String, StopPoint> alredyProcessedStopPoints = new HashMap<>();

    private String previousProcessedStopPoint;

    @Override
    public void validate(Context context) throws Exception {
        GtfsValidationReporter gtfsValidationReporter = (GtfsValidationReporter) context.get(GTFS_REPORTER);
        gtfsValidationReporter.getExceptions().clear();

        validateStopTimes(context);
        validateShapes(context);
        validateTrips(context);
        validateFrequencies(context);
    }

    private void validateStopTimes(Context context) throws Exception {

        GtfsImporter importer = (GtfsImporter) context.get(PARSER);
        GtfsValidationReporter gtfsValidationReporter = (GtfsValidationReporter) context.get(GTFS_REPORTER);
        Set<String> stopIds = new HashSet<String>();

        // stop_times.txt
        // log.info("validating stop_times");
        if (importer.hasStopTimeImporter()) { // the file "stop_times.txt"
            // exists ?
            gtfsValidationReporter.reportSuccess(context, GTFS_1_GTFS_Common_1, GTFS_STOP_TIMES_FILE);

            Index<GtfsStopTime> stopTimeParser = null;
            try { // Read and check the header line of the file "stop_times.txt"
                stopTimeParser = importer.getStopTimeByTrip();
            } catch (Exception ex) {
                if (ex instanceof GtfsException) {
                    gtfsValidationReporter.reportError(context, (GtfsException) ex, GTFS_STOP_TIMES_FILE);
                } else {
                    gtfsValidationReporter.throwUnknownError(context, ex, GTFS_STOP_TIMES_FILE);
                }
            }

            gtfsValidationReporter.validateOkCSV(context, GTFS_STOP_TIMES_FILE);

            if (stopTimeParser == null) { // importer.getStopTimeByTrip() fails
                // for any other reason
                gtfsValidationReporter.throwUnknownError(context, new Exception(
                        "Cannot instantiate StopTimeByTrip class"), GTFS_STOP_TIMES_FILE);
            } else {
                gtfsValidationReporter.validate(context, GTFS_STOP_TIMES_FILE, stopTimeParser.getOkTests());
                gtfsValidationReporter.validateUnknownError(context);
            }

            if (!stopTimeParser.getErrors().isEmpty()) {
                gtfsValidationReporter.reportErrors(context, stopTimeParser.getErrors(), GTFS_STOP_TIMES_FILE);
                stopTimeParser.getErrors().clear();
            }

            gtfsValidationReporter.validateOKGeneralSyntax(context, GTFS_STOP_TIMES_FILE);

            if (stopTimeParser.getLength() == 0) {
                gtfsValidationReporter.reportError(context, new GtfsException(GTFS_STOP_TIMES_FILE, 1, null,
                        GtfsException.ERROR.FILE_WITH_NO_ENTRY, null, null), GTFS_STOP_TIMES_FILE);
            } else {
                gtfsValidationReporter.validate(context, GTFS_STOP_TIMES_FILE, GtfsException.ERROR.FILE_WITH_NO_ENTRY);
            }

            GtfsException fatalException = null;
            stopTimeParser.setWithValidation(true);
            for (GtfsStopTime bean : stopTimeParser) {

                if (bean.getStopId() != null)
                    stopIds.add(bean.getStopId());
                try {
                    stopTimeParser.validate(bean, importer);
                } catch (Exception ex) {
                    if (ex instanceof GtfsException) {
                        gtfsValidationReporter.reportError(context, (GtfsException) ex, GTFS_STOP_TIMES_FILE);
                    } else {
                        gtfsValidationReporter.throwUnknownError(context, ex, GTFS_STOP_TIMES_FILE);
                    }
                }
                for (GtfsException ex : bean.getErrors()) {
                    if (ex.isFatal())
                        fatalException = ex;
                }
                gtfsValidationReporter.reportErrors(context, bean.getErrors(), GTFS_STOP_TIMES_FILE);
                gtfsValidationReporter.validate(context, GTFS_STOP_TIMES_FILE, bean.getOkTests());
            }
            // contrôle de la séquence
            stopTimeParser.setWithValidation(false);
            {
                Iterable<String> tripIds = stopTimeParser.keys();

                Map<Integer, Integer> stopSequences = new HashMap<>();
                for (String tripId : tripIds) {
                    stopSequences.clear();
                    Iterable<GtfsStopTime> stopTimes = stopTimeParser.values(tripId);
                    for (GtfsStopTime bean : stopTimes) {
                        Integer stopSequence = bean.getStopSequence();
                        if (stopSequence != null) {
                            if (stopSequences.containsKey(stopSequence)) {
                                gtfsValidationReporter.reportError(
                                        context,
                                        new GtfsException(stopTimeParser.getPath(), bean.getId(), stopTimeParser
                                                .getIndex(StopTimeByTrip.FIELDS.stop_sequence.name()),
                                                StopTimeByTrip.FIELDS.trip_id.name() + ","
                                                        + StopTimeByTrip.FIELDS.stop_sequence.name(),
                                                GtfsException.ERROR.DUPLICATE_STOP_SEQUENCE, null, tripId + ","
                                                + stopSequence), GTFS_STOP_TIMES_FILE);
                            } else {
                                stopSequences.put(stopSequence, bean.getId());
                                gtfsValidationReporter.validate(context, GTFS_STOP_TIMES_FILE,
                                        GtfsException.ERROR.DUPLICATE_STOP_SEQUENCE);
                            }
                        }
                    }
                }

            }
            int i = 1;
            boolean unsuedId = true;
            for (GtfsStop bean : importer.getStopById()) {
                if (LocationType.Stop.equals(bean.getLocationType())) {
                    if (stopIds.add(bean.getStopId())) {
                        unsuedId = false;
                        gtfsValidationReporter.reportError(context, new GtfsException(GTFS_STOPS_FILE, i,
                                        StopById.FIELDS.stop_id.name(), GtfsException.ERROR.UNUSED_ID, null, bean.getStopId()),
                                GTFS_STOPS_FILE);
                    }
                }
                i++;
            }
            if (unsuedId)
                gtfsValidationReporter.validate(context, GTFS_STOPS_FILE, GtfsException.ERROR.UNUSED_ID);
            gtfsValidationReporter.getExceptions().clear();
            if (fatalException != null)
                throw fatalException;
        } else {
            gtfsValidationReporter.reportError(context, new GtfsException(GTFS_STOP_TIMES_FILE, 1, null,
                    GtfsException.ERROR.MISSING_FILE, null, null), GTFS_STOP_TIMES_FILE);
        }
    }

    private void validateShapes(Context context) throws Exception {
        GtfsImporter importer = (GtfsImporter) context.get(PARSER);
        GtfsValidationReporter gtfsValidationReporter = (GtfsValidationReporter) context.get(GTFS_REPORTER);

        // shapes.txt
        // log.info("validating shapes");
        if (importer.hasShapeImporter()) {
            gtfsValidationReporter.reportSuccess(context, GTFS_1_GTFS_Common_1, GTFS_SHAPES_FILE);

            Index<GtfsShape> shapeParser = null;
            try { // Read and check the header line of the file "shapes.txt"
                shapeParser = importer.getShapeById();
            } catch (Exception ex) {
                if (ex instanceof GtfsException) {
                    gtfsValidationReporter.reportError(context, (GtfsException) ex, GTFS_SHAPES_FILE);
                } else {
                    gtfsValidationReporter.throwUnknownError(context, ex, GTFS_SHAPES_FILE);
                }
            }

            gtfsValidationReporter.validateOkCSV(context, GTFS_SHAPES_FILE);

            if (shapeParser == null) { // importer.getShapeById() fails for any
                // other reason
                gtfsValidationReporter.throwUnknownError(context, new Exception("Cannot instantiate ShapeById class"),
                        GTFS_SHAPES_FILE);
            } else {
                gtfsValidationReporter.validate(context, GTFS_SHAPES_FILE, shapeParser.getOkTests());
                gtfsValidationReporter.validateUnknownError(context);
            }

            if (!shapeParser.getErrors().isEmpty()) {
                gtfsValidationReporter.reportErrors(context, shapeParser.getErrors(), GTFS_SHAPES_FILE);
                shapeParser.getErrors().clear();
            }

            gtfsValidationReporter.validateOKGeneralSyntax(context, GTFS_SHAPES_FILE);

            if (shapeParser.getLength() == 0) {
                gtfsValidationReporter.reportError(context, new GtfsException(GTFS_SHAPES_FILE, 1, null,
                        GtfsException.ERROR.OPTIONAL_FILE_WITH_NO_ENTRY, null, null), GTFS_SHAPES_FILE);
            } else {
                gtfsValidationReporter.validate(context, GTFS_SHAPES_FILE, GtfsException.ERROR.FILE_WITH_NO_ENTRY);
            }

            GtfsException fatalException = null;
            shapeParser.setWithValidation(true);

            for (GtfsShape bean : shapeParser) {
                try {
                    shapeParser.validate(bean, importer);
                } catch (Exception ex) {
                    if (ex instanceof GtfsException) {
                        gtfsValidationReporter.reportError(context, (GtfsException) ex, GTFS_SHAPES_FILE);
                    } else {
                        gtfsValidationReporter.throwUnknownError(context, ex, GTFS_SHAPES_FILE);
                    }
                }
                for (GtfsException ex : bean.getErrors()) {
                    if (ex.isFatal())
                        fatalException = ex;
                }
                gtfsValidationReporter.reportErrors(context, bean.getErrors(), GTFS_SHAPES_FILE);
                gtfsValidationReporter.validate(context, GTFS_SHAPES_FILE, bean.getOkTests());
            }

            // contrôle de la séquence
            shapeParser.setWithValidation(false);
            {
                Iterable<String> tripIds = shapeParser.keys();

                Map<Integer, Integer> shapeSequences = new HashMap<>();
                for (String tripId : tripIds) {
                    shapeSequences.clear();
                    Iterable<GtfsShape> shapes = shapeParser.values(tripId);
                    for (GtfsShape bean : shapes) {
                        Integer stopSequence = bean.getShapePtSequence();
                        if (stopSequence != null) {
                            if (shapeSequences.containsKey(stopSequence)) {
                                gtfsValidationReporter.reportError(
                                        context,
                                        new GtfsException(shapeParser.getPath(), bean.getId(), shapeParser
                                                .getIndex(ShapeById.FIELDS.shape_pt_sequence.name()),
                                                ShapeById.FIELDS.shape_id.name() + ","
                                                        + ShapeById.FIELDS.shape_pt_sequence.name(),
                                                GtfsException.ERROR.DUPLICATE_STOP_SEQUENCE, null, tripId + ","
                                                + stopSequence), GTFS_SHAPES_FILE);
                            } else {
                                shapeSequences.put(stopSequence, bean.getId());
                                gtfsValidationReporter.validate(context, GTFS_SHAPES_FILE,
                                        GtfsException.ERROR.DUPLICATE_STOP_SEQUENCE);
                            }
                        }
                    }
                }

            }

            shapeParser.setWithValidation(false);
            if (fatalException != null)
                throw fatalException;
        } else {
            gtfsValidationReporter.reportError(context, new GtfsException(GTFS_SHAPES_FILE, 1, null,
                    GtfsException.ERROR.MISSING_OPTIONAL_FILE, null, null), GTFS_SHAPES_FILE);
        }
    }

    private void validateTrips(Context context) throws Exception {
        GtfsImporter importer = (GtfsImporter) context.get(PARSER);
        GtfsValidationReporter gtfsValidationReporter = (GtfsValidationReporter) context.get(GTFS_REPORTER);
        Set<String> routeIds = new HashSet<String>();

        // trips.txt
        // log.info("validating trips");
        if (importer.hasTripImporter()) { // the file "trips.txt" exists ?
            gtfsValidationReporter.reportSuccess(context, GTFS_1_GTFS_Common_1, GTFS_TRIPS_FILE);

            Index<GtfsTrip> tripParser = null;
            try { // Read and check the header line of the file "trips.txt"
                tripParser = importer.getTripById();
            } catch (Exception ex) {
                if (ex instanceof GtfsException) {
                    gtfsValidationReporter.reportError(context, (GtfsException) ex, GTFS_TRIPS_FILE);
                } else {
                    gtfsValidationReporter.throwUnknownError(context, ex, GTFS_TRIPS_FILE);
                }
            }

            gtfsValidationReporter.validateOkCSV(context, GTFS_TRIPS_FILE);

            if (tripParser == null) { // importer.getTripById() fails for any
                // other reason
                gtfsValidationReporter.throwUnknownError(context, new Exception("Cannot instantiate TripById class"),
                        GTFS_TRIPS_FILE);
            } else {
                gtfsValidationReporter.validate(context, GTFS_TRIPS_FILE, tripParser.getOkTests());
                gtfsValidationReporter.validateUnknownError(context);
            }

            if (!tripParser.getErrors().isEmpty()) {
                gtfsValidationReporter.reportErrors(context, tripParser.getErrors(), GTFS_TRIPS_FILE);
                tripParser.getErrors().clear();
            }

            gtfsValidationReporter.validateOKGeneralSyntax(context, GTFS_TRIPS_FILE);

            if (tripParser.getLength() == 0) {
                gtfsValidationReporter.reportError(context, new GtfsException(GTFS_TRIPS_FILE, 1, null,
                        GtfsException.ERROR.FILE_WITH_NO_ENTRY, null, null), GTFS_TRIPS_FILE);
            } else {
                gtfsValidationReporter.validate(context, GTFS_TRIPS_FILE, GtfsException.ERROR.FILE_WITH_NO_ENTRY);
            }

            GtfsException fatalException = null;
            tripParser.setWithValidation(true);
            for (GtfsTrip bean : tripParser) {
                if (bean.getRouteId() != null)
                    routeIds.add(bean.getRouteId());
                try {
                    tripParser.validate(bean, importer);
                } catch (Exception ex) {
                    if (ex instanceof GtfsException) {
                        gtfsValidationReporter.reportError(context, bean.getRouteId(), (GtfsException) ex,
                                GTFS_TRIPS_FILE);
                    } else {
                        gtfsValidationReporter.throwUnknownError(context, ex, GTFS_TRIPS_FILE);
                    }
                }
                for (GtfsException ex : bean.getErrors()) {
                    if (ex.isFatal())
                        fatalException = ex;
                }
                gtfsValidationReporter.reportErrors(context, bean.getRouteId(), bean.getErrors(), GTFS_TRIPS_FILE);
                gtfsValidationReporter.validate(context, GTFS_TRIPS_FILE, bean.getOkTests());
            }
            tripParser.setWithValidation(false);
            int i = 1;
            boolean unsuedId = true;
            for (GtfsRoute bean : importer.getRouteById()) {
                String newRouteId = bean.getRouteId().split("-")[0];
                bean.setRouteId(newRouteId);
                if (routeIds.add(bean.getRouteId())) {
                    unsuedId = false;
                    gtfsValidationReporter.reportError(context, new GtfsException(GTFS_ROUTES_FILE, i,
                                    RouteById.FIELDS.route_id.name(), GtfsException.ERROR.UNUSED_ID, null, bean.getRouteId()),
                            GTFS_TRIPS_FILE);
                }
                i++;
            }
            if (unsuedId)
                gtfsValidationReporter.validate(context, GTFS_ROUTES_FILE, GtfsException.ERROR.UNUSED_ID);
            if (fatalException != null)
                throw fatalException;
        } else {
            gtfsValidationReporter.reportError(context, new GtfsException(GTFS_TRIPS_FILE, 1, null,
                    GtfsException.ERROR.MISSING_FILE, null, null), GTFS_TRIPS_FILE);
        }
    }

    private void validateFrequencies(Context context) throws Exception {
        GtfsImporter importer = (GtfsImporter) context.get(PARSER);
        GtfsValidationReporter gtfsValidationReporter = (GtfsValidationReporter) context.get(GTFS_REPORTER);

        // frequencies.txt
        // log.info("validating frequencies");
        if (importer.hasFrequencyImporter()) {
            gtfsValidationReporter.reportSuccess(context, GTFS_1_GTFS_Common_1, GTFS_FREQUENCIES_FILE);

            Index<GtfsFrequency> frequencyParser = null;
            try { // Read and check the header line of the file
                // "frequenciess.txt"
                frequencyParser = importer.getFrequencyByTrip();
            } catch (Exception ex) {
                if (ex instanceof GtfsException) {
                    gtfsValidationReporter.reportError(context, (GtfsException) ex, GTFS_FREQUENCIES_FILE);
                } else {
                    gtfsValidationReporter.throwUnknownError(context, ex, GTFS_FREQUENCIES_FILE);
                }
            }

            gtfsValidationReporter.validateOkCSV(context, GTFS_FREQUENCIES_FILE);

            if (frequencyParser == null) { // importer.getFrequencyByTrip()
                // fails for any other reason
                gtfsValidationReporter.throwUnknownError(context, new Exception(
                        "Cannot instantiate FrequencyByTrip class"), GTFS_FREQUENCIES_FILE);
            } else {
                gtfsValidationReporter.validate(context, GTFS_FREQUENCIES_FILE, frequencyParser.getOkTests());
                gtfsValidationReporter.validateUnknownError(context);
            }

            if (!frequencyParser.getErrors().isEmpty()) {
                gtfsValidationReporter.reportErrors(context, frequencyParser.getErrors(), GTFS_FREQUENCIES_FILE);
                frequencyParser.getErrors().clear();
            }

            gtfsValidationReporter.validateOKGeneralSyntax(context, GTFS_FREQUENCIES_FILE);

            if (frequencyParser.getLength() == 0) {
                // validationReporter.reportUnsuccess(context,
                // GTFS_1_GTFS_Frequency_1, GTFS_FREQUENCIES_FILE);
                gtfsValidationReporter.reportError(context, new GtfsException(GTFS_FREQUENCIES_FILE, 1, null,
                        GtfsException.ERROR.OPTIONAL_FILE_WITH_NO_ENTRY, null, null), GTFS_FREQUENCIES_FILE);
            } else {
                gtfsValidationReporter.validate(context, GTFS_FREQUENCIES_FILE, GtfsException.ERROR.FILE_WITH_NO_ENTRY);
            }

            GtfsException fatalException = null;
            frequencyParser.setWithValidation(true);
            for (GtfsFrequency bean : frequencyParser) {
                try {
                    frequencyParser.validate(bean, importer);
                } catch (Exception ex) {
                    if (ex instanceof GtfsException) {
                        gtfsValidationReporter.reportError(context, (GtfsException) ex, GTFS_FREQUENCIES_FILE);
                    } else {
                        gtfsValidationReporter.throwUnknownError(context, ex, GTFS_FREQUENCIES_FILE);
                    }
                }
                for (GtfsException ex : bean.getErrors()) {
                    if (ex.isFatal())
                        fatalException = ex;
                }
                gtfsValidationReporter.reportErrors(context, bean.getErrors(), GTFS_FREQUENCIES_FILE);
                gtfsValidationReporter.validate(context, GTFS_FREQUENCIES_FILE, bean.getOkTests());
            }
            frequencyParser.setWithValidation(false);
            if (fatalException != null)
                throw fatalException;
        } else {
            gtfsValidationReporter.reportError(context, new GtfsException(GTFS_FREQUENCIES_FILE, 1, null,
                    GtfsException.ERROR.MISSING_OPTIONAL_FILE, null, null), GTFS_FREQUENCIES_FILE);
        }
    }

    @Override
    public void parse(Context context) throws Exception {

        Referential referential = (Referential) context.get(REFERENTIAL);
        GtfsImporter importer = (GtfsImporter) context.get(PARSER);
        GtfsImportParameters configuration = (GtfsImportParameters) context.get(CONFIGURATION);

        Map<String, JourneyPattern> journeyPatternByStopSequence = new HashMap<String, JourneyPattern>();

        // VehicleJourney
        Index<GtfsTrip> gtfsTrips = importer.getTripByRoute();

        for (GtfsTrip gtfsTrip : gtfsTrips.values(gtfsRouteId)) {

            if (!importer.getStopTimeByTrip().values(gtfsTrip.getTripId()).iterator().hasNext()) {
                continue;
            }
            boolean hasTimes = true;
            for (GtfsStopTime gtfsStopTime : importer.getStopTimeByTrip().values(gtfsTrip.getTripId())) {
                if (gtfsStopTime.getArrivalTime() == null) {
                    hasTimes = false;
                    break;
                }
                if (gtfsStopTime.getDepartureTime() == null) {
                    hasTimes = false;
                    break;
                }
            }
            if (!hasTimes)
                continue;

            String objectId = AbstractConverter.composeObjectId(configuration,
                    VehicleJourney.VEHICLEJOURNEY_KEY, gtfsTrip.getTripId(), log);
            VehicleJourney vehicleJourney = ObjectFactory.getVehicleJourney(referential, objectId);
            convert(context, gtfsTrip, vehicleJourney);

            // VehicleJourneyAtStop
            boolean afterMidnight = true;

            for (GtfsStopTime gtfsStopTime : importer.getStopTimeByTrip().values(gtfsTrip.getTripId())) {
                VehicleJourneyAtStopWrapper vehicleJourneyAtStop = new VehicleJourneyAtStopWrapper(
                        gtfsStopTime.getStopId(), gtfsStopTime.getStopSequence(), gtfsStopTime.getShapeDistTraveled(), gtfsStopTime.getDropOffType(), gtfsStopTime.getPickupType(), gtfsStopTime.getStopHeadsign());

                // TAD probalement à faire évoluer un jour pour les interruptions
                // 2X

                boolean isDropOff0 = vehicleJourneyAtStop.getDropOff() == null || vehicleJourneyAtStop.getDropOff().equals(DropOffType.Scheduled);
                boolean isPickUp0 = vehicleJourneyAtStop.getPickup() == null || vehicleJourneyAtStop.getPickup().equals(PickupType.Scheduled);
                boolean isDropOff1 = vehicleJourneyAtStop.getDropOff() != null && vehicleJourneyAtStop.getDropOff().equals(DropOffType.NoAvailable);
                boolean isPickUp1 = vehicleJourneyAtStop.getPickup() != null && vehicleJourneyAtStop.getPickup().equals(PickupType.NoAvailable);
                boolean isDropOff2 = vehicleJourneyAtStop.getDropOff() != null && vehicleJourneyAtStop.getDropOff().equals(DropOffType.AgencyCall);
                boolean isPickUp2 = vehicleJourneyAtStop.getPickup() != null && vehicleJourneyAtStop.getPickup().equals(PickupType.AgencyCall);

                if(isDropOff2) {
                    if (isPickUp2) {
                        vehicleJourneyAtStop.setBoardingAlightingPossibility(BoardingAlightingPossibilityEnum.BoardAndAlightOnRequest);
                    } else if (isPickUp0){
                        vehicleJourneyAtStop.setBoardingAlightingPossibility(BoardingAlightingPossibilityEnum.BoardAndAlightOnRequest);
                    } else if(isPickUp1){
                        vehicleJourneyAtStop.setBoardingAlightingPossibility(BoardingAlightingPossibilityEnum.AlightOnRequest);
                    }
                } else if (isDropOff1) {
                    if(isPickUp1) {
                        vehicleJourneyAtStop.setBoardingAlightingPossibility(BoardingAlightingPossibilityEnum.NeitherBoardOrAlight);
                    } else if(isPickUp0) {
                        vehicleJourneyAtStop.setBoardingAlightingPossibility(BoardingAlightingPossibilityEnum.BoardOnly);
                    } else if(isPickUp2){
                        vehicleJourneyAtStop.setBoardingAlightingPossibility(BoardingAlightingPossibilityEnum.BoardOnRequest);
                    }
                } else if(isDropOff0) {
                    if(isPickUp0){
                        vehicleJourneyAtStop.setBoardingAlightingPossibility(BoardingAlightingPossibilityEnum.BoardAndAlight);
                    } else if(isPickUp1) {
                        vehicleJourneyAtStop.setBoardingAlightingPossibility(BoardingAlightingPossibilityEnum.AlightOnly);
                    } else if(isPickUp2) {
                        vehicleJourneyAtStop.setBoardingAlightingPossibility(BoardingAlightingPossibilityEnum.BoardAndAlightOnRequest);
                    }
                }

                convert(context, gtfsStopTime, gtfsTrip, vehicleJourneyAtStop);

                if (afterMidnight) {
                    if (!gtfsStopTime.getArrivalTime().moreOneDay())
                        afterMidnight = false;
                    if (!gtfsStopTime.getDepartureTime().moreOneDay())
                        afterMidnight = false;
                }

                vehicleJourneyAtStop.setVehicleJourney(vehicleJourney);
            }

            Collections.sort(vehicleJourney.getVehicleJourneyAtStops(), VEHICLE_JOURNEY_AT_STOP_COMPARATOR);

            // Timetable
            String timetableId = AbstractConverter.composeObjectId(configuration,
                    Timetable.TIMETABLE_KEY, gtfsTrip.getServiceId(), log);

            // Disable linking to after midnight-calendar as this causes day offsets to be compensated twice.
//			if (afterMidnight) {
//				timetableId += GtfsCalendarParser.AFTER_MIDNIGHT_SUFFIX;
//			}
            Timetable timetable = ObjectFactory.getTimetable(referential, timetableId);
            vehicleJourney.getTimetables().add(timetable);

            // JourneyPattern
            String journeyKey = gtfsTrip.getRouteId() + "_" + gtfsTrip.getDirectionId().ordinal();
            Iterable<GtfsShape> gtfsShapes = null;
            if (gtfsTrip.getShapeId() != null && !gtfsTrip.getShapeId().isEmpty()
                    && importer.getShapeById().containsKey(gtfsTrip.getShapeId())) {
                journeyKey += "_" + gtfsTrip.getShapeId();
                gtfsShapes = importer.getShapeById().values(gtfsTrip.getShapeId());
            }
            for (VehicleJourneyAtStop vehicleJourneyAtStop : vehicleJourney.getVehicleJourneyAtStops()) {
                String stopIdWithDropOffPickup = createJourneyKeyFragment((VehicleJourneyAtStopWrapper) vehicleJourneyAtStop);
                journeyKey += "," + stopIdWithDropOffPickup;
            }
            journeyKey += "_" + vehicleJourney.getVehicleJourneyAtStops().size();
            JourneyPattern journeyPattern = journeyPatternByStopSequence.get(journeyKey);
            if (journeyPattern == null) {
                journeyPattern = createJourneyPattern(context, referential, configuration, gtfsTrip, gtfsShapes,
                        vehicleJourney, journeyKey, journeyPatternByStopSequence);
            }

            vehicleJourney.setRoute(journeyPattern.getRoute());
            vehicleJourney.setJourneyPattern(journeyPattern);

            int length = journeyPattern.getStopPoints().size();
            for (int i = 0; i < length; i++) {
                VehicleJourneyAtStop vehicleJourneyAtStop = vehicleJourney.getVehicleJourneyAtStops().get(i);
                vehicleJourneyAtStop.setStopPoint(journeyPattern.getStopPoints().get(i));
            }

            // apply frequencies if any
            if (importer.hasFrequencyImporter()) {
                createJourneyFrequencies(context, referential, importer, configuration, gtfsTrip, vehicleJourney);
            }


            if (configuration.isParseInterchanges() && importer.hasTransferImporter()) {
                createInterchanges(context, referential, importer, configuration, gtfsTrip, vehicleJourney);
            }

        }

        completeRoutesInformations(referential);

        // dispose collections
        journeyPatternByStopSequence.clear();
    }

    private void completeRoutesInformations(Referential referential){

        for (Route route : referential.getRoutes().values()) {
            completeRouteInformations(referential, route);
        }
    }

    private void completeRouteInformations(Referential referential, Route route){

        route.getStopPoints().sort(STOP_POINT_POSITION_COMPARATOR);

        if (route.getName() == null) {

            if (!route.getStopPoints().isEmpty()) {
                StopPoint firstStopPoint = route.getStopPoints().get(0);
                StopPoint lastStopPoint = route.getStopPoints().get(route.getStopPoints().size() - 1);

                if (firstStopPoint.getScheduledStopPoint().getContainedInStopAreaRef().getObject() != null && lastStopPoint.getScheduledStopPoint().getContainedInStopAreaRef().getObject() != null) {
                    String first = firstStopPoint.getScheduledStopPoint().getContainedInStopAreaRef().getObject().getName();
                    String last = lastStopPoint.getScheduledStopPoint().getContainedInStopAreaRef().getObject().getName();
                    route.setName(first + " -> " + last);
                }
            }
        }

        // Create route point from first an last stop point on route
        if (route.getRoutePoints().isEmpty()) {
            if (!route.getStopPoints().isEmpty()) {
                StopPoint firstStopPoint = route.getStopPoints().get(0);
                route.getRoutePoints().add(createRoutePointFromStopPoint(referential, firstStopPoint));

                StopPoint lastStopPoint = route.getStopPoints().get(route.getStopPoints().size() - 1);
                route.getRoutePoints().add(createRoutePointFromStopPoint(referential, lastStopPoint));
            }
        }

        route.setFilled(true);

        reOrderStopPointsInRoute(route);
    }


    /***
     * Read all journeys and reorder points in route, if needed
     * @param route
     *  The route on which stopPoints must be reordered
     */
    private void reOrderStopPointsInRoute(Route route){

        for (JourneyPattern journeyPattern : route.getJourneyPatterns()) {
            reorderStopPointsInRoute(route, journeyPattern);
        }
    }

    private void reorderStopPointsInRoute(Route route, JourneyPattern journeyPattern){
        journeyPattern.getStopPoints().sort(STOP_POINT_POSITION_COMPARATOR);
        Map<String, Integer> occurenceMap = new HashMap<>();

        Integer previousStopPointPositionInRoute = null;
        int currentOccurence;

        for (StopPoint stopPoint : journeyPattern.getStopPoints()) {

            String stopAreaId = stopPoint.getScheduledStopPoint().getContainedInStopAreaRef().getObjectId();

            if (!occurenceMap.containsKey(stopAreaId)){
                currentOccurence = 1;
                occurenceMap.put(stopAreaId, currentOccurence);
            }else{
                currentOccurence = occurenceMap.get(stopAreaId) + 1;
                occurenceMap.put(stopAreaId, currentOccurence);
            }


            int currentPointPositionInRoute = getPositionInRoute(route, stopAreaId,  currentOccurence);

            if (previousStopPointPositionInRoute != null && currentPointPositionInRoute < previousStopPointPositionInRoute){
                    //there is an issue. The current point has been located before his predecessor. It must be moved
                    moveStopPointInRoute(route, currentPointPositionInRoute,previousStopPointPositionInRoute);
                    previousStopPointPositionInRoute = getPositionInRoute(route, stopAreaId,  currentOccurence);
                    continue;
            }

            previousStopPointPositionInRoute = currentPointPositionInRoute;

        }
    }


    /**
     * Moves a stop point to a target position
     * e.g:
     *          Route : C-A-B-D
     *          currentPointPositionInRoute : 0
     *          targetPositionInRoute : 2
     *          C point will be moved to index 2
     *          A and B will be shifted to get : A-B-C-D at indexes 0-1-2-3
     *
     * @param route
     *      The route on which the reorder must be made
     * @param currentPointPositionInRoute
     *      The position on which the point to move is actually stored
     * @param targetPositionInRoute
     *      The position on which the
     */
    private void moveStopPointInRoute(Route route, int currentPointPositionInRoute, int targetPositionInRoute){
        StopPoint stopPointToMove = route.getStopPoints().stream()
                                                         .filter(sp-> sp.getPosition() == currentPointPositionInRoute)
                                                         .findFirst()
                                                         .orElseThrow(() -> new IllegalArgumentException("Point not found in route. position:" + currentPointPositionInRoute));

        List<StopPoint> impactedPointsToShift = route.getStopPoints().stream()
                                                                     .filter(sp-> sp.getPosition() > currentPointPositionInRoute && sp.getPosition() <= targetPositionInRoute)
                                                                     .collect(Collectors.toList());
        stopPointToMove.setPosition(targetPositionInRoute);

        impactedPointsToShift.forEach(sp-> sp.setPosition(sp.getPosition() - 1));

    }

    private int getPositionInRoute(Route route, String stopAreaId, int occurenceNb){

        route.getStopPoints().sort(STOP_POINT_POSITION_COMPARATOR);
        Map<String, Integer> occurenceMap = new HashMap<>();
        int currentOccurence;

        for (StopPoint stopPoint : route.getStopPoints()) {

             String currentStopAreaId = stopPoint.getScheduledStopPoint().getContainedInStopAreaRef().getObjectId();

             if (!occurenceMap.containsKey(currentStopAreaId)){
                currentOccurence = 1;
                occurenceMap.put(currentStopAreaId, currentOccurence);
            }else{
                currentOccurence = occurenceMap.get(currentStopAreaId) + 1;
                occurenceMap.put(currentStopAreaId, currentOccurence);
            }

            if (currentStopAreaId.equals(stopAreaId) && currentOccurence == occurenceNb){
                return stopPoint.getPosition();
            }
        }

        throw new IllegalArgumentException(" Unable to found point : " + stopAreaId + ", occurence :" + occurenceNb + " , in route :" + route.getObjectId());
    }



    private String createJourneyKeyFragment(VehicleJourneyAtStopWrapper vehicleJourneyAtStop) {
        DropOffType drop = (vehicleJourneyAtStop.dropOff == null ? DropOffType.Scheduled : vehicleJourneyAtStop.dropOff);
        PickupType pickup = (vehicleJourneyAtStop.pickup == null ? PickupType.Scheduled : vehicleJourneyAtStop.pickup);

        String result = null;

//        if (drop == DropOffType.Scheduled && pickup == PickupType.Scheduled) {
            result = vehicleJourneyAtStop.stopId;
//        } else {
//            result = vehicleJourneyAtStop.stopId + "." + drop.ordinal() + "" + pickup.ordinal();
//        }
//
//        if (vehicleJourneyAtStop.stopHeadsign != null) {
//            result += vehicleJourneyAtStop.stopHeadsign;
//        }

        return result;
    }

    private void createInterchanges(Context context, Referential referential, GtfsImporter importer, GtfsImportParameters configuration, GtfsTrip gtfsTrip,
                                    VehicleJourney vehicleJourney) {


        for (GtfsTransfer gtfsTransfer : importer.getTransferByFromTrip().values(gtfsTrip.getTripId())) {
            if (gtfsTransfer.getFromTripId() != null && gtfsTransfer.getToTripId() != null) {
                Interchange interchange = createInterchange(referential, configuration, gtfsTransfer);

                if (gtfsTransfer.getMinTransferTime() != null && gtfsTransfer.getTransferType() == TransferType.Minimal) {
                    interchange.setMinimumTransferTime(Duration.standardSeconds(gtfsTransfer.getMinTransferTime()));
                    interchange.setGuaranteed(Boolean.FALSE);
                } else if (gtfsTransfer.getTransferType().equals(TransferType.Timed)) {
                    interchange.setGuaranteed(Boolean.TRUE);
                }

                String feederStopAreaId = AbstractConverter.composeObjectId(configuration,
                        "Quay", gtfsTransfer.getFromStopId(), log);

                // find stoppoint for this journey
                JourneyPattern jp = vehicleJourney.getJourneyPattern();
                for (StopPoint sp : jp.getStopPoints()) {
                    ScheduledStopPoint ssp = sp.getScheduledStopPoint();
                    if (ssp.getContainedInStopAreaRef().getObjectId().equals(feederStopAreaId)) {
                        interchange.setFeederStopPoint(ssp);
                        // Can be multiple matches, but GTFS does not specify which visit
                        break;
                    }
                }

                interchange.setFeederVehicleJourney(vehicleJourney);
                vehicleJourney.getFeederInterchanges().add(interchange);

                createInterchangeName(interchange);
            }
        }
        for (GtfsTransfer gtfsTransfer : importer.getTransferByToTrip().values(gtfsTrip.getTripId())) {
            if (gtfsTransfer.getFromTripId() != null && gtfsTransfer.getToTripId() != null) {
                Interchange interchange = createInterchange(referential, configuration, gtfsTransfer);

                if (gtfsTransfer.getMinTransferTime() != null && gtfsTransfer.getTransferType() == TransferType.Minimal) {
                    interchange.setMinimumTransferTime(Duration.standardSeconds(gtfsTransfer.getMinTransferTime()));
                    interchange.setGuaranteed(Boolean.FALSE);
                } else if (gtfsTransfer.getTransferType().equals(TransferType.Timed)) {
                    interchange.setGuaranteed(Boolean.TRUE);
                }

                String consumerStopAreaId = AbstractConverter.composeObjectId(configuration,
                        "Quay", gtfsTransfer.getToStopId(), log);


                // find stoppoint for this journey
                JourneyPattern jp = vehicleJourney.getJourneyPattern();
                for (StopPoint sp : jp.getStopPoints()) {
                    ScheduledStopPoint ssp = sp.getScheduledStopPoint();
                    if (ssp.getContainedInStopAreaRef().getObjectId().equals(consumerStopAreaId)) {
                        interchange.setConsumerStopPoint(ssp);
                        // Can be multiple matches, but GTFS does not specify which visit
                        break;
                    }
                }

                interchange.setConsumerVehicleJourney(vehicleJourney);
                vehicleJourney.getConsumerInterchanges().add(interchange);

                createInterchangeName(interchange);
            }
        }

    }

    protected Interchange createInterchange(Referential referential, GtfsImportParameters configuration, GtfsTransfer gtfsTransfer) {
        String partialId = StringUtils.join(new String[]{
                gtfsTransfer.getFromStopId(),
                gtfsTransfer.getToStopId(),
                gtfsTransfer.getFromRouteId(),
                gtfsTransfer.getToRouteId(),
                gtfsTransfer.getFromTripId(),
                gtfsTransfer.getToTripId(),
        }, "_");
        String objectId = AbstractConverter.composeObjectId(configuration,
                Interchange.INTERCHANGE_KEY, partialId, log);
        Interchange interchange = ObjectFactory.getInterchange(referential, objectId);
        return interchange;
    }

    protected void createInterchangeName(Interchange interchange) {
        StringBuilder nameBuilder = new StringBuilder();
        nameBuilder.append("From ");
        if (interchange.getFeederVehicleJourneyObjectid() != null) {
            nameBuilder.append(interchange.getFeederVehicleJourneyObjectid());
        } else {
            nameBuilder.append(" <unknown> ");
        }

        nameBuilder.append(" at ");

        if (interchange.getFeederStopPointObjectid() != null) {
            nameBuilder.append(interchange.getFeederStopPointObjectid());
        } else {
            nameBuilder.append(" <unknown> ");
        }

        nameBuilder.append(" to ");

        if (interchange.getConsumerVehicleJourneyObjectid() != null) {
            nameBuilder.append(interchange.getConsumerVehicleJourneyObjectid());
        } else {
            nameBuilder.append(" <unknown> ");
        }

        nameBuilder.append(" at ");

        if (interchange.getConsumerStopPointObjectid() != null) {
            nameBuilder.append(interchange.getConsumerStopPointObjectid());
        } else {
            nameBuilder.append(" <unknown> ");
        }


        interchange.setName(nameBuilder.toString());
    }

    private void createJourneyFrequencies(Context context, Referential referential, GtfsImporter importer,
                                          GtfsImportParameters configuration, GtfsTrip gtfsTrip, VehicleJourney vehicleJourney) {
        int count = 0;
        for (GtfsFrequency frequency : importer.getFrequencyByTrip().values(gtfsTrip.getTripId())) {
            vehicleJourney.setJourneyCategory(JourneyCategoryEnum.Frequency);

            String timeBandObjectId = AbstractConverter.composeObjectId(configuration,
                    Timeband.TIMETABLE_KEY, gtfsTrip.getTripId() + "-" + count++, log);
            Timeband timeband = ObjectFactory.getTimeband(referential, timeBandObjectId);
            timeband.setName(getTimebandName(frequency));
            timeband.setStartTime(frequency.getStartTime().getTime());
            timeband.setEndTime(frequency.getEndTime().getTime());

            JourneyFrequency journeyFrequency = new JourneyFrequency();
            journeyFrequency.setExactTime(frequency.getExactTimes());
            journeyFrequency.setFirstDepartureTime(frequency.getStartTime().getTime());
            journeyFrequency.setLastDepartureTime(frequency.getEndTime().getTime());
            journeyFrequency.setScheduledHeadwayInterval(Duration.standardSeconds(frequency.getHeadwaySecs()));
            journeyFrequency.setTimeband(timeband);
            journeyFrequency.setVehicleJourney(vehicleJourney);

            List<VehicleJourneyAtStop> vjass = vehicleJourney.getVehicleJourneyAtStops();
            VehicleJourneyAtStop firstVjas = vjass.get(0);
            LocalTime firstArrivalTime = firstVjas.getArrivalTime();
            LocalTime firstDepartureTime = firstVjas.getDepartureTime();
            for (VehicleJourneyAtStop vjas : vjass) {
                LocalTime arrivalTime = new LocalTime(TimeUtil.subtract(vjas.getArrivalTime(), firstArrivalTime).getMillis());
                LocalTime departureTime = new LocalTime(TimeUtil.subtract(vjas.getDepartureTime(), firstDepartureTime).getMillis());
                vjas.setArrivalTime(arrivalTime);
                vjas.setDepartureTime(departureTime);
            }
        }
    }

    private String getTimebandName(GtfsFrequency frequency) {
        LocalTime start = frequency.getStartTime().getTime();
        LocalTime end = frequency.getEndTime().getTime();

        return (start.getHourOfDay() + ":" + start.getMinuteOfHour() + " - "
                + end.getHourOfDay() + ":" + end.getMinuteOfHour());
    }

    private JourneyPattern createJourneyPattern(Context context, Referential referential,
                                                GtfsImportParameters configuration, GtfsTrip gtfsTrip, Iterable<GtfsShape> gtfsShapes,
                                                VehicleJourney vehicleJourney, String journeyKey, Map<String, JourneyPattern> journeyPatternByStopSequence) throws NoSuchAlgorithmException {
        JourneyPattern journeyPattern;

        // Route
        Route route = getOrCreateRoute(referential, configuration, gtfsTrip);


        // JourneyPattern
        String journeyPatternId = generateJourneyPatternId(configuration,gtfsTrip,journeyKey,vehicleJourney.getVehicleJourneyAtStops());
        journeyPattern = ObjectFactory.getJourneyPattern(referential, journeyPatternId);
        journeyPattern.setName(gtfsTrip.getTripHeadSign());
        journeyPattern.setRoute(route);
        journeyPatternByStopSequence.put(journeyKey, journeyPattern);

        // StopPoints
        createStopPoint(route, journeyPattern, vehicleJourney.getVehicleJourneyAtStops(), referential, configuration);

        List<StopPoint> stopPoints = journeyPattern.getStopPoints();
        journeyPattern.setDepartureStopPoint(stopPoints.get(0));
        journeyPattern.setArrivalStopPoint(stopPoints.get(stopPoints.size() - 1));

        journeyPattern.setFilled(true);





        // Shape -> routeSections
        if (gtfsShapes != null) {
            List<RouteSection> sections = createRouteSections(context, referential, configuration, journeyPattern,
                    vehicleJourney, gtfsShapes);
            if (!sections.isEmpty()) {
                journeyPattern.setRouteSections(sections);
                journeyPattern.setSectionStatus(SectionStatusEnum.Completed);
            }
        }

        addSyntheticDestinationDisplayIfMissingOnFirstStopPoint(configuration, referential, journeyPattern);

        return journeyPattern;
    }

    private String generateJourneyPatternId( GtfsImportParameters configuration, GtfsTrip gtfsTrip , String journeyKey, List<VehicleJourneyAtStop> list) throws NoSuchAlgorithmException {

        String journeyPatternKey = gtfsTrip.getRouteId() + "_" + gtfsTrip.getDirectionId().ordinal();
        if (gtfsTrip.getShapeId() != null && !gtfsTrip.getShapeId().isEmpty())
            journeyPatternKey += "_" + gtfsTrip.getShapeId();

        journeyPatternKey += "_" + AbstractConverter.computeEndId(journeyKey);


        for (VehicleJourneyAtStop vehicleJourneyAtStop : list) {
            VehicleJourneyAtStopWrapper wrapper = (VehicleJourneyAtStopWrapper) vehicleJourneyAtStop;
            if(wrapper.stopSequence == 1 || wrapper.stopSequence == list.size()){
                String stopIdWithDropOffPickup = createJourneyKeyFragment(wrapper);
                journeyPatternKey += "_" + stopIdWithDropOffPickup;
            }
        }

        journeyPatternKey += "_" + list.size();
        return AbstractConverter.composeObjectId(configuration, JourneyPattern.JOURNEYPATTERN_KEY, journeyPatternKey, log);

    }

    private RoutePoint createRoutePointFromStopPoint(Referential referential, StopPoint firstStopPoint) {
        RoutePoint firstRoutePoint = ObjectFactory.getRoutePoint(referential, firstStopPoint.objectIdPrefix() + ":RoutePoint:" + firstStopPoint.objectIdSuffix());
        firstRoutePoint.setScheduledStopPoint(firstStopPoint.getScheduledStopPoint());
        firstRoutePoint.setFilled(true);
        return firstRoutePoint;
    }

    private void addSyntheticDestinationDisplayIfMissingOnFirstStopPoint(GtfsImportParameters configuration, Referential referential, JourneyPattern jp) {
        StopPoint departureStopPoint = jp.getDepartureStopPoint();
        if (departureStopPoint.getDestinationDisplay() == null) {
            // Create a forced DestinationDisplay
            // Use JourneyPattern->PublishedName

            String stopPointId = AbstractConverter.extractOriginalId(departureStopPoint.getObjectId());
            String journeyPatternId = AbstractConverter.extractOriginalId(jp.getObjectId());

            DestinationDisplay destinationDisplay = ObjectFactory.getDestinationDisplay(referential,
                    AbstractConverter.composeObjectId(configuration,
                            DestinationDisplay.DESTINATIONDISPLAY_KEY, journeyPatternId + "-" + stopPointId, null));

            if (jp.getArrivalStopPoint().getScheduledStopPoint().getContainedInStopAreaRef().getObject() != null) {
                String content = jp.getArrivalStopPoint().getScheduledStopPoint().getContainedInStopAreaRef().getObject().getName();

                if (content != null) {
                    destinationDisplay.setName("Generated: " + content);
                    destinationDisplay.setFrontText(content);
                    departureStopPoint.setDestinationDisplay(destinationDisplay);
                } else {
                    log.warn("Cannot create synthetic DestinationDisplay for StopPoint " + departureStopPoint + " as StopArea name is null");
                }
            } else {
                log.warn("Cannot create synthetic DestinationDisplay for StopPoint " + departureStopPoint + " as StopArea is null");
            }
        }

    }

    private static final double narrow = 0.0000001;

    private List<RouteSection> createRouteSections(Context context, Referential referential,
                                                   GtfsImportParameters configuration, JourneyPattern journeyPattern, VehicleJourney vehicleJourney,
                                                   Iterable<GtfsShape> gtfsShapes) {
        List<RouteSection> sections = new ArrayList<>();
        GeometryFactory factory = new GeometryFactory(new PrecisionModel(10), 4326);
        List<OrderedCoordinate> coordinates = new ArrayList<>();
        List<LineSegment> segments = new ArrayList<>();
        Coordinate previous = null;
        String shapeId = null;
        // Integer lineNumber = null;
        for (GtfsShape gtfsShape : gtfsShapes) {
            if (gtfsShape.getShapePtLon() == null || gtfsShape.getShapePtLat() == null) {
                log.error("line " + gtfsShape.getId() + " missing coordinates for shape " + gtfsShape.getShapeId());
                return sections;
            }
            if (shapeId == null) {
                shapeId = gtfsShape.getShapeId();
            }

            OrderedCoordinate current = new OrderedCoordinate(gtfsShape.getShapePtLon().doubleValue(), gtfsShape
                    .getShapePtLat().doubleValue(), gtfsShape.getShapePtSequence());
            if (previous != null) {
                // remove duplicate coords
                if (Math.abs(current.x - previous.x) < narrow && Math.abs(current.y - previous.y) < narrow) {
                    continue;
                }
                coordinates.add(current);
            } else {
                coordinates.add(current);
            }
            previous = current;
        }
        if (coordinates.size() < 2) {
            log.warn("no segments found");
            return sections;
        }

        previous = null;
        Collections.sort(coordinates, COORDINATE_SORTER);
        for (OrderedCoordinate current : coordinates) {
            if (previous != null) {
                LineSegment segment = new LineSegment(previous, current);
                segments.add(segment);
            }
            previous = current;
        }

        int segmentRank = 0;
        previous = null;
        String prefix = journeyPattern.objectIdPrefix();
        StopArea previousLocation = null;
        ScheduledStopPoint previousScheduledStopPoint = null;

        // sequence index to read the complete shape and divide it into sections
        int currentSequence = coordinates.get(0).order;

        journeyPattern.getStopPoints().sort(STOP_POINT_POSITION_COMPARATOR);

        for (StopPoint stop : journeyPattern.getStopPoints()) {

            if (previousLocation == null){
                previousLocation = stop.getScheduledStopPoint().getContainedInStopAreaRef().getObject();
                previousScheduledStopPoint = stop.getScheduledStopPoint();
                continue;
            }

            StopArea arrivalStopArea = stop.getScheduledStopPoint().getContainedInStopAreaRef().getObject();


        List<OrderedCoordinate> sectionPoints = createSectionPoints(coordinates, currentSequence, arrivalStopArea);

        sectionPoints.sort(COORDINATE_SORTER);

        //last sequence of the section will became currentSequence number
        currentSequence = sectionPoints.get(sectionPoints.size() - 1).order;


            List<Coordinate> coords = new ArrayList<>();
            sectionPoints.stream().forEach(coords::add);





        String routeSectionId = prefix + ":" + RouteSection.ROUTE_SECTION_KEY + ":" + journeyPattern.objectIdSuffix() + "_" + shapeId + "_"
                    + previousLocation.objectIdSuffix() + "_" + arrivalStopArea.objectIdSuffix();
        RouteSection section = ObjectFactory.getRouteSection(referential, routeSectionId);
            if (!section.isFilled()) {

                Coordinate[] inputCoords = new Coordinate[2];
                section.setFromScheduledStopPoint(previousScheduledStopPoint);
                inputCoords[0] = new Coordinate(previousLocation.getLongitude().doubleValue(), previousLocation.getLatitude().doubleValue());
                section.setToScheduledStopPoint(stop.getScheduledStopPoint());
                inputCoords[1] = new Coordinate(arrivalStopArea.getLongitude().doubleValue(), arrivalStopArea.getLatitude().doubleValue());
                section.setProcessedGeometry(factory.createLineString(coords.toArray(new Coordinate[coords.size()])));
                section.setInputGeometry(factory.createLineString(inputCoords));
                section.setNoProcessing(false);
                section.setFilled(true);
                sections.add(section);
            }

            previousLocation = stop.getScheduledStopPoint().getContainedInStopAreaRef().getObject();
            previousScheduledStopPoint = stop.getScheduledStopPoint();

        }

        return sections;
    }


    /**
     * Create the list of coordinates that will be inserted in the route section processed geometry
     * @param coordinates
     *      Complete list of coordinates, coming from shapes.txt
     * @param currentSequence
     *      sequence on which which the section starts
     * @param arrivalStopArea
     *      arrival stop area of the section
     * @return
     *      The list of coordinates
     */
    private List<OrderedCoordinate> createSectionPoints(List<OrderedCoordinate> coordinates, int currentSequence, StopArea arrivalStopArea) {

        List<OrderedCoordinate> resultList = new ArrayList<>();

        //sequence number of the arrival point
        int arrilvalStopSequence = getEndSequenceForSection(coordinates, currentSequence, arrivalStopArea);

        coordinates.stream()
                   .filter(coord -> coord.order >= currentSequence && coord.order <= arrilvalStopSequence)
                   .forEach(resultList::add);


        return resultList;
    }


    /**
     * Calculates the sequence of the end of the section
     * @param coordinates
     *      Complete list of coordinates, coming from shapes.txt
     * @param sectionStartSequence
     *      sequence on which which the section starts
     * @param arrivalStopArea
     *      arrival stop area of the section
     * @return
     *      the sequence on which the section ends
     */
    private int getEndSequenceForSection(List<OrderedCoordinate> coordinates, int sectionStartSequence, StopArea arrivalStopArea){



        Coordinate sourceCoordinate = new Coordinate(arrivalStopArea.getLongitude().doubleValue(), arrivalStopArea.getLatitude().doubleValue());


        // List of all points remaining in hte pattern, with their distance from the arrivalStopArea
        List<Pair<Double,OrderedCoordinate>> pointsByDistance = new ArrayList<>();


        coordinates.stream()
                   .filter(coord -> coord.order > sectionStartSequence)
                   .forEach(coord -> {
                       double distance = sourceCoordinate.distance(coord);
                       pointsByDistance.add(Pair.of(distance, coord));
                   });



        pointsByDistance.sort(new Comparator<Pair<Double,OrderedCoordinate>>() {
            @Override
            public int compare(Pair<Double,OrderedCoordinate> o1, Pair<Double,OrderedCoordinate> o2) {
                return o1.getLeft().compareTo(o2.getLeft());
            }
        });


        Double minDistance = pointsByDistance.get(0).getLeft();

        //detect if there are 2 points near stop area

        List<Pair<Double, OrderedCoordinate>> minPairs = pointsByDistance.stream()
                                                                    .filter(pair -> pair.getLeft().equals(minDistance))
                                                                    .collect(Collectors.toList());



        //taking the first point (to avoid issues when there is a loop in the pattern
        return minPairs.get(0).getRight().order;
    }


    /**
     * create route for trip
     *
     * @param referential
     * @param configuration
     * @param gtfsTrip
     * @return
     */
    private Route getOrCreateRoute(Referential referential, GtfsImportParameters configuration, GtfsTrip gtfsTrip) throws NoSuchAlgorithmException {
        String lineId = AbstractConverter.composeObjectId(configuration, Line.LINE_KEY, gtfsTrip.getRouteId(), log);
        Line line = ObjectFactory.getLine(referential, lineId);


        String routeKey = gtfsTrip.getRouteId() + "_" + gtfsTrip.getDirectionId().ordinal();


        String routeId = AbstractConverter.composeObjectId(configuration, Route.ROUTE_KEY,      routeKey, log);


        Route route = ObjectFactory.getRoute(referential, routeId);
        route.setLine(line);
        PTDirectionEnum wayBack = gtfsTrip.getDirectionId().equals(DirectionType.Outbound) ? PTDirectionEnum.A : PTDirectionEnum.R;
        route.setWayBack(wayBack.toString());
        route.setDirection(wayBack);
        return route;
    }

    protected void convert(Context context, GtfsStopTime gtfsStopTime, GtfsTrip gtfsTrip, VehicleJourneyAtStop vehicleJourneyAtStop) {

        Referential referential = (Referential) context.get(REFERENTIAL);
        GtfsImportParameters configuration = (GtfsImportParameters) context.get(CONFIGURATION);

        String vjasObjectId = AbstractConverter.composeObjectId(configuration,
                ObjectIdTypes.VEHICLE_JOURNEY_AT_STOP_KEY, UUID.randomUUID().toString(), log);

        vehicleJourneyAtStop.setObjectId(vjasObjectId);

        String objectId = gtfsStopTime.getStopId();
        StopPoint stopPoint = ObjectFactory.getStopPoint(referential, objectId);
        vehicleJourneyAtStop.setStopPoint(stopPoint);
        vehicleJourneyAtStop.setArrivalTime(gtfsStopTime.getArrivalTime().getTime());
        vehicleJourneyAtStop.setDepartureTime(gtfsStopTime.getDepartureTime().getTime());

        /**
         * GJT : Setting arrival and departure offset to vehicleJourneyAtStop
         * object
         */
        vehicleJourneyAtStop.setArrivalDayOffset(gtfsStopTime.getArrivalTime().getDay());
        vehicleJourneyAtStop.setDepartureDayOffset(gtfsStopTime.getDepartureTime().getDay());

        if (gtfsStopTime.getStopHeadsign() != null) {
            DestinationDisplay destinationDisplay = ObjectFactory.getDestinationDisplay(referential, gtfsStopTime.getTripId() + "-" + gtfsStopTime.getStopSequence() + "-" + gtfsStopTime.getStopId());
            destinationDisplay.setFrontText(gtfsStopTime.getStopHeadsign());
            stopPoint.setDestinationDisplay(destinationDisplay);
        }
    }

    private BoardingPossibilityEnum toBoardingPossibility(PickupType type) {
        if (type == null) {
            return BoardingPossibilityEnum.normal;
        }

        switch (type) {
            case Scheduled:
                return BoardingPossibilityEnum.normal;
            case NoAvailable:
                return BoardingPossibilityEnum.forbidden;
            case AgencyCall:
                return BoardingPossibilityEnum.is_flexible;
            case DriverCall:
                return BoardingPossibilityEnum.request_stop;
        }
        return null;
    }

    private AlightingPossibilityEnum toAlightingPossibility(DropOffType type) {
        if (type == null) {
            return AlightingPossibilityEnum.normal;
        }

        switch (type) {
            case Scheduled:
                return AlightingPossibilityEnum.normal;
            case NoAvailable:
                return AlightingPossibilityEnum.forbidden;
            case AgencyCall:
                return AlightingPossibilityEnum.is_flexible;
            case DriverCall:
                return AlightingPossibilityEnum.request_stop;
        }
        return null;
    }

    protected void convert(Context context, GtfsTrip gtfsTrip, VehicleJourney vehicleJourney) {

        if (gtfsTrip.getTripShortName() != null) {
            vehicleJourney.setPrivateCode(gtfsTrip.getTripShortName());
        }

        if (StringUtils.trimToNull(gtfsTrip.getTripHeadSign()) != null) {
            vehicleJourney.setPublishedJourneyName(gtfsTrip.getTripHeadSign());
        }

        if (gtfsTrip.getWheelchairAccessible() != null) {
            switch (gtfsTrip.getWheelchairAccessible()) {
                case NoInformation:
                    vehicleJourney.setMobilityRestrictedSuitability(null);
                    break;
                case NoAllowed:
                    vehicleJourney.setMobilityRestrictedSuitability(Boolean.FALSE);
                    break;
                case Allowed:
                    vehicleJourney.setMobilityRestrictedSuitability(Boolean.TRUE);
                    break;
            }
        }

        if (gtfsTrip.getBikesAllowed() != null) {
            switch (gtfsTrip.getBikesAllowed()) {
                case NoInformation:
                    vehicleJourney.setBikesAllowed(null);
                    break;
                case NoAllowed:
                    vehicleJourney.setBikesAllowed(Boolean.FALSE);
                    break;
                case Allowed:
                    vehicleJourney.setBikesAllowed(Boolean.TRUE);
                    break;
            }
        }

        vehicleJourney.setFilled(true);

    }

    /**
     * create stopPoints for Route
     * @param route
     * @param journeyPattern
     * @param list
     * @param referential
     * @param configuration
     */
    private void createStopPoint(Route route, JourneyPattern journeyPattern, List<VehicleJourneyAtStop> list,
                                 Referential referential, GtfsImportParameters configuration) {
        Set<String> stopPointKeys = new HashSet<String>();

        int position = 0;
        Map<String, Integer> occurenceMap = new HashMap<>();



        int currentOccurence = 0;
        alredyProcessedStopPoints.clear();
        previousProcessedStopPoint = null;

        for (VehicleJourneyAtStop vehicleJourneyAtStop : list) {

            VehicleJourneyAtStopWrapper wrapper = (VehicleJourneyAtStopWrapper) vehicleJourneyAtStop;
            String stopIdKeyFragment = createJourneyKeyFragment(wrapper);
            String baseKey = journeyPattern.getObjectId().replace( JourneyPattern.JOURNEYPATTERN_KEY, StopPoint.STOPPOINT_KEY) + "a"
                    + stopIdKeyFragment.trim();//.replaceAll("[^a-zA-Z_0-9\\-]", "_");
            String stopKey = baseKey;
            int dup = 1;
            while (stopPointKeys.contains(stopKey)) {
                stopKey = baseKey + "_" + (dup++);
            }
            stopPointKeys.add(stopKey);


            String stopAreaId = AbstractConverter.toStopAreaId(configuration,"Quay", wrapper.stopId);
            StopArea stopArea = ObjectFactory.getStopArea(referential, stopAreaId);

            if (!occurenceMap.containsKey(stopAreaId)){
                currentOccurence = 1;
                occurenceMap.put(stopAreaId, currentOccurence);
            }else{
                currentOccurence = occurenceMap.get(stopAreaId) + 1;
                occurenceMap.put(stopAreaId, currentOccurence);
            }

            StopPoint stopPoint = getStopPointFromRoute(referential, route , stopAreaId, currentOccurence, stopKey);


            String scheduledStopPointKey = stopPoint.getObjectId().replace(StopPoint.STOPPOINT_KEY, ObjectIdTypes.SCHEDULED_STOP_POINT_KEY);
            ScheduledStopPoint scheduledStopPoint = ObjectFactory.getScheduledStopPoint(referential, scheduledStopPointKey);
            stopPoint.setScheduledStopPoint(scheduledStopPoint);


            scheduledStopPoint.setContainedInStopAreaRef(new SimpleObjectReference(stopArea));

//            stopPoint.setForBoarding(toBoardingPossibility(wrapper.pickup));
//            stopPoint.setForAlighting(toAlightingPossibility(wrapper.dropOff));

            if (wrapper.stopHeadsign != null) {
                DestinationDisplay destinationDisplay = ObjectFactory.getDestinationDisplay(referential,
                        AbstractConverter.composeObjectId(configuration,
                                DestinationDisplay.DESTINATIONDISPLAY_KEY, stopKey, null));
                destinationDisplay.setFrontText(wrapper.stopHeadsign);
                destinationDisplay.setName(wrapper.stopHeadsign);

                stopPoint.setDestinationDisplay(destinationDisplay);
            }


            journeyPattern.addStopPoint(stopPoint);
            stopPoint.setFilled(true);
        }
        NeptuneUtil.refreshDepartureArrivals(journeyPattern);
    }

    private StopPoint getStopPointFromRoute(Referential referential, Route route,  String stopAreaId, int  occurenceNb, String stopKey){
        int currentOccurence = 1;

        //re-order route stop points before processing
        route.getStopPoints().sort(STOP_POINT_POSITION_COMPARATOR);


        for (StopPoint stopPoint : route.getStopPoints()) {
            if (stopPoint.getScheduledStopPoint().getContainedInStopAreaRef().getObjectId().equals(stopAreaId)){
                // a stop point with the correct StopAreaId has been found in route

                if (currentOccurence == occurenceNb){
                    String previousStopPoint = stopAreaId + "_" + occurenceNb;
                    alredyProcessedStopPoints.put(previousStopPoint, stopPoint);
                    previousProcessedStopPoint = previousStopPoint;
                    //This is the correct occurence, we can get this stopPoint
                    return stopPoint;
                }else{
                    //There is a loop in the journey pattern and this is not the correct occurence.
                    //we need to found the next stopPoint having the correct stopAreaId
                    currentOccurence++;
                }
            }
        }

        //StopPoint has not be found in the route. we need to create a new one
        StopPoint stopPoint = ObjectFactory.getStopPoint(referential, stopKey);

        int targetedPosition = calculateTargetPosition(stopAreaId, occurenceNb);
        shiftSuccessors(route, targetedPosition);
        stopPoint.setPosition(targetedPosition);
        stopPoint.setRoute(route);

        String previousStopPoint = stopAreaId + "_" + occurenceNb;
        previousProcessedStopPoint = previousStopPoint;
        alredyProcessedStopPoints.put(previousStopPoint, stopPoint);
        return stopPoint;

    }

    private void shiftSuccessors(Route route, int newPointPosition){
        route.getStopPoints().stream()
                            .filter(stopPoint -> stopPoint.getPosition() >= newPointPosition)
                            .forEach(stopPoint -> stopPoint.setPosition(stopPoint.getPosition() + 1));

    }

    private int calculateTargetPosition(String stopAreaId, int occurenceNb) {

        if (StringUtils.isEmpty(previousProcessedStopPoint)){
            //there is no previous point. Current point is the first of the pattern. We place it at the beginning
            return 0;
        }else{
            //there is a predecessor. Current point is placed after
            return alredyProcessedStopPoints.get(previousProcessedStopPoint).getPosition() + 1;
        }


    }

    @AllArgsConstructor
    class VehicleJourneyAtStopWrapper extends VehicleJourneyAtStop {

        private static final long serialVersionUID = 5052093726657799027L;
        String stopId;
        int stopSequence;
        Float shapeDistTraveled;
        @Getter
        DropOffType dropOff;
        @Getter
        PickupType pickup;
        String stopHeadsign;
    }

    public static final Comparator<VehicleJourneyAtStop> VEHICLE_JOURNEY_AT_STOP_COMPARATOR = new Comparator<VehicleJourneyAtStop>() {
        @Override
        public int compare(VehicleJourneyAtStop right, VehicleJourneyAtStop left) {
            int rightIndex = ((VehicleJourneyAtStopWrapper) right).stopSequence;
            int leftIndex = ((VehicleJourneyAtStopWrapper) left).stopSequence;
            return rightIndex - leftIndex;
        }
    };


    public static final Comparator<StopPoint> STOP_POINT_POSITION_COMPARATOR = new Comparator<StopPoint>() {
        @Override
        public int compare(StopPoint sp1, StopPoint sp2) {
            return Integer.compare(sp1.getPosition(), sp2.getPosition());
        }
    };




    class OrderedCoordinate extends Coordinate {
        private static final long serialVersionUID = 1L;
        public int order;

        public OrderedCoordinate(double x, double y, Integer order) {
            this.x = x;
            this.y = y;
            this.order = order.intValue();
        }
    }

    ;

    static class OrderedCoordinateComparator implements Comparator<OrderedCoordinate> {
        @Override
        public int compare(OrderedCoordinate o1, OrderedCoordinate o2) {

            return o1.order - o2.order;
        }
    }

    static {
        ParserFactory.register(GtfsTripParser.class.getName(), new ParserFactory() {
            @Override
            protected Parser create() {
                return new GtfsTripParser();
            }
        });
    }

    /**
     * Compute sub mode for French Rail.
     * Based on SNCF organizatio train codification.
     *
     * @param tripHeadSign
     * @return sub mode upon train codification in case of a rail typed route.
     */
    TransportSubModeNameEnum getSubModeFromTripHeadSign(String tripHeadSign) {
        if (tripHeadSign != null && tripHeadSign.length() > 3) {
            if (tripHeadSign.matches("8\\d{5}")) { // TER
                return TransportSubModeNameEnum.RegionalRail;
            } else if (Stream.of("1\\d{5}", "[34]\\d{3}").anyMatch(tripHeadSign::matches)) { // IC
                return TransportSubModeNameEnum.InterregionalRail;
            } else if (Stream.of("[857]\\d{3}", "[857]\\d{3}/\\d{2}", "[857]\\d{3}/\\d{4}").anyMatch(tripHeadSign::matches)) {
                return TransportSubModeNameEnum.LongDistance;
            }
        }
        return null;
    }
}
