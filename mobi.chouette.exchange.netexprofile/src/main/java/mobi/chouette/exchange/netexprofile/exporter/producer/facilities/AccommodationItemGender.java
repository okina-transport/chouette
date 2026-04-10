package mobi.chouette.exchange.netexprofile.exporter.producer.facilities;

import org.apache.commons.collections4.CollectionUtils;
import org.rutebanken.netex.model.*;

public class AccommodationItemGender extends FacilityProducer<CouchetteFacilityEnumeration> {


    public AccommodationItemGender(String id) {
        super();
        this.identifier = id;
    }

    @Override
    public void updateFacilitySet(ServiceFacilitySet facilitySet, String rawValues) {
        if (this.checkExistingValue(rawValues)) {
            Accommodation accommodation = initAccommodation();
            accommodation.setGenderLimitation(GenderLimitationEnumeration.valueOf(rawValues));
            if (facilitySet.getAccommodations() == null
                    || CollectionUtils.isEmpty(facilitySet.getAccommodations().getAccommodationRefOrAccommodation())) {
                Accommodations_RelStructure accommodationsRelStructure = new Accommodations_RelStructure();
                accommodationsRelStructure.withAccommodationRefOrAccommodation(accommodation);
                facilitySet.withAccommodations(accommodationsRelStructure);
            } else {
                int accommodationIndex = findAccommodation(this.identifier, facilitySet.getAccommodations().getAccommodationRefOrAccommodation());
                if (accommodationIndex > -1) {
                    ((Accommodation) facilitySet.getAccommodations().getAccommodationRefOrAccommodation().get(accommodationIndex))
                            .setGenderLimitation(accommodation.getGenderLimitation());
                } else {
                    facilitySet.getAccommodations().getAccommodationRefOrAccommodation().add(accommodation);
                }
            }
        }
    }

}
