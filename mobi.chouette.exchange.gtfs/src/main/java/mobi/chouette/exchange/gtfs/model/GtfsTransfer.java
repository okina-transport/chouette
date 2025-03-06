package mobi.chouette.exchange.gtfs.model;

import lombok.*;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.validation.constraints.Min;
import java.io.Serializable;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class GtfsTransfer extends GtfsObject implements Serializable {

    private static final long serialVersionUID = 1L;

    private String fromStopId;

    private String toStopId;

    private String fromRouteId;

    private String toRouteId;

    private String fromTripId;

    private String toTripId;

    @Enumerated(EnumType.STRING)
    private TransferType transferType;

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
