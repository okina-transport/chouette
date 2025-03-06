package mobi.chouette.exchange.gtfs.parser;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.ObjectIdUtil;
import mobi.chouette.exchange.gtfs.importer.GtfsImportParameters;
import mobi.chouette.exchange.gtfs.model.GtfsFareRule;
import mobi.chouette.exchange.gtfs.model.importer.GtfsException;
import mobi.chouette.exchange.gtfs.model.importer.GtfsImporter;
import mobi.chouette.exchange.gtfs.model.importer.Index;
import mobi.chouette.exchange.gtfs.validation.Constant;
import mobi.chouette.exchange.gtfs.validation.GtfsValidationReporter;
import mobi.chouette.exchange.importer.Parser;
import mobi.chouette.exchange.importer.ParserFactory;
import mobi.chouette.exchange.importer.Validator;
import mobi.chouette.model.FareRule;
import mobi.chouette.model.Route;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.Referential;

@Log4j
public class GtfsFareRuleParser implements Parser, Validator, Constant {

    static {
        ParserFactory.register(GtfsFareRuleParser.class.getName(), new ParserFactory() {
            @Override
            protected Parser create() {
                return new GtfsFareRuleParser();
            }
        });
    }

    @Getter
    @Setter
    private String gtfsFareId;

    @Override
    public void validate(Context context) throws Exception {
        GtfsImporter importer = (GtfsImporter) context.get(PARSER);
        GtfsValidationReporter gtfsValidationReporter = (GtfsValidationReporter) context.get(GTFS_REPORTER);
        gtfsValidationReporter.getExceptions().clear();

        // fare_attributes.txt
        if (importer.hasFareRuleImporter()) { // the file "fare_attributes.txt" exists ?
            gtfsValidationReporter.reportSuccess(context, GTFS_1_GTFS_Common_1, GTFS_FARE_ATTRIBUTES_FILE);

            Index<GtfsFareRule> parser = null;
            try {
                parser = importer.getFareRuleById();
            } catch (Exception ex) {
                if (ex instanceof GtfsException) {
                    gtfsValidationReporter.reportError(context, (GtfsException) ex, GTFS_FARE_ATTRIBUTES_FILE);
                } else {
                    gtfsValidationReporter.throwUnknownError(context, ex, GTFS_FARE_ATTRIBUTES_FILE);
                }
            }

            gtfsValidationReporter.validateOkCSV(context, GTFS_FARE_ATTRIBUTES_FILE);

            if (parser == null) { // importer.getFareRuleById() fails for any other reason
                gtfsValidationReporter.throwUnknownError(context, new Exception("Cannot instantiate FareRuleById class"), GTFS_FARE_ATTRIBUTES_FILE);
            } else {
                gtfsValidationReporter.validate(context, GTFS_FARE_ATTRIBUTES_FILE, parser.getOkTests());
                gtfsValidationReporter.validateUnknownError(context);
            }

            if (!parser.getErrors().isEmpty()) {
                gtfsValidationReporter.reportErrors(context, parser.getErrors(), GTFS_FARE_ATTRIBUTES_FILE);
                parser.getErrors().clear();
            }

            gtfsValidationReporter.validateOKGeneralSyntax(context, GTFS_FARE_ATTRIBUTES_FILE);

            if (parser.getLength() == 0) {
                gtfsValidationReporter.reportError(context, new GtfsException(GTFS_FARE_ATTRIBUTES_FILE, 1, null, GtfsException.ERROR.OPTIONAL_FILE_WITH_NO_ENTRY, null, null), GTFS_FARE_ATTRIBUTES_FILE);
            } else {
                gtfsValidationReporter.validate(context, GTFS_FARE_ATTRIBUTES_FILE, GtfsException.ERROR.FILE_WITH_NO_ENTRY);
            }

            GtfsException fatalException = null;
            parser.setWithValidation(true);
            for (GtfsFareRule bean : parser) {
                try {
                    parser.validate(bean, importer);
                } catch (Exception ex) {
                    if (ex instanceof GtfsException) {
                        gtfsValidationReporter.reportError(context, (GtfsException) ex, GTFS_FARE_ATTRIBUTES_FILE);
                    } else {
                        gtfsValidationReporter.throwUnknownError(context, ex, GTFS_FARE_ATTRIBUTES_FILE);
                    }
                }
                for (GtfsException ex : bean.getErrors()) {
                    if (ex.isFatal())
                        fatalException = ex;
                }
                gtfsValidationReporter.reportErrors(context, bean.getErrors(), GTFS_FARE_ATTRIBUTES_FILE);
                gtfsValidationReporter.validate(context, GTFS_FARE_ATTRIBUTES_FILE, bean.getOkTests());
            }
            parser.setWithValidation(false);
            if (fatalException != null)
                throw fatalException;
        } else {
            gtfsValidationReporter.reportError(context, new GtfsException(GTFS_FARE_ATTRIBUTES_FILE, 1, null, GtfsException.ERROR.MISSING_OPTIONAL_FILE, null, null), GTFS_FARE_ATTRIBUTES_FILE);
        }
    }

    @Override
    public void parse(Context context) throws Exception {

        Referential referential = (Referential) context.get(REFERENTIAL);
        GtfsImporter importer = (GtfsImporter) context.get(PARSER);

        for (GtfsFareRule gtfsFareRule : importer.getFareRuleById()) {
            GtfsImportParameters configuration = (GtfsImportParameters) context.get(CONFIGURATION);
            String id = ObjectIdUtil.composeNeptuneObjectId(configuration.getObjectIdPrefix(), FareRule.FARE_KEY, gtfsFareRule.getFareId());
            FareRule fareRule = ObjectFactory.getFareRule(referential, id);

            String routeId = ObjectIdUtil.composeNeptuneObjectId(configuration.getObjectIdPrefix(), FareRule.ROUTE_KEY, gtfsFareRule.getRouteId());
            Route route = ObjectFactory.getRoute(referential, routeId);
            convert(gtfsFareRule, fareRule, route);
        }
    }

    protected void convert(GtfsFareRule gtfsFareRule, FareRule fareRule, Route route) throws Exception {
        fareRule.setRoute(route);
        fareRule.setOriginId(gtfsFareRule.getOriginId());
        fareRule.setDestinationId(gtfsFareRule.getDestinationId());
        fareRule.setContainsId(gtfsFareRule.getContainsId());
    }

}
