package mobi.chouette.exchange.importer.updater;

import mobi.chouette.common.Context;
import mobi.chouette.dao.LineDAO;
import mobi.chouette.model.Line;
import mobi.chouette.model.LineTranslation;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.Referential;

import javax.ejb.EJB;
import javax.ejb.Stateless;

@Stateless(name = LineTranslationUpdater.BEAN_NAME)
public class LineTranslationUpdater extends TranslationUpdater<LineTranslation> implements Updater<LineTranslation> {

    public static final String BEAN_NAME = "LineTranslationUpdater";

    @EJB
    private LineDAO lineDAO;

    @Override
    public void update(Context context, LineTranslation oldValue, LineTranslation newValue) throws Exception {
        super.update(context, oldValue, newValue);
        if (newValue.isSaved()) {
            return;
        }
        newValue.setSaved(true);

        if (newValue.getTargetObjectId() == null) {
            oldValue.setLine(null);
            return;
        }

        Referential cache = (Referential) context.get(CACHE);
        String objectId = newValue.getTargetObjectId();
        Line line = cache.getLines().get(objectId);
        if (line == null) {
            line = lineDAO.findByObjectId(objectId);
            if (line != null) {
                cache.getLines().put(objectId, line);
            }
        }
        if (line == null) {
            line = ObjectFactory.getLine(cache, objectId);
        }
        oldValue.setLine(line);
    }

}
