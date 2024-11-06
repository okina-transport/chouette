package mobi.chouette.exchange.netexprofile.exporter.producer.facilities;

import org.rutebanken.netex.model.PassengerCommsFacilityEnumeration;
import org.rutebanken.netex.model.ServiceFacilitySet;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PassengerCommsFacility extends FacilityProducer<PassengerCommsFacilityEnumeration> {
    @Override
    public void updateFacilitySet(ServiceFacilitySet facilitySet, String rawValues) {
        if (this.checkExistingValue(rawValues)) {
            List<PassengerCommsFacilityEnumeration> facilityValues = Arrays.stream(rawValues.split(SEPARATOR))
                    .map(PassengerCommsFacilityEnumeration::valueOf)
                    .collect(Collectors.toList());
            facilitySet.withPassengerCommsFacilityList(facilityValues);
        }
    }
}
