package mobi.chouette.dao;

import mobi.chouette.model.TripCompany;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless
public class TripCompanyDAOImpl extends GenericDAOImpl<TripCompany> implements TripCompanyDAO {

	public TripCompanyDAOImpl() {
		super(TripCompany.class);
	}

	@PersistenceContext(unitName = "referential")
	public void setEntityManager(EntityManager em) {
		this.em = em;
	}

}
