package mobi.chouette.dao;

import mobi.chouette.model.Network;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

@Stateless
public class NetworkDAOImpl extends GenericDAOImpl<Network> implements NetworkDAO{

	public NetworkDAOImpl() {
		super(Network.class);
	}

	@PersistenceContext(unitName = "referential")
	public void setEntityManager(EntityManager em) {
		this.em = em;
	}

	@Override
	public List<Network> findByNameAndNotSupprime(String name) {
		return em.createQuery("SELECT n FROM Network n WHERE n.name = :name AND n.supprime = false", Network.class)
				.setParameter("name", name)
				.getResultList();
	}
}
