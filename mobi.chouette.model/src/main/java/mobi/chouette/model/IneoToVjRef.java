package mobi.chouette.model;

import lombok.Data;
import mobi.chouette.model.type.PTDirectionEnum;

import java.time.LocalTime;
import java.util.Date;

@Data
public class IneoToVjRef {

    private final Date date;

    private final LocalTime time;

    private final String stopAreaObjectId;

    private final String lineNumber;

    private final PTDirectionEnum routeDirection;

    private final String vehicleJourneyObjectId;

}
