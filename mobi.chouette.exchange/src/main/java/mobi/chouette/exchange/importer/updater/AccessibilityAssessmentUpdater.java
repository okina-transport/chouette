package mobi.chouette.exchange.importer.updater;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import mobi.chouette.common.Context;
import mobi.chouette.dao.AccessPointDAO;
import mobi.chouette.dao.AccessibilityAssessmentDAO;
import mobi.chouette.dao.AccessibilityLimitationDAO;
import mobi.chouette.model.AccessLink;
import mobi.chouette.model.AccessPoint;
import mobi.chouette.model.AccessibilityAssessment;
import mobi.chouette.model.AccessibilityLimitation;
import mobi.chouette.model.util.NamingUtil;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.Referential;

import java.util.Date;

@Stateless(name = AccessibilityAssessmentUpdater.BEAN_NAME)
public class AccessibilityAssessmentUpdater implements Updater<AccessibilityAssessment> {

    public static final String BEAN_NAME = "AccessibilityAssessmentUpdater";

    @EJB(beanName = AccessibilityLimitationUpdater.BEAN_NAME)
    private Updater<AccessibilityLimitation> accessibilityLimitationUpdater;

    @EJB
    private AccessibilityLimitationDAO accessibilityLimitationDAO;

    @Override
    public void update(Context context, AccessibilityAssessment oldValue, AccessibilityAssessment newValue) throws Exception {
        Referential cache = (Referential) context.get(CACHE);

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

        if (newValue.getMobilityImpairedAccess() != null
                && !newValue.getMobilityImpairedAccess().equals(oldValue.getMobilityImpairedAccess())) {
            oldValue.setMobilityImpairedAccess(newValue.getMobilityImpairedAccess());
        }

        // Accessibility limitation
        if (newValue.getAccessibilityLimitation() == null) {
            oldValue.setAccessibilityLimitation(null);
        } else {
            String objectId = newValue.getAccessibilityLimitation().getObjectId();
            AccessibilityLimitation accessibilityLimitation = cache.getAccessibilityLimitations().get(objectId);
            if (accessibilityLimitation == null) {
                accessibilityLimitation = accessibilityLimitationDAO.findByObjectId(objectId);
                if (accessibilityLimitation != null) {
                    cache.getAccessibilityLimitations().put(objectId, accessibilityLimitation);
                }
            }
            if (accessibilityLimitation == null) {
                accessibilityLimitation = ObjectFactory.getAccessibilityLimitation(cache, objectId);
            }
            oldValue.setAccessibilityLimitation(accessibilityLimitation);

            accessibilityLimitationUpdater.update(context, oldValue.getAccessibilityLimitation(), newValue.getAccessibilityLimitation());
        }
    }
}
