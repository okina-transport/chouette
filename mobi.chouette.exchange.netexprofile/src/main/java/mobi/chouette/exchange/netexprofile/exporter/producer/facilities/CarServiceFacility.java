package mobi.chouette.exchange.netexprofile.exporter.producer.facilities;

import org.rutebanken.netex.model.CarServiceFacilityEnumeration;
import org.rutebanken.netex.model.ServiceFacilitySet;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CarServiceFacility extends FacilityProducer<CarServiceFacilityEnumeration> {
    @Override
    public void updateFacilitySet(ServiceFacilitySet facilitySet, String rawValues) {
        if (this.checkExistingValue(rawValues)) {
            List<CarServiceFacilityEnumeration> facilityValues = Arrays.stream(rawValues.split(SEPARATOR))
                    .map(CarServiceFacilityEnumeration::valueOf)
                    .collect(Collectors.toList());
            facilitySet.withCarServiceFacilityList(facilityValues);
        }
    }
}
