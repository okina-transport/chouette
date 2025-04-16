package mobi.chouette.exchange.importer.utils;

import mobi.chouette.model.Provider;

import java.util.function.Predicate;

import static mobi.chouette.exchange.importer.utils.CsvGenerationConstants.PREFIX_SCHEMA_MOBIITI;
import static mobi.chouette.exchange.importer.utils.CsvGenerationConstants.SCHEMA_TECHNIQUE;

public class ProviderPredicate {

    private ProviderPredicate() {
        throw new IllegalStateException("Utility class");
    }

    public static Predicate<Provider> isProviderForCsvGeneration() {
        return provider -> !provider.getCode().startsWith(PREFIX_SCHEMA_MOBIITI)
                && !SCHEMA_TECHNIQUE.equals(provider.getCode());
    }
}
