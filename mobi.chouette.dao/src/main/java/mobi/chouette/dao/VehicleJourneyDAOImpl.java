package mobi.chouette.dao;

import lombok.extern.log4j.Log4j;
import mobi.chouette.model.VehicleJourney;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
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

@Stateless
@Log4j
public class VehicleJourneyDAOImpl extends GenericDAOImpl<VehicleJourney> implements VehicleJourneyDAO {

    public VehicleJourneyDAOImpl() {
        super(VehicleJourney.class);
    }

    @PersistenceContext(unitName = "referential")
    public void setEntityManager(EntityManager em) {
        this.em = em;
    }

    // Max nb of lines that will be sent to copyIn command at once
    private int MAX_NB_OF_LINES = 1000;

    private String[] linesToCopy;

    @Override
    public void deleteChildren(final List<String> vehicleJourneyObjectIds) {
        log.info("Delete vjas started");

        Session session = em.unwrap(Session.class);

        session.doWork(connection -> {

            final String SQL = "DELETE FROM vehicle_journey_at_stops WHERE vehicle_journey_id IN ("
                    + "SELECT id FROM vehicle_journeys WHERE objectid IN ( %s )"
                    + ")";

            // delete
            int size = vehicleJourneyObjectIds.size();
            if (size > 0) {
                StringBuffer buffer = new StringBuffer();
                for (int i = 0; i < size; i++) {

                    buffer.append('\'');
                    buffer.append(vehicleJourneyObjectIds.get(i));
                    buffer.append('\'');
                    if (i != size - 1) {
                        buffer.append(',');
                    }
                }

                Statement statement = connection.createStatement();
                String sql = String.format(SQL, buffer);
                statement.executeUpdate(sql);
            }
        });
        log.info("Delete vjas finished");
    }

    @Override
    public void copy(final String data) {
        log.info("Vjas copy started");

        Session session = em.unwrap(Session.class);

        session.doWork(connection -> {
            linesToCopy = data.split("\n");
            StringBuilder currentBatchOfLines = null;
            int nbOfLinesInBatch = 0;

            for (String s : linesToCopy) {
                if (!s.isEmpty()) {
                    if (currentBatchOfLines != null) {
                        currentBatchOfLines.append(s).append("\n");
                    } else {
                        currentBatchOfLines = new StringBuilder(s + "\n");
                    }
                    nbOfLinesInBatch++;
                }

                if (nbOfLinesInBatch == MAX_NB_OF_LINES) {
                    launchCopy(connection, currentBatchOfLines.toString());
                    currentBatchOfLines = null;
                    nbOfLinesInBatch = 0;
                }
            }

            if (currentBatchOfLines != null) {
                launchCopy(connection, currentBatchOfLines.toString());
            }

            log.info("Vjas copy finished");
        });
    }


    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void launchCopy(Connection connection, String currentBatchOfLines) throws SQLException {

        try {
            StringReader from = new StringReader(currentBatchOfLines);

            PGConnection pgConnection = (PGConnection) ((WrappedConnection) connection)
                    .getUnderlyingConnection();
            org.postgresql.copy.CopyManager manager = pgConnection
                    .getCopyAPI();
            String copyStatement = "COPY vehicle_journey_at_stops("
                    + "objectid, object_version, creation_time, creator_id, "
                    + "vehicle_journey_id, stop_point_id, "
                    + "arrival_time, departure_time, "
                    + "arrival_day_offset, departure_day_offset, boardingalightingpossibility)"
                    + " FROM STDIN WITH DELIMITER '|'";

            manager.copyIn(copyStatement, from);


        } catch (IOException e) {
            log.error("Error while vjas copy");
            log.error(e);
        }
    }



    public long updateAcessibilityId(Long accessibilityId, List<String> objectIds){

        String sql = "UPDATE vehicle_journeys SET accessibility_assessment_id = :accessId WHERE objectid IN (:objectIdList)";
        long nbModifiedLines = em.createNativeQuery(sql)
                            .setParameter("accessId", accessibilityId)
                            .setParameter("objectIdList", objectIds)
                            .executeUpdate();

        return nbModifiedLines;
    }

    public long updateDefaultAccessibility(Long defaultAccessibilityId){

        String sql = "UPDATE vehicle_journeys SET accessibility_assessment_id = :accessId WHERE accessibility_assessment_id IS NULL";
        long nbModifiedLines = em.createNativeQuery(sql)
                .setParameter("accessId", defaultAccessibilityId)
                .executeUpdate();

        return nbModifiedLines;
    }

}
