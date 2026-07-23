package mobi.chouette.exchange.importer.updater;

import mobi.chouette.common.Context;
import mobi.chouette.dao.NetworkDAO;
import mobi.chouette.model.Network;
import mobi.chouette.model.NetworkTranslation;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.Referential;

import javax.ejb.EJB;
import javax.ejb.Stateless;

@Stateless(name = NetworkTranslationUpdater.BEAN_NAME)
public class NetworkTranslationUpdater extends TranslationUpdater<NetworkTranslation> implements Updater<NetworkTranslation> {

    public static final String BEAN_NAME = "NetworkTranslationUpdater";

    @EJB
    private NetworkDAO networkDAO;

    @Override
    public void update(Context context, NetworkTranslation oldValue, NetworkTranslation newValue) throws Exception {
        super.update(context, oldValue, newValue);
        if (newValue.isSaved()) {
            return;
        }
        newValue.setSaved(true);

        if (newValue.getTargetObjectId() == null) {
            oldValue.setNetwork(null);
            return;
        }

        Referential cache = (Referential) context.get(CACHE);
        String objectId = newValue.getTargetObjectId();
        Network network = cache.getPtNetworks().get(objectId);
        if (network == null) {
            network = networkDAO.findByObjectId(objectId);
            if (network != null) {
                cache.getPtNetworks().put(objectId, network);
            }
        }
        if (network == null) {
            network = ObjectFactory.getPTNetwork(cache, objectId);
        }
        oldValue.setNetwork(network);
    }

}
