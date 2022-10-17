package mobi.chouette.exchange.netexprofile.importer.util;

import mobi.chouette.model.JourneyAtStop;
import org.rutebanken.netex.model.TimetabledPassingTime;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalTime;

public class NetexTimeConversionUtil {

	public static void populatePassingTimeUtc(TimetabledPassingTime passingTime, boolean arrival, JourneyAtStop vj) {
	    if((arrival && vj.getArrivalTime() == null || (!arrival && vj.getDepartureTime() == null))) {
	    	return;
	    }

		int dayOffset = arrival ? vj.getArrivalDayOffset() : vj.getDepartureDayOffset();
		LocalTime localTime = arrival ? vj.getArrivalTime() : vj.getDepartureTime();

		if(arrival) {
			passingTime.setArrivalTime(localTime);
			if(dayOffset != 0) {
				passingTime.setArrivalDayOffset(BigInteger.valueOf(dayOffset));
			}
		} else {
			passingTime.setDepartureTime(localTime);
			if(dayOffset != 0) {
				passingTime.setDepartureDayOffset(BigInteger.valueOf(dayOffset));
			}
		}
	}

	public static void parsePassingTime(TimetabledPassingTime passingTime, boolean arrival, JourneyAtStop jas) {
	    if((arrival && passingTime.getArrivalTime() == null || (!arrival && passingTime.getDepartureTime() == null))) {
	    	return;
	    }
	
	    LocalTime localTime = arrival ? passingTime.getArrivalTime() : passingTime.getDepartureTime();
	    BigInteger dayOffset = arrival? passingTime.getArrivalDayOffset() : passingTime.getDepartureDayOffset();
	    if(dayOffset == null) {
	    	dayOffset = BigInteger.ZERO;
	    }

	    
		if(arrival) {
			jas.setArrivalTime(localTime);

			if(!BigDecimal.ZERO.equals(dayOffset)) {
				jas.setArrivalDayOffset(dayOffset.intValue());
			}
		} else {
			jas.setDepartureTime(localTime);

			if(!BigDecimal.ZERO.equals(dayOffset)) {
				jas.setDepartureDayOffset(dayOffset.intValue());
			}
		}
	}

}
