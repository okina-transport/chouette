package mobi.chouette.dao;


import mobi.chouette.model.AccessibilityLimitation;

import java.util.List;


public interface AccessibilityLimitationDAO extends GenericDAO<AccessibilityLimitation> {

    void deleteUnusedAccessibilityLimitations();

    void copy(String data);

    void deleteAccessibilityLimitationVJ(List<String> accessibilityLimitationToDelete);
}
