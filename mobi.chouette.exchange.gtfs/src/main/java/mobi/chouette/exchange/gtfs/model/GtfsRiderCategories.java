package mobi.chouette.exchange.gtfs.model;

import lombok.*;

import java.io.Serializable;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class GtfsRiderCategories extends GtfsObject implements Serializable {

    private static final long serialVersionUID = 1L;

    private String riderCategoryId;

    private String riderCategoryName;

    private Boolean isDefaultFareCategory;

    private String eligibilityUrl;

    public void clear() {
        riderCategoryId = null;
        riderCategoryName = null;
        isDefaultFareCategory = null;
        eligibilityUrl = null;
    }

}
