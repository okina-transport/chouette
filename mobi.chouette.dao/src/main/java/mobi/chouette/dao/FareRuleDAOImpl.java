package mobi.chouette.dao;

import mobi.chouette.model.FareRule;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless(name = "FareRuleDAO")
public class FareRuleDAOImpl extends GenericDAOImpl<FareRule> implements FareRuleDAO {

	public FareRuleDAOImpl() {
		super(FareRule.class);
	}

	@PersistenceContext(unitName = "referential")
	public void setEntityManager(EntityManager em) {
		this.em = em;
	}
}
