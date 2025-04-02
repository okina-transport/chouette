package mobi.chouette.model;

import lombok.Data;

import lombok.Setter;
import mobi.chouette.model.type.ServicePosition;

import java.time.LocalTime;
import java.util.Date;

@Data
public class FirstOrLastJourneyInfo {
    private final Date date;

    private final LocalTime time;

    private final String lineId;

    private final String vehicleJourneyId;

    @Setter
    private ServicePosition servicePosition = ServicePosition.otherService;

}
