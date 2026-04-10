package mobi.chouette.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@AllArgsConstructor
public class TheoreticalStopMonitoringInfo {
    private LocalDate date;

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
