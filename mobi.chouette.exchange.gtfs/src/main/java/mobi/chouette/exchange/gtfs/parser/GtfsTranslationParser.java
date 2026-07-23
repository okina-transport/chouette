package mobi.chouette.exchange.gtfs.parser;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.ObjectIdUtil;
import mobi.chouette.exchange.gtfs.importer.GtfsImportParameters;
import mobi.chouette.exchange.gtfs.model.GtfsStop;
import mobi.chouette.exchange.gtfs.model.GtfsTranslation;
import mobi.chouette.exchange.gtfs.model.importer.GtfsException;
import mobi.chouette.exchange.gtfs.model.importer.GtfsImporter;
import mobi.chouette.exchange.gtfs.model.importer.Index;
import mobi.chouette.exchange.gtfs.validation.Constant;
import mobi.chouette.exchange.gtfs.validation.GtfsValidationReporter;
import mobi.chouette.exchange.importer.Parser;
import mobi.chouette.exchange.importer.ParserFactory;
import mobi.chouette.exchange.importer.Validator;
import mobi.chouette.model.*;
import mobi.chouette.model.util.ObjectIdTypes;
import mobi.chouette.model.util.Referential;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Imports GTFS translations.txt.
 * <p>
 * Rows keyed by record_id only have their owner's objectId computed here (stops -> StopArea,
 * routes -> Line, trips -> VehicleJourney, agency -> Network + Authority Company) and stashed on the
 * translation's transient targetObjectId - the owner entity itself is never fetched or created during
 * parsing. Resolving and attaching the real owner happens later, in the corresponding *TranslationUpdater,
 * at persist time. Rows keyed by field_value (no record_id, per the GTFS spec's legacy translation style)
 * leave targetObjectId null and keep the raw field_value instead, so they can be matched later (at
 * export/publication time) against the untranslated raw value of whichever entity of that same type is
 * being processed. Rows for GTFS tables with no NeTEx-exportable counterpart are ignored (no *Translation
 * entity exists for them).
 */
@Log4j
public class GtfsTranslationParser implements Parser, Validator, Constant {

    private static final String TABLE_AGENCY = "agency";
    private static final String TABLE_STOPS = "stops";
    private static final String TABLE_ROUTES = "routes";
    private static final String TABLE_TRIPS = "trips";
    private static final String TABLE_STOP_TIMES = "stop_times";

    private static final Map<String, Map<String, List<String>>> TABLE_NAME_TO_FIELD_NAME_MAPPER = Map.of(
            TABLE_AGENCY,
            Map.of(
                    "agency_name", List.of("name"),
                    "agency_url", List.of("url"),
                    "agency_phone", List.of("phone"),
                    "agency_email", List.of("email")
            ),
            TABLE_STOPS,
            Map.of(
                    "stop_code", List.of("registrationNumber"),
                    "stop_name", List.of("stopName"),
                    "tts_stop_name", List.of("ttsStopName"),
                    "stop_desc", List.of("comment"),
                    "stop_url", List.of("url"),
                    "platform_code", List.of("platformCode")
            ),
            TABLE_ROUTES,
            Map.of(
                    "route_short_name", List.of("number"),
                    "route_long_name", List.of("name", "publishedName"),
                    "route_desc", List.of("comment"),
                    "route_url", List.of("url")
            ),
            TABLE_TRIPS,
            Map.of(
                    "trip_headsign", List.of("publishedJourneyName"),
                    "trip_short_name", List.of("publishedJourneyIdentifier", "privateCode")
            ),
            TABLE_STOP_TIMES,
            Map.of(
                    "stop_headsign", List.of("frontText")
            )
    );

    static {
        ParserFactory.register(GtfsTranslationParser.class.getName(), new ParserFactory() {
            @Override
            protected Parser create() {
                return new GtfsTranslationParser();
            }
        });
    }

    @Override
    public void validate(Context context) throws Exception {
        GtfsImporter importer = (GtfsImporter) context.get(PARSER);
        GtfsValidationReporter gtfsValidationReporter = (GtfsValidationReporter) context.get(GTFS_REPORTER);
        gtfsValidationReporter.getExceptions().clear();

        if (importer.hasTranslationImporter()) {
            gtfsValidationReporter.reportSuccess(context, GTFS_1_GTFS_Common_1, GTFS_TRANSLATIONS_FILE);

            Index<GtfsTranslation> parser = null;
            try {
                parser = importer.getTranslationIndex();
            } catch (Exception ex) {
                if (ex instanceof GtfsException) {
                    gtfsValidationReporter.reportError(context, (GtfsException) ex, GTFS_TRANSLATIONS_FILE);
                } else {
                    gtfsValidationReporter.throwUnknownError(context, ex, GTFS_TRANSLATIONS_FILE);
                }
            }

            gtfsValidationReporter.validateOkCSV(context, GTFS_TRANSLATIONS_FILE);

            if (parser == null) {
                gtfsValidationReporter.throwUnknownError(context, new Exception("Cannot instantiate TranslationIndex class"), GTFS_TRANSLATIONS_FILE);
            } else {
                gtfsValidationReporter.validate(context, GTFS_TRANSLATIONS_FILE, parser.getOkTests());
                gtfsValidationReporter.validateUnknownError(context);
            }

            if (!parser.getErrors().isEmpty()) {
                gtfsValidationReporter.reportErrors(context, parser.getErrors(), GTFS_TRANSLATIONS_FILE);
                parser.getErrors().clear();
            }

            gtfsValidationReporter.validateOKGeneralSyntax(context, GTFS_TRANSLATIONS_FILE);

            if (parser.getLength() == 0) {
                gtfsValidationReporter.reportError(context, new GtfsException(GTFS_TRANSLATIONS_FILE, 1, null, GtfsException.ERROR.OPTIONAL_FILE_WITH_NO_ENTRY, null, null), GTFS_TRANSLATIONS_FILE);
            } else {
                gtfsValidationReporter.validate(context, GTFS_TRANSLATIONS_FILE, GtfsException.ERROR.FILE_WITH_NO_ENTRY);
            }
        } else {
            gtfsValidationReporter.reportError(context, new GtfsException(GTFS_TRANSLATIONS_FILE, 1, null, GtfsException.ERROR.MISSING_OPTIONAL_FILE, null, null), GTFS_TRANSLATIONS_FILE);
        }
    }

    @Override
    public void parse(Context context) throws Exception {
        GtfsImporter importer = (GtfsImporter) context.get(PARSER);
        if (!importer.hasTranslationImporter()) {
            return;
        }

        // we do not use agency from agency.txt when target network object id is set
        boolean ignoreAgencyTranslations = (context.get(TARGET_NETWORK_OBJECT_ID) != null);

        Referential referential = (Referential) context.get(REFERENTIAL);
        GtfsImportParameters configuration = (GtfsImportParameters) context.get(CONFIGURATION);
        boolean splitOnDot = configuration.isSplitIdOnDot();
        String prefix = configuration.getObjectIdPrefix();

        for (GtfsTranslation gtfsTranslation : importer.getTranslationIndex()) {
            String tableName = StringUtils.trimToEmpty(gtfsTranslation.getTableName()).toLowerCase();
            String recordId = gtfsTranslation.getRecordId();
            String recordSubId = gtfsTranslation.getRecordSubId();
            boolean keyedByFieldValue = StringUtils.isBlank(recordId);

            if (keyedByFieldValue && StringUtils.isBlank(gtfsTranslation.getFieldValue())) {
                log.warn("Ignoring translations.txt row " + gtfsTranslation.getId() + " : either record_id or field_value is required");
                continue;
            }

            // record_id and field_value are mutually exclusive per spec; whichever is set is the
            // row's natural key component
            String rowKey = keyedByFieldValue ? gtfsTranslation.getFieldValue() : recordId;

            switch (tableName) {
                case TABLE_AGENCY:
                    if (ignoreAgencyTranslations) {
                        break;
                    }
                    String networkObjectId = keyedByFieldValue ? null :
                            ObjectIdUtil.composeObjectId(splitOnDot, prefix, ObjectIdTypes.PTNETWORK_KEY, recordId);
                    addNetworkTranslation(referential, prefix, gtfsTranslation, rowKey, networkObjectId);

                    String authorityObjectId = keyedByFieldValue ? null :
                            ObjectIdUtil.composeObjectId(splitOnDot, prefix, ObjectIdTypes.AUTHORITY_KEY, recordId);
                    addCompanyTranslation(referential, prefix, gtfsTranslation, rowKey, authorityObjectId);
                    String operatorObjectId = keyedByFieldValue ? null :
                            ObjectIdUtil.composeObjectId(splitOnDot, prefix, ObjectIdTypes.OPERATOR_KEY, recordId + "o");
                    addCompanyTranslation(referential, prefix, gtfsTranslation, rowKey, operatorObjectId);
                    break;
                case TABLE_STOPS:
                    String stopAreaObjectId = keyedByFieldValue ? null : resolveStopAreaObjectId(importer, configuration, splitOnDot, prefix, recordId);
                    addStopAreaTranslation(referential, prefix, gtfsTranslation, rowKey, stopAreaObjectId);
                    break;
                case TABLE_ROUTES:
                    String lineObjectId = keyedByFieldValue ? null :
                            ObjectIdUtil.composeObjectId(splitOnDot, prefix, ObjectIdTypes.LINE_KEY, recordId);
                    addLineTranslation(referential, prefix, gtfsTranslation, rowKey, lineObjectId);
                    break;
                case TABLE_TRIPS:
                    String vehicleJourneyObjectId = keyedByFieldValue ? null :
                            ObjectIdUtil.composeObjectId(splitOnDot, prefix, ObjectIdTypes.VEHICLEJOURNEY_KEY, recordId);
                    addVehicleJourneyTranslation(referential, prefix, gtfsTranslation, rowKey, vehicleJourneyObjectId);
                    break;
                case TABLE_STOP_TIMES:
                    String vehicleJourneyAtStopObjectId = keyedByFieldValue ? null :
                            ObjectIdUtil.composeObjectId(splitOnDot, prefix,
                                    ObjectIdTypes.VEHICLE_JOURNEY_AT_STOP_KEY, recordId + "-" + recordSubId);
                    addVehicleJourneyAtStopTranslation(referential, prefix, gtfsTranslation, rowKey, vehicleJourneyAtStopObjectId);
                    break;
                default:
                    // not handled ATM
                    log.debug("Ignoring translations.txt row " + gtfsTranslation.getId() + " / table " + gtfsTranslation.getTableName() + " / field " + gtfsTranslation.getFieldName());
                    break;
            }
        }
    }

    // only computes the objectId the row's owner *would* have - does not fetch or create the entity;
    // resolving the real owner is the *TranslationUpdater's job, at persist time
    private String resolveStopAreaObjectId(GtfsImporter importer, GtfsImportParameters configuration, boolean splitOnDot, String prefix, String stopId) {
        Index<GtfsStop> stops = importer.getStopById();
        if (!stops.containsKey(stopId)) {
            return null;
        }
        GtfsStop gtfsStop = stops.getValue(stopId);
        if (gtfsStop.getLocationType() == GtfsStop.LocationType.Access) {
            return null;
        }
        String type = gtfsStop.getLocationType() == GtfsStop.LocationType.Station ? "StopPlace" : "Quay";
        String prefixToStrip = gtfsStop.getLocationType() == GtfsStop.LocationType.Station
                ? configuration.getCommercialPointIdPrefixToRemove()
                : configuration.getQuayIdPrefixToRemove();
        String strippedStopId = stopId.replaceFirst("^" + prefixToStrip, "").trim();
        return ObjectIdUtil.toStopAreaId(splitOnDot, prefix, type, strippedStopId);
    }

    private void addNetworkTranslation(Referential referential, String prefix, GtfsTranslation gtfsTranslation, String rowKey, String networkObjectId) {
        for (String objectFieldName : objectFieldNames(gtfsTranslation)) {
            NetworkTranslation translation = new NetworkTranslation();
            translation.setDetached(true);
            translation.setObjectId(translationObjectId(prefix, gtfsTranslation, rowKey, "agency-network", objectFieldName));
            translation.setTargetObjectId(networkObjectId);
            fillCommonFields(translation, objectFieldName, gtfsTranslation);
            addToReferential(referential.getNetworkTranslationsByObjectId(), referential.getNetworkTranslationsByFieldValue(), translation, networkObjectId);
        }
    }

    private void addCompanyTranslation(Referential referential, String prefix, GtfsTranslation gtfsTranslation, String rowKey, String companyObjectId) {
        for (String objectFieldName : objectFieldNames(gtfsTranslation)) {
            CompanyTranslation translation = new CompanyTranslation();
            translation.setDetached(true);
            translation.setObjectId(translationObjectId(prefix, gtfsTranslation, rowKey, "agency-authority", objectFieldName));
            translation.setTargetObjectId(companyObjectId);
            fillCommonFields(translation, objectFieldName, gtfsTranslation);
            addToReferential(referential.getCompanyTranslationsByObjectId(), referential.getCompanyTranslationsByFieldValue(), translation, companyObjectId);
        }
    }

    private void addLineTranslation(Referential referential, String prefix, GtfsTranslation gtfsTranslation, String rowKey, String lineObjectId) {
        for (String objectFieldName : objectFieldNames(gtfsTranslation)) {
            LineTranslation translation = new LineTranslation();
            translation.setDetached(true);
            translation.setObjectId(translationObjectId(prefix, gtfsTranslation, rowKey, "route", objectFieldName));
            translation.setTargetObjectId(lineObjectId);
            fillCommonFields(translation, objectFieldName, gtfsTranslation);
            addToReferential(referential.getLineTranslationsByObjectId(), referential.getLineTranslationsByFieldValue(), translation, lineObjectId);
        }
    }

    private void addStopAreaTranslation(Referential referential, String prefix, GtfsTranslation gtfsTranslation, String rowKey, String stopAreaObjectId) {
        for (String objectFieldName : objectFieldNames(gtfsTranslation)) {
            StopAreaTranslation translation = new StopAreaTranslation();
            translation.setDetached(true);
            translation.setObjectId(translationObjectId(prefix, gtfsTranslation, rowKey, "stop", objectFieldName));
            translation.setTargetObjectId(stopAreaObjectId);
            fillCommonFields(translation, objectFieldName, gtfsTranslation);
            addToReferential(referential.getStopAreaTranslationsByObjectId(), referential.getStopAreaTranslationsByFieldValue(), translation, stopAreaObjectId);
        }
    }

    private void addVehicleJourneyTranslation(Referential referential, String prefix, GtfsTranslation gtfsTranslation, String rowKey, String vehicleJourneyObjectId) {
        for (String objectFieldName : objectFieldNames(gtfsTranslation)) {
            VehicleJourneyTranslation translation = new VehicleJourneyTranslation();
            translation.setDetached(true);
            translation.setObjectId(translationObjectId(prefix, gtfsTranslation, rowKey, "trip", objectFieldName));
            translation.setTargetObjectId(vehicleJourneyObjectId);
            fillCommonFields(translation, objectFieldName, gtfsTranslation);
            addToReferential(referential.getVehicleJourneyTranslationsByObjectId(), referential.getVehicleJourneyTranslationsByFieldValue(), translation, vehicleJourneyObjectId);
        }
    }

    private void addVehicleJourneyAtStopTranslation(Referential referential, String prefix, GtfsTranslation gtfsTranslation, String rowKey, String vehicleJourneyAtStopObjectId) {
        for (String objectFieldName : objectFieldNames(gtfsTranslation)) {
            VehicleJourneyAtStopTranslation translation = new VehicleJourneyAtStopTranslation();
            translation.setDetached(true);
            translation.setObjectId(translationObjectId(prefix, gtfsTranslation, rowKey, "stop-time", objectFieldName));
            translation.setTargetObjectId(vehicleJourneyAtStopObjectId);
            fillCommonFields(translation, objectFieldName, gtfsTranslation);
            addToReferential(referential.getVehicleJourneyAtStopTranslationsByObjectId(), referential.getVehicleJourneyAtStopTranslationsByFieldValue(), translation, vehicleJourneyAtStopObjectId);
        }
    }

    // indexes a just-created translation by its resolved owner's objectId, or - when it has no owner
    // (legacy field_value-keyed row) - appends it to the flat field_value list; built once, during this
    // single parse pass. Does not attach the translation to any entity - *TranslationUpdater resolves
    // and sets the real owner at persist time, from targetObjectId.
    private <T extends Translation> void addToReferential(Map<String, List<T>> byObjectId, List<T> byFieldValue,
                                                          T translation, String ownerObjectId) {
        if (StringUtils.isNotBlank(ownerObjectId)) {
            byObjectId.computeIfAbsent(ownerObjectId, k -> new ArrayList<>()).add(translation);
        } else if (StringUtils.isNotBlank(translation.getFieldValue())) {
            byFieldValue.add(translation);
        }
    }

    // one row per mapped object attribute, so a single GTFS field mapped to several attributes
    // (e.g. one field feeding two entity properties) is stored as distinct translation rows
    private List<String> objectFieldNames(GtfsTranslation gtfsTranslation) {
        String tableName = StringUtils.trimToEmpty(gtfsTranslation.getTableName()).toLowerCase();
        Map<String, List<String>> fieldNameMapper = TABLE_NAME_TO_FIELD_NAME_MAPPER.getOrDefault(tableName, Map.of());
        return fieldNameMapper.getOrDefault(gtfsTranslation.getFieldName(), List.of(gtfsTranslation.getFieldName()));
    }

    // stable natural-key suffix (table_name+field_name+language+rowKey uniquely identify a
    // translations.txt row per spec) so re-importing the same feed updates rather than duplicates
    private String translationObjectId(String prefix, GtfsTranslation gtfsTranslation, String rowKey, String discriminator, String objectFieldName) {
        String naturalKey = gtfsTranslation.getTableName() + "-" + gtfsTranslation.getFieldName() + "-"
                + gtfsTranslation.getLanguage() + "-" + rowKey + "-" + discriminator + "-" + objectFieldName;
        return ObjectIdUtil.composeObjectId(false, prefix, ObjectIdTypes.TRANSLATION_KEY, naturalKey);
    }

    private void fillCommonFields(Translation translation, String objectFieldName, GtfsTranslation gtfsTranslation) {
        translation.setFieldName(objectFieldName);
        translation.setLanguage(gtfsTranslation.getLanguage());
        translation.setTranslation(gtfsTranslation.getTranslation());
        translation.setFieldValue(gtfsTranslation.getFieldValue());
    }

}
