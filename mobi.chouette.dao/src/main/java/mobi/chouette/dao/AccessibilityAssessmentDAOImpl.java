package mobi.chouette.dao;

import mobi.chouette.model.AccessibilityAssessment;


import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;


@Stateless (name="AccessibilityAssessmentDAO")
public class AccessibilityAssessmentDAOImpl extends GenericDAOImpl<AccessibilityAssessment> implements AccessibilityAssessmentDAO {

	public AccessibilityAssessmentDAOImpl() {
		super(AccessibilityAssessment.class);
	}

	@PersistenceContext(unitName = "referential")
	public void setEntityManager(EntityManager em) {
		this.em = em;
	}


}
