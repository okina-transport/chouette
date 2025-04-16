package mobi.chouette.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalTime;
import java.util.Date;

@Data
@AllArgsConstructor
public class TheoreticalStopMonitoringInfo {
    private Date date;

    private String monitoringRef;

    private String stopPointName;

    private String monitoredVehicleJourneyRef;

    private String lineRef;

    private String publishedLineName;

    private String directionName;

    private LocalTime aimedDepartureTime;

    private LocalTime aimedArrivalTime;

    private String originRef;

    private String originName;

    private String destinationRef;

    private String destinationName;
}
