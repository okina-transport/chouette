package mobi.chouette.exchange.importer.updater;

import mobi.chouette.common.Context;
import mobi.chouette.model.VehicleJourneyFacility;

import javax.ejb.Stateless;

@Stateless(name = VehicleJourneyFacilitiesUpdater.BEAN_NAME)
public class VehicleJourneyFacilitiesUpdater implements Updater<VehicleJourneyFacility> {

    public static final String BEAN_NAME = "VehicleJourneyFacilitiesUpdater";

    @Override
    public void update(Context context, VehicleJourneyFacility oldValue, VehicleJourneyFacility newValue) throws Exception {
        if (newValue.isSaved()) {
            return;
        }

        newValue.setSaved(true);

        if (oldValue.isDetached()) {
            oldValue.setDescription(newValue.getDescription());
            oldValue.setProvider(newValue.getProvider());
            oldValue.setKeyValues(newValue.getKeyValues());
            oldValue.setDetached(false);
            oldValue.setVehicleJourney(newValue.getVehicleJourney());
        } else {
            if (newValue.getProvider() != null && !newValue.getProvider().equals(oldValue.getProvider())) {
                oldValue.setProvider(newValue.getProvider());
            }
            if (newValue.getDescription() != null && !newValue.getDescription().equals(oldValue.getDescription())) {
                oldValue.setDescription(newValue.getDescription());
            }
            oldValue.setKeyValues(newValue.getKeyValues());
        }

    }
}
