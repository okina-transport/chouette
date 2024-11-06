package mobi.chouette.exchange.netexprofile.exporter.producer.facilities;

import org.rutebanken.netex.model.AccommodationAccessEnumeration;
import org.rutebanken.netex.model.ServiceFacilitySet;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AccommodationAccess extends FacilityProducer<AccommodationAccessEnumeration> {
    @Override
    public void updateFacilitySet(ServiceFacilitySet facilitySet, String rawValues) {
        if (this.checkExistingValue(rawValues)) {
            List<AccommodationAccessEnumeration> facilityValues = Arrays.stream(rawValues.split(SEPARATOR))
                    .map(AccommodationAccessEnumeration::valueOf)
                    .collect(Collectors.toList());
            facilitySet.withAccommodationAccessList(facilityValues);
        }
    }
}
