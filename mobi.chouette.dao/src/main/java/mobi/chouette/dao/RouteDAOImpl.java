package mobi.chouette.dao;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import mobi.chouette.model.Route;
import mobi.chouette.model.type.PTDirectionEnum;

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
