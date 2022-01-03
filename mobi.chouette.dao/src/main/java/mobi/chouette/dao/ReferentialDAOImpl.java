package mobi.chouette.dao;

import mobi.chouette.core.CoreException;
import mobi.chouette.core.CoreExceptionCode;
import mobi.chouette.model.Referential;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.transaction.Transactional;
import java.util.List;

import static mobi.chouette.common.Constant.SUPERSPACE_PREFIX;

@Stateless
public class ReferentialDAOImpl extends GenericDAOImpl<Referential> implements ReferentialDAO {

    public ReferentialDAOImpl() { super(Referential.class); }

    @PersistenceContext(unitName = "public")
    EntityManager em;


//    @PersistenceContext(unitName = "public")
//    public void setEntityManager(EntityManager em) {
//        this.em = em;
//    }

    @Override
    public List<String> getReferentials() {
        Query query = em.createNativeQuery("SELECT SLUG FROM PUBLIC.REFERENTIALS");
        return query.getResultList();
    }

    @Override
    public String getReferentialNameBySlug(String slug) {
        String result = (String) em.createNativeQuery("SELECT name " +
                " FROM PUBLIC.REFERENTIALS " +
                "WHERE LOWER(slug) = :slug")
                .setParameter("slug", slug.toLowerCase())
                .getSingleResult();
        return result;
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void dropSchema(String schema) {
        em.createNativeQuery("DROP SCHEMA " + schema + " CASCADE" ).executeUpdate();
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void renameSchemaForSimulation(String schema) {
        em.createNativeQuery("ALTER SCHEMA " + schema + " RENAME TO " + SUPERSPACE_PREFIX + "_" + schema).executeUpdate();
    }


}