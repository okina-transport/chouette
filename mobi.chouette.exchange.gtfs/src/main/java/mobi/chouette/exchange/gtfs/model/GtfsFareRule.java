package mobi.chouette.exchange.gtfs.model;

import lombok.*;

import java.io.Serializable;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class GtfsFareRule extends GtfsObject implements Serializable {

    private static final long serialVersionUID = 1L;

    @Getter
    @Setter
    private String fareId;

    @Getter
    @Setter
    private String routeId;

    @Getter
    @Setter
    private String originId;

    @Getter
    @Setter
    private String destinationId;

    @Getter
    @Setter
    private String containsId;

    public void clear() {
        fareId = null;
        routeId = null;
        originId = null;
        destinationId = null;
        containsId = null;
    }
}
