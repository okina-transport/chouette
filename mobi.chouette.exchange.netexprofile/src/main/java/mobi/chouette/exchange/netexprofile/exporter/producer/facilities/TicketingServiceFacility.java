package mobi.chouette.exchange.netexprofile.exporter.producer.facilities;

import org.rutebanken.netex.model.ServiceFacilitySet;
import org.rutebanken.netex.model.TicketingServiceFacilityEnumeration;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TicketingServiceFacility extends FacilityProducer<TicketingServiceFacilityEnumeration> {
    @Override
    public void updateFacilitySet(ServiceFacilitySet facilitySet, String rawValues) {
        if (this.checkExistingValue(rawValues)) {
            List<TicketingServiceFacilityEnumeration> facilityValues = Arrays.stream(rawValues.split(SEPARATOR))
                    .map(TicketingServiceFacilityEnumeration::valueOf)
                    .collect(Collectors.toList());
            facilitySet.withTicketingServiceFacilityList(facilityValues);
        }
    }
}
