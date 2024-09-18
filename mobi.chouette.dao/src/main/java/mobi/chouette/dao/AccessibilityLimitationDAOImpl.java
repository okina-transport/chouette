package mobi.chouette.dao;

import lombok.extern.log4j.Log4j;
import mobi.chouette.model.AccessibilityLimitation;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;


@Stateless (name="AccessibilityLimitationDAO")
@Log4j
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

	@Override
	public List<String> findAllAccessibilityLimitationObjectIds() {
		String jpql = "SELECT a.objectId FROM AccessibilityLimitation a";
		return em.createQuery(jpql, String.class).getResultList();
	}
}
