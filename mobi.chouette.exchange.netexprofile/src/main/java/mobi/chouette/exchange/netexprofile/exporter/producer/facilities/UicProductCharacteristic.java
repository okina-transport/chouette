package mobi.chouette.exchange.netexprofile.exporter.producer.facilities;

import org.rutebanken.netex.model.ServiceFacilitySet;
import org.rutebanken.netex.model.UicProductCharacteristicEnumeration;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class UicProductCharacteristic extends FacilityProducer<UicProductCharacteristicEnumeration> {
    @Override
    public void updateFacilitySet(ServiceFacilitySet facilitySet, String rawValues) {
        if (this.checkExistingValue(rawValues)) {
            List<UicProductCharacteristicEnumeration> facilityValues = Arrays.stream(rawValues.split(SEPARATOR))
                    .map(UicProductCharacteristicEnumeration::valueOf)
                    .collect(Collectors.toList());
            facilitySet.withUicProductCharacteristicList(facilityValues);
        }
    }
}
