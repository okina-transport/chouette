package mobi.chouette.model;

import lombok.Data;

@Data
public class SearchAddressGeometry {
    private String type;

    private String[] coordinates;
}
