package mobi.chouette.exchange.netexprofile.exporter.producer;

import mobi.chouette.exchange.netexprofile.exporter.producer.facilities.FacilityProducerFactory;
import mobi.chouette.model.KeyValue;
import mobi.chouette.model.VehicleJourneyFacility;
import org.apache.commons.collections.CollectionUtils;
import org.rutebanken.netex.model.ServiceFacilitySet;

import static mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducer.netexFactory;

public class ServiceFacilitiesProducer {

    public ServiceFacilitySet mapToServiceFacilitySet(VehicleJourneyFacility facility) {
        ServiceFacilitySet serviceFacilitySet = netexFactory.createServiceFacilitySet()
                .withId(facility.getObjectId()).withVersion("any");
        if (CollectionUtils.isNotEmpty(facility.getKeyValues())) {
            for (KeyValue keyValue : facility.getKeyValues()) {
                String objectType = keyValue.getTypeOfKey();
                String enumName = keyValue.getKey();
                String enumValues = keyValue.getValue();
                FacilityProducerFactory.init(objectType, enumName).build().updateFacilitySet(serviceFacilitySet, enumValues);
            }
        }
        return serviceFacilitySet;
    }
}
