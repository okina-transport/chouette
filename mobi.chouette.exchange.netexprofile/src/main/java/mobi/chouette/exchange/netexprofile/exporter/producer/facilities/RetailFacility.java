package mobi.chouette.exchange.netexprofile.exporter.producer.facilities;

import org.rutebanken.netex.model.RetailFacilityEnumeration;
import org.rutebanken.netex.model.ServiceFacilitySet;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RetailFacility extends FacilityProducer<RetailFacilityEnumeration> {
    @Override
    public void updateFacilitySet(ServiceFacilitySet facilitySet, String rawValues) {
        if (this.checkExistingValue(rawValues)) {
            List<RetailFacilityEnumeration> facilityValues = Arrays.stream(rawValues.split(SEPARATOR))
                    .map(RetailFacilityEnumeration::valueOf)
                    .collect(Collectors.toList());
            facilitySet.withRetailFacilityList(facilityValues);
        }
    }
}
