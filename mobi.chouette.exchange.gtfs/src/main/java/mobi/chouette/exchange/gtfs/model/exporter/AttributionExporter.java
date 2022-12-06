package mobi.chouette.exchange.gtfs.model.exporter;

import mobi.chouette.exchange.gtfs.model.GtfsAgency;
import mobi.chouette.exchange.gtfs.model.GtfsAttribution;
import mobi.chouette.exchange.gtfs.model.importer.Context;
import mobi.chouette.exchange.gtfs.model.importer.GtfsConverter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AttributionExporter extends ExporterImpl<GtfsAttribution> implements
		GtfsConverter {

	public static enum FIELDS {
		attribution_id, agency_id, route_id, trip_id, organization_name, is_producer, is_operator, is_authority, attribution_url, attribution_email, attribution_phone;
	};

	public static final String FILENAME = "attribution.txt";

	public AttributionExporter(String path) throws IOException {
		super(path);
	}

	@Override
	public void writeHeader() throws IOException {
		write(FIELDS.values());
	}

	@Override
	public void export(GtfsAttribution bean) throws IOException {
		write(CONVERTER.to(_context, bean));
	}

	public static Converter<String, GtfsAttribution> CONVERTER = new Converter<String, GtfsAttribution>() {

		@Override
		public GtfsAttribution from(Context context, String input) {
			GtfsAttribution bean = new GtfsAttribution();
			List<String> values = Tokenizer.tokenize(input);

			int i = 0;
			bean.setAttributionId(STRING_CONVERTER.from(context, FIELDS.attribution_id,
					values.get(i++), false));
			bean.setAgencyId(STRING_CONVERTER.from(context,
					FIELDS.agency_id, values.get(i++), false));
			bean.setRouteId(STRING_CONVERTER.from(context, FIELDS.route_id,
					values.get(i++), false));
			bean.setTripId(STRING_CONVERTER.from(context,
					FIELDS.trip_id, values.get(i++), false));
			bean.setOrganizationName(STRING_CONVERTER.from(context,
					FIELDS.organization_name,
					values.get(i++), true));
			bean.setIsProducer(INTEGER_CONVERTER.from(context,
					FIELDS.is_producer, values.get(i++), false));
			bean.setIsOperator(INTEGER_CONVERTER.from(context,
					FIELDS.is_operator, values.get(i++), false));
			bean.setIsAuthority(INTEGER_CONVERTER.from(context,
					FIELDS.is_authority, values.get(i++), false));
			bean.setAttributionUrl(STRING_CONVERTER.from(context,
					FIELDS.attribution_url, values.get(i++), false));
			bean.setAttributionEmail(STRING_CONVERTER.from(context,
					FIELDS.attribution_email, values.get(i++), false));
			bean.setAttributionPhone(STRING_CONVERTER.from(context,
					FIELDS.attribution_phone, values.get(i), false));
			return bean;
		}

		@Override
		public String to(Context context, GtfsAttribution input) {
			String result = null;
			List<String> values = new ArrayList<String>();
			values.add(STRING_CONVERTER.to(context, FIELDS.attribution_id,
					input.getAttributionId(), false));
			values.add(STRING_CONVERTER.to(context, FIELDS.agency_id,
					input.getAgencyId(), false));
			values.add(STRING_CONVERTER.to(context, FIELDS.route_id,
					input.getRouteId(), false));
			values.add(STRING_CONVERTER.to(context, FIELDS.trip_id,
					input.getTripId(), false));
			values.add(STRING_CONVERTER.to(context, FIELDS.organization_name,
					input.getOrganizationName(), true));
			values.add(INTEGER_CONVERTER.to(context, FIELDS.is_producer,
					input.getIsProducer(), false));
			values.add(INTEGER_CONVERTER.to(context, FIELDS.is_operator,
					input.getIsOperator(), false));
			values.add(INTEGER_CONVERTER.to(context, FIELDS.is_authority,
					input.getIsAuthority(), false));
			values.add(STRING_CONVERTER.to(context, FIELDS.attribution_url,
					input.getAttributionUrl(), false));
			values.add(STRING_CONVERTER.to(context, FIELDS.attribution_email,
					input.getAttributionEmail(), false));
			values.add(STRING_CONVERTER.to(context, FIELDS.attribution_phone,
					input.getAttributionPhone(), false));
			result = Tokenizer.untokenize(values);
			return result;
		}

	};

	public static class DefaultExporterFactory extends ExporterFactory {

		@Override
		protected Exporter<GtfsAttribution> create(String path) throws IOException {
			return new AttributionExporter(path);
		}
	}

	static {
		ExporterFactory factory = new DefaultExporterFactory();
		ExporterFactory.factories.put(AttributionExporter.class.getName(), factory);
	}

}