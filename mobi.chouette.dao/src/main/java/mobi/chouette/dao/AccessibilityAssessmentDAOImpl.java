package mobi.chouette.dao;

import mobi.chouette.model.OkinaAccessibilityAssessment;


import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;


@Stateless (name="AccessibilityAssessmentDAO")
public class AccessibilityAssessmentDAOImpl extends GenericDAOImpl<OkinaAccessibilityAssessment> implements AccessibilityAssessmentDAO {

	public AccessibilityAssessmentDAOImpl() {
		super(OkinaAccessibilityAssessment.class);
	}

	@PersistenceContext(unitName = "referential")
	public void setEntityManager(EntityManager em) {
		this.em = em;
	}


}
