package mobi.chouette.dao;

import mobi.chouette.model.TripExtension;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless
public class TripExtensionDAOImpl extends GenericDAOImpl<TripExtension> implements TripExtensionDAO {

	public TripExtensionDAOImpl() {
		super(TripExtension.class);
	}

	@PersistenceContext(unitName = "referential")
	public void setEntityManager(EntityManager em) {
		this.em = em;
	}

}
