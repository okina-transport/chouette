package mobi.chouette.exchange.regtopp.beanio;

import org.beanio.types.TypeConversionException;
import org.beanio.types.TypeHandler;
import java.time.Duration;

public class DrivingDurationTypeHandler implements TypeHandler {

	@Override
	public String format(Object minutes) {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public Class<Duration> getType() {
		return Duration.class;
	}

	@Override
	public Object parse(String minutes) throws TypeConversionException {
		if ("999".equals(minutes) || "9999".equals(minutes)) {
			return null;
		} else {
			minutes = minutes.replace(" ", "");
			
			// Minutes to ms
			return Duration.ofMillis(Long.parseLong(minutes) * 1000 * 60);
		}
	}

}
