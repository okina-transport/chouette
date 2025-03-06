package mobi.chouette.exchange.gtfs.model.importer;

import mobi.chouette.exchange.gtfs.model.GtfsFareAttribute;

import java.io.IOException;

public class FareAttributeById extends FareAttributeIndex {

    public static final String KEY = FIELDS.fare_id.name();

    static {
        IndexFactory factory = new DefaultImporterFactory();
        IndexFactory.factories.put(FareAttributeById.class.getName(), factory);
    }

    public FareAttributeById(String name) throws IOException {
        super(name, KEY, false);
    }

    public static class DefaultImporterFactory extends IndexFactory {
        @Override
        protected Index<GtfsFareAttribute> create(String name) throws IOException {
            return new FareAttributeById(name);
        }
    }
}
