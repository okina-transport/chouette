package mobi.chouette.exchange.netexprofile.exporter.producer.facilities;

import org.rutebanken.netex.model.BoardingPermissionEnumeration;
import org.rutebanken.netex.model.ServiceFacilitySet;

public class BoardingPermission extends FacilityProducer<BoardingPermissionEnumeration> {
    @Override
    public void updateFacilitySet(ServiceFacilitySet facilitySet, String rawValues) {
        if (this.checkExistingValue(rawValues)) {
            facilitySet.withBoardingPermission(BoardingPermissionEnumeration.valueOf(rawValues));
        }
    }
}
