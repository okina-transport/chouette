package mobi.chouette.exchange.regtopp.beanio;

import org.beanio.types.TypeConversionException;
import org.beanio.types.TypeHandler;
import java.time.DateTime;
import java.time.LocalDate;
import java.time.ReadableInstant;

import java.time.format.DateTimeFormatter;

public class LocalDateTypeHandler implements TypeHandler {

	@Override
	public String format(Object localDate) {
		DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyMMdd");
		return fmt.print((ReadableInstant) localDate);
	}

	@Override
	public Class<LocalDate> getType() {
		return LocalDate.class;
	}

	@Override
	public Object parse(String localDate) throws TypeConversionException {
		DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyMMdd");

		DateTime dt = fmt.parseDateTime(localDate);
		return dt.toLocalDate();
	}

}
