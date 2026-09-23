package mobi.chouette.dao;

import mobi.chouette.model.Company;
import mobi.chouette.model.type.OrganisationTypeEnum;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
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
	public List<Company> findActiveCompaniesByNameAndOrganisationType(String name, OrganisationTypeEnum organisationType) {
		return em.createQuery("SELECT c FROM Company c WHERE c.active = true AND c.name = :name AND c.organisationType = :orgType", Company.class)
				.setParameter("name", name)
				.setParameter("orgType", organisationType)
				.getResultList();
	}

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    @Override
    public List<Company> findActiveCompaniesNewTransaction() {
        return em.createQuery("SELECT c FROM Company c WHERE c.active = true", Company.class)
                .getResultList();
    }

}
