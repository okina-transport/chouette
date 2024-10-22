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
import mobi.chouette.common.ObjectIdUtil;
import mobi.chouette.common.TimeUtil;
import mobi.chouette.exchange.gtfs.importer.GtfsImportParameters;
import mobi.chouette.exchange.gtfs.model.*;
import mobi.chouette.exchange.gtfs.model.GtfsStop.LocationType;
import mobi.chouette.exchange.gtfs.model.GtfsTransfer.TransferType;
import mobi.chouette.exchange.gtfs.model.GtfsTrip.DirectionType;
import mobi.chouette.exchange.gtfs.model.importer.*;
import mobi.chouette.exchange.gtfs.validation.Constant;
import mobi.chouette.exchange.gtfs.validation.GtfsValidationReporter;
import mobi.chouette.exchange.importer.Parser;
import mobi.chouette.exchange.importer.ParserFactory;
import mobi.chouette.exchange.importer.Validator;
import mobi.chouette.exchange.report.AnalyzeReport;
import mobi.chouette.model.*;
import mobi.chouette.model.type.*;
import mobi.chouette.model.util.NeptuneUtil;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.ObjectIdTypes;
import mobi.chouette.model.util.Referential;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.Duration;
import org.joda.time.LocalTime;

import javax.xml.bind.DatatypeConverter;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Log4j
public class GtfsTripParser implements Parser, Validator, Constant {

    private static final Comparator<OrderedCoordinate> COORDINATE_SORTER = new OrderedCoordinateComparator();

    @Getter
    @Setter
    private String gtfsRouteId;

    @Getter
    @Setter
    private Integer position;

    @Override
    public void validate(Context context) throws Exception {
        GtfsValidationReporter gtfsValidationReporter = (GtfsValidationReporter) context.get(GTFS_REPORTER);
        GtfsImportParameters configuration = (GtfsImportParameters) context.get(CONFIGURATION);
        gtfsValidationReporter.getExceptions().clear();

        validateStopTimes(context);
        if(configuration.isImportShapesFile()){
            validateShapes(context);
        }
        validateTrips(context);
        validateFrequencies(context);
    }

    private void validateStopTimes(Context context) throws Exception {

        GtfsImporter importer = (GtfsImporter) context.get(PARSER);
        GtfsValidationReporter gtfsValidationReporter = (GtfsValidationReporter) context.get(GTFS_REPORTER);
        Set<String> stopIds = new HashSet<>();

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

            if (CollectionUtils.isNotEmpty(stopTimeParser.getErrors())) {
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

            Iterable<String> tripIds = stopTimeParser.keys();

            Map<Integer, Integer> stopSequences = new HashMap<>();
            boolean enoughStopTimes = true;
            boolean duplicateConsecutiveStops = false;
            GtfsImportParameters params = (GtfsImportParameters) context.get(CONFIGURATION);
            for (String tripId : tripIds) {
                stopSequences.clear();
                Iterable<GtfsStopTime> stopTimes = stopTimeParser.values(tripId);


                if (StreamSupport.stream(stopTimes.spliterator(), false).count() < 2) {
                    enoughStopTimes = false;
                    gtfsValidationReporter.reportError(
                            context,
                            new GtfsException(stopTimeParser.getPath(), stopTimeParser.getValue(tripId).getId(),
                                    stopTimeParser.getIndex(StopTimeByTrip.FIELDS.stop_sequence.name()),
                                    StopTimeByTrip.FIELDS.trip_id.name() + "," + StopTimeByTrip.FIELDS.stop_sequence.name(),
                                    GtfsException.ERROR.NOT_ENOUGH_ROUTE_POINTS, null, tripId), GTFS_STOP_TIMES_FILE);
                }

                List<GtfsStopTime> tripIdStopTimes = new ArrayList<>();

                for (GtfsStopTime bean : stopTimes) {
                    Integer stopSequence = bean.getStopSequence();
                    if (params.isRoutesReorganization()) {
                        GtfsStopTime tmp = new GtfsStopTime(bean.getTripId(), null, null, bean.getStopId(), stopSequence, null, null
                                , null, null, null);
                        tmp.setId(bean.getId());
                        tripIdStopTimes.add(tmp);
                    }
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
                AnalyzeReport analyzeReport = (AnalyzeReport) context.get(ANALYSIS_REPORT);

                // check that there is not 2 identical consecutive stops when route reorganization is on
                if (params.isRoutesReorganization()) {
                    tripIdStopTimes = tripIdStopTimes.stream().sorted(Comparator.comparingInt(GtfsStopTime::getStopSequence)).collect(Collectors.toList());
                    String prevStopId = "";
                    for (GtfsStopTime stopTime : tripIdStopTimes) {
                        if (prevStopId.equals(stopTime.getStopId())) {
                            String stopTimePrimaryKey = stopTime.getTripId() + "," + stopTime.getStopSequence();
                            analyzeReport.getDuplicateConsecutiveStopTimes().add(stopTimePrimaryKey);
                            // identical consecutive stops break route reorganization algorithm therefore we reject it
                            // (this is a GTFS error to have 0.0 distance between 2 consecutive stops anyway)
                            gtfsValidationReporter.reportError(
                                    context,
                                    new GtfsException(
                                            stopTimeParser.getPath(),
                                            stopTime.getId(),
                                            stopTimeParser.getIndex(StopTimeByTrip.FIELDS.stop_id.name()),
                                            StopTimeByTrip.FIELDS.trip_id.name() + ", " + StopTimeByTrip.FIELDS.stop_sequence.name(),
                                            GtfsException.ERROR.DUPLICATE_CONSECUTIVE_STOP_TIME,
                                            null,
                                            stopTimePrimaryKey), GTFS_STOP_TIMES_FILE);
                            duplicateConsecutiveStops = true;
                        }
                        prevStopId = stopTime.getStopId();
                    }
                }

            }

            findTripIdsWithSameTimes(stopTimeParser, context);

            if (enoughStopTimes) {
                gtfsValidationReporter.validate(context, GTFS_STOP_TIMES_FILE, GtfsException.ERROR.NOT_ENOUGH_ROUTE_POINTS);
            }

            if (!duplicateConsecutiveStops) {
                gtfsValidationReporter.validate(context, GTFS_STOP_TIMES_FILE, GtfsException.ERROR.DUPLICATE_CONSECUTIVE_STOP_TIME);
            }

            int i = 1;
            boolean unusedId = true;
            for (GtfsStop bean : importer.getStopById()) {
                if (LocationType.Stop.equals(bean.getLocationType())) {
                    if (stopIds.add(bean.getStopId())) {
                        unusedId = false;
                        gtfsValidationReporter.reportError(context, new GtfsException(GTFS_STOPS_FILE, i,
                                        StopById.FIELDS.stop_id.name(), GtfsException.ERROR.UNUSED_ID, null, bean.getStopId()),
                                GTFS_STOPS_FILE);
                    }
                }
                i++;
            }

            if (unusedId) {
                gtfsValidationReporter.validate(context, GTFS_STOPS_FILE, GtfsException.ERROR.UNUSED_ID);
            }

            for (GtfsException ex: gtfsValidationReporter.getExceptions()) {
                if (ex.isFatal()) {
                    fatalException = ex;
                }
            }

            gtfsValidationReporter.getExceptions().clear();

            if (fatalException != null)
                throw fatalException;

        } else {
            gtfsValidationReporter.reportError(context, new GtfsException(GTFS_STOP_TIMES_FILE, 1, null,
                    GtfsException.ERROR.MISSING_FILE, null, null), GTFS_STOP_TIMES_FILE);
        }
    }

    /**
     * Identifies trip_ids that share the same stop times and adds them to the context.
     * Each trip's stop times are mapped and compared, and groups of trips with matching times are identified.
     * Only trip_ids with identical times are added to the resulting list.
     *
     * @param stopTimeParser  an index used to retrieve stop times associated with trip_ids
     * @param context         the context where the list of trip_ids with the same times is stored
     */
    private void findTripIdsWithSameTimes(Index<GtfsStopTime> stopTimeParser, Context context) {
        Map<String, String> tripTimeMap = new HashMap<>();

        // Iterate on each trip_id in stop_time and build the tripTimeMap
        for (String tripId : stopTimeParser.keys()) {
            for (GtfsStopTime stopTime : stopTimeParser.values(tripId)) {
                String departure = stopTime.getDepartureTime() != null ? stopTime.getDepartureTime().getTime().toString() : "null";
                String arrival = stopTime.getArrivalTime() != null ? stopTime.getArrivalTime().getTime().toString() : "null";
                String departureArrival = departure + "+" + arrival;
                tripTimeMap.merge(tripId, departureArrival, (existing, newValue) -> existing + "+" + newValue);
            }
        }

        Map<String, String> timeTripIdMap = new HashMap<>();
        tripTimeMap.forEach((tripId, times) ->
                timeTripIdMap.merge(times, tripId, (existingIds, newId) -> existingIds + "+" + newId)
        );

        List<String> tripIdsWithSameTimesList = new ArrayList<>();
        timeTripIdMap.forEach((times, ids) -> {
            if (ids.contains("+")) {
                tripIdsWithSameTimesList.add(ids);
            }
        });

        context.putIfAbsent("tripIdsWithSameTimesList", tripIdsWithSameTimesList);
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

            if (CollectionUtils.isNotEmpty(shapeParser.getErrors())) {
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
        GtfsImportParameters configuration = (GtfsImportParameters) context.get(CONFIGURATION);
        Set<String> routeIds = new HashSet<>();

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

            if (CollectionUtils.isNotEmpty(tripParser.getErrors())) {
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
                if(!configuration.isImportShapesFile()){
                    bean.setShapeId(null);
                }
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

            filterTripsWithSameServiceIdAndPutToContext(context, tripParser);

            tripParser.setWithValidation(false);
            int i = 1;
            boolean unsuedId = true;
            for (GtfsRoute bean : importer.getRouteById()) {
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

    /**
     * Filters trips with identical stop times and adds those sharing the same service_id to the context.
     * If a group contains 3 or more trips, only those with the most common service_id are kept.
     * If a group contains fewer than 3 trips, all must have the same service_id to be added.
     *
     * @param context     the context containing data and used to store results
     * @param tripParser  an index used to retrieve GtfsTrip objects associated with trip_ids
     */
    private void filterTripsWithSameServiceIdAndPutToContext(Context context, Index<GtfsTrip> tripParser) {
        List<String> duplicateTripStructureInStopTimes = new ArrayList<>();
        List<String> tripIdsWithSameTimesList = (List<String>) context.get("tripIdsWithSameTimesList");

        for (String tripIds : tripIdsWithSameTimesList) {
            String[] splitTripIds = tripIds.split("\\+");
            Map<String, List<String>> serviceIdToTripIds = new HashMap<>();

            // Browse trip_ids and organize them by service_id
            for (String tripId : splitTripIds) {
                GtfsTrip gtfsTrip = tripParser.getValue(tripId);

                if (gtfsTrip != null) {
                    String serviceId = gtfsTrip.getServiceId();
                    serviceIdToTripIds.computeIfAbsent(serviceId, k -> new ArrayList<>()).add(tripId);
                }
            }

            if (splitTripIds.length >= 3) {
                // Find the group of trip_ids with the largest number of the same service_id
                List<String> largestGroup = serviceIdToTripIds.values().stream()
                        .max(Comparator.comparingInt(List::size))
                        .orElse(new ArrayList<>());

                // Add the largest group if it contains at least 2 elements
                if (largestGroup.size() >= 2) {
                    duplicateTripStructureInStopTimes.add(String.join("+", largestGroup));
                }
            } else {
                boolean allSameServiceId = serviceIdToTripIds.size() == 1;

                if (allSameServiceId) {
                    duplicateTripStructureInStopTimes.add(tripIds);
                }
            }
        }

        if (!duplicateTripStructureInStopTimes.isEmpty()) {
            context.put(DUPLICATE_TRIP_STRUCTURE_IN_STOP_TIMES, duplicateTripStructureInStopTimes);
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
                // "frequencies.txt"
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

            if (CollectionUtils.isNotEmpty(frequencyParser.getErrors())) {
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
        String quayIdPrefixToRemove = configuration.getQuayIdPrefixToRemove();

        Map<String, JourneyPattern> journeyPatternByStopSequence = new HashMap<>();

        Map<GtfsTrip.WheelchairAccessibleType, List<String>> accessibilityTripMap;
        if(context.get(GTFS_ACCESSIBILITY_MAP) == null){
            accessibilityTripMap = new HashMap<>();
            accessibilityTripMap.put(GtfsTrip.WheelchairAccessibleType.Allowed, new ArrayList<>());
            accessibilityTripMap.put(GtfsTrip.WheelchairAccessibleType.NoAllowed, new ArrayList<>());
        }
        else{
            accessibilityTripMap = (Map<GtfsTrip.WheelchairAccessibleType, List<String>>) context.get(GTFS_ACCESSIBILITY_MAP);
        }


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

            String objectId = ObjectIdUtil.composeObjectId(configuration.isSplitIdOnDot(), configuration.getObjectIdPrefix(),
                    VehicleJourney.VEHICLEJOURNEY_KEY, gtfsTrip.getTripId());
            VehicleJourney vehicleJourney = ObjectFactory.getVehicleJourney(referential, objectId);

            if (gtfsTrip.getWheelchairAccessible() != null && !GtfsTrip.WheelchairAccessibleType.NoInformation.equals(gtfsTrip.getWheelchairAccessible())){
                accessibilityTripMap.get(gtfsTrip.getWheelchairAccessible()).add(vehicleJourney.getObjectId());
            }

            if (gtfsTrip.getTripShortName() != null) {
                vehicleJourney.setPrivateCode(gtfsTrip.getTripShortName());
            }

            if (StringUtils.trimToNull(gtfsTrip.getTripHeadSign()) != null) {
                vehicleJourney.setPublishedJourneyName(gtfsTrip.getTripHeadSign());
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
            vehicleJourney.setPublishedJourneyIdentifier(gtfsTrip.getTripShortName());

            // VehicleJourneyAtStop
            boolean afterMidnight = true;

            for (GtfsStopTime gtfsStopTime : importer.getStopTimeByTrip().values(gtfsTrip.getTripId())) {

                String stopId = StringUtils.isNotEmpty(quayIdPrefixToRemove) ?  gtfsStopTime.getStopId().replaceFirst("^"+ quayIdPrefixToRemove,"").trim() : gtfsStopTime.getStopId();

                VehicleJourneyAtStopWrapper vehicleJourneyAtStop = new VehicleJourneyAtStopWrapper(
                        stopId, gtfsStopTime.getStopSequence(), gtfsStopTime.getShapeDistTraveled(), gtfsStopTime.getDropOffType(), gtfsStopTime.getPickupType(), gtfsStopTime.getStopHeadsign());

                // TAD probalement à faire évoluer un jour pour les interruptions
                // 2X

                vehicleJourneyAtStop.setBoardingAlightingPossibility(BoardingAlightingPossibilityEnum.fromDropOffAndPickUp(gtfsStopTime.getDropOffType(),gtfsStopTime.getPickupType()));

                convert(context, gtfsStopTime, vehicleJourneyAtStop);

                if (afterMidnight) {
                    if (!gtfsStopTime.getArrivalTime().moreOneDay())
                        afterMidnight = false;
                    if (!gtfsStopTime.getDepartureTime().moreOneDay())
                        afterMidnight = false;
                }

                vehicleJourneyAtStop.setVehicleJourney(vehicleJourney);
            }

            vehicleJourney.getVehicleJourneyAtStops().sort(VEHICLE_JOURNEY_AT_STOP_COMPARATOR);

            // Timetable
            String timetableId = ObjectIdUtil.composeObjectId(configuration.isSplitIdOnDot(), configuration.getObjectIdPrefix(),
                    Timetable.TIMETABLE_KEY, gtfsTrip.getServiceId());

            Timetable timetable = ObjectFactory.getTimetable(referential, timetableId);
            vehicleJourney.getTimetables().add(timetable);

            // JourneyPattern
            StringBuilder objectIdKey = createObjectIdKeyUsedForJourneyPatternAndRoute(gtfsTrip, vehicleJourney);

            JourneyPattern journeyPattern = journeyPatternByStopSequence.get(objectIdKey.toString());
            if (journeyPattern == null) {
                journeyPattern = createJourneyPattern(referential, configuration, gtfsTrip, importer,
                        vehicleJourney, objectIdKey.toString(), journeyPatternByStopSequence, position);
            }

            if(StringUtils.isBlank(vehicleJourney.getPublishedJourneyName())){
                vehicleJourney.setPublishedJourneyName(journeyPattern.getArrivalStopPoint().getScheduledStopPoint().getContainedInStopAreaRef().getObject().getName());
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
                createJourneyFrequencies(referential, importer, configuration, gtfsTrip, vehicleJourney);
            }


            if (configuration.isParseInterchanges() && importer.hasTransferImporter()) {
                createInterchanges(referential, importer, configuration, gtfsTrip, vehicleJourney);
            }

        }

        context.put(GTFS_ACCESSIBILITY_MAP, accessibilityTripMap);

        // dispose collections
        journeyPatternByStopSequence.clear();
    }

    /**
     * Creation of object id (route and journey pattern)
     */
    private StringBuilder createObjectIdKeyUsedForJourneyPatternAndRoute(GtfsTrip gtfsTrip, VehicleJourney vehicleJourney) throws NoSuchAlgorithmException {
        StringBuilder objectIdKey = new StringBuilder();

        for (VehicleJourneyAtStop vehicleJourneyAtStop : vehicleJourney.getVehicleJourneyAtStops()) {
            String stopIdWithDropOffPickup = createJourneyKeyFragment((VehicleJourneyAtStopWrapper) vehicleJourneyAtStop);
            objectIdKey.append(stopIdWithDropOffPickup);
        }

        objectIdKey = new StringBuilder(gtfsTrip.getRouteId() + "_" + computeEndId(objectIdKey.toString()));
        return objectIdKey;
    }

    private String createJourneyKeyFragment(VehicleJourneyAtStopWrapper vehicleJourneyAtStop) {
        DropOffTypeEnum drop = (vehicleJourneyAtStop.dropOff == null ? DropOffTypeEnum.Scheduled : vehicleJourneyAtStop.dropOff);
        PickUpTypeEnum pickup = (vehicleJourneyAtStop.pickup == null ? PickUpTypeEnum.Scheduled : vehicleJourneyAtStop.pickup);

        return vehicleJourneyAtStop.stopId.replace(":", COLON_REPLACEMENT_CODE) + drop + pickup;
    }

    private void createInterchanges(Referential referential, GtfsImporter importer, GtfsImportParameters configuration, GtfsTrip gtfsTrip,
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

                String feederStopAreaId = ObjectIdUtil.composeObjectId(configuration.isSplitIdOnDot(), configuration.getObjectIdPrefix(),
                        "Quay", gtfsTransfer.getFromStopId());

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

                String consumerStopAreaId = ObjectIdUtil.composeObjectId(configuration.isSplitIdOnDot(), configuration.getObjectIdPrefix(),
                        "Quay", gtfsTransfer.getToStopId());


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
        String objectId = ObjectIdUtil.composeObjectId(configuration.isSplitIdOnDot(), configuration.getObjectIdPrefix(),
                Interchange.INTERCHANGE_KEY, partialId);
        return ObjectFactory.getInterchange(referential, objectId);
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

    private void createJourneyFrequencies(Referential referential, GtfsImporter importer,
                                          GtfsImportParameters configuration, GtfsTrip gtfsTrip, VehicleJourney vehicleJourney) {
        int count = 0;
        for (GtfsFrequency frequency : importer.getFrequencyByTrip().values(gtfsTrip.getTripId())) {
            vehicleJourney.setJourneyCategory(JourneyCategoryEnum.Frequency);

            String timeBandObjectId = ObjectIdUtil.composeObjectId(configuration.isSplitIdOnDot(), configuration.getObjectIdPrefix(),
                    Timeband.TIMETABLE_KEY, gtfsTrip.getTripId() + "-" + count++);
            Timeband timeband = ObjectFactory.getTimeband(referential, timeBandObjectId);
            timeband.setName(getTimebandName(frequency));
            timeband.setStartTime(frequency.getStartTime().getTime());
            timeband.setEndTime(frequency.getEndTime().getTime());

            String journeyFrequencyObjectId = ObjectIdUtil.composeObjectId(configuration.isSplitIdOnDot(), configuration.getObjectIdPrefix(),
                    "HeadwayJourney", gtfsTrip.getTripId() + "-" + count++);

            JourneyFrequency journeyFrequency = new JourneyFrequency();
            journeyFrequency.setExactTime(frequency.getExactTimes());
            journeyFrequency.setFirstDepartureTime(frequency.getStartTime().getTime());
            journeyFrequency.setLastDepartureTime(frequency.getEndTime().getTime());
            journeyFrequency.setScheduledHeadwayInterval(Duration.standardSeconds(frequency.getHeadwaySecs()));
            journeyFrequency.setTimeband(timeband);
            journeyFrequency.setObjectId(journeyFrequencyObjectId);

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

    private JourneyPattern createJourneyPattern(Referential referential,
                                                GtfsImportParameters configuration, GtfsTrip gtfsTrip, GtfsImporter importer,
                                                VehicleJourney vehicleJourney, String objectIdKey, Map<String, JourneyPattern> journeyPatternByStopSequence, Integer position) {
        JourneyPattern journeyPattern;

        // Route
        Route route = createRoute(referential, configuration, gtfsTrip, objectIdKey);

        // JourneyPattern
        String journeyPatternId = route.getObjectId().replace(Route.ROUTE_KEY, JourneyPattern.JOURNEYPATTERN_KEY);
        journeyPattern = ObjectFactory.getJourneyPattern(referential, journeyPatternId);
        journeyPattern.setRoute(route);
        journeyPatternByStopSequence.put(objectIdKey, journeyPattern);

        // StopPoints
        createStopPoint(route, journeyPattern, vehicleJourney.getVehicleJourneyAtStops(), referential, configuration, position);

        List<StopPoint> stopPoints = journeyPattern.getStopPoints();
        journeyPattern.setDepartureStopPoint(stopPoints.get(0));
        journeyPattern.setArrivalStopPoint(stopPoints.get(stopPoints.size() - 1));

        if(StringUtils.isEmpty(gtfsTrip.getTripHeadSign())) {
            journeyPattern.setName(journeyPattern.getArrivalStopPoint().getScheduledStopPoint().getContainedInStopAreaRef().getObject().getName());
            journeyPattern.setPublishedName(journeyPattern.getArrivalStopPoint().getScheduledStopPoint().getContainedInStopAreaRef().getObject().getName());
        } else {
            journeyPattern.setName(gtfsTrip.getTripHeadSign());
            journeyPattern.setPublishedName(gtfsTrip.getTripHeadSign());
        }

        journeyPattern.setFilled(true);
        route.setFilled(true);

        if (route.getName() == null) {

            if (!route.getStopPoints().isEmpty()) {
                StopPoint firstStopPoint = route.getStopPoints().get(0);
                StopPoint lastStopPoint = route.getStopPoints().get(route.getStopPoints().size() - 1);

                if (firstStopPoint.getScheduledStopPoint().getContainedInStopAreaRef().getObject() != null &&
                        lastStopPoint.getScheduledStopPoint().getContainedInStopAreaRef().getObject() != null) {
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


        // Shape -> routeSections
        if (configuration.isImportShapesFile() &&
                gtfsTrip.getShapeId() != null &&
                !gtfsTrip.getShapeId().isEmpty() &&
                importer.getShapeById().containsKey(gtfsTrip.getShapeId())) {
            List<RouteSection> sections = createRouteSections(referential, journeyPattern, importer.getShapeById().values(gtfsTrip.getShapeId()));
            if (!sections.isEmpty()) {
                journeyPattern.setRouteSections(sections);
                journeyPattern.setSectionStatus(SectionStatusEnum.Completed);
            }
        }

        addSyntheticDestinationDisplayIfMissingOnFirstStopPoint(configuration, referential, journeyPattern);

        return journeyPattern;
    }

    private String computeEndId(String journeyKey) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(journeyKey.getBytes());
        byte[] digest = md.digest();
        return DatatypeConverter.printHexBinary(digest).toUpperCase();
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

            String stopPointId = ObjectIdUtil.extractOriginalId(departureStopPoint.getObjectId());
            String journeyPatternId = ObjectIdUtil.extractOriginalId(jp.getObjectId());

            DestinationDisplay destinationDisplay = ObjectFactory.getDestinationDisplay(referential,
                    ObjectIdUtil.composeObjectId(configuration.isSplitIdOnDot(), configuration.getObjectIdPrefix(),
                            DestinationDisplay.DESTINATIONDISPLAY_KEY, journeyPatternId + "-" + stopPointId));

            if (jp.getArrivalStopPoint() != null && jp.getArrivalStopPoint().getScheduledStopPoint() != null &&
                    jp.getArrivalStopPoint().getScheduledStopPoint().getContainedInStopAreaRef() != null &&
                    jp.getArrivalStopPoint().getScheduledStopPoint().getContainedInStopAreaRef().getObject() != null) {
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

    private List<RouteSection> createRouteSections(Referential referential, JourneyPattern journeyPattern, Iterable<GtfsShape> gtfsShapes) {
        List<RouteSection> sections = new ArrayList<>();
        GeometryFactory factory = new GeometryFactory(new PrecisionModel(10), 4326);
        List<OrderedCoordinate> coordinates = new ArrayList<>();
        List<LineSegment> segments = new ArrayList<>();
        Coordinate previous = null;
        String shapeId = null;
        TransportModeNameEnum transportmodeName = journeyPattern.getRoute().getLine().getTransportModeName();

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
        coordinates.sort(COORDINATE_SORTER);
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
        for (StopPoint stop : journeyPattern.getStopPoints()) {
            // find nearest segment and project point on it
            StopArea location = stop.getScheduledStopPoint().getContainedInStopAreaRef().getObject();
            Coordinate point = new Coordinate(location.getLongitude().doubleValue(), location.getLatitude()
                    .doubleValue());
            double distance_min = Double.MAX_VALUE;
            int rank = 0;
            for (int i = segmentRank; i < segments.size(); i++) {
                double distance = segments.get(i).distance(point);
                if (distance < distance_min) {
                    distance_min = distance;
                    rank = i;
                }
            }
            // compose routeSection
            Coordinate projection;
            boolean lastSegmentIncluded = false;
            double factor = segments.get(rank).projectionFactor(point);
            int intFactor = (int) (factor * 100.);
            if (factor <= 0.05) {
                // projection near or before first point
                projection = segments.get(rank).getCoordinate(0);
                intFactor = 0;
            } else if (factor >= 0.95) {
                // projection near or after last point
                projection = segments.get(rank).getCoordinate(1);
                lastSegmentIncluded = true;
                intFactor = 100;
            } else {
                // projection inside segment
                projection = segments.get(rank).project(point);
            }
            if (previous != null) {
                List<Coordinate> coords = new ArrayList<>();
                coords.add(previous);
                for (int i = segmentRank; i < rank; i++) {
                    coords.add(segments.get(i).getCoordinate(1));
                }
                coords.add(projection);
                if (lastSegmentIncluded)
                    rank++;
                String routeSectionId = prefix + ":" + RouteSection.ROUTE_SECTION_KEY + ":" + journeyPattern.objectIdSuffix() + "_" + shapeId + "_"
                        + previousLocation.objectIdSuffix() + "_" + location.objectIdSuffix() + "_" + intFactor;
                RouteSection section = ObjectFactory.getRouteSection(referential, routeSectionId);
                if (!section.isFilled()) {
                    Coordinate[] inputCoords = new Coordinate[2];
                    section.setFromScheduledStopPoint(previousScheduledStopPoint);
                    inputCoords[0] = new Coordinate(previousLocation.getLongitude().doubleValue(), previousLocation
                            .getLatitude().doubleValue());
                    section.setToScheduledStopPoint(stop.getScheduledStopPoint());
                    inputCoords[1] = new Coordinate(location.getLongitude().doubleValue(), location.getLatitude()
                            .doubleValue());

                    if (TransportModeNameEnum.Air.equals(transportmodeName) || TransportModeNameEnum.Ferry.equals(transportmodeName)){
                        // No trace calculated for airplanes. Only a straight line between start and destination
                        section.setProcessedGeometry(factory.createLineString(inputCoords));
                    }else{
                        section.setProcessedGeometry(factory.createLineString(coords.toArray(new Coordinate[coords.size()])));
                    }

                    section.setInputGeometry(factory.createLineString(inputCoords));
                    section.setNoProcessing(false);
                    try {
                        double distance = section.getProcessedGeometry().getLength();
                        distance *= (Math.PI / 180) * 6378137;
                        section.setDistance(BigDecimal.valueOf(distance));
                    } catch (NumberFormatException e) {
                        log.error(shapeId + " : problem with section between " + previousLocation.getName() + "("
                                + previousLocation.getObjectId() + " and " + location.getName() + "("
                                + location.getObjectId());
                        log.error("coords (" + coords.size() + ") :");
                        for (Coordinate coordinate : coords) {
                            log.error("lat = " + coordinate.y + " , lon = " + coordinate.x);
                        }
                        sections.clear();
                        return sections;
                    }
                }
                section.setFilled(true);
                sections.add(section);
            }
            previous = projection;
            previousLocation = location;
            previousScheduledStopPoint = stop.getScheduledStopPoint();
            segmentRank = rank;

        }

        return sections;
    }

    /**
     * create route for trip
     *
     * @param referential
     * @param configuration
     * @param gtfsTrip
     * @param objectIdKey
     * @return
     */
    private Route createRoute(Referential referential, GtfsImportParameters configuration, GtfsTrip gtfsTrip, String objectIdKey) {
        String lineId = ObjectIdUtil.composeObjectId(configuration.isSplitIdOnDot(), configuration.getObjectIdPrefix(), Line.LINE_KEY, gtfsTrip.getRouteId());
        Line line = ObjectFactory.getLine(referential, lineId);
        String routeId = ObjectIdUtil.composeObjectId(configuration.isSplitIdOnDot(), configuration.getObjectIdPrefix(), Route.ROUTE_KEY, objectIdKey);
        Route route = ObjectFactory.getRoute(referential, routeId);
        route.setLine(line);
        PTDirectionEnum wayBack = gtfsTrip.getDirectionId().equals(DirectionType.Outbound) ? PTDirectionEnum.A : PTDirectionEnum.R;
        route.setWayBack(wayBack.toString());
        route.setDirection(wayBack);
        return route;
    }

    protected void convert(Context context, GtfsStopTime gtfsStopTime, VehicleJourneyAtStop vehicleJourneyAtStop) {

        Referential referential = (Referential) context.get(REFERENTIAL);
        GtfsImportParameters configuration = (GtfsImportParameters) context.get(CONFIGURATION);

        String vjasObjectId = ObjectIdUtil.composeObjectId(configuration.isSplitIdOnDot(), configuration.getObjectIdPrefix(),
                ObjectIdTypes.VEHICLE_JOURNEY_AT_STOP_KEY, UUID.randomUUID().toString());

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

    private BoardingPossibilityEnum toBoardingPossibility(PickUpTypeEnum type) {
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

    private AlightingPossibilityEnum toAlightingPossibility(DropOffTypeEnum type) {
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

    /**
     * Create stopPoints for Route
     * @param route
     * @param journeyPattern
     * @param list
     * @param referential
     * @param configuration
     */
    private void createStopPoint(Route route, JourneyPattern journeyPattern, List<VehicleJourneyAtStop> list,
                                 Referential referential, GtfsImportParameters configuration, Integer position) {
        Set<String> stopPointKeys = new HashSet<String>();

        int positionInitial = 0;
        for (VehicleJourneyAtStop vehicleJourneyAtStop : list) {
            VehicleJourneyAtStopWrapper wrapper = (VehicleJourneyAtStopWrapper) vehicleJourneyAtStop;
            String stopIdKeyFragment = createJourneyKeyFragment(wrapper);
            String baseKey = route.getObjectId().replace(Route.ROUTE_KEY, StopPoint.STOPPOINT_KEY) + "a"
                    + stopIdKeyFragment.trim();
            String stopKey = baseKey;
            int dup = 1;
            while (stopPointKeys.contains(stopKey)) {
                stopKey = baseKey + "_" + (dup++);
            }
            stopPointKeys.add(stopKey);

            StopPoint stopPoint = ObjectFactory.getStopPoint(referential, stopKey);

            String stopAreaId = ObjectIdUtil.toStopAreaId(configuration.isSplitIdOnDot(), configuration.getObjectIdPrefix(),
                    "Quay", wrapper.stopId);
            StopArea stopArea = ObjectFactory.getStopArea(referential, stopAreaId);

            String scheduledStopPointKey = stopKey.replace(StopPoint.STOPPOINT_KEY, ObjectIdTypes.SCHEDULED_STOP_POINT_KEY);
            ScheduledStopPoint scheduledStopPoint = ObjectFactory.getScheduledStopPoint(referential, scheduledStopPointKey);
            stopPoint.setScheduledStopPoint(scheduledStopPoint);


            scheduledStopPoint.setContainedInStopAreaRef(new SimpleObjectReference(stopArea));
            stopPoint.setRoute(route);
            if (configuration.isRouteSortOrder() && position != null) {
                stopPoint.setPosition(position);
            } else {
                stopPoint.setPosition(positionInitial++);
            }

            stopPoint.setForBoarding(toBoardingPossibility(wrapper.pickup));
            stopPoint.setForAlighting(toAlightingPossibility(wrapper.dropOff));

            if (wrapper.stopHeadsign != null) {
                DestinationDisplay destinationDisplay = ObjectFactory.getDestinationDisplay(referential,
                        ObjectIdUtil.composeObjectId(configuration.isSplitIdOnDot(), configuration.getObjectIdPrefix(),
                                DestinationDisplay.DESTINATIONDISPLAY_KEY, stopKey));
                destinationDisplay.setFrontText(wrapper.stopHeadsign);
                destinationDisplay.setName(wrapper.stopHeadsign);

                stopPoint.setDestinationDisplay(destinationDisplay);
            }


            journeyPattern.addStopPoint(stopPoint);
            stopPoint.setFilled(true);
        }
        NeptuneUtil.refreshDepartureArrivals(journeyPattern);
    }

    @AllArgsConstructor
    class VehicleJourneyAtStopWrapper extends VehicleJourneyAtStop {

        private static final long serialVersionUID = 5052093726657799027L;
        String stopId;
        int stopSequence;
        Float shapeDistTraveled;
        @Getter
        DropOffTypeEnum dropOff;
        @Getter
        PickUpTypeEnum pickup;
        String stopHeadsign;
    }

    public static final Comparator<VehicleJourneyAtStop> VEHICLE_JOURNEY_AT_STOP_COMPARATOR = (right, left) -> {
        int rightIndex = ((VehicleJourneyAtStopWrapper) right).stopSequence;
        int leftIndex = ((VehicleJourneyAtStopWrapper) left).stopSequence;
        return rightIndex - leftIndex;
    };

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
