package mobi.chouette.dao;


import mobi.chouette.model.Attribution;
import mobi.chouette.model.util.ObjectIdTypes;

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

	@Override
	public void insertAttribution(Attribution attribution) {
		//On persiste un fake objectId en premier afin de générer l'id via la séquence bdd
		attribution.setObjectId(String.valueOf(System.currentTimeMillis()));
		em.persist(attribution);

		//On corrige ensuite l'objectId
		attribution.setObjectId("MOBIITI:" + ObjectIdTypes.ATTRIBUTION_KEY + ":" + attribution.getId());
		em.merge(attribution);
	}

}
