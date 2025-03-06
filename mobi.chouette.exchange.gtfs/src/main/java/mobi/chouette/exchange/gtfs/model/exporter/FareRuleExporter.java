package mobi.chouette.exchange.gtfs.model.exporter;

import mobi.chouette.exchange.gtfs.model.GtfsFareRule;
import mobi.chouette.exchange.gtfs.model.importer.Context;
import mobi.chouette.exchange.gtfs.model.importer.GtfsConverter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FareRuleExporter extends ExporterImpl<GtfsFareRule> implements
        GtfsConverter {
    public static final String FILENAME = "fare_rules.txt";

    public static Converter<String, GtfsFareRule> CONVERTER = new Converter<String, GtfsFareRule>() {

        @Override
        public GtfsFareRule from(Context context, String input) {
            GtfsFareRule bean = new GtfsFareRule();
            List<String> values = Tokenizer.tokenize(input);

            int i = 0;
            bean.setFareId(STRING_CONVERTER.from(context, FIELDS.fare_id, values.get(i++), true));
            bean.setRouteId(STRING_CONVERTER.from(context, FIELDS.route_id, values.get(i++), false));
            bean.setOriginId(INTEGER_CONVERTER.from(context, FIELDS.origin_id, values.get(i++), false));
            bean.setDestinationId(INTEGER_CONVERTER.from(context, FIELDS.destination_id, values.get(i++), false));
            bean.setContainsId(INTEGER_CONVERTER.from(context, FIELDS.contains_id, values.get(i++), false));

            return bean;
        }

        @Override
        public String to(Context context, GtfsFareRule input) {
            String result = null;
            List<String> values = new ArrayList<String>();
            values.add(STRING_CONVERTER.to(context, FIELDS.fare_id, input.getFareId(), true));
            values.add(STRING_CONVERTER.to(context, FIELDS.route_id, input.getRouteId(), false));
            values.add(INTEGER_CONVERTER.to(context, FIELDS.origin_id, input.getOriginId(), false));
            values.add(INTEGER_CONVERTER.to(context, FIELDS.destination_id, input.getDestinationId(), false));
            values.add(INTEGER_CONVERTER.to(context, FIELDS.contains_id, input.getContainsId(), false));

            result = Tokenizer.untokenize(values);
            return result;
        }

    };

    static {
        ExporterFactory factory = new DefaultExporterFactory();
        ExporterFactory.factories
                .put(FareRuleExporter.class.getName(), factory);
    }

    public FareRuleExporter(String name) throws IOException {
        super(name);
    }

    @Override
    public void writeHeader() throws IOException {
        write(FIELDS.values());
    }

    @Override
    public void export(GtfsFareRule bean) throws IOException {
        write(CONVERTER.to(_context, bean));
    }

    public enum FIELDS {
        fare_id, route_id, origin_id, destination_id, contains_id
    }

    public static class DefaultExporterFactory extends ExporterFactory {

        @SuppressWarnings({"unchecked", "rawtypes"})
        @Override
        protected Exporter create(String path) throws IOException {
            return new FareRuleExporter(path);
        }
    }

}
