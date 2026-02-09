package mobi.chouette.exchange.gtfs.model;

import lombok.*;

import java.io.Serializable;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class GtfsFareProducts extends GtfsObject implements Serializable {

    private static final long serialVersionUID = 1L;

    private String fareProductId;

    private String fareProductName;

    private String riderCategoryId;

    private String fareMediaId;

    private float amount;

    private String currency;

    public void clear() {
        fareProductId = null;
        fareProductName = null;
        riderCategoryId = null;
        fareMediaId = null;
        amount = 0;
        currency = null;
    }

}
