package mobi.chouette.exchange.regtopp.parser;

import static mobi.chouette.common.Constant.CONFIGURATION;
import static mobi.chouette.common.Constant.MAIN_VALIDATION_REPORT;
import static mobi.chouette.common.Constant.PARSER;
import static mobi.chouette.common.Constant.REFERENTIAL;
import static mobi.chouette.exchange.regtopp.validation.Constant.REGTOPP_FILE_HPL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.exchange.importer.Parser;
import mobi.chouette.exchange.importer.ParserFactory;
import mobi.chouette.exchange.importer.Validator;
import mobi.chouette.exchange.regtopp.RegtoppConstant;
import mobi.chouette.exchange.regtopp.importer.RegtoppImportParameters;
import mobi.chouette.exchange.regtopp.importer.version.Regtopp12NovusVersionHandler;
import mobi.chouette.exchange.regtopp.importer.version.VersionHandler;
import mobi.chouette.exchange.regtopp.model.importer.parser.FileParserValidationError;
import mobi.chouette.exchange.regtopp.model.importer.parser.RegtoppException;
import mobi.chouette.exchange.regtopp.model.importer.parser.RegtoppImporter;
import mobi.chouette.exchange.regtopp.model.importer.parser.index.Index;
import mobi.chouette.exchange.regtopp.model.v12.RegtoppStopHPL;
import mobi.chouette.exchange.regtopp.validation.RegtoppValidationReporter;
import mobi.chouette.exchange.validation.report.CheckPoint;
import mobi.chouette.exchange.validation.report.ValidationReport;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.type.ChouetteAreaEnum;
import mobi.chouette.model.type.LongLatTypeEnum;
import mobi.chouette.model.util.Coordinate;
import mobi.chouette.model.util.CoordinateUtil;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.Referential;

@Log4j
public class RegtoppStopParser implements Parser, Validator {

	@Override
	public void parse(Context context) throws Exception {

		Referential referential = (Referential) context.get(REFERENTIAL);
		RegtoppImporter importer = (RegtoppImporter) context.get(PARSER);
		RegtoppImportParameters configuration = (RegtoppImportParameters) context.get(CONFIGURATION);
		VersionHandler versionHandler = (VersionHandler)context.get(RegtoppConstant.VERSION_HANDLER);
		
		for (RegtoppStopHPL stop : importer.getStopById()) {
			
			String objectId = AbstractConverter.composeObjectId(configuration.getObjectIdPrefix(), StopArea.STOPAREA_KEY, stop.getFullStopId());
			StopArea stopArea = ObjectFactory.getStopArea(referential, objectId);

			//stopArea.setRegistrationNumber(stop.getFullStopId());
			
			Coordinate wgs84Coordinate = CoordinateUtil.transform(Coordinate.UTM_32N, Coordinate.WGS84, new Coordinate(stop.getX(), stop.getY()));

			stopArea.setLongitude(wgs84Coordinate.getY());
			stopArea.setLatitude(wgs84Coordinate.getX());
			stopArea.setLongLatType(LongLatTypeEnum.WGS84);

			// UTM coordinates
			stopArea.setX(stop.getX());
			stopArea.setY(stop.getY());
			stopArea.setProjectionType("UTM");

			stopArea.setName(stop.getFullName());

			// TODO set correct, some stops are in other countries
			// Could use a reverse geocoder for this, would obtain address etc etc.
			//stopArea.setCountryCode("NO");

			stopArea.setAreaType(ChouetteAreaEnum.BoardingPosition);
		}
		
		
		if(versionHandler instanceof Regtopp12NovusVersionHandler) {
			// Build parent stop area (commercial stop point)

			// Group boarding positions by original stopId
			Map<String, List<StopArea>> boardingPositionsByStopArea = new HashMap<String,List<StopArea>>();
			for(StopArea sa : referential.getStopAreas().values()) {
				String commercialStopAreaId = AbstractConverter.extractOriginalId(sa.getObjectId()).substring(0,8);
				List<StopArea> list = boardingPositionsByStopArea.get(commercialStopAreaId);
				if(list == null) {
					list = new ArrayList<StopArea>();
					boardingPositionsByStopArea.put(commercialStopAreaId, list);
				}
				list.add(sa);
			}
			
			for(String commercialStopAreaId : boardingPositionsByStopArea.keySet()) {
				List<StopArea> list = boardingPositionsByStopArea.get(commercialStopAreaId);
				if(list.size() > 1) {
					// Create parent stopArea
					String objectId = AbstractConverter.composeObjectId(configuration.getObjectIdPrefix(), StopArea.STOPAREA_KEY, commercialStopAreaId);
					StopArea stopArea = ObjectFactory.getStopArea(referential, objectId);
					stopArea.setName(list.get(0).getName()); // TODO using name of first stoppoint, should be identical for all boarding positions according to spec
					stopArea.setAreaType(ChouetteAreaEnum.StopPlace);
					
					for(StopArea bp : list) {
						bp.setParent(stopArea);
					}
					
					// Calculate center coordinate
					// TODO currently using only first boarding position stop coordinates. Need to be centered
					stopArea.setLongitude(list.get(0).getLongitude());
					stopArea.setLatitude(list.get(0).getLatitude());
					stopArea.setLongLatType(list.get(0).getLongLatType());
					
				}
			}
			
		}
	}

	@Override
	public void validate(Context context) throws Exception {

		// Konsistenssjekker, kjøres før parse-metode

		RegtoppImporter importer = (RegtoppImporter) context.get(PARSER);
		RegtoppValidationReporter validationReporter = (RegtoppValidationReporter) context.get(RegtoppConstant.REGTOPP_REPORTER);
		validationReporter.getExceptions().clear();

		ValidationReport mainReporter = (ValidationReport) context.get(MAIN_VALIDATION_REPORT);

		mainReporter.getCheckPoints().add(new CheckPoint(REGTOPP_FILE_HPL, CheckPoint.RESULT.UNCHECK, CheckPoint.SEVERITY.ERROR));

		// stops.txt
		if (importer.hasHPLImporter()) { // the file "*.hpl" exists ?
			validationReporter.reportSuccess(context, REGTOPP_FILE_HPL, RegtoppStopHPL.FILE_EXTENSION);

			Index<RegtoppStopHPL> index = importer.getStopById();

			if (index.getLength() == 0) {
				FileParserValidationError fileError = new FileParserValidationError(RegtoppStopHPL.FILE_EXTENSION, 0, null,
						RegtoppException.ERROR.FILE_WITH_NO_ENTRY, null, "Empty file");
				validationReporter.reportError(context, new RegtoppException(fileError), RegtoppStopHPL.FILE_EXTENSION);
			}

			for (RegtoppStopHPL bean : index) {
				try {
					// Call index validator
					index.validate(bean, importer);
				} catch (Exception ex) {
					if (ex instanceof RegtoppException) {
						validationReporter.reportError(context, (RegtoppException) ex, RegtoppStopHPL.FILE_EXTENSION);
					} else {
						validationReporter.throwUnknownError(context, ex, RegtoppStopHPL.FILE_EXTENSION);
					}
				}
			}
		}
	}

	static {
		ParserFactory.register(RegtoppStopParser.class.getName(), new ParserFactory() {
			@Override
			protected Parser create() {
				return new RegtoppStopParser();
			}
		});
	}

}
