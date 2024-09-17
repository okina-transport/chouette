package mobi.chouette.dao;


import mobi.chouette.model.AccessibilityLimitation;

import java.util.List;


public interface AccessibilityLimitationDAO extends GenericDAO<AccessibilityLimitation> {

    void deleteUnusedAccessibilityLimitations();

    List<String> findAllAccessibilityLimitationObjectIds();
}
