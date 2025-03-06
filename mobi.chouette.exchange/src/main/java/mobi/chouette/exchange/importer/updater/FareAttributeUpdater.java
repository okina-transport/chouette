package mobi.chouette.exchange.importer.updater;

import mobi.chouette.common.Context;
import mobi.chouette.model.FareAttribute;

import javax.ejb.Stateless;

@Stateless(name = FareAttributeUpdater.BEAN_NAME)
public class FareAttributeUpdater implements Updater<FareAttribute> {

    public static final String BEAN_NAME = "FareAttributeUpdater";

    @Override
    public void update(Context context, FareAttribute oldValue, FareAttribute newValue) throws Exception {

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
            oldValue.setPrice(newValue.getPrice());
            oldValue.setCurrencyType(newValue.getCurrencyType());
            oldValue.setPaymentMethod(newValue.getPaymentMethod());
            oldValue.setTransfers(newValue.getTransfers());
            oldValue.setAgency(newValue.getAgency());
            oldValue.setTransferDuration(newValue.getTransferDuration());
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
            if (newValue.getPrice() != null && !newValue.getPrice().equals(oldValue.getPrice())) {
                oldValue.setPrice(newValue.getPrice());
            }
            if (newValue.getCurrencyType() != null && !newValue.getCurrencyType().equals(oldValue.getCurrencyType())) {
                oldValue.setCurrencyType(newValue.getCurrencyType());
            }
            if (newValue.getPaymentMethod() != null && !newValue.getPaymentMethod().equals(oldValue.getPaymentMethod())) {
                oldValue.setPaymentMethod(newValue.getPaymentMethod());
            }
            if (newValue.getTransfers() != null && !newValue.getTransfers().equals(oldValue.getTransfers())) {
                oldValue.setTransfers(newValue.getTransfers());
            }
            if (newValue.getAgency() != null && !newValue.getAgency().equals(oldValue.getAgency())) {
                oldValue.setAgency(newValue.getAgency());
            }
            if (newValue.getTransferDuration() != null && !newValue.getTransferDuration().equals(oldValue.getTransferDuration())) {
                oldValue.setTransferDuration(newValue.getTransferDuration());
            }
        }
    }
}
