package mobi.chouette.dao;

import lombok.extern.log4j.Log4j;
import mobi.chouette.model.VehicleJourneyFacility;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless(name = "VehicleJourneyFacilitiesDAO")
@Log4j
public class VehicleJourneyFacilitiesDAOImpl extends GenericDAOImpl<VehicleJourneyFacility> implements VehicleJourneyFacilitiesDAO
{

    public VehicleJourneyFacilitiesDAOImpl() {
        super(VehicleJourneyFacility.class);
    }

    @PersistenceContext(unitName = "referential")
    public void setEntityManager(EntityManager em) {
        this.em = em;
    }

}
