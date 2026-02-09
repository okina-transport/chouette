package mobi.chouette.exchange.gtfs.model;

import lombok.*;

import java.io.Serializable;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class GtfsFareLegRules extends GtfsObject implements Serializable {

    private static final long serialVersionUID = 1L;

    private String legGroupId;

    private String networkId;

    private String fromAreaId;

    private String toAreaId;

    private String fromTimeFrameGroupId;

    private String toTimeFrameGroupId;

    private String fareProductId;

    private int rulePriority;

    public void clear() {
        legGroupId = null;
        networkId = null;
        fromAreaId = null;
        toAreaId = null;
        fromTimeFrameGroupId = null;
        toTimeFrameGroupId = null;
        fareProductId = null;
        rulePriority = 0;
    }

}
