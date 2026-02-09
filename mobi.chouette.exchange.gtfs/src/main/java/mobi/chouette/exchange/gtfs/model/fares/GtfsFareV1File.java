package mobi.chouette.exchange.gtfs.model.fares;

import lombok.Getter;

@Getter
public enum GtfsFareV1File {
    FARE_ATTRIBUTES("fare_attributes.txt"),
    FARE_RULES("fare_rules.txt");

    private final String filename;

    GtfsFareV1File(String filename) {
        this.filename = filename;
    }
}
