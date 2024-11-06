package mobi.chouette.exchange.netexprofile.exporter.producer.facilities;

import org.rutebanken.netex.model.GroupBookingEnumeration;
import org.rutebanken.netex.model.ServiceFacilitySet;

public class GroupBooking extends FacilityProducer<GroupBookingEnumeration> {
    @Override
    public void updateFacilitySet(ServiceFacilitySet facilitySet, String rawValues) {
        if (this.checkExistingValue(rawValues)) {
            facilitySet.withGroupBookingFacility(GroupBookingEnumeration.valueOf(rawValues));
        }
    }
}
