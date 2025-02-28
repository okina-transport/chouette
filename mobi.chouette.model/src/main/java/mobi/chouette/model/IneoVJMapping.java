package mobi.chouette.model;

import lombok.Data;
import mobi.chouette.model.type.PTDirectionEnum;

import java.math.BigInteger;
import java.time.LocalTime;
import java.util.Date;

@Data
public class IneoVJMapping {

    private final Date date;

    private final LocalTime time;

    private final String originalStopId;

    private final String originalParentStopId;

    private final String lineNumber;

    private final PTDirectionEnum routeDirection;

    private final String vehicleJourneyObjectId;

    private final BigInteger vehicleJourneyAtStopId;

}
