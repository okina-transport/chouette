package mobi.chouette.exchange.gtfs.model;

import lombok.*;

import javax.validation.constraints.Min;
import java.io.Serializable;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class GtfsTransfer extends GtfsObject implements Serializable {

    private static final long serialVersionUID = 1L;

    @Getter
    @Setter
    private String fromStopId;

    @Getter
    @Setter
    private String toStopId;

    @Getter
    @Setter
    private String fromRouteId;

    @Getter
    @Setter
    private String toRouteId;

    @Getter
    @Setter
    private String fromTripId;

    @Getter
    @Setter
    private String toTripId;

    @Getter
    @Setter
    private TransferType transferType;

    @Getter
    @Setter
    @Min(0)
    private Integer minTransferTime;

    public void clear() {
        fromStopId = null;
        toStopId = null;
        fromRouteId = null;
        toRouteId = null;
        fromTripId = null;
        toTripId = null;
        transferType = null;
        minTransferTime = null;
    }

    public enum TransferType implements Serializable {
        Recommended, Timed, Minimal, NoAllowed, InSeatAllowed, InSeatNotAllowed

    }
}
