package mobi.chouette.exchange.gtfs.parser;

import mobi.chouette.common.Constant;

import java.net.URL;
import java.util.TimeZone;

public abstract class AbstractConverter implements Constant{

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
