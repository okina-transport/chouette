package mobi.chouette.exchange.netexprofile.exporter.producer.facilities;

import org.apache.commons.collections.CollectionUtils;
import org.rutebanken.netex.model.BoardingPermissionEnumeration;
import org.rutebanken.netex.model.OnboardStay;
import org.rutebanken.netex.model.OnboardStays_RelStructure;
import org.rutebanken.netex.model.ServiceFacilitySet;

public class OnboardItemFacility extends FacilityProducer<BoardingPermissionEnumeration> {

    public OnboardItemFacility(String id) {
        super();
        this.identifier = id;
    }

    @Override
    public void updateFacilitySet(ServiceFacilitySet facilitySet, String rawValues) {
        if (this.checkExistingValue(rawValues)) {
            OnboardStay onboardStay = new OnboardStay();
            onboardStay.setId(this.identifier);
            onboardStay.setVersion("any");
            onboardStay.setBoardingPermission(BoardingPermissionEnumeration.valueOf(rawValues));
            if (facilitySet.getOnboardStays() == null
                    || CollectionUtils.isEmpty(facilitySet.getOnboardStays().getOnboardStay())) {
                OnboardStays_RelStructure onboardRelStructure = new OnboardStays_RelStructure();
                onboardRelStructure.withOnboardStay(onboardStay);
                facilitySet.withOnboardStays(onboardRelStructure);
            } else {
                facilitySet.getOnboardStays().getOnboardStay().add(onboardStay);
            }
        }
    }
}
