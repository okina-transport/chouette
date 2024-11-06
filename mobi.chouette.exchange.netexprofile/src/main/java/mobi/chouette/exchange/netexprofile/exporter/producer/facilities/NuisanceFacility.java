package mobi.chouette.exchange.netexprofile.exporter.producer.facilities;

import org.rutebanken.netex.model.NuisanceFacilityEnumeration;
import org.rutebanken.netex.model.ServiceFacilitySet;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class NuisanceFacility extends FacilityProducer<NuisanceFacilityEnumeration> {
    @Override
    public void updateFacilitySet(ServiceFacilitySet facilitySet, String rawValues) {
        if (this.checkExistingValue(rawValues)) {
            List<NuisanceFacilityEnumeration> facilityValues = Arrays.stream(rawValues.split(SEPARATOR))
                    .map(NuisanceFacilityEnumeration::valueOf)
                    .collect(Collectors.toList());
            facilitySet.withNuisanceFacilityList(facilityValues);
        }
    }
}
