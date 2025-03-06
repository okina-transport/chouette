package mobi.chouette.exchange.gtfs.model;

import lombok.*;

import java.io.Serializable;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class GtfsFareRule extends GtfsObject implements Serializable {

    private static final long serialVersionUID = 1L;

    private String fareId;

    private String routeId;

    private Integer originId;

    private Integer destinationId;

    private Integer containsId;

    public void clear() {
        fareId = null;
        routeId = null;
        originId = null;
        destinationId = null;
        containsId = null;
    }
}
