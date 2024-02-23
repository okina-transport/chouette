package mobi.chouette.dao;


import mobi.chouette.model.AccessibilityLimitation;


public interface AccessibilityLimitationDAO extends GenericDAO<AccessibilityLimitation> {

    void deleteUnusedAccessibilityLimitations();

    void copy(String data);
}
