package mobi.chouette.exchange.importer;

import lombok.extern.log4j.Log4j;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;
import java.text.ParseException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static mobi.chouette.common.TimeUtil.toLocalDateTime;

@Log4j
public class ParserUtils {

	private static DatatypeFactory factory = null;

	static {
		try {
			factory = DatatypeFactory.newInstance();
		} catch (DatatypeConfigurationException e) {
			log.error("DatatypeConfigurationException", e);
		}
	}

	public static String getText(String value) {
		String result = null;
		if (value != null) {
			result = value.trim();
			result = (result.isEmpty() ? null : result);
		}
		return result;
	}

	public static Integer getInt(String value) {
		Integer result = null;
		if (value != null) {
			result = Integer.valueOf(value);
		}
		return result;
	}

	public static Long getLong(String value) {
		Long result = null;
		if (value != null) {
			result = Long.valueOf(value);
		}
		return result;
	}

	public static Boolean getBoolean(String value) {
		Boolean result = null;
		if (value != null) {
			result = Boolean.valueOf(value);
		}
		return result;
	}

	public static <T extends Enum<T>> T getEnum(Class<T> type, String value) {
		T result = null;
		if (value != null) {
			try {
				result = Enum.valueOf(type, value);
			} catch (Exception ignored) {
				log.warn("Failed to getEnum for value : " + value);
			}
		}
		return result;
	}
	public static java.time.Duration getDuration(String value) {
		java.time.Duration result = null;

		if (value != null) {
			try {
				result = java.time.Duration.ofMillis(factory.newDuration(value).getTimeInMillis(new java.util.Date(0)));
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
		return result;
	}


	public static java.time.Duration getDurationFromTime(String value) throws DateTimeParseException {
		java.time.Duration result = null;

		if (value != null) {
			LocalTime time = getLocalTime(value);
			result = Duration.ofSeconds(ChronoUnit.SECONDS.between(LocalTime.of(0,0), time));
		}
		return result;
	}


	public static LocalTime getLocalTime(String value) throws DateTimeParseException {
		LocalTime result = null;

		if (value != null) {
			result = LocalTime.parse(value, DateTimeFormatter.ofPattern("HH:mm:ss"));
		}
		return result;
	}

	public static LocalDate getLocalDate(String value) throws DateTimeParseException {
		LocalDate result = null;

		if (value != null) {
			result = LocalDate.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		}
		return result;

	}

	public static LocalDate getDate(DateTimeFormatter format, String value)
			throws DateTimeParseException {
		LocalDate result = null;

		if (value != null) {
			result = LocalDate.parse(value, format);
		}
		return result;
	}

	public static LocalDate getDate(String value) throws DateTimeParseException {
		DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(
				"yyyy-MM-dd'T'HH:mm:ss'Z'");
		return getDate(dateFormat, value);
	}

	public static LocalDateTime getLocalDateTime(String value) throws DateTimeParseException {
		LocalDateTime result = null;

		if (value != null) {
			XMLGregorianCalendar calendar = factory
					.newXMLGregorianCalendar(value);
			result = toLocalDateTime(calendar);
		}
		return result;
	}

	public static BigDecimal getBigDecimal(String value) {
		BigDecimal result = null;
		if (value != null) {
			try {
				result = BigDecimal.valueOf(Double.parseDouble(value));
			} catch (Exception ignored) {
				log.warn("Failed to getBigDecimal for value : " + value);
			}
		}
		return result;
	}

	public static BigDecimal getBigDecimal(String value, String pattern) {
		BigDecimal result = null;

		if (value != null) {
			Matcher m = Pattern.compile(pattern).matcher(value.trim());
			if (m.matches()) {
				result = getBigDecimal(m.group(1));

			}
		}
		return result;
	}

	public static BigDecimal getX(String value) {
		return ParserUtils.getBigDecimal(value, "([\\d\\.]+) [\\d\\.]+");
	}

	public static BigDecimal getY(String value) {
		return ParserUtils.getBigDecimal(value, "[\\d\\.]+ ([\\d\\.]+)");
	}

	public static String objectIdPrefix(String objectId) {
		if (objectIdArray(objectId).length > 2) {
			return objectIdArray(objectId)[0].trim();
		} else
			return "";
	}

	public static String objectIdSuffix(String objectId) {
		if (objectIdArray(objectId).length > 2)
			return objectIdArray(objectId)[2].trim();
		else
			return "";
	}

	private static String[] objectIdArray(String objectId) {
		return objectId.split(":");
	}

}
