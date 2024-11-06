package mobi.chouette.exchange.netexprofile.exporter.producer.facilities;

import org.rutebanken.netex.model.PassengerInformationFacilityEnumeration;
import org.rutebanken.netex.model.ServiceFacilitySet;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PassengerInformationFacility extends FacilityProducer<PassengerInformationFacilityEnumeration> {
    @Override
    public void updateFacilitySet(ServiceFacilitySet facilitySet, String rawValues) {
        if (this.checkExistingValue(rawValues)) {
            List<PassengerInformationFacilityEnumeration> facilityValues = Arrays.stream(rawValues.split(SEPARATOR))
                    .map(PassengerInformationFacilityEnumeration::valueOf)
                    .collect(Collectors.toList());
            facilitySet.withPassengerInformationFacilityList(facilityValues);
        }
    }
}
