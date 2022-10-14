package mobi.chouette.exchange.gtfs.parser;

import java.net.URL;
import java.util.TimeZone;

import mobi.chouette.common.Constant;
import mobi.chouette.exchange.gtfs.importer.GtfsImportParameters;
import mobi.chouette.exchange.gtfs.model.GtfsTime;

import org.joda.time.LocalTime;

public abstract class AbstractConverter implements Constant{


	public static String extractOriginalId(String chouetteObjectId) {
		return chouetteObjectId.split(":")[2];
	}

	/**
	 * @param source
	 * @return
	 */
	public static String getNonEmptyTrimedString(String source) {
		if (source == null)
			return null;
		String target = source.trim();
		return (target.length() == 0 ? null : target);
	}

	/**
	 * @param gtfsTime
	 * @return
	 */
	public static LocalTime getTime(GtfsTime gtfsTime) {
		if (gtfsTime == null)
			return null;

		return gtfsTime.getTime();
	}

	/**
	 * Convert GTFS stop id to StopArea id with correct type (StopPlace / Quay).
	 *
	 * If gtfs id is structured like a full ID (containing : type :) it used as is. If not a new Id is composed.
	 */
	public static String toStopAreaId(GtfsImportParameters configuration, String type, String id) {
		if (id != null && id.contains(":" + type + ":")) {
			return id;
		}
		return composeObjectId(configuration, type, id);
	}

	public static String composeObjectId(GtfsImportParameters configuration, String type, String id) {
		if (id == null || id.isEmpty() ) return "";
		
		if(configuration.isSplitIdOnDot()) {
			String[] tokens = id.split("\\.");
			if (tokens.length == 2) {
 				return tokens[0].trim() + ":" + type + ":"+ tokens[1].trim();
			}
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
