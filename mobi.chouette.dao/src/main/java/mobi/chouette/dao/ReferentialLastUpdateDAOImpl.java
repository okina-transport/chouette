package mobi.chouette.dao;

import lombok.extern.log4j.Log4j;
import mobi.chouette.model.ReferentialLastUpdate;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.time.LocalDateTime;

@Stateless(name = "ReferentialLastUpdateDAO")
@Log4j
public class ReferentialLastUpdateDAOImpl implements ReferentialLastUpdateDAO {

    @PersistenceContext(unitName = "referential")
    private EntityManager em;

    @Override
    public LocalDateTime getLastUpdateTimestamp() {
        TypedQuery<LocalDateTime> lastUpdateQuery = em.createQuery("select rlu.lastUpdateTimestamp from ReferentialLastUpdate rlu", LocalDateTime.class);
        return lastUpdateQuery.getSingleResult();
    }

    @Override
    public void setLastUpdateTimestamp(LocalDateTime lastUpdateTimestamp) {
        TypedQuery<ReferentialLastUpdate> lastUpdateQuery = em.createQuery("select rlu from ReferentialLastUpdate rlu", ReferentialLastUpdate.class);
        ReferentialLastUpdate referentialLastUpdate = lastUpdateQuery.getSingleResult();
        referentialLastUpdate.setLastUpdateTimestamp(lastUpdateTimestamp);
    }

}
