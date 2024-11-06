package mobi.chouette.exchange.netexprofile.exporter.producer.facilities;

import org.rutebanken.netex.model.CouchetteFacilityEnumeration;
import org.rutebanken.netex.model.ServiceFacilitySet;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CouchetteFacility extends FacilityProducer<CouchetteFacilityEnumeration> {
    @Override
    public void updateFacilitySet(ServiceFacilitySet facilitySet, String rawValues) {
        if (this.checkExistingValue(rawValues)) {
            List<CouchetteFacilityEnumeration> facilityValues = Arrays.stream(rawValues.split(SEPARATOR))
                    .map(CouchetteFacilityEnumeration::valueOf)
                    .collect(Collectors.toList());
            facilitySet.withCouchetteFacilityList(facilityValues);
        }
    }
}
