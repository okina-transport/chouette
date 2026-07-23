package mobi.chouette.exchange.importer.updater;

import mobi.chouette.common.Context;
import mobi.chouette.dao.VehicleJourneyAtStopDAO;
import mobi.chouette.model.VehicleJourneyAtStop;
import mobi.chouette.model.VehicleJourneyAtStopTranslation;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.Referential;

import javax.ejb.EJB;
import javax.ejb.Stateless;

@Stateless(name = VehicleJourneyAtStopTranslationUpdater.BEAN_NAME)
public class VehicleJourneyAtStopTranslationUpdater extends TranslationUpdater<VehicleJourneyAtStopTranslation> implements
        Updater<VehicleJourneyAtStopTranslation> {

    public static final String BEAN_NAME = "VehicleJourneyAtStopTranslationUpdater";

    @EJB
    private VehicleJourneyAtStopDAO vehicleJourneyAtStopDAO;

    @Override
    public void update(Context context, VehicleJourneyAtStopTranslation oldValue, VehicleJourneyAtStopTranslation newValue) throws Exception {
        super.update(context, oldValue, newValue);
        if (newValue.isSaved()) {
            return;
        }
        newValue.setSaved(true);

        if (newValue.getTargetObjectId() == null) {
            oldValue.setVehicleJourneyAtStop(null);
            return;
        }

        Referential cache = (Referential) context.get(CACHE);
        String objectId = newValue.getTargetObjectId();
        VehicleJourneyAtStop vehicleJourneyAtStop = cache.getVehicleJourneyAtStops().get(objectId);
        if (vehicleJourneyAtStop == null) {
            vehicleJourneyAtStop = vehicleJourneyAtStopDAO.findByObjectId(objectId);
            if (vehicleJourneyAtStop != null) {
                cache.getVehicleJourneyAtStops().put(objectId, vehicleJourneyAtStop);
            }
        }
        if (vehicleJourneyAtStop == null) {
            vehicleJourneyAtStop = ObjectFactory.getVehicleJourneyAtStop(cache, objectId);
        }
        oldValue.setVehicleJourneyAtStop(vehicleJourneyAtStop);
    }

}
