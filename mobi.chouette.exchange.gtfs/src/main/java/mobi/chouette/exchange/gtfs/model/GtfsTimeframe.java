package mobi.chouette.exchange.gtfs.model;

import lombok.*;

import java.io.Serializable;
import java.time.LocalTime;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class GtfsTimeframe extends GtfsObject implements Serializable {

    private static final long serialVersionUID = 1L;

    private String timeframeGroupId;

    private LocalTime startTime;

    private LocalTime endTime;

    private String serviceId;

    public void clear() {
        timeframeGroupId = null;
        startTime = null;
        endTime = null;
        serviceId = null;
    }

}
