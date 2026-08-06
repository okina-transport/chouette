package mobi.chouette.exchange.gtfs.model.exporter;

import mobi.chouette.exchange.gtfs.model.GtfsTripExtension;
import mobi.chouette.exchange.gtfs.model.importer.Context;
import mobi.chouette.exchange.gtfs.model.importer.GtfsConverter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TripExtensionExporter extends ExporterImpl<GtfsTripExtension> implements GtfsConverter {
	public static final String FILENAME = "trips_extensions.txt";

	public static Converter<String, GtfsTripExtension> CONVERTER = new Converter<String, GtfsTripExtension>() {

		@Override
		public GtfsTripExtension from(Context context, String input) {
			GtfsTripExtension bean = new GtfsTripExtension();
			List<String> values = Tokenizer.tokenize(input);

			int i = 0;
			bean.setTripId(STRING_CONVERTER.from(context, FIELDS.trip_id, values.get(i++), true));
			bean.setRouteId(STRING_CONVERTER.from(context, FIELDS.route_id, values.get(i++), false));
			bean.setContractCompanyId(STRING_CONVERTER.from(context, FIELDS.contract_company_id, values.get(i++), false));
			bean.setExecCompanyId(STRING_CONVERTER.from(context, FIELDS.exec_company_id, values.get(i++), false));
			bean.setIndicReservation(STRING_CONVERTER.from(context, FIELDS.indic_reservation, values.get(i++), false));

			return bean;
		}

		@Override
		public String to(Context context, GtfsTripExtension input) {
			String result = null;
			List<String> values = new ArrayList<String>();
			values.add(STRING_CONVERTER.to(context, FIELDS.trip_id, input.getTripId(), true));
			values.add(STRING_CONVERTER.to(context, FIELDS.route_id, input.getRouteId(), false));
			values.add(STRING_CONVERTER.to(context, FIELDS.contract_company_id, input.getContractCompanyId(), false));
			values.add(STRING_CONVERTER.to(context, FIELDS.exec_company_id, input.getExecCompanyId(), false));
			values.add(STRING_CONVERTER.to(context, FIELDS.indic_reservation, input.getIndicReservation(), false));

			result = Tokenizer.untokenize(values);
			return result;
		}

	};

	static {
		ExporterFactory factory = new DefaultExporterFactory();
		ExporterFactory.factories.put(TripExtensionExporter.class.getName(), factory);
	}

	public TripExtensionExporter(String name) throws IOException {
		super(name);
	}

	@Override
	public void writeHeader() throws IOException {
		write(FIELDS.values());
	}

	@Override
	public void export(GtfsTripExtension bean) throws IOException {
		write(CONVERTER.to(_context, bean));
	}

	public static class DefaultExporterFactory extends ExporterFactory {

		@SuppressWarnings({"unchecked", "rawtypes"})
		@Override
		protected Exporter create(String path) throws IOException {
			return new TripExtensionExporter(path);
		}
	}

	public enum FIELDS {
		trip_id, route_id, contract_company_id, exec_company_id, indic_reservation
	}

}
