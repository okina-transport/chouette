package mobi.chouette.exchange.netexprofile.exporter.producer.facilities;

import org.rutebanken.netex.model.ServiceFacilitySet;
import org.rutebanken.netex.model.UicRateTypeEnumeration;

public class UicRateType extends FacilityProducer<UicRateTypeEnumeration> {
    @Override
    public void updateFacilitySet(ServiceFacilitySet facilitySet, String rawValues) {
        if (this.checkExistingValue(rawValues)) {
            facilitySet.withUicTrainRate(UicRateTypeEnumeration.valueOf(rawValues));
        }
    }
}
