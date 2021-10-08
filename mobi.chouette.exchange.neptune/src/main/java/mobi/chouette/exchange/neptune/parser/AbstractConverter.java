package mobi.chouette.exchange.neptune.parser;

import mobi.chouette.common.Constant;
import mobi.chouette.exchange.neptune.importer.NeptuneImportParameters;
import org.apache.log4j.Logger;

import java.net.URL;
import java.util.TimeZone;

public abstract class AbstractConverter implements Constant{


	/**
	 * Convert Neptune stop id to StopArea id with correct type (StopPlace / Quay).
	 *
	 * If Neptune id is structured like a full ID (containing : type :) it used as is. If not a new Id is composed.
	 */
	public static String toStopAreaId(NeptuneImportParameters configuration, String type, String id) {
		if (id != null && id.contains(":" + type + ":")) {
			return id;
		}
		return composeObjectId(configuration, type, id);
	}

	public static String composeObjectId(NeptuneImportParameters configuration, String type, String id) {

		if (id == null || id.isEmpty() ) return "";
		
//		if(configuration.isSplitIdOnDot()) {
//			String[] tokens = id.split("\\.");
//			if (tokens.length == 2) {
//				// id should be produced by Chouette
// 				return tokens[0].trim() + ":" + type + ":"+ tokens[1].trim();
//				//return tokens[0].trim().replaceAll("[^a-zA-Z_0-9]", "_") + ":" + type + ":"+ tokens[1].trim().replaceAll("[^a-zA-Z_0-9\\-]", "_");
//			}
//		}

		String[] tokens = id.split(":");
		if(tokens.length == 3){
			id = tokens[2];
		}
		else if(tokens.length == 1){
			id = tokens[0];
		}
		else {
			return "";
		}

		return configuration.getObjectIdPrefix() + ":" + type + ":" + replaceColons(id.trim());
	}

	/**
	 * Replace colons(:) in input string by a special code handled by application (##3A##)
	 * @param inputString
	 * @return the string with colons encoded
	 */

	private static String replaceColons(String inputString){
 		return inputString.replace(":",COLON_REPLACEMENT_CODE);
	}

	public static String toString(URL url) {
		if (url == null)
			return null;
		return url.toString();
	}

	public static String toString(TimeZone tz) {
		if (tz == null)
			return null;
		return tz.getID();
	}

}
