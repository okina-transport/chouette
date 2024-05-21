package mobi.chouette.exchange.importer.updater;

import mobi.chouette.common.Context;
import mobi.chouette.model.Train;

import javax.ejb.Stateless;

@Stateless(name = TrainUpdater.BEAN_NAME)
public class TrainUpdater implements Updater<Train> {

    public static final String BEAN_NAME = "TrainUpdater";

    @Override
    public void update(Context context, Train oldValue, Train newValue) throws Exception {
        if (newValue.isSaved()) {
            return;
        }

        newValue.setSaved(true);

        if (oldValue.isDetached()) {
            oldValue.setVersion(newValue.getVersion());
            oldValue.setDescription(newValue.getDescription());
            oldValue.setPublishedName(newValue.getPublishedName());
            oldValue.setDetached(false);
        } else {
            if (newValue.getVersion() != null && !newValue.getVersion().equals(oldValue.getVersion())) {
                oldValue.setVersion(newValue.getVersion());
            }
            if (newValue.getDescription() != null && !newValue.getDescription().equals(oldValue.getDescription())) {
                oldValue.setDescription(newValue.getDescription());
            }
            if (newValue.getPublishedName() != null && !newValue.getPublishedName().equals(oldValue.getPublishedName())) {
                oldValue.setPublishedName(newValue.getPublishedName());
            }
        }

    }
}