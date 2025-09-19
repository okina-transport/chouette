package mobi.chouette.dao;

import mobi.chouette.model.Referential;
import org.apache.commons.lang.StringEscapeUtils;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.transaction.Transactional;
import java.util.List;

@Stateless
public class ReferentialDAOImpl extends GenericDAOImpl<Referential> implements ReferentialDAO {

    @PersistenceContext(unitName = "public")
    EntityManager em;

    public ReferentialDAOImpl() { super(Referential.class); }


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
    @Override
    public void dropSchemaIfExists(String schema) {
        // can't use a parametrized query parameter for 'schema'
        em.createNativeQuery("DROP SCHEMA IF EXISTS " + StringEscapeUtils.escapeSql(schema)  + " CASCADE").executeUpdate();
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    @Override
    public void renameSchema(String from,  String to) {
        // can't use a parametrized query parameter for 'from' and 'to'
        em.createNativeQuery("ALTER SCHEMA " + StringEscapeUtils.escapeSql(from) + " RENAME TO " + StringEscapeUtils.escapeSql(to)).executeUpdate();
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    @Override
    public Boolean checkSchemaExists(String schema) {
        return (Boolean) em.createNativeQuery("SELECT EXISTS(SELECT 1 FROM pg_namespace WHERE nspname = :schema);")
                .setParameter("schema", schema)
                .getSingleResult();
    }


}