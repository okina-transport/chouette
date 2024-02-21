package mobi.chouette.dao;


import mobi.chouette.model.AccessibilityAssessment;

import java.util.List;


public interface AccessibilityAssessmentDAO extends GenericDAO<AccessibilityAssessment> {

    void deleteUnusedAccessibilityAssessments();

    void copy(String data);

    void deleteAccessibilityAssessmentVJ(List<String> objectIds);
}
