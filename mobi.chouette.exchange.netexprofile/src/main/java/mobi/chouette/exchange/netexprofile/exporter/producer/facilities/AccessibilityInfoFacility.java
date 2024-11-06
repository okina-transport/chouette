package mobi.chouette.exchange.netexprofile.exporter.producer.facilities;

import org.rutebanken.netex.model.AccessibilityInfoFacilityEnumeration;
import org.rutebanken.netex.model.ServiceFacilitySet;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AccessibilityInfoFacility extends FacilityProducer<AccessibilityInfoFacilityEnumeration> {
    @Override
    public void updateFacilitySet(ServiceFacilitySet facilitySet, String rawValues) {
        if (this.checkExistingValue(rawValues)) {
            List<AccessibilityInfoFacilityEnumeration> facilityValues = Arrays.stream(rawValues.split(SEPARATOR))
                    .map(AccessibilityInfoFacilityEnumeration::valueOf)
                    .collect(Collectors.toList());
            facilitySet.withAccessibilityInfoFacilityList(facilityValues);
        }
    }
}
