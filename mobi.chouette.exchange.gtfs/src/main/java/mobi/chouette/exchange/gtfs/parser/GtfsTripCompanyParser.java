package mobi.chouette.exchange.gtfs.parser;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.ObjectIdUtil;
import mobi.chouette.exchange.gtfs.importer.GtfsImportParameters;
import mobi.chouette.exchange.gtfs.model.GtfsTripCompany;
import mobi.chouette.exchange.gtfs.model.importer.GtfsException;
import mobi.chouette.exchange.gtfs.model.importer.GtfsImporter;
import mobi.chouette.exchange.gtfs.model.importer.Index;
import mobi.chouette.exchange.gtfs.validation.Constant;
import mobi.chouette.exchange.gtfs.validation.GtfsValidationReporter;
import mobi.chouette.exchange.importer.Parser;
import mobi.chouette.exchange.importer.ParserFactory;
import mobi.chouette.exchange.importer.Validator;
import mobi.chouette.model.TripCompany;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.ObjectIdTypes;
import mobi.chouette.model.util.Referential;
import org.apache.commons.collections4.CollectionUtils;

@Log4j
public class GtfsTripCompanyParser implements Parser, Validator, Constant {

    static {
        ParserFactory.register(GtfsTripCompanyParser.class.getName(), new ParserFactory() {
            @Override
            protected Parser create() {
                return new GtfsTripCompanyParser();
            }
        });
    }

    @Override
    public void validate(Context context) throws Exception {
        GtfsImporter importer = (GtfsImporter) context.get(PARSER);
        GtfsValidationReporter gtfsValidationReporter = (GtfsValidationReporter) context.get(GTFS_REPORTER);
        gtfsValidationReporter.getExceptions().clear();

        if (importer.hasTripCompanyImporter()) {
            gtfsValidationReporter.reportSuccess(context, GTFS_1_GTFS_Common_1, GTFS_TRIP_COMPANIES_FILE);

            Index<GtfsTripCompany> parser = null;
            try {
                parser = importer.getTripCompanyById();
            } catch (Exception ex) {
                if (ex instanceof GtfsException) {
                    gtfsValidationReporter.reportError(context, (GtfsException) ex, GTFS_TRIP_COMPANIES_FILE);
                } else {
                    gtfsValidationReporter.throwUnknownError(context, ex, GTFS_TRIP_COMPANIES_FILE);
                }
            }

            gtfsValidationReporter.validateOkCSV(context, GTFS_TRIP_COMPANIES_FILE);

            if (parser == null) {
                gtfsValidationReporter.throwUnknownError(context, new Exception("Cannot instantiate TripCompanyById class"), GTFS_TRIP_COMPANIES_FILE);
            } else {
                gtfsValidationReporter.validate(context, GTFS_TRIP_COMPANIES_FILE, parser.getOkTests());
                gtfsValidationReporter.validateUnknownError(context);
            }

            if (CollectionUtils.isNotEmpty(parser.getErrors())) {
                gtfsValidationReporter.reportErrors(context, parser.getErrors(), GTFS_TRIP_COMPANIES_FILE);
                parser.getErrors().clear();
            }

            gtfsValidationReporter.validateOKGeneralSyntax(context, GTFS_TRIP_COMPANIES_FILE);

            if (parser.getLength() == 0) {
                gtfsValidationReporter.reportError(context, new GtfsException(GTFS_TRIP_COMPANIES_FILE, 1, null, GtfsException.ERROR.OPTIONAL_FILE_WITH_NO_ENTRY, null, null), GTFS_TRIP_COMPANIES_FILE);
            } else {
                gtfsValidationReporter.validate(context, GTFS_TRIP_COMPANIES_FILE, GtfsException.ERROR.FILE_WITH_NO_ENTRY);
            }
        } else {
            gtfsValidationReporter.reportError(context, new GtfsException(GTFS_TRIP_COMPANIES_FILE, 1, null, GtfsException.ERROR.MISSING_OPTIONAL_FILE, null, null), GTFS_TRIP_COMPANIES_FILE);
        }
    }

    @Override
    public void parse(Context context) throws Exception {
        GtfsImporter importer = (GtfsImporter) context.get(PARSER);
        if (!importer.hasTripCompanyImporter()) {
            return;
        }

        GtfsImportParameters configuration = (GtfsImportParameters) context.get(CONFIGURATION);
        Referential referential = (Referential) context.get(REFERENTIAL);
        boolean splitOnDot = configuration.isSplitIdOnDot();
        String prefix = configuration.getObjectIdPrefix();

        for (GtfsTripCompany gtfsTripCompany : importer.getTripCompanyById()) {
            String objectId = ObjectIdUtil.composeObjectId(splitOnDot, prefix, ObjectIdTypes.TRIP_COMPANY_KEY, gtfsTripCompany.getCompanyId());
            TripCompany tripCompany = ObjectFactory.getTripCompany(referential, objectId);
            tripCompany.setDetached(true);
            tripCompany.setName(gtfsTripCompany.getCompanyName());
            tripCompany.setAddress(gtfsTripCompany.getCompanyAddress());
            tripCompany.setZipcode(gtfsTripCompany.getCompanyZipcode());
            tripCompany.setCity(gtfsTripCompany.getCompanyCity());
            tripCompany.setPhone(gtfsTripCompany.getCompanyPhone());
            tripCompany.setEmail(gtfsTripCompany.getCompanyEmail());
			tripCompany.setOriginalCompanyId(gtfsTripCompany.getCompanyId());
        }
    }

}
