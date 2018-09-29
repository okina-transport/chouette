package mobi.chouette.exchange.gtfs.parser;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.PrecisionModel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.TimeUtil;
import mobi.chouette.exchange.gtfs.importer.GtfsImportParameters;
import mobi.chouette.exchange.gtfs.model.*;
import mobi.chouette.exchange.gtfs.model.GtfsStop.LocationType;
import mobi.chouette.exchange.gtfs.model.GtfsStopTime.DropOffType;
import mobi.chouette.exchange.gtfs.model.GtfsStopTime.PickupType;
import mobi.chouette.exchange.gtfs.model.GtfsTransfer.TransferType;
import mobi.chouette.exchange.gtfs.model.GtfsTrip.DirectionType;
import mobi.chouette.exchange.gtfs.model.importer.*;
import mobi.chouette.exchange.gtfs.validation.Constant;
import mobi.chouette.exchange.gtfs.validation.GtfsValidationReporter;
import mobi.chouette.exchange.importer.Parser;
import mobi.chouette.exchange.importer.ParserFactory;
import mobi.chouette.exchange.importer.Validator;
import mobi.chouette.model.*;
import mobi.chouette.model.type.*;
import mobi.chouette.model.util.NeptuneUtil;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.ObjectIdTypes;
import mobi.chouette.model.util.Referential;
import org.apache.commons.lang.StringUtils;
import org.joda.time.Duration;
import org.joda.time.LocalTime;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.Adler32;
import java.util.zip.Checksum;

@Log4j
public class GtfsTripParser implements Parser, Validator, Constant {

    private static final Comparator<OrderedCoordinate> COORDINATE_SORTER = new OrderedCoordinateComparator();

    @Getter
    @Setter
    private String gtfsRouteId;

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
        List<Route> lstNotShapedRoute = new ArrayList<Route>();
        Map<String, List<Route>> mapRoutesByShapes = new HashMap<>();
        List<VehicleJourneyAtStop> lstShapeVjas = null;

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
            boolean hasShapeDistTraveled = true;

            for (GtfsStopTime gtfsStopTime : importer.getStopTimeByTrip().values(gtfsTrip.getTripId())) {
                VehicleJourneyAtStopWrapper vehicleJourneyAtStop = null;
                BoardingPossibilityEnum bPE = convertGtfsPickUpTypeToBoardingPossibility(gtfsStopTime.getPickupType());
                AlightingPossibilityEnum aPE = convertGtfsDropOffTypeToAlightingPossibility(gtfsStopTime
                        .getDropOffType());

                if (gtfsStopTime.getPickupType() != null)
                    bPE = convertGtfsPickUpTypeToBoardingPossibility(gtfsStopTime.getPickupType());

                if (gtfsStopTime.getDropOffType() != null)
                    aPE = convertGtfsDropOffTypeToAlightingPossibility(gtfsStopTime.getDropOffType());

                if (hasShapeDistTraveled && gtfsStopTime.getShapeDistTraveled() == null)
                    hasShapeDistTraveled = false;

                vehicleJourneyAtStop = new VehicleJourneyAtStopWrapper(gtfsStopTime.getStopId(),
                        gtfsStopTime.getStopSequence(), gtfsStopTime.getShapeDistTraveled(), bPE, aPE, gtfsStopTime.getStopHeadsign());

                convert(context, gtfsStopTime, vehicleJourneyAtStop);

                if (afterMidnight) {
                    if (!gtfsStopTime.getArrivalTime().moreOneDay())
                        afterMidnight = false;
                    if (!gtfsStopTime.getDepartureTime().moreOneDay())
                        afterMidnight = false;
                }

                vehicleJourneyAtStop.setVehicleJourney(vehicleJourney);
            }
			// check if VJ is all on demand (flexible service)
			ajustOnDemand(vehicleJourney);

            // Mantis 58964 : if trip with shape and shapeDistTraveled on all stopTimes
            if (hasShapeDistTraveled && gtfsTrip.getShapeId() != null && !gtfsTrip.getShapeId().isEmpty()
                    && importer.getShapeById().containsKey(gtfsTrip.getShapeId())) {
                Collections.sort(vehicleJourney.getVehicleJourneyAtStops(), VEHICLE_JOURNEY_AT_STOP_SHAPE_COMPARATOR);
            } else {
                Collections.sort(vehicleJourney.getVehicleJourneyAtStops(), VEHICLE_JOURNEY_AT_STOP_COMPARATOR);
            }

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

            if (lstShapeVjas == null)
                lstShapeVjas = new ArrayList<VehicleJourneyAtStop>();
            else
                lstShapeVjas.clear();

            // if route with shape
            if (hasShapeDistTraveled && gtfsTrip.getShapeId() != null && !gtfsTrip.getShapeId().isEmpty()
                    && importer.getShapeById().containsKey(gtfsTrip.getShapeId())) {
                for (VehicleJourneyAtStop vehicleJourneyAtStop : vehicleJourney.getVehicleJourneyAtStops()) {
                    float shapeValue = ((VehicleJourneyAtStopWrapper) vehicleJourneyAtStop).shapeDistTraveled;
                    if (Float.valueOf(shapeValue) != null) {
                        // add point with shape position
                        lstShapeVjas.add(vehicleJourneyAtStop);
                    } else {
                        // problem on point without shape position
                        vehicleJourneyAtStop.setVehicleJourney(null);
                    }
                }
                journeyKey += ":" + buildShapeKey(vehicleJourney);

            } else {
                journeyKey += "," + buildStopsKey(vehicleJourney);
            }

            JourneyPattern journeyPattern = journeyPatternByStopSequence.get(journeyKey);
            if (journeyPattern == null) {
                journeyPattern = createJourneyPattern(context, referential, configuration, gtfsTrip, gtfsShapes,
                        vehicleJourney, journeyKey, journeyPatternByStopSequence, lstNotShapedRoute, mapRoutesByShapes);
            }

            vehicleJourney.setRoute(journeyPattern.getRoute());
            vehicleJourney.setJourneyPattern(journeyPattern);

            int length = journeyPattern.getStopPoints().size();

            // if route with shape
            if (hasShapeDistTraveled && gtfsTrip.getShapeId() != null && !gtfsTrip.getShapeId().isEmpty()
                    && importer.getShapeById().containsKey(gtfsTrip.getShapeId())) {
                for (int i = 0; i < length; i++) {
                    VehicleJourneyAtStop vehicleJourneyAtStop = lstShapeVjas.get(i);
                    vehicleJourneyAtStop.setStopPoint(journeyPattern.getStopPoints().get(i));
                }
            } else {
                for (int i = 0; i < length; i++) {
                    VehicleJourneyAtStop vehicleJourneyAtStop = vehicleJourney.getVehicleJourneyAtStops().get(i);
                    vehicleJourneyAtStop.setStopPoint(journeyPattern.getStopPoints().get(i));
                }
            }

            // apply frequencies if any
            if (importer.hasFrequencyImporter()) {
                createJourneyFrequencies(context, referential, importer, configuration, gtfsTrip, vehicleJourney);
            }


            if (configuration.isParseInterchanges() && importer.hasTransferImporter()) {
                createInterchanges(context, referential, importer, configuration, gtfsTrip, vehicleJourney);
            }

        }
        // dispose collections
        journeyPatternByStopSequence.clear();

        // merge routes without shapes
        mergeRoutes(referential, lstNotShapedRoute);
        lstNotShapedRoute.clear();

        // merge routes with shapes
        for (List<Route> routes : mapRoutesByShapes.values()) {
            mergeRoutes(referential, routes);
            routes.clear();
        }
        mapRoutesByShapes.clear();

        // Check if line has all trips as flexible service
        int cptVjTad = 0;
        for (VehicleJourney vj : referential.getVehicleJourneys().values()) {
            if (vj.getFlexibleService() != null && vj.getFlexibleService())
                cptVjTad++;
        }
        // if all trips as flexible service : set flag on line and remove it on trips
        if (cptVjTad == referential.getVehicleJourneys().size()) {
            if (referential.getLines().size() > 0) {
                Line line = referential.getLines().values().iterator().next();
                line.setFlexibleService(true);
                for (VehicleJourney vj : referential.getVehicleJourneys().values())
                    vj.setFlexibleService(null);
            }
        }

        // clean boarding and alighting info when all normal in route
        for (Route route : referential.getRoutes().values()) {
            boolean allNominal = true;
            for (StopPoint point : route.getStopPoints()) {
                if (point.getForAlighting() != null && !point.getForAlighting().equals(AlightingPossibilityEnum.normal)) {
                    allNominal = false;
                    break;
                }
                if (point.getForBoarding() != null && !point.getForBoarding().equals(BoardingPossibilityEnum.normal)) {
                    allNominal = false;
                    break;
                }
            }
            if (allNominal) {
                for (StopPoint point : route.getStopPoints()) {
                    point.setForAlighting(null);
                    point.setForBoarding(null);
                }
            }
        }

        // Clean empty journeyPatterns and routes
        List<Route> rToBeRemoved = new ArrayList<>();
        for (Route route : referential.getRoutes().values()) {
            if (route.getJourneyPatterns().isEmpty()) {
                rToBeRemoved.add(route);
            }
        }
        for (Route route : rToBeRemoved) {
            route.setLine(null);
            referential.getRoutes().remove(route);
        }
        lstShapeVjas.clear();

    }

    private void mergeRoutes(Referential referential, List<Route> routeList) {
        // Sort routes by stopPointCount desc
        orderRouteListByStopPointListSize(routeList);

        for (int i = 0; i < routeList.size(); i++) {
            Route route1 = routeList.get(i);
            if (route1.getStopPoints().size() == 0) {
                continue;
            }
            for (int j = i + 1; j < routeList.size(); j++) {
                Route route2 = routeList.get(j);

                if (route2.getStopPoints().size() == 0) {
                    continue;
                }
                if (route1.getStopPoints().size() > route2.getStopPoints().size()) {
                    mergeIfRouteInclude(route2, route1, referential);
                } else {
                    continue;

                }

            }
        }
    }


    /**
     * report onDemand flag on vehicleJourney if all stop-times are on demand
     *
     * @param vehicleJourney
     */
    private void ajustOnDemand(VehicleJourney vehicleJourney) {
        boolean onDemand = true;
        for (VehicleJourneyAtStop vjas : vehicleJourney.getVehicleJourneyAtStops()) {
            VehicleJourneyAtStopWrapper vjasw = (VehicleJourneyAtStopWrapper) vjas;
            if (!vjasw.pickUpType.equals(BoardingPossibilityEnum.is_flexible)) {
                onDemand = false;
                break;
            }
            if (!vjasw.dropOffType.equals(AlightingPossibilityEnum.is_flexible)) {
                onDemand = false;
                break;
            }
        }
        if (onDemand) {
            for (VehicleJourneyAtStop vjas : vehicleJourney.getVehicleJourneyAtStops()) {
                VehicleJourneyAtStopWrapper vjasw = (VehicleJourneyAtStopWrapper) vjas;
                vjasw.pickUpType = BoardingPossibilityEnum.normal;
                vjasw.dropOffType = AlightingPossibilityEnum.normal;
            }
            vehicleJourney.setFlexibleService(Boolean.TRUE);
        }
    }


    /**
     * @param lstRoute
     */
    private void orderRouteListByStopPointListSize(List<Route> lstRoute) {
        Collections.sort(lstRoute, new Comparator<Route>() {
            @Override
            public int compare(Route r1, Route r2) {
                Integer n1 = new Integer(r1.getStopPoints().size());
                Integer n2 = new Integer(r2.getStopPoints().size());
                return n2.compareTo(n1);
            }
        });
    }

    /**
     * Is route included in another route
     *
     * @param routeIncluded
     * @param routeIncluding
     * @param referential
     */
    private boolean mergeIfRouteInclude(Route routeIncluded, Route routeIncluding, Referential referential) {
        if (!routeIncluded.getWayBack().equals(routeIncluding.getWayBack()))
            return false;
        int rank = 0;
        Map<StopPoint, StopPoint> includedSPMap = new HashMap<>();
        List<StopPoint> includingSPList = routeIncluding.getStopPoints();
        boolean match = true;
        for (StopPoint includedStop : routeIncluded.getStopPoints()) {
            while (rank < includingSPList.size()) {
                if (checkIfTwoPointAreEquivalent(includedStop, includingSPList.get(rank)))
                    break;
                rank++;
            }
            if (rank == includingSPList.size()) {
                match = false;
                break;
            }
            includedSPMap.put(includedStop, includingSPList.get(rank));
        }
        if (match) {
            for (Iterator<JourneyPattern> iterator = routeIncluded.getJourneyPatterns().iterator(); iterator.hasNext(); ) {
                JourneyPattern journeyPattern = iterator.next();
                iterator.remove();
                List<StopPoint> points = new ArrayList<>();
                for (StopPoint stopPoint : journeyPattern.getStopPoints()) {
                    if (includedSPMap.get(stopPoint) != null)
                        points.add(includedSPMap.get(stopPoint));
                }
                if (points.size() != journeyPattern.getStopPoints().size()) {
                    log.error("missing points when merging");
                    throw new RuntimeException("missing points when merging");
                }
                journeyPattern.setRoute(routeIncluding);
                journeyPattern.setStopPoints(points);
                journeyPattern.setDepartureStopPoint(points.get(0));
                journeyPattern.setArrivalStopPoint(points.get(points.size() - 1));
                for (VehicleJourney vj : journeyPattern.getVehicleJourneys()) {
                    vj.setRoute(routeIncluding);
                    for (VehicleJourneyAtStop vjas : vj.getVehicleJourneyAtStops()) {
                        vjas.setStopPoint(includedSPMap.get(vjas.getStopPoint()));
                    }
                }
            }
            routeIncluded.getStopPoints().clear();
            for (StopPoint stopPoint : includedSPMap.keySet()) {
                referential.getStopPoints().remove(stopPoint.getObjectId());
            }
        }
        includedSPMap.clear();
        return match;
    }

    /**
     * Check if two gtfs stopPoint are equivalent according to their stopArea
     * and pickuptime and dropofftime
     *
     * @param sp1
     * @param sp2
     * @return
     */
    private boolean checkIfTwoPointAreEquivalent(StopPoint sp1, StopPoint sp2) {
        ScheduledStopPoint scheduledStopPoint1 = sp1.getScheduledStopPoint();
        ObjectReference<StopArea> containedInStopAreaRef1 = scheduledStopPoint1.getContainedInStopAreaRef();
        StopArea stopArea1 = containedInStopAreaRef1.getObject();

        ScheduledStopPoint scheduledStopPoint2 = sp2.getScheduledStopPoint();
        ObjectReference<StopArea> containedInStopAreaRef2 = scheduledStopPoint2.getContainedInStopAreaRef();
        StopArea stopArea2 = containedInStopAreaRef2.getObject();

        if (sp1.getForBoarding() == null) sp1.setForBoarding(BoardingPossibilityEnum.normal);
        if (sp2.getForBoarding() == null) sp2.setForBoarding(BoardingPossibilityEnum.normal);
        if (sp1.getForAlighting() == null) sp1.setForAlighting(AlightingPossibilityEnum.normal);
        if (sp2.getForAlighting() == null) sp2.setForAlighting(AlightingPossibilityEnum.normal);

        return stopArea1.equals(stopArea2)
                && sp1.getForBoarding().equals(sp2.getForBoarding())
                && sp1.getForAlighting().equals(sp2.getForAlighting());
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

    /**
     * @param context
     * @param referential
     * @param configuration
     * @param gtfsTrip
     * @param gtfsShapes
     * @param vehicleJourney
     * @param journeyKey
     * @param journeyPatternByStopSequence
     * @param lstNotShapedRoute
     * @param mapRoutesByShapes
     * @return
     */
    private JourneyPattern createJourneyPattern(Context context, Referential referential,
                                                GtfsImportParameters configuration, GtfsTrip gtfsTrip, Iterable<GtfsShape> gtfsShapes,
                                                VehicleJourney vehicleJourney, String journeyKey, Map<String, JourneyPattern> journeyPatternByStopSequence,
                                                List<Route> lstNotShapedRoute, Map<String, List<Route>> mapRoutesByShapes) {
        JourneyPattern journeyPattern;

        // Route
        Route route = createRoute(referential, configuration, gtfsTrip, vehicleJourney);

        // log.info(Color.CYAN + "createJourneyPattern : route " +
        // route.getObjectId() + Color.NORMAL);

        // JourneyPattern
        String journeyPatternId = route.getObjectId().replace(Route.ROUTE_KEY, JourneyPattern.JOURNEYPATTERN_KEY);
        journeyPattern = ObjectFactory.getJourneyPattern(referential, journeyPatternId);
        journeyPattern.setName(gtfsTrip.getTripHeadSign());
        journeyPattern.setRoute(route);
        journeyPatternByStopSequence.put(journeyKey, journeyPattern);
        // log.info(Color.CYAN + "createJourneyPattern : journeyPattern " +
        // journeyPattern.getObjectId() + Color.NORMAL);

        // StopPoints
        createStopPoint(route, journeyPattern, vehicleJourney.getVehicleJourneyAtStops(), referential, configuration);

        List<StopPoint> stopPoints = journeyPattern.getStopPoints();
        journeyPattern.setDepartureStopPoint(stopPoints.get(0));
        journeyPattern.setArrivalStopPoint(stopPoints.get(stopPoints.size() - 1));

        journeyPattern.setFilled(true);
        route.setFilled(true);

        if (route.getName() == null) {

            if (!route.getStopPoints().isEmpty()) {
                String first = route.getStopPoints().get(0).getScheduledStopPoint().getContainedInStopAreaRef().getObject().getName();
                String last = route.getStopPoints().get(route.getStopPoints().size() - 1).getScheduledStopPoint().getContainedInStopAreaRef().getObject().getName();
                route.setName(first + " -> " + last);
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
        if (gtfsShapes != null) {
            List<RouteSection> sections = createRouteSections(context, referential, configuration, journeyPattern,
                    vehicleJourney, gtfsShapes);
            if (!sections.isEmpty()) {
                journeyPattern.setRouteSections(sections);
                journeyPattern.setSectionStatus(SectionStatusEnum.Completed);
            }
        }

        // trip without shape ?
        if (gtfsTrip.getShapeId() == null || gtfsTrip.getShapeId().isEmpty()) {
            lstNotShapedRoute.add(route);
        } else {
            List<Route> list = mapRoutesByShapes.get(gtfsTrip.getShapeId());
            if (list == null) {
                list = new ArrayList<>();
                mapRoutesByShapes.put(gtfsTrip.getShapeId(), list);
            }
            list.add(route);
        }

        //addSyntheticDestinationDisplayIfMissingOnFirstStopPoint(configuration, referential, journeyPattern);

        return journeyPattern;
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

    /**
     * @param context
     * @param referential
     * @param configuration
     * @param journeyPattern
     * @param vehicleJourney
     * @param gtfsShapes
     * @return
     */
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
            Coordinate projection = null;
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
                    section.setProcessedGeometry(factory.createLineString(coords.toArray(new Coordinate[coords.size()])));
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
     * @param vehicleJourney
     * @return
     */
    private Route createRoute(Referential referential, GtfsImportParameters configuration, GtfsTrip gtfsTrip,
                              VehicleJourney vehicleJourney) {
        String lineId = AbstractConverter.composeObjectId(configuration, Line.LINE_KEY,
                gtfsTrip.getRouteId(), log);
        Line line = ObjectFactory.getLine(referential, lineId);

        // For SNCF rail data (France), update line submode according to trip "trip_headsign" column.
        if (line.getTransportModeName() != null && line.getTransportModeName().equals(TransportModeNameEnum.Rail)) {
            TransportSubModeNameEnum subModeFromTripHeadSign = getSubModeFromTripHeadSign(gtfsTrip.getTripHeadSign());
            line.setTransportSubModeName(subModeFromTripHeadSign);
            log.info("Set sub transport mode for line " + line.getName() + " - " + line.getObjectId() + " to : " + subModeFromTripHeadSign);
        }

        String routeKey = gtfsTrip.getRouteId() + "_" + gtfsTrip.getDirectionId().ordinal();
        if (gtfsTrip.getShapeId() != null && !gtfsTrip.getShapeId().isEmpty())
            routeKey += "_" + gtfsTrip.getShapeId();
        // routeKey += "_" + line.getRoutes().size();
        routeKey += "_" + buildStopsKey(vehicleJourney);
        String routeId = AbstractConverter.composeObjectId(configuration, Route.ROUTE_KEY,
                routeKey, log);
        // log.info(Color.LIGHT_BLUE + "createRoute : route " + routeId +
        // Color.NORMAL);

        Route route = ObjectFactory.getRoute(referential, routeId);
        route.setLine(line);
        PTDirectionEnum wayBack = gtfsTrip.getDirectionId().equals(DirectionType.Outbound) ? PTDirectionEnum.A : PTDirectionEnum.R;
        route.setWayBack(wayBack.toString());
        route.setDirection(wayBack);
        return route;
    }

    private String buildStopsKey(VehicleJourney vehicleJourney) {
        String stopsKey = "";
        for (VehicleJourneyAtStop vjas : vehicleJourney.getVehicleJourneyAtStops()) {
            VehicleJourneyAtStopWrapper vjasw = (VehicleJourneyAtStopWrapper) vjas;
            String stopId = vjasw.stopId;

            stopId += "_" + getPickUpTypeOrdinal(vjasw) + "_" + getDropOffTypeOrdinal(vjasw);

            stopsKey += stopId + " ";
        }
        Checksum checksum = new Adler32();
        byte bytes[] = stopsKey.getBytes();
        checksum.update(bytes, 0, bytes.length);
        return Long.toHexString(checksum.getValue());
    }

    private String buildShapeKey(VehicleJourney vehicleJourney) {
        String shapeKey = "";
        for (VehicleJourneyAtStop vjas : vehicleJourney.getVehicleJourneyAtStops()) {
            VehicleJourneyAtStopWrapper vjasw = (VehicleJourneyAtStopWrapper) vjas;
            String stopId = "";
            if (vjasw.shapeDistTraveled != null)
                stopId += vjasw.shapeDistTraveled;
            else
                stopId = vjasw.stopId;

            stopId += "_" + getPickUpTypeOrdinal(vjasw) + "_" + getDropOffTypeOrdinal(vjasw);

            shapeKey += stopId + " ";
        }
        Checksum checksum = new Adler32();
        byte bytes[] = shapeKey.getBytes();
        checksum.update(bytes, 0, bytes.length);
        return Long.toHexString(checksum.getValue());
    }

    private int getPickUpTypeOrdinal(VehicleJourneyAtStopWrapper vjas) {
        return (vjas.pickUpType == null ? 0 : vjas.pickUpType.ordinal());
    }

    private int getDropOffTypeOrdinal(VehicleJourneyAtStopWrapper vjas) {
        return (vjas.dropOffType == null ? 0 : vjas.dropOffType.ordinal());
    }

    /**
     * @param context
     * @param gtfsStopTime
     * @param vehicleJourneyAtStop
     */
    protected void convert(Context context, GtfsStopTime gtfsStopTime, VehicleJourneyAtStop vehicleJourneyAtStop) {

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

    /**
     * @param context
     * @param gtfsTrip
     * @param vehicleJourney
     */
    protected void convert(Context context, GtfsTrip gtfsTrip, VehicleJourney vehicleJourney) {

        if (gtfsTrip.getTripShortName() != null) {
            try {
                vehicleJourney.setNumber(Long.parseLong(gtfsTrip.getTripShortName()));
            } catch (NumberFormatException e) {
                vehicleJourney.setNumber(Long.valueOf(0));
                vehicleJourney.setPublishedJourneyName(gtfsTrip.getTripShortName());
            }
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
        vehicleJourney.setFilled(true);

    }

    /**
     * create stopPoints for Route
     *
     * @param route
     * @param journeyPattern
     * @param list
     * @param referential
     * @param configuration
     * @return
     */
    private void createStopPoint(Route route, JourneyPattern journeyPattern, List<VehicleJourneyAtStop> list,
                                 Referential referential, GtfsImportParameters configuration) {
        Set<String> stopPointKeys = new HashSet<String>();

        int position = 0;
        for (VehicleJourneyAtStop vehicleJourneyAtStop : list) {
            VehicleJourneyAtStopWrapper wrapper = (VehicleJourneyAtStopWrapper) vehicleJourneyAtStop;
            String baseKey = route.getObjectId().replace(Route.ROUTE_KEY, StopPoint.STOPPOINT_KEY) + "a"
                    + wrapper.stopId.trim().replaceAll("[^a-zA-Z_0-9\\-]", "_");
            String stopKey = baseKey;
            int dup = 1;
            while (stopPointKeys.contains(stopKey)) {
                stopKey = baseKey + "_" + (dup++);
            }
            stopPointKeys.add(stopKey);

            StopPoint stopPoint = ObjectFactory.getStopPoint(referential, stopKey);

            String stopAreaId = AbstractConverter.toStopAreaId(configuration,
                    "Quay", wrapper.stopId);
            StopArea stopArea = ObjectFactory.getStopArea(referential, stopAreaId);

            String scheduledStopPointKey = stopKey.replace(StopPoint.STOPPOINT_KEY, ObjectIdTypes.SCHEDULED_STOP_POINT_KEY);
            ScheduledStopPoint scheduledStopPoint = ObjectFactory.getScheduledStopPoint(referential, scheduledStopPointKey);
            stopPoint.setScheduledStopPoint(scheduledStopPoint);


            scheduledStopPoint.setContainedInStopAreaRef(new SimpleObjectReference(stopArea));
            stopPoint.setRoute(route);
            stopPoint.setPosition(position++);
//            stopPoint.setForBoarding(toBoardingPossibility(wrapper.pickUp));
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

    /**
     * Convert GtfsStopTime pickUpType to VehicleJourneyAtStopPickUpType
     *
     * @param pickupType
     * @return
     */
    public BoardingPossibilityEnum convertGtfsPickUpTypeToBoardingPossibility(GtfsStopTime.PickupType pickupType) {
        if (pickupType != null) {

            switch (pickupType) {
                case Scheduled:
                    return BoardingPossibilityEnum.normal;
                case NoAvailable:
                    return BoardingPossibilityEnum.forbidden;
                case AgencyCall:
                    return BoardingPossibilityEnum.is_flexible;
                case DriverCall:
                    return BoardingPossibilityEnum.request_stop;
                default:
                    return BoardingPossibilityEnum.normal;
            }
        }

        return BoardingPossibilityEnum.normal;
    }

    /**
     * Convert GtfsStopTime pickUpType to VehicleJourneyAtStopPickUpType
     *
     * @param dropOffType
     * @return
     */
    public AlightingPossibilityEnum convertGtfsDropOffTypeToAlightingPossibility(GtfsStopTime.DropOffType dropOffType) {
        if (dropOffType != null) {

            switch (dropOffType) {
                case Scheduled:
                    return AlightingPossibilityEnum.normal;
                case NoAvailable:
                    return AlightingPossibilityEnum.forbidden;
                case AgencyCall:
                    return AlightingPossibilityEnum.is_flexible;
                case DriverCall:
                    return AlightingPossibilityEnum.request_stop;
                default:
                    return AlightingPossibilityEnum.normal;
            }
        }

        return AlightingPossibilityEnum.normal;
    }

    public static final Comparator<VehicleJourneyAtStop> VEHICLE_JOURNEY_AT_STOP_COMPARATOR = new Comparator<VehicleJourneyAtStop>() {
        @Override
        public int compare(VehicleJourneyAtStop right, VehicleJourneyAtStop left) {
            int rightIndex = ((VehicleJourneyAtStopWrapper) right).stopSequence;
            int leftIndex = ((VehicleJourneyAtStopWrapper) left).stopSequence;
            return rightIndex - leftIndex;
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

    public static final Comparator<VehicleJourneyAtStop> VEHICLE_JOURNEY_AT_STOP_SHAPE_COMPARATOR = new Comparator<VehicleJourneyAtStop>() {
        @Override
        public int compare(VehicleJourneyAtStop right, VehicleJourneyAtStop left) {
            int rightIndexF = 0;
            int leftIndexF = 0;
            int value = 0;

            rightIndexF = Math.round(((VehicleJourneyAtStopWrapper) right).shapeDistTraveled);
            leftIndexF = Math.round(((VehicleJourneyAtStopWrapper) left).shapeDistTraveled);
            value = Math.round(rightIndexF - leftIndexF);

            return value;
        }
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
