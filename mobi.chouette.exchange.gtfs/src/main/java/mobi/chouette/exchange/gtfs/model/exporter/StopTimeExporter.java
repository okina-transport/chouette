package mobi.chouette.exchange.gtfs.model.exporter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import mobi.chouette.exchange.gtfs.model.GtfsStopTime;
import mobi.chouette.exchange.gtfs.model.GtfsStopTime.DropOffType;
import mobi.chouette.exchange.gtfs.model.GtfsStopTime.PickupType;
import mobi.chouette.exchange.gtfs.model.importer.Context;
import mobi.chouette.exchange.gtfs.model.importer.GtfsConverter;

public class StopTimeExporter extends ExporterImpl<GtfsStopTime> implements GtfsConverter {

	public static enum FIELDS {
		trip_id, arrival_time, departure_time, stop_id, stop_sequence, stop_headsign, pickup_type, drop_off_type, shape_dist_traveled;
	};

	public static final String FILENAME = "stop_times.txt";

	public StopTimeExporter(String name) throws IOException {
		super(name);
	}

	@Override
	public void writeHeader() throws IOException {
		write(Arrays.stream(FIELDS.values())
					.filter(field->!field.equals(FIELDS.shape_dist_traveled))
					.toArray(FIELDS[]::new));
	}

	@Override
	public void export(GtfsStopTime bean) throws IOException {
		write(CONVERTER.to(_context, bean));
	}

	public static Converter<String, GtfsStopTime> CONVERTER = new Converter<String, GtfsStopTime>() {

		@Override
		public GtfsStopTime from(Context context, String input) {
			GtfsStopTime bean = new GtfsStopTime();
			List<String> values = Tokenizer.tokenize(input);

			int i = 0;
			bean.setTripId(STRING_CONVERTER.from(context, FIELDS.trip_id, values.get(i++), true));
			bean.setStopId(STRING_CONVERTER.from(context, FIELDS.stop_id, values.get(i++), true));
			bean.setStopSequence(INTEGER_CONVERTER.from(context, FIELDS.stop_sequence, values.get(i++), true));
			bean.setArrivalTime(GTFSTIME_CONVERTER.from(context, FIELDS.arrival_time, values.get(i++), true));
			bean.setDepartureTime(GTFSTIME_CONVERTER.from(context, FIELDS.departure_time, values.get(i++), true));
			bean.setPickupType(PICKUP_CONVERTER.from(context, FIELDS.pickup_type, values.get(i++),
					PickupType.Scheduled, false));
			bean.setDropOffType(DROPOFFTYPE_CONVERTER.from(context, FIELDS.drop_off_type, values.get(i++),
					DropOffType.Scheduled, false));
 			bean.setShapeDistTraveled(FLOAT_CONVERTER.from(context, FIELDS.shape_dist_traveled, values.get(i++), false));
			bean.setStopHeadsign(STRING_CONVERTER.from(context, FIELDS.stop_headsign, values.get(i++), false));

			return bean;
		}

		@Override
		public String to(Context context, GtfsStopTime input) {
			String result = null;
			List<String> values = new ArrayList<String>();
			values.add(STRING_CONVERTER.to(context, FIELDS.trip_id, input.getTripId(), true));
			values.add(GTFSTIME_CONVERTER.to(context, FIELDS.arrival_time, input.getArrivalTime(), true));
			values.add(GTFSTIME_CONVERTER.to(context, FIELDS.departure_time, input.getDepartureTime(), true));
			values.add(STRING_CONVERTER.to(context, FIELDS.stop_id, input.getStopId(), true));
			values.add(INTEGER_CONVERTER.to(context, FIELDS.stop_sequence, input.getStopSequence() + 1, true)); // bougé ici car peut-être avant et on peut re avoir 0
			values.add(STRING_CONVERTER.to(context, FIELDS.stop_headsign, input.getStopHeadsign(), false));
			values.add(PICKUP_CONVERTER.to(context, FIELDS.pickup_type, input.getPickupType(), false));
			values.add(DROPOFFTYPE_CONVERTER.to(context, FIELDS.drop_off_type, input.getDropOffType(), false));

			result = Tokenizer.untokenize(values);
			return result;
		}

	};

	public static class DefaultExporterFactory extends ExporterFactory {

		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		protected Exporter create(String path) throws IOException {
			return new StopTimeExporter(path);
		}
	}

	static {
		ExporterFactory factory = new DefaultExporterFactory();
		ExporterFactory.factories.put(StopTimeExporter.class.getName(), factory);
	}

}
