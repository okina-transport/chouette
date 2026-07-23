package mobi.chouette.dao;

import mobi.chouette.model.VehicleJourneyTranslation;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless(name = "VehicleJourneyTranslationDAO")
public class VehicleJourneyTranslationDAOImpl extends GenericDAOImpl<VehicleJourneyTranslation> implements VehicleJourneyTranslationDAO {

    public VehicleJourneyTranslationDAOImpl() {
        super(VehicleJourneyTranslation.class);
    }

    @PersistenceContext(unitName = "referential")
    public void setEntityManager(EntityManager em) {
        this.em = em;
    }

}
