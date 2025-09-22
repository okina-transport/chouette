package mobi.chouette.dao;

import mobi.chouette.model.Storage;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import java.util.Optional;

@Stateless
public class StorageDAOImpl extends GenericDAOImpl<Storage> implements StorageDAO {

    public StorageDAOImpl() {
        super(Storage.class);
    }

    @PersistenceContext(unitName = "referential")
    public void setEntityManager(EntityManager em) {
        this.em = em;
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public Optional<Storage> findByMaxStoredAt() {
        try {
            return Optional.of((Storage) em.createNativeQuery(
                            "SELECT * FROM storage WHERE stored_at = (SELECT max(stored_at) FROM storage)",
                            Storage.class)
                    .getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public Optional<Storage> findByMaxStoredAtAndRestoredAtNotNull() {
        try {
            return Optional.of((Storage) em.createNativeQuery(
                            "SELECT * FROM storage WHERE stored_at = (SELECT max(stored_at) FROM storage) AND restored_at is not null",
                            Storage.class)
                    .getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

}
