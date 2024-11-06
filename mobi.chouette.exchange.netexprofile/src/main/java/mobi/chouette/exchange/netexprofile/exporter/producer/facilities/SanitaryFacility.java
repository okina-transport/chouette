package mobi.chouette.exchange.netexprofile.exporter.producer.facilities;

import org.rutebanken.netex.model.SanitaryFacilityEnumeration;
import org.rutebanken.netex.model.ServiceFacilitySet;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SanitaryFacility extends FacilityProducer<SanitaryFacilityEnumeration> {
    @Override
    public void updateFacilitySet(ServiceFacilitySet facilitySet, String rawValues) {
        if (this.checkExistingValue(rawValues)) {
            List<SanitaryFacilityEnumeration> facilityValues = Arrays.stream(rawValues.split(SEPARATOR))
                    .map(SanitaryFacilityEnumeration::valueOf)
                    .collect(Collectors.toList());
            facilitySet.withSanitaryFacilityList(facilityValues);
        }
    }
}
