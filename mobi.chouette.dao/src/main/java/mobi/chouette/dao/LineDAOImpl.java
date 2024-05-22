package mobi.chouette.dao;

import mobi.chouette.core.CoreException;
import mobi.chouette.core.CoreExceptionCode;
import mobi.chouette.model.Line;
import org.hibernate.Hibernate;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Stateless (name="LineDAO")
public class LineDAOImpl extends GenericDAOImpl<Line> implements LineDAO {

	public LineDAOImpl() {
		super(Line.class);
	}

	@PersistenceContext(unitName = "referential")
	public void setEntityManager(EntityManager em) {
		this.em = em;
	}

	public String updateStopareasForIdfmLineCommand(Long lineId) throws Exception
	{
		String retour = null;
		try {
			retour = (String) em.createNativeQuery("SELECT update_sa_for_idfm_line FROM update_sa_for_idfm_line(:lineId);")
					.setParameter("lineId", lineId)
					.getSingleResult();
		} catch (PersistenceException e){
			if(e.getCause().getCause().getMessage().contains("MOBIITI_SQL_ERROR:")){
				String[] splitError = e.getCause().getCause().getMessage().split("MOBIITI_SQL_ERROR:");
				throw new Exception("MOBIITI_SQL_ERROR:" + splitError[1]);
			} else {
				throw e;
			}
		} catch (Exception e) {
			throw e;
		}

		return retour;
	}

	@Override
	public void mergeDuplicateJourneyPatternsOfLineAndAddSuffix(Long lineId, String lineName) {
		em.createNativeQuery("SELECT merge_identicals_journey_patterns_for_line FROM merge_identicals_journey_patterns_for_line(:lineId, :lineName);")
				.setParameter("lineId", lineId)
				.setParameter("lineName", lineName)
				.getSingleResult();

		em.createNativeQuery("SELECT rename_identicals_journey_patterns_for_line FROM rename_identicals_journey_patterns_for_line(:lineId);")
				.setParameter("lineId", lineId)
				.getSingleResult();

	}

	@Override
	public List<Line> findByNetworkId(Long networkId) {
		return em.createQuery("SELECT l " +
				"                   FROM Line l " +
				"                   JOIN l.network n" +
				"                  WHERE n.id = :networkId", Line.class)
				.setParameter("networkId", networkId)
				.getResultList();
	}

	@Override
	public List<Line> findByNetworkIdNotDeleted(Long networkId) {
		return em.createQuery("SELECT l " +
						"              FROM Line l " +
						"              INNER JOIN l.network n" +
						"              ON n.id = :networkId" +
						"              WHERE l.supprime = false", Line.class)
				.setParameter("networkId", networkId)
				.getResultList();
	}

	@Override
	public List<Line> findNotDeleted() {
		return em.createQuery("SELECT l " +
						"              FROM Line l " +
						"              WHERE l.supprime = false", Line.class)
				.getResultList();
	}

	@Override
	public List<String> findObjectIdLinesInFirstDataspace(List<Long> ids, String dataspace) {
		return em.createNativeQuery("SELECT l.objectid " +
				"                   FROM " + dataspace + ".lines l " +
				"                  WHERE l.id IN :ids")
				.setParameter("ids", ids)
				.getResultList();
	}

	@Override
	public String removeDeletedLines() throws CoreException {

		String deletedLines = "";
		try {
			Object result = em.createNativeQuery(
							"SELECT remove_deleted_lines()")
							.getSingleResult();

			if (result instanceof String){
				deletedLines = (String) result;
			}

			return deletedLines;
		} catch (Exception e) {
			throw new CoreException(CoreExceptionCode.DELETE_IMPOSSIBLE,"Error while trying to remove deleted lines");
		}
	}

	@Override
	public Line findByObjectIdAndInitialize(String objectId) {

		Line  result = findByObjectId(objectId);

		if (result != null){
			Hibernate.initialize(result.getRoutes());
		}

		return result;
	}

	@Override
	public Map<String, String> findColorLines() {
		List<Object[]> results = em.createNativeQuery("SELECT l.objectid, l.color FROM lines l ")
				.getResultList();

		return results.stream()
				.filter(result -> result[1] != null)
				.collect(Collectors.toMap(
					result -> (String) result[0],
					result -> (String) result[1]
		));
	}
}
