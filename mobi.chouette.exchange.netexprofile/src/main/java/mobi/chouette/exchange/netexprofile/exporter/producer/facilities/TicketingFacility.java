package mobi.chouette.exchange.netexprofile.exporter.producer.facilities;

import org.rutebanken.netex.model.ServiceFacilitySet;
import org.rutebanken.netex.model.TicketingFacilityEnumeration;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TicketingFacility extends FacilityProducer<TicketingFacilityEnumeration> {
    @Override
    public void updateFacilitySet(ServiceFacilitySet facilitySet, String rawValues) {
        if (this.checkExistingValue(rawValues)) {
            List<TicketingFacilityEnumeration> facilityValues = Arrays.stream(rawValues.split(SEPARATOR))
                    .map(TicketingFacilityEnumeration::valueOf)
                    .collect(Collectors.toList());
            facilitySet.withTicketingFacilityList(facilityValues);
        }
    }
}
