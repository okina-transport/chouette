package mobi.chouette.dao;

import mobi.chouette.core.CoreException;
import mobi.chouette.core.CoreExceptionCode;
import mobi.chouette.model.Route;
import mobi.chouette.model.type.PTDirectionEnum;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

@Stateless
public class RouteDAOImpl extends GenericDAOImpl<Route> implements RouteDAO{

	public RouteDAOImpl() {
		super(Route.class);
	}

	@PersistenceContext(unitName = "referential")
	public void setEntityManager(EntityManager em) {
		this.em = em;
	}


	@Override
	public String removeDeletedRoutes() throws CoreException {

		String deletedRoutes = "";
		try {
			Object result = em.createNativeQuery(
					"SELECT remove_deleted_routes()")
					.getSingleResult();

			if (result instanceof String){
				deletedRoutes = (String) result;
			}

			return deletedRoutes;
		} catch (Exception e) {
			throw new CoreException(CoreExceptionCode.DELETE_IMPOSSIBLE,"Error while trying to remove deleted routes");
		}
	}

	@Override
	public List<Route> findByLineIdAndDirection(Long lineId, PTDirectionEnum direction) {
		return em.createQuery("SELECT r " +
				"                   FROM Route r " +
				"                   JOIN r.line l" +
				"                  WHERE l.id = :lineId" +
				"                AND r.direction = :direction", Route.class)
				.setParameter("lineId", lineId)
				.setParameter("direction", direction)
				.getResultList();
	}

}
