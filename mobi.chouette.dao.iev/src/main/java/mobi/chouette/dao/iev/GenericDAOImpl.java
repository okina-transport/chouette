package mobi.chouette.dao.iev;

import com.google.common.collect.Iterables;
import mobi.chouette.dao.GenericDAO;
import org.hibernate.Session;

import javax.persistence.*;
import javax.persistence.criteria.*;
import java.util.Collection;
import java.util.List;

public abstract class GenericDAOImpl<T> implements GenericDAO<T> {

	protected EntityManager em;

	protected Class<T> type;

	public GenericDAOImpl(Class<T> type) {
		this.type = type;
	}

	@Override
	public T find(final Object id) {
		return em.find(type, id);
	}

	@Override
	public List<T> find(final String hql, final List<Object> values) {
		List<T> result = null;
		if (values.isEmpty()) {
			TypedQuery<T> query = em.createQuery(hql, type);
			result = query.getResultList();
		} else {
			TypedQuery<T> query = em.createQuery(hql, type);
			int pos = 0;
			for (Object value : values) {
				query.setParameter(pos++, value);
			}
			result = query.getResultList();
		}
		return result;
	}

	@Override
	public List<T> findAll(final Collection<Long> ids) {
		List<T> result = null;
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<T> criteria = builder.createQuery(type);
		Root<T> root = criteria.from(type);
		Predicate predicate = builder.in(root.get("id")).value(ids);
		criteria.where(predicate);
		TypedQuery<T> query = em.createQuery(criteria);
		result = query.getResultList();
		return result;
	}
	
	@Override
	public List<T> findAll() {
		List<T> result = null;
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<T> criteria = builder.createQuery(type);
		criteria.from(type);
		TypedQuery<T> query = em.createQuery(criteria);
		result = query.getResultList();
		return result;
	}

	@SuppressWarnings("unchecked")
	@Override
	public T findByObjectId(final String objectId) {
		Session session = em.unwrap(Session.class);
		T result = (T) session.bySimpleNaturalId(type).load(objectId);

		return result;
	}


	/**
	 * Find entities by object ids.
	 * @param objectIds
	 * @return
	 */
	@Override
	public List<T> findByObjectId(final Collection<String> objectIds) {
		return findByObjectId(objectIds, true);
	}

	/**
	 * Find entities by object ids, without flushing the session first for performance.
	 * This assumes that there is no pending update in the persistence context
	 * @param objectIds
	 * @return
	 */
	@Override
	public List<T> findByObjectIdNoFlush(final Collection<String> objectIds) {
		return findByObjectId(objectIds, false);
	}

	private List<T> findByObjectId(final Collection<String> objectIds, boolean flush) {
		List<T> result = null;
		if (objectIds.isEmpty())
			return result;

		Iterable<List<String>> iterator = Iterables.partition(objectIds, 32000);
		for (List<String> ids : iterator) {
			CriteriaBuilder builder = em.getCriteriaBuilder();
			CriteriaQuery<T> criteria = builder.createQuery(type);
			Root<T> root = criteria.from(type);
			Predicate predicate = builder.in(root.get("objectId")).value(ids);
			criteria.where(predicate);
			TypedQuery<T> query = em.createQuery(criteria);
			if(!flush) {
				query.setFlushMode(FlushModeType.COMMIT);
			}
			if (result == null)
				result = query.getResultList();
			else
				result.addAll(query.getResultList());
		}
		return result;
	}

	@Override
	public void create(final T entity) {		
		em.persist(entity);
	}

	@Override
	public T update(final T entity) {
		return em.merge(entity);
	}

	@Override
	public void delete(final T entity) {
		em.remove(entity);
	}

	@Override
	public void detach(final T entity) {
		em.detach(entity);
	}

	@Override
	public int deleteAll() {
		int result = 0;
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaDelete<T> criteria = builder.createCriteriaDelete(type);
		criteria.from(type);
		Query query = em.createQuery(criteria);
		result = query.executeUpdate();
		return result;
	}
	
	@Override
	public int truncate() {
		String query = "TRUNCATE TABLE " +
				type.getAnnotation(Table.class).name() +
				" CASCADE";
        return em.createNativeQuery(query).executeUpdate();
	}


	@Override
	public void evictAll() {
		EntityManagerFactory factory = em.getEntityManagerFactory();
		Cache cache = factory.getCache();
		cache.evictAll();
	}

	@Override
	public void flush() {
		em.flush();
	}

	@Override
	public void detach(Collection<?> list) {
		for (Object object : list) {
			em.detach(object);
		}
	}

}
