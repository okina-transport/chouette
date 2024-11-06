package mobi.chouette.exchange.netexprofile.exporter.producer.facilities;

import org.apache.commons.collections.CollectionUtils;
import org.rutebanken.netex.model.Accommodation;
import org.rutebanken.netex.model.AccommodationFacilityEnumeration;
import org.rutebanken.netex.model.Accommodations_RelStructure;
import org.rutebanken.netex.model.ServiceFacilitySet;

public class AccommodationItemFacility extends FacilityProducer<AccommodationFacilityEnumeration> {

    public AccommodationItemFacility(String id) {
        super();
        this.identifier = id;
    }

    @Override
    public void updateFacilitySet(ServiceFacilitySet facilitySet, String rawValues) {
        if (this.checkExistingValue(rawValues)) {
            Accommodation accommodation = initAccommodation();
            accommodation.setAccommodationFacility(AccommodationFacilityEnumeration.valueOf(rawValues));
            if (facilitySet.getAccommodations() == null
                    || CollectionUtils.isEmpty(facilitySet.getAccommodations().getAccommodationRefOrAccommodation())) {
                Accommodations_RelStructure accommodationsRelStructure = new Accommodations_RelStructure();
                accommodationsRelStructure.withAccommodationRefOrAccommodation(accommodation);
                facilitySet.withAccommodations(accommodationsRelStructure);
            } else {
                int accommodationIndex = findAccommodation(this.identifier, facilitySet.getAccommodations().getAccommodationRefOrAccommodation());
                if (accommodationIndex > -1) {
                    ((Accommodation) facilitySet.getAccommodations().getAccommodationRefOrAccommodation().get(accommodationIndex))
                            .setAccommodationFacility(accommodation.getAccommodationFacility());
                } else {
                    facilitySet.getAccommodations().getAccommodationRefOrAccommodation().add(accommodation);
                }
            }
        }
    }
}
