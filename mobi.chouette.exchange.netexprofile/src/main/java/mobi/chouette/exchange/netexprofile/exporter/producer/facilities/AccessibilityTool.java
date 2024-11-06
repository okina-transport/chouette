package mobi.chouette.exchange.netexprofile.exporter.producer.facilities;

import org.rutebanken.netex.model.AccessibilityToolEnumeration;
import org.rutebanken.netex.model.ServiceFacilitySet;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AccessibilityTool extends FacilityProducer<AccessibilityToolEnumeration> {
    @Override
    public void updateFacilitySet(ServiceFacilitySet facilitySet, String rawValues) {
        if (this.checkExistingValue(rawValues)) {
            List<AccessibilityToolEnumeration> facilityValues = Arrays.stream(rawValues.split(SEPARATOR))
                    .map(AccessibilityToolEnumeration::valueOf)
                    .collect(Collectors.toList());
            facilitySet.withAccessibilityToolList(facilityValues);
        }
    }
}
