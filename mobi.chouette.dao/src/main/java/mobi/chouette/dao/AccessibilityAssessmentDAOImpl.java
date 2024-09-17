package mobi.chouette.dao;

import lombok.extern.log4j.Log4j;
import mobi.chouette.model.AccessibilityAssessment;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import java.util.List;


@Stateless(name = "AccessibilityAssessmentDAO")
@Log4j
public class AccessibilityAssessmentDAOImpl extends GenericDAOImpl<AccessibilityAssessment> implements AccessibilityAssessmentDAO {

    public AccessibilityAssessmentDAOImpl() {
        super(AccessibilityAssessment.class);
    }

    @PersistenceContext(unitName = "referential")
    public void setEntityManager(EntityManager em) {
        this.em = em;
    }

    private String[] linesToCopy;

    private int MAX_NB_OF_LINES = 1000;


    @Override
    public void deleteUnusedAccessibilityAssessments() {
        em.createQuery("DELETE FROM AccessibilityAssessment aa " +
                        "WHERE NOT EXISTS (SELECT 1 FROM Line l WHERE l.accessibilityAssessment.id = aa.id) " +
                        "AND NOT EXISTS (SELECT 1 FROM VehicleJourney vj WHERE vj.accessibilityAssessment.id = aa.id)")
                .executeUpdate();
    }

    @Override
    public AccessibilityAssessment findByAttributes(AccessibilityAssessment accessibilityAssessment) {
        String jpql = "SELECT a FROM AccessibilityAssessment a " +
                "WHERE a.accessibilityLimitation.wheelchairAccess = :wheelchairAccess " +
                "AND a.accessibilityLimitation.visualSignsAvailable = :visualSignsAvailable " +
                "AND a.accessibilityLimitation.stepFreeAccess = :stepFreeAccess " +
                "AND a.accessibilityLimitation.liftFreeAccess = :liftFreeAccess " +
                "AND a.accessibilityLimitation.escalatorFreeAccess = :escalatorFreeAccess " +
                "AND a.accessibilityLimitation.audibleSignalsAvailable = :audibleSignalsAvailable " +
                "AND a.mobilityImpairedAccess = :mobilityImpairedAccess ";
        try {
            return em.createQuery(jpql, AccessibilityAssessment.class)
                    .setParameter("wheelchairAccess", accessibilityAssessment.getAccessibilityLimitation().getWheelchairAccess())
                    .setParameter("visualSignsAvailable", accessibilityAssessment.getAccessibilityLimitation().getVisualSignsAvailable())
                    .setParameter("stepFreeAccess", accessibilityAssessment.getAccessibilityLimitation().getStepFreeAccess())
                    .setParameter("liftFreeAccess", accessibilityAssessment.getAccessibilityLimitation().getLiftFreeAccess())
                    .setParameter("escalatorFreeAccess", accessibilityAssessment.getAccessibilityLimitation().getEscalatorFreeAccess())
                    .setParameter("audibleSignalsAvailable", accessibilityAssessment.getAccessibilityLimitation().getAudibleSignalsAvailable())
                    .setParameter("mobilityImpairedAccess", accessibilityAssessment.getMobilityImpairedAccess())
                    .getResultList().stream().findFirst().orElse(null);
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public List<String> findAllAccessibilityAssessmentObjectIds() {
        String jpql = "SELECT a.objectId FROM AccessibilityAssessment a";
        return em.createQuery(jpql, String.class).getResultList();
    }

}
