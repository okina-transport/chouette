package mobi.chouette.exchange.netexprofile.exporter.producer.facilities;

import org.rutebanken.netex.model.BookingProcessEnumeration;
import org.rutebanken.netex.model.ServiceFacilitySet;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class BookingProcess extends FacilityProducer<BookingProcessEnumeration> {
    @Override
    public void updateFacilitySet(ServiceFacilitySet facilitySet, String rawValues) {
        if (this.checkExistingValue(rawValues)) {
            List<BookingProcessEnumeration> facilityValues = Arrays.stream(rawValues.split(SEPARATOR))
                    .map(BookingProcessEnumeration::valueOf)
                    .collect(Collectors.toList());
            facilitySet.withBookingProcessFacilityList(facilityValues);
        }
    }
}
