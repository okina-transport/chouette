package mobi.chouette.exchange.netexprofile.exporter.producer.facilities;

import org.rutebanken.netex.model.AccommodationFacilityEnumeration;
import org.rutebanken.netex.model.ServiceFacilitySet;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AccommodationFacility extends FacilityProducer<AccommodationFacilityEnumeration> {
    @Override
    public void updateFacilitySet(ServiceFacilitySet facilitySet, String rawValues) {
        if (this.checkExistingValue(rawValues)) {
            List<AccommodationFacilityEnumeration> facilityValues = Arrays.stream(rawValues.split(SEPARATOR))
                    .map(AccommodationFacilityEnumeration::valueOf)
                    .collect(Collectors.toList());
            facilitySet.withAccommodationFacilityList(facilityValues);
        }
    }
}
