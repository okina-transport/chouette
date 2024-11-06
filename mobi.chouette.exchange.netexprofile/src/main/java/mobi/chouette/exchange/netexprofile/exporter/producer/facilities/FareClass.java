package mobi.chouette.exchange.netexprofile.exporter.producer.facilities;

import org.rutebanken.netex.model.FareClassEnumeration;
import org.rutebanken.netex.model.ServiceFacilitySet;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FareClass extends FacilityProducer<FareClassEnumeration> {
    @Override
    public void updateFacilitySet(ServiceFacilitySet facilitySet, String rawValues) {
        if (this.checkExistingValue(rawValues)) {
            List<FareClassEnumeration> facilityValues = Arrays.stream(rawValues.split(SEPARATOR))
                    .map(FareClassEnumeration::valueOf)
                    .collect(Collectors.toList());
            facilitySet.withFareClasses(facilityValues);
        }
    }
}
