package mobi.chouette.exchange.importer.utils;

import lombok.extern.log4j.Log4j;import mobi.chouette.model.*;

@Log4j
public class IdGeneration {

	public static String translateObjectId(String original, String newType) {
		String[] splittedParts = original.split(":");
		if (splittedParts.length == 3) {
			return original.replaceAll(splittedParts[1], newType);
		} else {
			log.warn("Could not transform identifier " + original + " to type " + newType + " as it does not conform to id standard (XXX:Type:YYY)");
			return original;
		}
	}

	public static String translateType(NeptuneObject v) {
		if (v instanceof VehicleJourney) {
			return "VehicleJourney";
		}
		return null;
	}

	public static void populateObjectId(NeptuneIdentifiedObject source) {
		if (source == null) {
			log.error("Cannot set id since either source is null");
			return;
		}
		String newType = translateType(source);
		if (newType != null) {
			source.setObjectId(translateObjectId(source.getObjectId(), newType));
		} else {
			source.setObjectId(source.getObjectId());
		}
	}
}
