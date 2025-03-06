package mobi.chouette.dao;

import mobi.chouette.model.Agency;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

@Stateless
public class AgencyDAOImpl extends GenericDAOImpl<Agency> implements AgencyDAO {

    public AgencyDAOImpl() {
        super(Agency.class);
    }

    @PersistenceContext(unitName = "referential")
    public void setEntityManager(EntityManager em) {
        this.em = em;
    }

    @Override
    public List<Agency> findByAgencyIds(List<String> agencyIds) {
        return em.createQuery("SELECT a " +
                        "                   FROM Agency a " +
                        "                  WHERE a.agencyId in :agencyIds", Agency.class)
                .setParameter("agencyIds", agencyIds)
                .getResultList();
    }

    @Override
    public Agency findByAgencyId(String agencyId) {
        return em.createQuery("SELECT a " +
                        "                   FROM Agency a " +
                        "                  WHERE a.agencyId = :agencyId", Agency.class)
                .setParameter("agencyId", agencyId)
                .getResultList().stream().findFirst().orElse(null);
    }
}
