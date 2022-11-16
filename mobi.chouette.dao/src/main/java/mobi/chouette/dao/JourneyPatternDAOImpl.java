package mobi.chouette.dao;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import mobi.chouette.core.CoreException;
import mobi.chouette.core.CoreExceptionCode;
import mobi.chouette.model.JourneyPattern;
import mobi.chouette.model.Line;
import mobi.chouette.model.StopPoint;
import mobi.chouette.model.type.SectionStatusEnum;
import org.hibernate.Hibernate;
import org.wololo.geojson.GeoJSON;

import java.util.List;

@Stateless
public class JourneyPatternDAOImpl extends GenericDAOImpl<JourneyPattern> implements JourneyPatternDAO{

	public JourneyPatternDAOImpl() {
		super(JourneyPattern.class);
	}

	@PersistenceContext(unitName = "referential")
	public void setEntityManager(EntityManager em) {
		this.em = em;
	}

	@Override
	public String removeDeletedJourneyPatterns() throws CoreException {

		String deletedJourneyPatterns = "";
		try {
			Object result = em.createNativeQuery(
					"SELECT remove_deleted_journey_patterns()")
					.getSingleResult();

			if (result instanceof String){
				deletedJourneyPatterns = (String) result;
			}

			return deletedJourneyPatterns;
		} catch (Exception e) {
			throw new CoreException(CoreExceptionCode.DELETE_IMPOSSIBLE,"Error while trying to remove deleted journey patterns");
		}
	}

	@Override
	public JourneyPattern findByIdMapMatchingLazyDeps(Long journeyPatternId) {
		JourneyPattern journeyPattern = find(journeyPatternId);
		if (journeyPattern != null) {
			Hibernate.initialize(journeyPattern.getRoute());
			Hibernate.initialize(journeyPattern.getRoute().getLine());
			Hibernate.initialize(journeyPattern.getStopPoints());
			for (StopPoint stopPoint : journeyPattern.getStopPoints()) {
				Hibernate.initialize(stopPoint.getScheduledStopPoint());
				Hibernate.initialize(stopPoint.getScheduledStopPoint().getContainedInStopAreaRef().getObject());
			}
			Hibernate.initialize(journeyPattern.getRouteSections());
			Hibernate.initialize(journeyPattern.getProfileOSRMJourneyPatterns());
			Hibernate.initialize(journeyPattern.getLatLngMapMatching());
			journeyPattern.getLatLngMapMatching().stream().filter(latLngMapMatching -> latLngMapMatching.getStopPoint() != null).forEach(latLngMapMatching -> {
				Hibernate.initialize(latLngMapMatching.getStopPoint());
				Hibernate.initialize(latLngMapMatching.getStopPoint().getScheduledStopPoint().getContainedInStopAreaRef().getObject());
			});
			journeyPattern.getProfileOSRMJourneyPatterns().forEach(profileOSRMJourneyPattern -> {
				Hibernate.initialize(profileOSRMJourneyPattern.getProfileOSRMInterStopJourneyPatternList());
				profileOSRMJourneyPattern.getProfileOSRMInterStopJourneyPatternList().forEach(pInterStop -> {
					Hibernate.initialize(pInterStop.getArrivalStopPoint());
					Hibernate.initialize(pInterStop.getArrivalStopPoint().getScheduledStopPoint().getContainedInStopAreaRef().getObject());
					Hibernate.initialize(pInterStop.getDepartureStopPoint());
					Hibernate.initialize(pInterStop.getDepartureStopPoint().getScheduledStopPoint().getContainedInStopAreaRef().getObject());
				});
			});
		}
		return journeyPattern;
	}

	@Override
	public List<JourneyPattern> getEnabledJourneyOfLine(Line line) {
		List<JourneyPattern> resultList = em.createQuery("SELECT j FROM JourneyPattern j JOIN j.route r"
						+ " WHERE r.line = :line"
						+ " AND (j.supprime = false OR j.supprime IS NULL) "
						+ " ORDER BY j.name",
				JourneyPattern.class)
				.setParameter("line", line)
				.getResultList();

		for (JourneyPattern journeyPattern : resultList) {
			Hibernate.initialize(journeyPattern.getRoute());
			Hibernate.initialize(journeyPattern.getRoute().getLine());
			Hibernate.initialize(journeyPattern.getDepartureStopPoint());
			Hibernate.initialize(journeyPattern.getArrivalStopPoint());
		}

		return resultList;
	}

	@Override
	public JourneyPattern updateGeoJson(JourneyPattern journeyPattern, GeoJSON geoJson) {
		journeyPattern.setGeojson(geoJson);
		journeyPattern.setSectionStatus(SectionStatusEnum.Completed);
		return em.merge(journeyPattern);
	}

}
