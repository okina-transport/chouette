package mobi.chouette.exchange.gtfs.parser;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.ObjectIdUtil;
import mobi.chouette.exchange.gtfs.importer.GtfsImportParameters;
import mobi.chouette.exchange.gtfs.model.GtfsTripExtension;
import mobi.chouette.exchange.gtfs.model.importer.GtfsException;
import mobi.chouette.exchange.gtfs.model.importer.GtfsImporter;
import mobi.chouette.exchange.gtfs.model.importer.Index;
import mobi.chouette.exchange.gtfs.validation.Constant;
import mobi.chouette.exchange.gtfs.validation.GtfsValidationReporter;
import mobi.chouette.exchange.importer.Parser;
import mobi.chouette.exchange.importer.ParserFactory;
import mobi.chouette.exchange.importer.Validator;
import mobi.chouette.model.TripExtension;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.ObjectIdTypes;
import mobi.chouette.model.util.Referential;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

@Log4j
public class GtfsTripExtensionParser implements Parser, Validator, Constant {

    static {
        ParserFactory.register(GtfsTripExtensionParser.class.getName(), new ParserFactory() {
            @Override
            protected Parser create() {
                return new GtfsTripExtensionParser();
            }
        });
    }

    @Override
    public void validate(Context context) throws Exception {
        GtfsImporter importer = (GtfsImporter) context.get(PARSER);
        GtfsValidationReporter gtfsValidationReporter = (GtfsValidationReporter) context.get(GTFS_REPORTER);
        gtfsValidationReporter.getExceptions().clear();

        if (importer.hasTripExtensionImporter()) {
            gtfsValidationReporter.reportSuccess(context, GTFS_1_GTFS_Common_1, GTFS_TRIP_EXTENSIONS_FILE);

            Index<GtfsTripExtension> parser = null;
            try {
                parser = importer.getTripExtensionById();
            } catch (Exception ex) {
                if (ex instanceof GtfsException) {
                    gtfsValidationReporter.reportError(context, (GtfsException) ex, GTFS_TRIP_EXTENSIONS_FILE);
                } else {
                    gtfsValidationReporter.throwUnknownError(context, ex, GTFS_TRIP_EXTENSIONS_FILE);
                }
            }

            gtfsValidationReporter.validateOkCSV(context, GTFS_TRIP_EXTENSIONS_FILE);

            if (parser == null) {
                gtfsValidationReporter.throwUnknownError(context, new Exception("Cannot instantiate TripExtensionById class"), GTFS_TRIP_EXTENSIONS_FILE);
            } else {
                gtfsValidationReporter.validate(context, GTFS_TRIP_EXTENSIONS_FILE, parser.getOkTests());
                gtfsValidationReporter.validateUnknownError(context);
            }

            if (CollectionUtils.isNotEmpty(parser.getErrors())) {
                gtfsValidationReporter.reportErrors(context, parser.getErrors(), GTFS_TRIP_EXTENSIONS_FILE);
                parser.getErrors().clear();
            }

            gtfsValidationReporter.validateOKGeneralSyntax(context, GTFS_TRIP_EXTENSIONS_FILE);

            if (parser.getLength() == 0) {
                gtfsValidationReporter.reportError(context, new GtfsException(GTFS_TRIP_EXTENSIONS_FILE, 1, null, GtfsException.ERROR.OPTIONAL_FILE_WITH_NO_ENTRY, null, null), GTFS_TRIP_EXTENSIONS_FILE);
            } else {
                gtfsValidationReporter.validate(context, GTFS_TRIP_EXTENSIONS_FILE, GtfsException.ERROR.FILE_WITH_NO_ENTRY);
            }
        } else {
            gtfsValidationReporter.reportError(context, new GtfsException(GTFS_TRIP_EXTENSIONS_FILE, 1, null, GtfsException.ERROR.MISSING_OPTIONAL_FILE, null, null), GTFS_TRIP_EXTENSIONS_FILE);
        }
    }

    @Override
    public void parse(Context context) throws Exception {
        GtfsImporter importer = (GtfsImporter) context.get(PARSER);
        if (!importer.hasTripExtensionImporter()) {
            return;
        }

        Referential referential = (Referential) context.get(REFERENTIAL);
        // trip_extensions.txt is present this run - tells VehicleJourneyUpdater it is safe to
        // clear a trip's stale TripExtension when no matching row is found for it
        referential.setTripExtensionsProcessed(true);

        GtfsImportParameters configuration = (GtfsImportParameters) context.get(CONFIGURATION);
        boolean splitOnDot = configuration.isSplitIdOnDot();
        String prefix = configuration.getObjectIdPrefix();

        for (GtfsTripExtension gtfsTripExtension : importer.getTripExtensionById()) {
            if (StringUtils.isBlank(gtfsTripExtension.getTripId())) {
                log.warn("Ignoring trip_extensions.txt row " + gtfsTripExtension.getId() + " : trip_id is required");
                continue;
            }

            String vehicleJourneyObjectId = ObjectIdUtil.composeObjectId(splitOnDot, prefix, ObjectIdTypes.VEHICLEJOURNEY_KEY, gtfsTripExtension.getTripId());

            TripExtension tripExtension = new TripExtension();
            tripExtension.setDetached(true);
            tripExtension.setObjectId(ObjectIdUtil.composeObjectId(splitOnDot, prefix, ObjectIdTypes.TRIP_EXTENSION_KEY, gtfsTripExtension.getTripId()));
            tripExtension.setIndicReservation(gtfsTripExtension.getIndicReservation());

            if (StringUtils.isNotBlank(gtfsTripExtension.getRouteId())) {
                String lineObjectId = ObjectIdUtil.composeObjectId(splitOnDot, prefix, ObjectIdTypes.LINE_KEY, gtfsTripExtension.getRouteId());
                tripExtension.setLine(ObjectFactory.getLine(referential, lineObjectId));
            }
            if (StringUtils.isNotBlank(gtfsTripExtension.getContractCompanyId())) {
                String contractCompanyObjectId = ObjectIdUtil.composeObjectId(splitOnDot, prefix, ObjectIdTypes.TRIP_COMPANY_KEY, gtfsTripExtension.getContractCompanyId());
                tripExtension.setContractCompany(ObjectFactory.getTripCompany(referential, contractCompanyObjectId));
            }
            if (StringUtils.isNotBlank(gtfsTripExtension.getExecCompanyId())) {
                String execCompanyObjectId = ObjectIdUtil.composeObjectId(splitOnDot, prefix, ObjectIdTypes.TRIP_COMPANY_KEY, gtfsTripExtension.getExecCompanyId());
                tripExtension.setExecCompany(ObjectFactory.getTripCompany(referential, execCompanyObjectId));
            }

            referential.getTripExtensionsByObjectId().put(vehicleJourneyObjectId, tripExtension);
        }
    }

}
