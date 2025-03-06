package mobi.chouette.dao;

import mobi.chouette.model.Transfers;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless(name = "TransfersDAO")
public class TransfersDAOImpl extends GenericDAOImpl<Transfers> implements TransfersDAO {

    public TransfersDAOImpl() {
        super(Transfers.class);
    }

    @PersistenceContext(unitName = "referential")
    public void setEntityManager(EntityManager em) {
        this.em = em;
    }
}
