package mobi.chouette.dao;


import mobi.chouette.model.Attribution;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless
public class AttributionDAOImpl extends GenericDAOImpl<Attribution> implements AttributionDAO{

	public AttributionDAOImpl() {
		super(Attribution.class);
	}

	@PersistenceContext(unitName = "referential")
	public void setEntityManager(EntityManager em) {
		this.em = em;
	}

}
