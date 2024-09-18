package mobi.chouette.dao;


import mobi.chouette.model.AccessibilityAssessment;

import java.util.List;


public interface AccessibilityAssessmentDAO extends GenericDAO<AccessibilityAssessment> {

    void deleteUnusedAccessibilityAssessments();

    AccessibilityAssessment findByAttributes(AccessibilityAssessment accessibilityAssessment);

    List<String> findAllAccessibilityAssessmentObjectIds();
}
