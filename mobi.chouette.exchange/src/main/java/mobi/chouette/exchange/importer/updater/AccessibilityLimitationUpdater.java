package mobi.chouette.exchange.importer.updater;

import mobi.chouette.common.Context;
import mobi.chouette.model.AccessibilityLimitation;

import javax.ejb.Stateless;

@Stateless(name = AccessibilityLimitationUpdater.BEAN_NAME)
public class AccessibilityLimitationUpdater implements Updater<AccessibilityLimitation> {

    public static final String BEAN_NAME = "AccessibilityLimitationUpdater";

    @Override
    public void update(Context context, AccessibilityLimitation oldValue, AccessibilityLimitation newValue) {

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

        if (newValue.getWheelchairAccess() != null
                && !newValue.getWheelchairAccess().equals(oldValue.getWheelchairAccess())) {
            oldValue.setWheelchairAccess(newValue.getWheelchairAccess());
        }

        if (newValue.getVisualSignsAvailable() != null
                && !newValue.getVisualSignsAvailable().equals(oldValue.getVisualSignsAvailable())) {
            oldValue.setVisualSignsAvailable(newValue.getVisualSignsAvailable());
        }

        if (newValue.getStepFreeAccess() != null
                && !newValue.getStepFreeAccess().equals(oldValue.getStepFreeAccess())) {
            oldValue.setStepFreeAccess(newValue.getStepFreeAccess());
        }

        if (newValue.getLiftFreeAccess() != null
                && !newValue.getLiftFreeAccess().equals(oldValue.getLiftFreeAccess())) {
            oldValue.setLiftFreeAccess(newValue.getLiftFreeAccess());
        }

        if (newValue.getEscalatorFreeAccess() != null
                && !newValue.getEscalatorFreeAccess().equals(oldValue.getEscalatorFreeAccess())) {
            oldValue.setEscalatorFreeAccess(newValue.getEscalatorFreeAccess());
        }

        if (newValue.getAudibleSignalsAvailable() != null
                && !newValue.getAudibleSignalsAvailable().equals(oldValue.getAudibleSignalsAvailable())) {
            oldValue.setAudibleSignalsAvailable(newValue.getAudibleSignalsAvailable());
        }
    }

}
