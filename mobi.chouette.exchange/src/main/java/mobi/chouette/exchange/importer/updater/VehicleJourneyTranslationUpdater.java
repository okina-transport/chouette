package mobi.chouette.exchange.importer.updater;

import mobi.chouette.common.Context;
import mobi.chouette.dao.VehicleJourneyDAO;
import mobi.chouette.model.VehicleJourney;
import mobi.chouette.model.VehicleJourneyTranslation;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.Referential;

import javax.ejb.EJB;
import javax.ejb.Stateless;

@Stateless(name = VehicleJourneyTranslationUpdater.BEAN_NAME)
public class VehicleJourneyTranslationUpdater extends TranslationUpdater<VehicleJourneyTranslation> implements
        Updater<VehicleJourneyTranslation> {

    public static final String BEAN_NAME = "VehicleJourneyTranslationUpdater";

    @EJB
    private VehicleJourneyDAO vehicleJourneyDAO;

    @Override
    public void update(Context context, VehicleJourneyTranslation oldValue, VehicleJourneyTranslation newValue) throws Exception {
        super.update(context, oldValue, newValue);
        if (newValue.isSaved()) {
            return;
        }
        newValue.setSaved(true);

        if (newValue.getTargetObjectId() == null) {
            oldValue.setVehicleJourney(null);
            return;
        }

        Referential cache = (Referential) context.get(CACHE);
        String objectId = newValue.getTargetObjectId();
        VehicleJourney vehicleJourney = cache.getVehicleJourneys().get(objectId);
        if (vehicleJourney == null) {
            vehicleJourney = vehicleJourneyDAO.findByObjectId(objectId);
            if (vehicleJourney != null) {
                cache.getVehicleJourneys().put(objectId, vehicleJourney);
            }
        }
        if (vehicleJourney == null) {
            vehicleJourney = ObjectFactory.getVehicleJourney(cache, objectId);
        }
        oldValue.setVehicleJourney(vehicleJourney);
    }

}
