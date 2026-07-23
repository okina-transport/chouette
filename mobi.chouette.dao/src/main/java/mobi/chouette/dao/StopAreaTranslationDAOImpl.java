package mobi.chouette.dao;

import mobi.chouette.model.StopAreaTranslation;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless(name = "StopAreaTranslationDAO")
public class StopAreaTranslationDAOImpl extends GenericDAOImpl<StopAreaTranslation> implements StopAreaTranslationDAO {

    public StopAreaTranslationDAOImpl() {
        super(StopAreaTranslation.class);
    }

    @PersistenceContext(unitName = "referential")
    public void setEntityManager(EntityManager em) {
        this.em = em;
    }

}
