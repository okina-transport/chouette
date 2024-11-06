package mobi.chouette.exchange.netexprofile.exporter.producer.facilities;

import org.rutebanken.netex.model.GenderLimitationEnumeration;
import org.rutebanken.netex.model.ServiceFacilitySet;

public class GenderLimitation extends FacilityProducer<GenderLimitationEnumeration> {
    @Override
    public void updateFacilitySet(ServiceFacilitySet facilitySet, String rawValues) {
        if (this.checkExistingValue(rawValues)) {
            facilitySet.withGenderLimitation(GenderLimitationEnumeration.valueOf(rawValues));
        }
    }
}
