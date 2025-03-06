package mobi.chouette.exchange.gtfs.model.importer;

import mobi.chouette.exchange.gtfs.model.GtfsFareRule;

import java.io.IOException;

public class FareRuleById extends FareRuleIndex {

    public static final String KEY = FIELDS.fare_id.name();

    static {
        IndexFactory factory = new DefaultImporterFactory();
        IndexFactory.factories.put(FareRuleById.class.getName(), factory);
    }

    public FareRuleById(String name) throws IOException {
        super(name, KEY, false);
    }

    public static class DefaultImporterFactory extends IndexFactory {
        @Override
        protected Index<GtfsFareRule> create(String name) throws IOException {
            return new FareRuleById(name);
        }
    }
}
