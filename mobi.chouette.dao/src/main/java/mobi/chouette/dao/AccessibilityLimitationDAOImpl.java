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

	@Override
	public void copy(final String data) {

		log.info("Copy accessibility limitation started");

		Session session = em.unwrap(Session.class);

		session.doWork(connection -> {
            // Monitor monitor = MonitorFactory.start("COPY");

            linesToCopy = data.split("\n");
            String currentBatchOfLines = null;
            int nbOfLinesInBatch = 0;

            for (int i = 0; i < linesToCopy.length; i++) {

                if (!linesToCopy[i].isEmpty()) {
                    if (currentBatchOfLines != null) {
                        currentBatchOfLines = currentBatchOfLines + linesToCopy[i] + "\n";
                    } else {
                        currentBatchOfLines = linesToCopy[i] + "\n";
                    }
                    nbOfLinesInBatch++;
                }

                if (nbOfLinesInBatch == MAX_NB_OF_LINES) {
                    launchCopyAccessibilityLimitation(connection, currentBatchOfLines);
                    currentBatchOfLines = null;
                    nbOfLinesInBatch = 0;
                }
            }

            if (currentBatchOfLines != null) {
                launchCopyAccessibilityLimitation(connection, currentBatchOfLines);
            }

            log.info("Copy accessibility limitation finished");
            // log.info(Color.MAGENTA + monitor.stop() + Color.NORMAL);
        });
	}


	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public void launchCopyAccessibilityLimitation(Connection connection, String currentBatchOfLines) throws SQLException {

		try {
			StringReader from = new StringReader(currentBatchOfLines);

			PGConnection pgConnection = (PGConnection) ((WrappedConnection) connection)
					.getUnderlyingConnection();
			org.postgresql.copy.CopyManager manager = pgConnection
					.getCopyAPI();
			String copyStatement = "COPY accessibility_limitation("
					+ "objectid, object_version, creation_time, creator_id, "
					+ "wheelchair_access, visual_signs_available, step_free_access, lift_free_access, escalator_free_access, audible_signals_available)"
					+ " FROM STDIN WITH DELIMITER '|'";

			manager.copyIn(copyStatement, from);


		} catch (IOException e) {
			log.error("Error while copying accessibility limitation");
			log.error(e);
		}
	}
}
