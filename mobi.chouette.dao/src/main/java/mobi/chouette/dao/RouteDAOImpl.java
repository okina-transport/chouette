package mobi.chouette.dao;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import mobi.chouette.core.CoreException;
import mobi.chouette.core.CoreExceptionCode;
import mobi.chouette.model.Route;

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

}
