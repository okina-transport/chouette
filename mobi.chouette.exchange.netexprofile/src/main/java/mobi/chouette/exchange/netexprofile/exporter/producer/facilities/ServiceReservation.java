package mobi.chouette.exchange.netexprofile.exporter.producer.facilities;

import org.rutebanken.netex.model.ReservationEnumeration;
import org.rutebanken.netex.model.ServiceFacilitySet;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ServiceReservation extends FacilityProducer<ReservationEnumeration> {
    @Override
    public void updateFacilitySet(ServiceFacilitySet facilitySet, String rawValues) {
        if (this.checkExistingValue(rawValues)) {
            List<ReservationEnumeration> facilityValues = Arrays.stream(rawValues.split(SEPARATOR))
                    .map(ReservationEnumeration::valueOf)
                    .collect(Collectors.toList());
            facilitySet.withServiceReservationFacilityList(facilityValues);
        }
    }
}
