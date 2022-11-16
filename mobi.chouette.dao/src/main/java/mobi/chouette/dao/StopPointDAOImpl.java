package mobi.chouette.dao;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import mobi.chouette.model.JourneyPattern;
import mobi.chouette.model.StopPoint;
import org.hibernate.Hibernate;

import java.util.List;

@Stateless
public class StopPointDAOImpl extends GenericDAOImpl<StopPoint> implements StopPointDAO {

    public StopPointDAOImpl() {
        super(StopPoint.class);
    }

    @PersistenceContext(unitName = "referential")
    public void setEntityManager(EntityManager em) {
        this.em = em;
    }

    public List<StopPoint> getStopPointsofJourneyPattern(JourneyPattern jp) {
        List<StopPoint> stopPoints = em
                .createNativeQuery("SELECT s.* from stop_points s " +
                                "JOIN journey_patterns_stop_points jpsp ON jpsp.stop_point_id = s.id " +
                                "WHERE jpsp.journey_pattern_id = :pattern " +
                                "ORDER BY s.position",
                        StopPoint.class)
                .setParameter("pattern", jp.getId())
                .getResultList();

        for (StopPoint stopPoint : stopPoints) {
            Hibernate.initialize(stopPoint.getScheduledStopPoint());
            Hibernate.initialize(stopPoint.getScheduledStopPoint().getContainedInStopAreaRef().getObject());
            Hibernate.initialize(stopPoint.getScheduledStopPoint().getContainedInStopAreaRef().getObject().getParent());
        }

        return stopPoints;
    }
}
