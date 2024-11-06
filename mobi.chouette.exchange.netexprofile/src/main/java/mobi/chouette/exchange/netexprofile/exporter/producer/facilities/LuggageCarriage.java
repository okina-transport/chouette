package mobi.chouette.exchange.netexprofile.exporter.producer.facilities;

import org.rutebanken.netex.model.LuggageCarriageEnumeration;
import org.rutebanken.netex.model.ServiceFacilitySet;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class LuggageCarriage extends FacilityProducer<LuggageCarriageEnumeration> {
    @Override
    public void updateFacilitySet(ServiceFacilitySet facilitySet, String rawValues) {
        if (this.checkExistingValue(rawValues)) {
            List<LuggageCarriageEnumeration> facilityValues = Arrays.stream(rawValues.split(SEPARATOR))
                    .map(LuggageCarriageEnumeration::valueOf)
                    .collect(Collectors.toList());
            facilitySet.withLuggageCarriageFacilityList(facilityValues);
        }
    }
}
