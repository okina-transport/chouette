package mobi.chouette.common;

import java.time.LocalDate;
import java.time.ZoneId;

import org.apache.log4j.Logger;
import org.joda.time.DateTimeConstants;
import org.joda.time.Duration;
import org.joda.time.LocalTime;
import org.joda.time.Seconds;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

public class TimeUtil {

    private static final Logger logger = Logger.getLogger(TimeUtil.class);

    public static Duration subtract(LocalTime thisDeparture, LocalTime firstDeparture) {
        int seconds;
        // Assuming journeys last no more than 24 hours
        if (firstDeparture.isBefore(thisDeparture)) {
            seconds = Seconds.secondsBetween(firstDeparture, thisDeparture).getSeconds();
        } else {
            seconds = DateTimeConstants.SECONDS_PER_DAY - Seconds.secondsBetween(thisDeparture, firstDeparture).getSeconds();
        }

        return Duration.standardSeconds(seconds);
    }

    public static java.time.LocalTime toLocalTimeFromJoda(org.joda.time.LocalTime jodaTime) {
        if (jodaTime == null) {
            return null;
        }
        return java.time.LocalTime.of(jodaTime.getHourOfDay(), jodaTime.getMinuteOfHour(), jodaTime.getSecondOfMinute());
    }

    public static org.joda.time.LocalTime toJodaLocalTime(java.time.LocalTime localTime) {
        if (localTime == null) {
            return null;
        }
        return new org.joda.time.LocalTime(localTime.getHour(), localTime.getMinute(), localTime.getSecond());
    }

    public static org.joda.time.Duration toJodaDuration(javax.xml.datatype.Duration duration) {
        if (duration == null) {
            return null;
        }


        Duration result = org.joda.time.Duration.parse(duration.toString());
        return org.joda.time.Duration.millis(duration.getSeconds() * 1000);
    }

    public static javax.xml.datatype.Duration toDurationFromJodaDuration(Duration jodaDuration) {
        if (jodaDuration == null) {
            return null;
        }

        try {
            return DatatypeFactory.newInstance().newDuration(jodaDuration.toString());

        } catch (DatatypeConfigurationException e) {
            logger.error("can t convert duration");
            return null;
        }

    }

    public static org.joda.time.LocalDate toJodaLocalDate(LocalDate localDate) {
        if (localDate == null) {
            return null;
        }
        return new org.joda.time.LocalDate(localDate.getYear(), localDate.getMonthValue(), localDate.getDayOfMonth());
    }

    public static LocalDate toLocalDateFromJoda(org.joda.time.LocalDate jodaDate) {
        if (jodaDate == null) {
            return null;
        }
        return LocalDate.of(jodaDate.getYear(), jodaDate.getMonthOfYear(), jodaDate.getDayOfMonth());
    }

    public static org.joda.time.LocalDateTime toJodaLocalDateTime(java.time.LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return new org.joda.time.LocalDateTime(localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
    }
    /**
     * Convert localDateTime to joda LocalDate, ignoring time.
     *
     * This is a bit shady, but necessary as long as incoming data, while semantically a LocalDate, is represented as xs:dateTime.
     */
    public static org.joda.time.LocalDate toJodaLocalDateIgnoreTime(java.time.LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }

        return new org.joda.time.LocalDate(localDateTime.getYear(),localDateTime.getMonthValue(),localDateTime.getDayOfMonth());
    }

}
