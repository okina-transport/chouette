package mobi.chouette.exchange.importer.updater;

import mobi.chouette.common.Context;
import mobi.chouette.model.FareRule;

import javax.ejb.Stateless;

@Stateless(name = FareRuleUpdater.BEAN_NAME)
public class FareRuleUpdater implements Updater<FareRule> {

    public static final String BEAN_NAME = "FareRuleUpdater";

    @Override
    public void update(Context context, FareRule oldValue, FareRule newValue) throws Exception {

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
            oldValue.setRoute(newValue.getRoute());
            oldValue.setOriginId(newValue.getOriginId());
            oldValue.setDestinationId(newValue.getDestinationId());
            oldValue.setContainsId(newValue.getContainsId());
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
            if (newValue.getRoute() != null && !newValue.getRoute().equals(oldValue.getRoute())) {
                oldValue.setRoute(newValue.getRoute());
            }
            if (newValue.getOriginId() != null && !newValue.getOriginId().equals(oldValue.getOriginId())) {
                oldValue.setOriginId(newValue.getOriginId());
            }
            if (newValue.getDestinationId() != null && !newValue.getDestinationId().equals(oldValue.getDestinationId())) {
                oldValue.setDestinationId(newValue.getDestinationId());
            }
            if (newValue.getContainsId() != null && !newValue.getContainsId().equals(oldValue.getContainsId())) {
                oldValue.setContainsId(newValue.getContainsId());
            }
        }
    }
}
