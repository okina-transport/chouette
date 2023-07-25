package mobi.chouette.exchange.gtfs.parser;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.exchange.gtfs.importer.GtfsImportParameters;
import mobi.chouette.exchange.gtfs.model.GtfsStop;
import mobi.chouette.exchange.gtfs.model.GtfsStop.LocationType;
import mobi.chouette.exchange.gtfs.model.GtfsStop.WheelchairBoardingType;
import mobi.chouette.exchange.gtfs.model.importer.GtfsException;
import mobi.chouette.exchange.gtfs.model.importer.GtfsImporter;
import mobi.chouette.exchange.gtfs.model.importer.Index;
import mobi.chouette.exchange.gtfs.validation.Constant;
import mobi.chouette.exchange.gtfs.validation.GtfsValidationReporter;
import mobi.chouette.exchange.importer.Parser;
import mobi.chouette.exchange.importer.ParserFactory;
import mobi.chouette.exchange.importer.Validator;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.type.ChouetteAreaEnum;
import mobi.chouette.model.type.LongLatTypeEnum;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.Referential;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log4j
public class GtfsStopParser implements Parser, Validator, Constant {

	private String railUICregexp;
	
	@Override
	public void validate(Context context) throws Exception {
		GtfsImporter importer = (GtfsImporter) context.get(PARSER);
		GtfsValidationReporter gtfsValidationReporter = (GtfsValidationReporter) context.get(GTFS_REPORTER);
		gtfsValidationReporter.getExceptions().clear();
		
		// stops.txt
		// log.info("validating stops");
		if (importer.hasStopImporter()) { // the file "stops.txt" exists ?
			gtfsValidationReporter.reportSuccess(context, GTFS_1_GTFS_Common_1, GTFS_STOPS_FILE);

			Index<GtfsStop> parser = null;
			try { // Read and check the header line of the file "stops.txt"
				parser = importer.getStopById(); 
			} catch (Exception ex ) {
				if (ex instanceof GtfsException) {
					gtfsValidationReporter.reportError(context, (GtfsException)ex, GTFS_STOPS_FILE);
				} else {
					gtfsValidationReporter.throwUnknownError(context, ex, GTFS_STOPS_FILE);
				}
			}

			gtfsValidationReporter.validateOkCSV(context, GTFS_STOPS_FILE);
		
			if (parser == null) { // importer.getStopById() fails for any other reason
				gtfsValidationReporter.throwUnknownError(context, new Exception("Cannot instantiate StopById class"), GTFS_STOPS_FILE);
			} else {
				gtfsValidationReporter.validate(context, GTFS_STOPS_FILE, parser.getOkTests());
				gtfsValidationReporter.validateUnknownError(context);
			}
			
			if (!parser.getErrors().isEmpty()) {
				gtfsValidationReporter.reportErrors(context, parser.getErrors(), GTFS_STOPS_FILE);
				parser.getErrors().clear();
			}
			
			gtfsValidationReporter.validateOKGeneralSyntax(context, GTFS_STOPS_FILE);
		
			if (parser.getLength() == 0) {
				gtfsValidationReporter.reportError(context, new GtfsException(GTFS_STOPS_FILE, 1, null, GtfsException.ERROR.FILE_WITH_NO_ENTRY, null, null), GTFS_STOPS_FILE);
			} else {
				gtfsValidationReporter.validate(context, GTFS_STOPS_FILE, GtfsException.ERROR.FILE_WITH_NO_ENTRY);
			}
		
			GtfsException fatalException = null;
			boolean hasLocationType = false;
			parser.setWithValidation(true);
			for (GtfsStop bean : parser) {
				try {
					if (bean.getLocationType() == null)
						;//bean.setLocationType(LocationType.Stop);
					else
						hasLocationType = true;
					parser.validate(bean, importer);
				} catch (Exception ex) {
					if (ex instanceof GtfsException) {
						gtfsValidationReporter.reportError(context, (GtfsException)ex, GTFS_STOPS_FILE);
					} else {
						gtfsValidationReporter.throwUnknownError(context, ex, GTFS_STOPS_FILE);
					}
				}
				for(GtfsException ex : bean.getErrors()) {
					if (ex.isFatal())
						fatalException = ex;
				}
				gtfsValidationReporter.reportErrors(context, bean.getErrors(), GTFS_STOPS_FILE);
				gtfsValidationReporter.validate(context, GTFS_STOPS_FILE, bean.getOkTests());
			}
			parser.setWithValidation(false);
			if (hasLocationType)
				gtfsValidationReporter.validate(context, GTFS_STOPS_FILE, GtfsException.ERROR.NO_LOCATION_TYPE);
			else
				gtfsValidationReporter.reportError(context, new GtfsException(GTFS_STOPS_FILE, 1, null, GtfsException.ERROR.NO_LOCATION_TYPE, null, null), GTFS_STOPS_FILE);
			if (fatalException != null)
				throw fatalException;
		} else {
			gtfsValidationReporter.reportError(context, new GtfsException(GTFS_STOPS_FILE, 1, null, GtfsException.ERROR.MISSING_FILE, null, null), GTFS_STOPS_FILE);
		}
	}	
	
	@Override
	public void parse(Context context) throws Exception {

		Referential referential = (Referential) context.get(REFERENTIAL);
		GtfsImporter importer = (GtfsImporter) context.get(PARSER);
		GtfsImportParameters configuration = (GtfsImportParameters) context.get(CONFIGURATION);

		if(context.get(RAIL_UIC_REGEXP) != null){
			railUICregexp = (String) context.get(RAIL_UIC_REGEXP);
		}


		for (GtfsStop gtfsStop : importer.getStopById()) {
			handlePrefixes(gtfsStop,configuration);

			if (gtfsStop.getLocationType() != GtfsStop.LocationType.Access) {
				// Since we do not parse Access, only Station and Stop remains.
				String objectId = AbstractConverter.toStopAreaId(configuration,
						gtfsStop.getLocationType() == LocationType.Station ? "StopPlace" : "Quay", gtfsStop.getStopId());

				StopArea stopArea = ObjectFactory.getStopArea(referential, objectId);
				convert(context, gtfsStop, stopArea);
			}
		}
	}


	private void handlePrefixes(GtfsStop gtfsStop, GtfsImportParameters configuration){

		if (gtfsStop.getLocationType() == LocationType.Station){
			gtfsStop.setStopId(gtfsStop.getStopId().replaceFirst("^"+configuration.getCommercialPointIdPrefixToRemove(),"").trim());
		}else{
			gtfsStop.setStopId(gtfsStop.getStopId().replaceFirst("^"+configuration.getQuayIdPrefixToRemove(),"").trim());
			if (gtfsStop.getParentStation() != null){
				gtfsStop.setParentStation(gtfsStop.getParentStation().replaceFirst("^"+configuration.getCommercialPointIdPrefixToRemove(),"").trim());
				checkUniqueIdWithParent(gtfsStop);
			}
		}
	}

	private void checkUniqueIdWithParent(GtfsStop gtfsStop){
		if (gtfsStop.getStopId().equals(gtfsStop.getParentStation())){
			log.error("Error on prefix removal. Duplicate id for parent and child on id :" + gtfsStop.getStopId());
		}
	}
	
	protected void convert(Context context, GtfsStop gtfsStop, StopArea stopArea) {
		Referential referential = (Referential) context.get(REFERENTIAL);
		GtfsImporter importer = (GtfsImporter) context.get(PARSER);
		GtfsImportParameters configuration = (GtfsImportParameters) context.get(CONFIGURATION);

		if (LocationType.Station.equals(gtfsStop.getLocationType()) && configuration.isRailUICprocessing()){
			addRailUICToStopArea(gtfsStop, stopArea);
		}

		stopArea.setLatitude(gtfsStop.getStopLat());
		stopArea.setLongitude(gtfsStop.getStopLon());
		stopArea.setLongLatType(LongLatTypeEnum.WGS84);
		stopArea.setName(AbstractConverter.getNonEmptyTrimedString(gtfsStop.getStopName()));

		stopArea.setUrl(AbstractConverter.toString(gtfsStop.getStopUrl()));
		stopArea.setComment(AbstractConverter.getNonEmptyTrimedString(gtfsStop.getStopDesc()));
		stopArea.setTimeZone(AbstractConverter.toString(gtfsStop.getStopTimezone()));
		stopArea.setFareCode(0);
		stopArea.setOriginalStopId(gtfsStop.getStopId());

		if (gtfsStop.getLocationType() == GtfsStop.LocationType.Station) {
			stopArea.setAreaType(ChouetteAreaEnum.CommercialStopPoint);

			stopArea.setOriginalStopId(stopArea.getOriginalStopId().replaceFirst("^"+configuration.getCommercialPointIdPrefixToRemove(),"").trim());
			if (AbstractConverter.getNonEmptyTrimedString(gtfsStop.getParentStation()) != null) {
				// TODO report
			}
		} else {
			stopArea.setOriginalStopId(stopArea.getOriginalStopId().replaceFirst("^"+configuration.getQuayIdPrefixToRemove(),"").trim());
			if (!importer.getStopById().containsKey(gtfsStop.getParentStation())) {
				// TODO report
			}
			stopArea.setAreaType(ChouetteAreaEnum.BoardingPosition);
			if (gtfsStop.getParentStation() != null && !configuration.isRemoveParentStations()) {
				String parentId = AbstractConverter.toStopAreaId(configuration,
						"StopPlace", gtfsStop.getParentStation());
				StopArea parent = ObjectFactory.getStopArea(referential, parentId);
				stopArea.setParent(parent);
			}
		}

		stopArea.setRegistrationNumber(gtfsStop.getStopCode());

		if(gtfsStop.getWheelchairBoarding() != null){
			if(gtfsStop.getWheelchairBoarding().equals(WheelchairBoardingType.Allowed)){
				stopArea.setMobilityRestrictedSuitable(true);
			}
			if(gtfsStop.getWheelchairBoarding().equals(WheelchairBoardingType.NoAllowed)){
				stopArea.setMobilityRestrictedSuitable(false);
			}
			if(gtfsStop.getWheelchairBoarding().equals(WheelchairBoardingType.NoInformation)){
				stopArea.setMobilityRestrictedSuitable(null);
			}
		}
		else {
			stopArea.setMobilityRestrictedSuitable(null);
		}


		stopArea.setStreetName(gtfsStop.getAddressLine());
		stopArea.setCityName(gtfsStop.getLocality());
		stopArea.setZipCode(gtfsStop.getPostalCode());
		stopArea.setPlatformCode(gtfsStop.getPlatformCode());
		
		if(gtfsStop.getVehicleType() != null) {
			stopArea.setTransportModeName(gtfsStop.getVehicleType().getTransportMode());
			stopArea.setTransportSubMode(gtfsStop.getVehicleType().getSubMode());
		}
		
		stopArea.setFilled(true);

		if(gtfsStop.getZoneId() != null){
			stopArea.setZoneId(AbstractConverter.composeObjectId(configuration, "TariffZone", gtfsStop.getZoneId()));
		}
	}

	/**
	 * Try to identify rail UIC code, using pattern. If the rail UIC is found, it is feeded in the stopArea
	 * @param gtfsStop
	 * 	gtfsStop from stops.txt file
	 * @param stopArea
	 * 	the created stopArea
	 */
	private void addRailUICToStopArea(GtfsStop gtfsStop, StopArea stopArea) {
		Pattern pattern = Pattern.compile(railUICregexp);
		Matcher matcher = pattern.matcher(gtfsStop.getStopId());
		if (matcher.matches()) {
			stopArea.setRailUic(matcher.group(1));
		}
	}

	static {
		ParserFactory.register(GtfsStopParser.class.getName(), new ParserFactory() {
			@Override
			protected Parser create() {
				return new GtfsStopParser();
			}
		});
	}
}
