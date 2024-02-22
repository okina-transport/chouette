package mobi.chouette.dao;

import mobi.chouette.model.AccessibilityLimitation;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;


@Stateless (name="AccessibilityLimitationDAO")
public class AccessibilityLimitationDAOImpl extends GenericDAOImpl<AccessibilityLimitation> implements AccessibilityLimitationDAO {

	public AccessibilityLimitationDAOImpl() {
		super(AccessibilityLimitation.class);
	}

	@PersistenceContext(unitName = "referential")
	public void setEntityManager(EntityManager em) {
		this.em = em;
	}

	@Override
	public void deleteUnusedAccessibilityLimitations() {
		em.createQuery("DELETE FROM AccessibilityLimitation al " +
						"WHERE NOT EXISTS (SELECT 1 FROM AccessibilityAssessment aa WHERE aa.accessibilityLimitation.id = al.id)")
				.executeUpdate();
	}


}
