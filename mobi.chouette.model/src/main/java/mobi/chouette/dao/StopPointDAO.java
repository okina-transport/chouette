package mobi.chouette.dao;

import mobi.chouette.model.JourneyPattern;
import mobi.chouette.model.StopPoint;
import org.hibernate.Hibernate;

import java.util.List;

public interface StopPointDAO extends GenericDAO<StopPoint> {

    List<StopPoint> getStopPointsofJourneyPattern(JourneyPattern jp);

}
