package mobi.chouette.exchange.gtfs.parser;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.exchange.gtfs.model.GtfsFareLegRules;
import mobi.chouette.exchange.gtfs.model.GtfsTimeframe;
import mobi.chouette.exchange.gtfs.model.fares.GtfsFareV2File;
import mobi.chouette.exchange.gtfs.model.importer.GtfsException;
import mobi.chouette.exchange.gtfs.model.importer.GtfsImporter;
import mobi.chouette.exchange.gtfs.model.importer.Index;
import mobi.chouette.exchange.gtfs.validation.Constant;
import mobi.chouette.exchange.gtfs.validation.GtfsValidationReporter;
import mobi.chouette.exchange.importer.Parser;
import mobi.chouette.exchange.importer.ParserFactory;
import mobi.chouette.exchange.importer.Validator;
import org.apache.commons.collections4.CollectionUtils;

@Log4j
public class GtfsFareLegRulesParser implements Parser, Validator, Constant {

	static {
		ParserFactory.register(GtfsFareLegRulesParser.class.getName(), new ParserFactory() {
			@Override
			protected Parser create() {
				return new GtfsFareLegRulesParser();
			}
		});
	}

	@Override
	public void validate(Context context) throws Exception {
		GtfsImporter importer = (GtfsImporter) context.get(PARSER);
		GtfsValidationReporter gtfsValidationReporter = (GtfsValidationReporter) context.get(GTFS_REPORTER);
		gtfsValidationReporter.getExceptions().clear();

		if (importer.hasFareLegRulesImporter()) {
			gtfsValidationReporter.reportSuccess(context, GTFS_1_GTFS_Common_1, GtfsFareV2File.FARE_LEG_RULES.getFilename());
			Index<GtfsFareLegRules> parser = null;
			try {
				parser = importer.getFareLegRulesIndex();
			} catch (GtfsException ex) {
				gtfsValidationReporter.reportError(context, ex, GtfsFareV2File.FARE_LEG_RULES.getFilename());
			} catch (Exception ex) {
				gtfsValidationReporter.throwUnknownError(context, ex, GtfsFareV2File.FARE_LEG_RULES.getFilename());
			}

			gtfsValidationReporter.validateOkCSV(context,  GtfsFareV2File.FARE_LEG_RULES.getFilename());

			if (parser == null) {
				gtfsValidationReporter.throwUnknownError(context, new Exception("Cannot instantiate FareLegRulesIndex class"), GtfsFareV2File.FARE_MEDIA.getFilename());
			} else {
				gtfsValidationReporter.validate(context, GtfsFareV2File.FARE_LEG_RULES.getFilename(), parser.getOkTests());
				gtfsValidationReporter.validateUnknownError(context);
			}

			if (parser != null && CollectionUtils.isNotEmpty(parser.getErrors())) {
				gtfsValidationReporter.reportErrors(context, parser.getErrors(), GtfsFareV2File.FARE_LEG_RULES.getFilename());
				parser.getErrors().clear();
			}

			gtfsValidationReporter.validateOKGeneralSyntax(context, GtfsFareV2File.FARE_LEG_RULES.getFilename());

			if (parser != null && parser.getLength() == 0) {
				gtfsValidationReporter.reportError(context, new GtfsException(GtfsFareV2File.FARE_LEG_RULES.getFilename(), 1, null, GtfsException.ERROR.FILE_WITH_NO_ENTRY, null, null), GtfsFareV2File.FARE_LEG_RULES.getFilename());
			} else {
				gtfsValidationReporter.validate(context, GtfsFareV2File.FARE_LEG_RULES.getFilename(), GtfsException.ERROR.FILE_WITH_NO_ENTRY);
			}

			GtfsException fatalException = null;
			if (parser != null) {
				parser.setWithValidation(true);
				for (GtfsFareLegRules bean : parser) {
					try {
						parser.validate(bean, importer);
					} catch (GtfsException ex) {
						gtfsValidationReporter.reportError(context, ex, GtfsFareV2File.FARE_LEG_RULES.getFilename());
					} catch (Exception ex) {
						gtfsValidationReporter.throwUnknownError(context, ex, GtfsFareV2File.FARE_LEG_RULES.getFilename());
					}

					for (GtfsException ex : bean.getErrors()) {
						if (ex.isFatal())
							fatalException = ex;
					}
					gtfsValidationReporter.reportErrors(context, bean.getErrors(), GtfsFareV2File.FARE_LEG_RULES.getFilename());
					gtfsValidationReporter.validate(context, GtfsFareV2File.FARE_LEG_RULES.getFilename(), bean.getOkTests());
				}
				parser.setWithValidation(false);
				if (fatalException != null) {
					throw fatalException;
				}
			}

			gtfsValidationReporter.reportSuccess(context, GTFS_1_GTFS_Common_1, GtfsFareV2File.FARE_LEG_RULES.getFilename());
		}
		if (importer.hasTimeBasedFaresFileImporter()) {
			gtfsValidationReporter.reportSuccess(context, GTFS_1_GTFS_Common_1, GtfsFareV2File.TIME_BASED_FARES.getFilename());
			Index<GtfsTimeframe> parser = null;
			try {
				parser = importer.getFareTimeframeIndex();
			} catch (GtfsException ex) {
				gtfsValidationReporter.reportError(context, ex, GtfsFareV2File.TIME_BASED_FARES.getFilename());
			} catch (Exception ex) {
				gtfsValidationReporter.throwUnknownError(context, ex, GtfsFareV2File.TIME_BASED_FARES.getFilename());
			}

			gtfsValidationReporter.validateOkCSV(context,  GtfsFareV2File.TIME_BASED_FARES.getFilename());

			if (parser == null) {
				gtfsValidationReporter.throwUnknownError(context, new Exception("Cannot instantiate TimeframesIndex class"), GtfsFareV2File.FARE_MEDIA.getFilename());
			} else {
				gtfsValidationReporter.validate(context, GtfsFareV2File.TIME_BASED_FARES.getFilename(), parser.getOkTests());
				gtfsValidationReporter.validateUnknownError(context);
			}

			if (parser != null && CollectionUtils.isNotEmpty(parser.getErrors())) {
				gtfsValidationReporter.reportErrors(context, parser.getErrors(), GtfsFareV2File.TIME_BASED_FARES.getFilename());
				parser.getErrors().clear();
			}

			gtfsValidationReporter.validateOKGeneralSyntax(context, GtfsFareV2File.TIME_BASED_FARES.getFilename());

			if (parser != null && parser.getLength() == 0) {
				gtfsValidationReporter.reportError(context, new GtfsException(GtfsFareV2File.TIME_BASED_FARES.getFilename(), 1, null, GtfsException.ERROR.FILE_WITH_NO_ENTRY, null, null), GtfsFareV2File.TIME_BASED_FARES.getFilename());
			} else {
				gtfsValidationReporter.validate(context, GtfsFareV2File.TIME_BASED_FARES.getFilename(), GtfsException.ERROR.FILE_WITH_NO_ENTRY);
			}

			GtfsException fatalException = null;
			if (parser != null) {
				parser.setWithValidation(true);
				for (GtfsTimeframe bean : parser) {
					try {
						parser.validate(bean, importer);
					} catch (GtfsException ex) {
						gtfsValidationReporter.reportError(context, ex, GtfsFareV2File.TIME_BASED_FARES.getFilename());
					} catch (Exception ex) {
						gtfsValidationReporter.throwUnknownError(context, ex, GtfsFareV2File.TIME_BASED_FARES.getFilename());
					}

					for (GtfsException ex : bean.getErrors()) {
						if (ex.isFatal())
							fatalException = ex;
					}
					gtfsValidationReporter.reportErrors(context, bean.getErrors(), GtfsFareV2File.TIME_BASED_FARES.getFilename());
					gtfsValidationReporter.validate(context, GtfsFareV2File.TIME_BASED_FARES.getFilename(), bean.getOkTests());
				}
				parser.setWithValidation(false);
				if (fatalException != null) {
					throw fatalException;
				}
			}
		}
	}

	@Override
	public void parse(Context context) {
		log.debug("No actions - Delegate to fare referential application");
	}

}
