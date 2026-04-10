package mobi.chouette.exchange.netexprofile.exporter.producer.facilities;

import org.apache.commons.lang3.StringUtils;
import org.rutebanken.netex.model.Accommodation;
import org.rutebanken.netex.model.ServiceFacilitySet;

import java.util.List;

public abstract class FacilityProducer<T extends Enum<T>> {
    protected static final String SEPARATOR = ";";

    protected String identifier;

    public abstract void updateFacilitySet(ServiceFacilitySet facilitySet, String rawValues);

    protected boolean checkExistingValue(String rawValues) {
        return StringUtils.isNotBlank(rawValues);
    }

    protected Accommodation initAccommodation() {
        Accommodation accommodation = new Accommodation();
        accommodation.setId(this.identifier);
        accommodation.setVersion("any");
        return accommodation;
    }

    protected int findAccommodation(String identifier, List<Object> accommodationRefOrAccommodation) {
        int accommodationIndex = 0;
        for (Object o : accommodationRefOrAccommodation) {
            if (o instanceof Accommodation && identifier.equals(((Accommodation) o).getId())) {
                return accommodationIndex;
            }
            accommodationIndex++;
        }
        return -1;
    }
}
