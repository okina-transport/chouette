package mobi.chouette.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SearchAddress {
    private String limit;

    private String licence;

    private String query;

    private List<SearchAddressFeatures> features = new ArrayList<>(0);

    private String type;

    private String attribution;

    private String version;
}
