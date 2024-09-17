package mobi.chouette.dao;


import mobi.chouette.model.AccessibilityAssessment;

import java.util.HashMap;


public interface AccessibilityAssessmentDAO extends GenericDAO<AccessibilityAssessment> {

    void deleteUnusedAccessibilityAssessments();

    AccessibilityAssessment findByAttributes(AccessibilityAssessment accessibilityAssessment);
}
