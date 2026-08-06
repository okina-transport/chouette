package mobi.chouette.exchange.gtfs.model.exporter;

import mobi.chouette.exchange.gtfs.model.GtfsTripCompany;
import mobi.chouette.exchange.gtfs.model.importer.Context;
import mobi.chouette.exchange.gtfs.model.importer.GtfsConverter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TripCompanyExporter extends ExporterImpl<GtfsTripCompany> implements GtfsConverter {
	public static final String FILENAME = "companies.txt";

	public static Converter<String, GtfsTripCompany> CONVERTER = new Converter<String, GtfsTripCompany>() {

		@Override
		public GtfsTripCompany from(Context context, String input) {
			GtfsTripCompany bean = new GtfsTripCompany();
			List<String> values = Tokenizer.tokenize(input);

			int i = 0;
			bean.setCompanyId(STRING_CONVERTER.from(context, FIELDS.company_id, values.get(i++), true));
			bean.setCompanyName(STRING_CONVERTER.from(context, FIELDS.company_name, values.get(i++), false));
			bean.setCompanyAddress(STRING_CONVERTER.from(context, FIELDS.company_address, values.get(i++), false));
			bean.setCompanyZipcode(STRING_CONVERTER.from(context, FIELDS.company_zipcode, values.get(i++), false));
			bean.setCompanyCity(STRING_CONVERTER.from(context, FIELDS.company_city, values.get(i++), false));
			bean.setCompanyPhone(STRING_CONVERTER.from(context, FIELDS.company_phone, values.get(i++), false));
			bean.setCompanyEmail(STRING_CONVERTER.from(context, FIELDS.company_email, values.get(i++), false));

			return bean;
		}

		@Override
		public String to(Context context, GtfsTripCompany input) {
			String result = null;
			List<String> values = new ArrayList<String>();
			values.add(STRING_CONVERTER.to(context, FIELDS.company_id, input.getCompanyId(), true));
			values.add(STRING_CONVERTER.to(context, FIELDS.company_name, input.getCompanyName(), false));
			values.add(STRING_CONVERTER.to(context, FIELDS.company_address, input.getCompanyAddress(), false));
			values.add(STRING_CONVERTER.to(context, FIELDS.company_zipcode, input.getCompanyZipcode(), false));
			values.add(STRING_CONVERTER.to(context, FIELDS.company_city, input.getCompanyCity(), false));
			values.add(STRING_CONVERTER.to(context, FIELDS.company_phone, input.getCompanyPhone(), false));
			values.add(STRING_CONVERTER.to(context, FIELDS.company_email, input.getCompanyEmail(), false));

			result = Tokenizer.untokenize(values);
			return result;
		}

	};

	static {
		ExporterFactory factory = new DefaultExporterFactory();
		ExporterFactory.factories.put(TripCompanyExporter.class.getName(), factory);
	}

	public TripCompanyExporter(String name) throws IOException {
		super(name);
	}

	@Override
	public void writeHeader() throws IOException {
		write(FIELDS.values());
	}

	@Override
	public void export(GtfsTripCompany bean) throws IOException {
		write(CONVERTER.to(_context, bean));
	}

	public static class DefaultExporterFactory extends ExporterFactory {

		@SuppressWarnings({"unchecked", "rawtypes"})
		@Override
		protected Exporter create(String path) throws IOException {
			return new TripCompanyExporter(path);
		}
	}

	public enum FIELDS {
		company_id, company_name, company_address, company_zipcode, company_city, company_phone, company_email
	}

}
