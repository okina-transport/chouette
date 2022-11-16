package mobi.chouette.model;

import lombok.Data;

@Data
public class SearchAddressFeatures {
    private SearchAddressProperties properties;

    private String type;

    private SearchAddressGeometry geometry;
}

