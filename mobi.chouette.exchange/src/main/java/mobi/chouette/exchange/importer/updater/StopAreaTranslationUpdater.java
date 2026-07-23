package mobi.chouette.exchange.importer.updater;

import mobi.chouette.common.Context;
import mobi.chouette.dao.StopAreaDAO;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.StopAreaTranslation;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.Referential;

import javax.ejb.EJB;
import javax.ejb.Stateless;

@Stateless(name = StopAreaTranslationUpdater.BEAN_NAME)
public class StopAreaTranslationUpdater extends TranslationUpdater<StopAreaTranslation> implements
        Updater<StopAreaTranslation> {

    public static final String BEAN_NAME = "StopAreaTranslationUpdater";

    @EJB
    private StopAreaDAO stopAreaDAO;

    @Override
    public void update(Context context, StopAreaTranslation oldValue, StopAreaTranslation newValue) throws Exception {
        super.update(context, oldValue, newValue);
        if (newValue.isSaved()) {
            return;
        }
        newValue.setSaved(true);

        if (newValue.getTargetObjectId() == null) {
            oldValue.setStopArea(null);
            return;
        }

        Referential cache = (Referential) context.get(CACHE);
        String objectId = newValue.getTargetObjectId();
        StopArea stopArea = cache.getStopAreas().get(objectId);
        if (stopArea == null) {
            stopArea = stopAreaDAO.findByObjectId(objectId);
            if (stopArea != null) {
                cache.getStopAreas().put(objectId, stopArea);
            }
        }
        if (stopArea == null) {
            stopArea = ObjectFactory.getStopArea(cache, objectId);
        }
        oldValue.setStopArea(stopArea);
    }

}
