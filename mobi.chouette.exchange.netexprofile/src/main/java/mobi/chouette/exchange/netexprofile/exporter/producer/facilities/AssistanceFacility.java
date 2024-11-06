package mobi.chouette.exchange.netexprofile.exporter.producer.facilities;

import org.rutebanken.netex.model.AssistanceFacilityEnumeration;
import org.rutebanken.netex.model.ServiceFacilitySet;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AssistanceFacility extends FacilityProducer<AssistanceFacilityEnumeration> {
    @Override
    public void updateFacilitySet(ServiceFacilitySet facilitySet, String rawValues) {
        if (this.checkExistingValue(rawValues)) {
            List<AssistanceFacilityEnumeration> facilityValues = Arrays.stream(rawValues.split(SEPARATOR))
                    .map(AssistanceFacilityEnumeration::valueOf)
                    .collect(Collectors.toList());
            facilitySet.withAssistanceFacilityList(facilityValues);
        }
    }
}
