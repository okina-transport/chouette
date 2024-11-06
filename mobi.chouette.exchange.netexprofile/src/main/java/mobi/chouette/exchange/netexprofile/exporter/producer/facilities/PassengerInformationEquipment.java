package mobi.chouette.exchange.netexprofile.exporter.producer.facilities;

import org.rutebanken.netex.model.PassengerInformationEquipmentEnumeration;
import org.rutebanken.netex.model.ServiceFacilitySet;

public class PassengerInformationEquipment extends FacilityProducer<PassengerInformationEquipmentEnumeration> {
    @Override
    public void updateFacilitySet(ServiceFacilitySet facilitySet, String rawValues) {
        if (this.checkExistingValue(rawValues)) {
            facilitySet.withPassengerInformationEquipmentList(PassengerInformationEquipmentEnumeration.valueOf(rawValues));
        }
    }
}
