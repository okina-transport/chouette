package mobi.chouette.dao;

import mobi.chouette.model.OkinaAccessibilityLimitation;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;


@Stateless (name="AccessibilityLimitationDAO")
public class AccessibilityLimitationDAOImpl extends GenericDAOImpl<OkinaAccessibilityLimitation> implements AccessibilityLimitationDAO {

	public AccessibilityLimitationDAOImpl() {
		super(OkinaAccessibilityLimitation.class);
	}

	@PersistenceContext(unitName = "referential")
	public void setEntityManager(EntityManager em) {
		this.em = em;
	}


}
