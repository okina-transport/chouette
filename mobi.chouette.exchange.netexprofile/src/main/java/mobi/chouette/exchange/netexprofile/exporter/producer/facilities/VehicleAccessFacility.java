package mobi.chouette.exchange.netexprofile.exporter.producer.facilities;

import org.rutebanken.netex.model.ServiceFacilitySet;
import org.rutebanken.netex.model.VehicleAccessFacilityEnumeration;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class VehicleAccessFacility extends FacilityProducer<VehicleAccessFacilityEnumeration> {
    @Override
    public void updateFacilitySet(ServiceFacilitySet facilitySet, String rawValues) {
        if (this.checkExistingValue(rawValues)) {
            List<VehicleAccessFacilityEnumeration> facilityValues = Arrays.stream(rawValues.split(SEPARATOR))
                    .map(VehicleAccessFacilityEnumeration::valueOf)
                    .collect(Collectors.toList());
            facilitySet.withVehicleAccessFacilityList(facilityValues);
        }
    }
}
