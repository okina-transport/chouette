package mobi.chouette.dao;

import lombok.extern.log4j.Log4j;
import mobi.chouette.model.VehicleJourney;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

@Stateless
@Log4j
public class VehicleJourneyDAOImpl extends GenericDAOImpl<VehicleJourney> implements VehicleJourneyDAO {

    public VehicleJourneyDAOImpl() {
        super(VehicleJourney.class);
    }

    @PersistenceContext(unitName = "referential")
    public void setEntityManager(EntityManager em) {
        this.em = em;
    }

    public long updateAcessibilityId(Long accessibilityId, List<String> objectIds){
        return em.createNativeQuery("UPDATE vehicle_journeys SET accessibility_assessment_id = :accessId WHERE objectid IN (:objectIdList)")
                            .setParameter("accessId", accessibilityId)
                            .setParameter("objectIdList", objectIds)
                            .executeUpdate();
    }

    @Override
    public long updateBrandingId(Long brandingId, List<String> objectIds) {
       return em.createNativeQuery("UPDATE vehicle_journeys SET branding_id = :brandId WHERE objectid IN (:objectIdList)")
                .setParameter("brandId", brandingId)
                .setParameter("objectIdList", objectIds)
                .executeUpdate();
    }

    public long updateDefaultAccessibility(Long defaultAccessibilityId){
        return em.createNativeQuery("UPDATE vehicle_journeys SET accessibility_assessment_id = :accessId WHERE accessibility_assessment_id IS NULL")
                .setParameter("accessId", defaultAccessibilityId)
                .executeUpdate();
    }

}
