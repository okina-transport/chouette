package mobi.chouette.dao;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import mobi.chouette.model.JourneyPattern;
import mobi.chouette.model.RouteSection;

@Stateless
public class RouteSectionDAOImpl extends GenericDAOImpl<RouteSection> implements RouteSectionDAO{

	public RouteSectionDAOImpl() {
		super(RouteSection.class);
	}

	@PersistenceContext(unitName = "referential")
	public void setEntityManager(EntityManager em) {
		this.em = em;
	}

	@Override
	public void deleteSectionUsedByJourneyPattern(JourneyPattern journeyPattern) {
		em.createNativeQuery("DELETE FROM route_sections rs " +
				" WHERE EXISTS (SELECT 1 FROM journey_pattern_sections jps WHERE jps.journey_pattern_id = :jpId AND jps.route_section_id = rs.id)" +
				"   AND NOT EXISTS (SELECT 1 FROM journey_pattern_sections jps WHERE jps.route_section_id = rs.id AND jps.journey_pattern_id != :jpId)")
				.setParameter("jpId", journeyPattern.getId())
				.executeUpdate();

		journeyPattern.getRouteSections().clear();
		em.flush();
		em.clear();
	}

	public EntityManager getEm() {
		return em;
	}

}
