package mobi.chouette.dao;

import mobi.chouette.model.Company;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

@Stateless
public class CompanyDAOImpl extends GenericDAOImpl<Company> implements CompanyDAO{

	public CompanyDAOImpl() {
		super(Company.class);
	}

	@PersistenceContext(unitName = "referential")
	public void setEntityManager(EntityManager em) {
		this.em = em;
	}

	@Override
	public List<Company> findByNameAndActive(String name) {
		return em.createQuery("SELECT c FROM Company c WHERE c.name = :name and c.active = true ", Company.class)
				.setParameter("name", name)
				.getResultList();
	}

	@Override
	public List<Company> findByName(String name) {
		return em.createQuery("SELECT c FROM Company c WHERE c.name = :name", Company.class)
				.setParameter("name", name)
				.getResultList();
	}
}
