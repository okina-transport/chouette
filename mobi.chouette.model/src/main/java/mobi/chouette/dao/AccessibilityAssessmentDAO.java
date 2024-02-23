package mobi.chouette.dao;


import mobi.chouette.model.AccessibilityAssessment;

import java.util.HashMap;


public interface AccessibilityAssessmentDAO extends GenericDAO<AccessibilityAssessment> {

    void deleteUnusedAccessibilityAssessments();

    void copy(String data);

    void updateVjAccessiblityAssessment(HashMap<String, String> mapIdsAaAl);

    void updateAccessiblityAssessmentAccessibilityLimitation(HashMap<String, String> mapIdsAaAl);
}
