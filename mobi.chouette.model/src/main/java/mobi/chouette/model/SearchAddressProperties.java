package mobi.chouette.model;

import lombok.Data;

@Data
public class SearchAddressProperties {
    private String id;

    private String citycode;

    private String importance;

    private String name;

    private String context;

    private String score;

    private String label;

    private String type;

    private String postcode;

    private String y;

    private String x;

    private String city;
}