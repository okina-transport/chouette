package mobi.chouette.exchange.gtfs.parser;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.ObjectIdUtil;
import mobi.chouette.exchange.gtfs.importer.GtfsImportParameters;
import mobi.chouette.exchange.gtfs.model.GtfsFareAttribute;
import mobi.chouette.exchange.gtfs.model.importer.GtfsException;
import mobi.chouette.exchange.gtfs.model.importer.GtfsImporter;
import mobi.chouette.exchange.gtfs.model.importer.Index;
import mobi.chouette.exchange.gtfs.validation.Constant;
import mobi.chouette.exchange.gtfs.validation.GtfsValidationReporter;
import mobi.chouette.exchange.importer.Parser;
import mobi.chouette.exchange.importer.ParserFactory;
import mobi.chouette.exchange.importer.Validator;
import mobi.chouette.model.Agency;
import mobi.chouette.model.FareAttribute;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.Referential;

@Log4j
public class GtfsFareAttributeParser implements Parser, Validator, Constant {

	static {
		ParserFactory.register(GtfsFareAttributeParser.class.getName(), new ParserFactory() {
			@Override
			protected Parser create() {
				return new GtfsFareAttributeParser();
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
		if (importer.hasFareAttributeImporter()) { // the file "fare_attributes.txt" exists ?
			gtfsValidationReporter.reportSuccess(context, GTFS_1_GTFS_Common_1, GTFS_FARE_ATTRIBUTES_FILE);

			Index<GtfsFareAttribute> parser = null;
			try {
				parser = importer.getFareAttributeById();
			} catch (Exception ex) {
				if (ex instanceof GtfsException) {
					gtfsValidationReporter.reportError(context, (GtfsException) ex, GTFS_FARE_ATTRIBUTES_FILE);
				} else {
					gtfsValidationReporter.throwUnknownError(context, ex, GTFS_FARE_ATTRIBUTES_FILE);
				}
			}

			gtfsValidationReporter.validateOkCSV(context, GTFS_FARE_ATTRIBUTES_FILE);

			if (parser == null) { // importer.getFareAttributeById() fails for any other reason
				gtfsValidationReporter.throwUnknownError(context, new Exception("Cannot instantiate FareAttributeById class"), GTFS_FARE_ATTRIBUTES_FILE);
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
			for (GtfsFareAttribute bean : parser) {
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

		for (GtfsFareAttribute gtfsFareAttribute : importer.getFareAttributeById()) {
			GtfsImportParameters configuration = (GtfsImportParameters) context.get(CONFIGURATION);
			String id = ObjectIdUtil.composeNeptuneObjectId(configuration.getObjectIdPrefix(), FareAttribute.FARE_KEY, gtfsFareAttribute.getFareId());
			FareAttribute fareAttribute = ObjectFactory.getFareAttribute(referential, id);

			String agencyId = ObjectIdUtil.composeNeptuneObjectId(configuration.getObjectIdPrefix(), FareAttribute.AGENCY_KEY, gtfsFareAttribute.getAgencyId());
			Agency agency = ObjectFactory.getAgency(referential, agencyId);
			convert(context, gtfsFareAttribute, fareAttribute, agency);
		}
	}

	protected void convert(Context context, GtfsFareAttribute gtfsFareAttribute, FareAttribute fareAttribute, Agency agency) throws Exception {
		GtfsImportParameters configuration = (GtfsImportParameters) context.get(CONFIGURATION);

		agency.setName(configuration.getReferentialName());
		fareAttribute.setAgency(agency);

		fareAttribute.setPrice(gtfsFareAttribute.getPrice());
		fareAttribute.setCurrencyType(gtfsFareAttribute.getCurrencyType());
		fareAttribute.setPaymentMethod(FareAttribute.PaymentMethodType.valueOf(String.valueOf(gtfsFareAttribute.getPaymentMethod())));

		if (gtfsFareAttribute.getTransfers() != null) {
			fareAttribute.setTransfers(FareAttribute.AttributeTransfersType.valueOf(String.valueOf(gtfsFareAttribute.getTransfers())));
		}
		fareAttribute.setTransferDuration(gtfsFareAttribute.getTransferDuration());
	}

}
