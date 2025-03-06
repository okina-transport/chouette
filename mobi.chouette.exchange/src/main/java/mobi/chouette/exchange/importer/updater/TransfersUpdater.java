package mobi.chouette.exchange.importer.updater;

import mobi.chouette.common.Context;
import mobi.chouette.model.Transfers;
import mobi.chouette.model.type.ChouetteAreaEnum;

import javax.ejb.Stateless;

@Stateless(name = TransfersUpdater.BEAN_NAME)
public class TransfersUpdater implements Updater<Transfers> {

    public static final String BEAN_NAME = "TransfersUpdater";

    @Override
    public void update(Context context, Transfers oldValue, Transfers newValue) throws Exception {

        if (newValue.isSaved()) {
            return;
        }
        newValue.setSaved(true);

        if (oldValue.isDetached()) {
            // object does not exist in database
            oldValue.setObjectId(newValue.getObjectId());
            oldValue.setObjectVersion(newValue.getObjectVersion());
            oldValue.setCreationTime(newValue.getCreationTime());
            oldValue.setCreatorId(newValue.getCreatorId());
            oldValue.setFromRoute(newValue.getFromRoute());
            oldValue.setToRoute(newValue.getToRoute());
            oldValue.setFromStop(newValue.getFromStop());
            oldValue.setToStop(newValue.getToStop());
            oldValue.setFromTripId(newValue.getFromTripId());
            oldValue.setToTripId(newValue.getToTripId());
            oldValue.setTransferType(newValue.getTransferType());
            oldValue.setMinTransferTime(newValue.getMinTransferTime());
            oldValue.setDetached(false);
        } else {
            if (newValue.getObjectId() != null && !newValue.getObjectId().equals(oldValue.getObjectId())) {
                oldValue.setObjectId(newValue.getObjectId());
            }
            if (newValue.getObjectVersion() != null && !newValue.getObjectVersion().equals(oldValue.getObjectVersion())) {
                oldValue.setObjectVersion(newValue.getObjectVersion());
            }
            if (newValue.getCreationTime() != null && !newValue.getCreationTime().equals(oldValue.getCreationTime())) {
                oldValue.setCreationTime(newValue.getCreationTime());
            }
            if (newValue.getCreatorId() != null && !newValue.getCreatorId().equals(oldValue.getCreatorId())) {
                oldValue.setCreatorId(newValue.getCreatorId());
            }
            if (newValue.getFromRoute() != null && !newValue.getFromRoute().equals(oldValue.getFromRoute())) {
                oldValue.setFromRoute(newValue.getFromRoute());
            }
            if (newValue.getToRoute() != null && !newValue.getToRoute().equals(oldValue.getToRoute())) {
                oldValue.setToRoute(newValue.getToRoute());
            }
            if (newValue.getFromStop() != null && !newValue.getFromStop().equals(oldValue.getFromStop())) {
                if (newValue.getFromStop().getAreaType() == null) {
                    newValue.getFromStop().setAreaType(ChouetteAreaEnum.StopPlace);
                }
                oldValue.setFromStop(newValue.getFromStop());
            }
            if (newValue.getToStop() != null && !newValue.getToStop().equals(oldValue.getToStop())) {
                if (newValue.getToStop().getAreaType() == null) {
                    newValue.getToStop().setAreaType(ChouetteAreaEnum.StopPlace);
                }
                oldValue.setToStop(newValue.getToStop());
            }
            if (newValue.getFromTripId() != null && !newValue.getFromTripId().equals(oldValue.getFromTripId())) {
                oldValue.setFromTripId(newValue.getFromTripId());
            }
            if (newValue.getToTripId() != null && !newValue.getToTripId().equals(oldValue.getToTripId())) {
                oldValue.setToTripId(newValue.getToTripId());
            }
            if (newValue.getTransferType() != null && !newValue.getTransferType().equals(oldValue.getTransferType())) {
                oldValue.setTransferType(newValue.getTransferType());
            }
            if (newValue.getMinTransferTime() != null && !newValue.getMinTransferTime().equals(oldValue.getMinTransferTime())) {
                oldValue.setMinTransferTime(newValue.getMinTransferTime());
            }
        }
    }
}
