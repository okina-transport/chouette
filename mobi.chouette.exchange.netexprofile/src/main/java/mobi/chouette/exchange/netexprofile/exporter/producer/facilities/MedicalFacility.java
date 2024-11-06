package mobi.chouette.exchange.netexprofile.exporter.producer.facilities;

import org.rutebanken.netex.model.MedicalFacilityEnumeration;
import org.rutebanken.netex.model.ServiceFacilitySet;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MedicalFacility extends FacilityProducer<MedicalFacilityEnumeration> {
    @Override
    public void updateFacilitySet(ServiceFacilitySet facilitySet, String rawValues) {
        if (this.checkExistingValue(rawValues)) {
            List<MedicalFacilityEnumeration> facilityValues = Arrays.stream(rawValues.split(SEPARATOR))
                    .map(MedicalFacilityEnumeration::valueOf)
                    .collect(Collectors.toList());
            facilitySet.withMedicalFacilityList(facilityValues);
        }
    }
}
