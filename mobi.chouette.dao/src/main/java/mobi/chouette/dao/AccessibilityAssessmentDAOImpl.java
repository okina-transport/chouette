package mobi.chouette.dao;

import lombok.extern.log4j.Log4j;
import mobi.chouette.model.AccessibilityAssessment;
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
import java.sql.Statement;
import java.util.List;


@Stateless (name="AccessibilityAssessmentDAO")
@Log4j
public class AccessibilityAssessmentDAOImpl extends GenericDAOImpl<AccessibilityAssessment> implements AccessibilityAssessmentDAO {

	public AccessibilityAssessmentDAOImpl() {
		super(AccessibilityAssessment.class);
	}

	@PersistenceContext(unitName = "referential")
	public void setEntityManager(EntityManager em) {
		this.em = em;
	}

	private String[] linesToCopy;

	private int MAX_NB_OF_LINES = 1000;


	@Override
	public void deleteUnusedAccessibilityAssessments() {
		em.createQuery("DELETE FROM AccessibilityAssessment aa " +
				"WHERE NOT EXISTS (SELECT 1 FROM Line l WHERE l.accessibilityAssessment.id = aa.id) " +
				"AND NOT EXISTS (SELECT 1 FROM VehicleJourney vj WHERE vj.accessibilityAssessment.id = aa.id)")
				.executeUpdate();
	}

	@Override
	public void deleteAccessibilityAssessmentVJ(List<String> objectIds) {
		Session session = em.unwrap(Session.class);

		session.doWork(connection -> {

			final String SQL = "DELETE FROM accessibility_assessment WHERE objectid IN ( %s )";

			// delete
			int size = objectIds.size();
			if (size > 0) {
				StringBuffer buffer = new StringBuffer();
				for (int i = 0; i < size; i++) {

					buffer.append('\'');
					buffer.append(objectIds.get(i));
					buffer.append('\'');
					if (i != size - 1) {
						buffer.append(',');
					}
				}

				Statement statement = connection.createStatement();
				String sql = String.format(SQL, buffer);
				int count = statement.executeUpdate(sql);
				log.info("Accessibility assessment deleted before copycommand : " + count + " objects.");
			}
		});
	}

	@Override
	public void copy(final String data) {

		log.info("Copy command accessibility assessment started");

		Session session = em.unwrap(Session.class);

		session.doWork(connection -> {
			// Monitor monitor = MonitorFactory.start("COPY");

			linesToCopy = data.split("\n");
			String currentBatchOfLines = null;
			int nbOfLinesInBatch = 0;

			for (int i = 0 ; i < linesToCopy.length ; i++){

				if (!linesToCopy[i].isEmpty()) {
					if (currentBatchOfLines != null) {
						currentBatchOfLines = currentBatchOfLines + linesToCopy[i] + "\n";
					} else {
						currentBatchOfLines = linesToCopy[i] + "\n";
					}
					nbOfLinesInBatch++;
				}

				if (nbOfLinesInBatch == MAX_NB_OF_LINES){
					launchCopyAccessibilityAssessment(connection,currentBatchOfLines);
					currentBatchOfLines = null;
					nbOfLinesInBatch=0;
				}
			}

			if (currentBatchOfLines != null){
				launchCopyAccessibilityAssessment(connection, currentBatchOfLines);
			}

			log.info("Copy command accessibility assessment finished");
			// log.info(Color.MAGENTA + monitor.stop() + Color.NORMAL);
		});
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public void launchCopyAccessibilityAssessment(Connection connection, String currentBatchOfLines) throws SQLException {

		try {
			StringReader from = new StringReader(currentBatchOfLines);

			PGConnection pgConnection = (PGConnection) ((WrappedConnection) connection)
					.getUnderlyingConnection();
			org.postgresql.copy.CopyManager manager = pgConnection
					.getCopyAPI();
			String copyStatement = "COPY accessibility_assessment("
					+ "objectid, object_version, creation_time, creator_id, "
					+ "mobility_impaired_access, accessibility_limitation_id)"
					+ " FROM STDIN WITH DELIMITER '|'";

			manager.copyIn(copyStatement, from);


		} catch (IOException e) {
			log.error("Error while copying accessibility assessment");
			log.error(e);
		}
	}

}
