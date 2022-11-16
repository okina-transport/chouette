package mobi.chouette.dao;

import mobi.chouette.model.JourneyPattern;
import mobi.chouette.model.RouteSection;

import javax.persistence.EntityManager;

public interface RouteSectionDAO extends GenericDAO<RouteSection> {

    void deleteSectionUsedByJourneyPattern(JourneyPattern journeyPattern);

    EntityManager getEm();

}
