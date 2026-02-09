package mobi.chouette.exchange.gtfs.model.fares;

import lombok.Getter;

@Getter
public enum GtfsFareV2File {
    FARE_MEDIA("fare_media.txt"),
    FARE_PRODUCTS("fare_products.txt"),
    RIDER_CATEGORIES("rider_categories.txt"),
    FARE_LEG_RULES("fare_leg_rules.txt"),
    TIME_BASED_FARES("timeframes.txt"),
    FARE_TRANSFERS("fare_transfer_rules.txt");

    private final String filename;

    GtfsFareV2File(String filename) {
        this.filename = filename;
    }
}
