package mobi.chouette.dao;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import lombok.extern.slf4j.Slf4j;;

@Stateless(name = "DbStatusChecker")
@Slf4j
public class DbStatusCheckerImpl implements DbStatusChecker {

	@PersistenceContext(unitName = "public")
	private EntityManager entityManager;

	public boolean isDbUp() {
		try {
			if ((Integer) entityManager.createNativeQuery("SELECT 1").getSingleResult() == 1) {
				return true;
			}
		} catch (RuntimeException e) {
			log.warn("Failed while testing DB connection", e);
		}
		return false;
	}
}