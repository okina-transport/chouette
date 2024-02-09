package mobi.chouette.dao;

import mobi.chouette.model.AccessibilityAssessment;
import mobi.chouette.model.ConnectionLink;
import mobi.chouette.model.ConnectionLink_;
import mobi.chouette.model.Provider;


import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;


@Stateless (name="AccessibilityAssessmentDAO")
public class AccessibilityAssessmentDAOImpl extends GenericDAOImpl<AccessibilityAssessment> implements AccessibilityAssessmentDAO {

	public AccessibilityAssessmentDAOImpl() {
		super(AccessibilityAssessment.class);
	}

	@PersistenceContext(unitName = "referential")
	public void setEntityManager(EntityManager em) {
		this.em = em;
	}


	@Override
	public void deleteUnusedAccessibilityAssessments() {
		em.createQuery("DELETE FROM AccessibilityAssessment aa " +
				"WHERE NOT EXISTS (SELECT 1 FROM Line l WHERE l.accessibilityAssessment.id = aa.id) " +
				"AND NOT EXISTS (SELECT 1 FROM VehicleJourney vj WHERE vj.accessibilityAssessment.id = aa.id)")
				.executeUpdate();
	}

}
