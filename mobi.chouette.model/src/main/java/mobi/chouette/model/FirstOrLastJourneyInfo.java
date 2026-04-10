package mobi.chouette.model;

import lombok.Data;
import lombok.Setter;
import mobi.chouette.model.type.ServicePosition;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class FirstOrLastJourneyInfo {
    private final LocalDate date;

    private final LocalTime time;

    private final String lineId;

    private final String vehicleJourneyId;

    @Setter
    private ServicePosition servicePosition = ServicePosition.otherService;

}
