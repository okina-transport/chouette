package mobi.chouette.dao;

import lombok.extern.log4j.Log4j;
import mobi.chouette.model.AccessibilityLimitation;
import org.hibernate.Session;
import org.jboss.jca.adapters.jdbc.WrappedConnection;
import org.postgresql.PGConnection;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.IOException;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.SQLException;


@Stateless (name="AccessibilityLimitationDAO")
@Log4j
public class AccessibilityLimitationDAOImpl extends GenericDAOImpl<AccessibilityLimitation> implements AccessibilityLimitationDAO {

	public AccessibilityLimitationDAOImpl() {
		super(AccessibilityLimitation.class);
	}

	@PersistenceContext(unitName = "referential")
	public void setEntityManager(EntityManager em) {
		this.em = em;
	}

	private String[] linesToCopy;

	private int MAX_NB_OF_LINES = 1000;

	@Override
	public void deleteUnusedAccessibilityLimitations() {
		em.createQuery("DELETE FROM AccessibilityLimitation al " +
						"WHERE NOT EXISTS (SELECT 1 FROM AccessibilityAssessment aa WHERE aa.accessibilityLimitation.id = al.id)")
				.executeUpdate();
	}
}
