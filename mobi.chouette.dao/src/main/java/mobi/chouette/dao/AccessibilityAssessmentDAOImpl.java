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
import java.util.HashMap;
import java.util.Map;


@Stateless(name = "AccessibilityAssessmentDAO")
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
    public void copy(final String data) {

        log.info("Copy accessibility assessment started");

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
                    launchCopyAccessibilityAssessment(connection, currentBatchOfLines);
                    currentBatchOfLines = null;
                    nbOfLinesInBatch = 0;
                }
            }

            if (currentBatchOfLines != null) {
                launchCopyAccessibilityAssessment(connection, currentBatchOfLines);
            }

            log.info("Copy accessibility assessment finished");
            // log.info(Color.MAGENTA + monitor.stop() + Color.NORMAL);
        });
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void updateVjAccessiblityAssessment(HashMap<String, String> mapIdsVjAa) {
        if (mapIdsVjAa.isEmpty()) {
            return;
        }
        em.createNativeQuery("CREATE TEMPORARY TABLE temp_mapping (vj_objectid TEXT, aa_objectid TEXT)").executeUpdate();

        for (Map.Entry<String, String> entry : mapIdsVjAa.entrySet()) {
            String vjObjectId = entry.getKey();
            String aaObjectId = entry.getValue();
            em.createNativeQuery("INSERT INTO temp_mapping (vj_objectid, aa_objectid) VALUES (?, ?)")
                    .setParameter(1, vjObjectId)
                    .setParameter(2, aaObjectId)
                    .executeUpdate();
        }

        String updateQuery = "UPDATE vehicle_journeys vj " +
                "SET accessibility_assessment_id = aa.id " +
                "FROM temp_mapping tm " +
                "JOIN accessibility_assessment aa ON tm.aa_objectid = aa.objectid " +
                "WHERE vj.objectid = tm.vj_objectid";
        em.createNativeQuery(updateQuery).executeUpdate();

        em.createNativeQuery("DROP TABLE temp_mapping").executeUpdate();
    }


    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void updateAccessiblityAssessmentAccessibilityLimitation(HashMap<String, String> mapIdsAaAl) {
        if (mapIdsAaAl.isEmpty()) {
            return;
        }
        em.createNativeQuery("CREATE TEMPORARY TABLE temp_mapping (aa_objectid TEXT, al_objectid TEXT)").executeUpdate();

        for (Map.Entry<String, String> entry : mapIdsAaAl.entrySet()) {
            String aaObjectId = entry.getKey();
            String alObjectId = entry.getValue();
            em.createNativeQuery("INSERT INTO temp_mapping (aa_objectid, al_objectid) VALUES (?, ?)")
                    .setParameter(1, aaObjectId)
                    .setParameter(2, alObjectId)
                    .executeUpdate();
        }

        String updateQuery = "UPDATE accessibility_assessment aa " +
                "SET accessibility_limitation_id = al.id " +
                "FROM temp_mapping tm " +
                "JOIN accessibility_limitation al ON tm.al_objectid = al.objectid " +
                "WHERE aa.objectid = tm.aa_objectid";
        em.createNativeQuery(updateQuery).executeUpdate();

        em.createNativeQuery("DROP TABLE temp_mapping").executeUpdate();
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
                    + "mobility_impaired_access)"
                    + " FROM STDIN WITH DELIMITER '|'";

            manager.copyIn(copyStatement, from);


        } catch (IOException e) {
            log.error("Error while copying accessibility assessment");
            log.error(e);
        }
    }

}
