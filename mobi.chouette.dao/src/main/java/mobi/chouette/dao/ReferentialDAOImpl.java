package mobi.chouette.dao;

import mobi.chouette.model.Referential;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.transaction.Transactional;
import java.util.List;

@Stateless
public class ReferentialDAOImpl extends GenericDAOImpl<Referential> implements ReferentialDAO {

    private static final String SCHEMA_NAME_REGEXP = "^[a-zA-Z0-9_]+$";

    @PersistenceContext(unitName = "public")
    EntityManager em;

    public ReferentialDAOImpl() { super(Referential.class); }

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
        if (!schema.matches(SCHEMA_NAME_REGEXP)) {
            throw new IllegalArgumentException("Invalid schema name");
        }
        em.createNativeQuery("DROP SCHEMA IF EXISTS " + schema  + " CASCADE").executeUpdate();
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    @Override
    public void renameSchema(String from,  String to) {
        // can't use a parametrized query parameter for 'from' and 'to'
        if (!from.matches(SCHEMA_NAME_REGEXP) || !to.matches(SCHEMA_NAME_REGEXP)) {
            throw new IllegalArgumentException("Invalid schema name");
        }
        em.createNativeQuery("ALTER SCHEMA " + from + " RENAME TO " + to).executeUpdate();
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    @Override
    public Boolean checkSchemaExists(String schema) {
        return (Boolean) em.createNativeQuery("SELECT EXISTS(SELECT 1 FROM pg_namespace WHERE nspname = :schema);")
                .setParameter("schema", schema)
                .getSingleResult();
    }


}