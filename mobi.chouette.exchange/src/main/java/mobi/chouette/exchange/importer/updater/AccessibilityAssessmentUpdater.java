package mobi.chouette.exchange.importer.updater;

import mobi.chouette.common.Context;
import mobi.chouette.model.AccessibilityAssessment;
import mobi.chouette.model.AccessibilityLimitation;

import javax.ejb.EJB;
import javax.ejb.Stateless;

@Stateless(name = AccessibilityAssessmentUpdater.BEAN_NAME)
public class AccessibilityAssessmentUpdater implements Updater<AccessibilityAssessment> {

    public static final String BEAN_NAME = "AccessibilityAssessmentUpdater";

    @EJB(beanName = AccessibilityLimitationUpdater.BEAN_NAME)
    private Updater<AccessibilityLimitation> accessibilityLimitationUpdater;

    @Override
    public void update(Context context, AccessibilityAssessment oldValue, AccessibilityAssessment newValue) throws Exception {
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
            accessibilityLimitationUpdater.update(context, oldValue.getAccessibilityLimitation(), newValue.getAccessibilityLimitation());
        }
    }

}
