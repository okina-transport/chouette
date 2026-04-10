package mobi.chouette.model;

import lombok.Data;
import mobi.chouette.model.type.PTDirectionEnum;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class IneoVJMapping {

    private final LocalDate date;

    private final LocalTime time;

    private final String originalStopId;

    private final String originalParentStopId;

    private final String lineNumber;

    private final PTDirectionEnum routeDirection;

    private final String vehicleJourneyObjectId;

    private final Integer position;

}
