package mobi.chouette.exchange.gtfs.model;

import lombok.*;

import java.io.Serializable;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class GtfsFareMedia extends GtfsObject implements Serializable {

    private static final long serialVersionUID = 1L;

    private String fareMediaId;

    private String fareMediaName;

    private FareMediaType fareMediaType;


    public void clear() {
        fareMediaId = null;
        fareMediaName = null;
        fareMediaType = FareMediaType.NONE;
    }

}
