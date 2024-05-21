package mobi.chouette.dao;

import mobi.chouette.model.Train;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless
public class TrainDAOImpl extends GenericDAOImpl<Train> implements TrainDAO {

    public TrainDAOImpl() {
        super(Train.class);
    }

    @PersistenceContext(unitName = "referential")
    public void setEntityManager(EntityManager em) {
        this.em = em;
    }

}
