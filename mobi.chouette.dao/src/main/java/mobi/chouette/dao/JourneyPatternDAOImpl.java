package mobi.chouette.dao;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import mobi.chouette.core.CoreException;
import mobi.chouette.core.CoreExceptionCode;
import mobi.chouette.model.JourneyPattern;

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

}
