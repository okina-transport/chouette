package mobi.chouette.exchange.gtfs.model.exporter;

import mobi.chouette.exchange.gtfs.model.GtfsFareAttribute;
import mobi.chouette.exchange.gtfs.model.importer.Context;
import mobi.chouette.exchange.gtfs.model.importer.GtfsConverter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FareAttributeExporter extends ExporterImpl<GtfsFareAttribute> implements
        GtfsConverter {
    public static final String FILENAME = "fare_attributes.txt";

    public static Converter<String, GtfsFareAttribute> CONVERTER = new Converter<String, GtfsFareAttribute>() {

        @Override
        public GtfsFareAttribute from(Context context, String input) {
            GtfsFareAttribute bean = new GtfsFareAttribute();
            List<String> values = Tokenizer.tokenize(input);

            int i = 0;
            bean.setFareId(STRING_CONVERTER.from(context, FIELDS.fare_id, values.get(i++), true));
            bean.setPrice(INTEGER_CONVERTER.from(context, FIELDS.price, values.get(i++), true));
            bean.setCurrencyType(STRING_CONVERTER.from(context, FIELDS.currency_type, values.get(i++), true));
            bean.setPaymentMethod(PAYMENTMETHODTYPE_CONVERTER.from(context, FIELDS.payment_method, values.get(i++), true));
            bean.setTransfers(ATTRIBUTETRANSFERSTYPE_CONVERTER.from(context, FIELDS.transfers, values.get(i++), true));
            bean.setAgencyId(STRING_CONVERTER.from(context, FIELDS.agency_id, values.get(i++), true));
            bean.setTransferDuration(INTEGER_CONVERTER.from(context, FIELDS.transfer_duration, values.get(i++), false));

            return bean;
        }

        @Override
        public String to(Context context, GtfsFareAttribute input) {
            String result = null;
            List<String> values = new ArrayList<String>();
            values.add(STRING_CONVERTER.to(context, FIELDS.fare_id, input.getFareId(), true));
            values.add(INTEGER_CONVERTER.to(context, FIELDS.price, input.getPrice(), true));
            values.add(STRING_CONVERTER.to(context, FIELDS.currency_type, input.getCurrencyType(), true));
            values.add(PAYMENTMETHODTYPE_CONVERTER.to(context, FIELDS.payment_method, input.getPaymentMethod(), true));
            values.add(ATTRIBUTETRANSFERSTYPE_CONVERTER.to(context, FIELDS.transfers, input.getTransfers(), true));
            values.add(STRING_CONVERTER.to(context, FIELDS.agency_id, input.getAgencyId(), true));
            values.add(INTEGER_CONVERTER.to(context, FIELDS.transfer_duration, input.getTransferDuration(), false));

            result = Tokenizer.untokenize(values);
            return result;
        }

    };

    static {
        ExporterFactory factory = new DefaultExporterFactory();
        ExporterFactory.factories
                .put(FareAttributeExporter.class.getName(), factory);
    }

    public FareAttributeExporter(String name) throws IOException {
        super(name);
    }

    @Override
    public void writeHeader() throws IOException {
        write(FIELDS.values());
    }

    @Override
    public void export(GtfsFareAttribute bean) throws IOException {
        write(CONVERTER.to(_context, bean));
    }

    public enum FIELDS {
        fare_id, price, currency_type, payment_method, transfers, agency_id, transfer_duration
    }

    public static class DefaultExporterFactory extends ExporterFactory {

        @SuppressWarnings({"unchecked", "rawtypes"})
        @Override
        protected Exporter create(String path) throws IOException {
            return new FareAttributeExporter(path);
        }
    }

}
