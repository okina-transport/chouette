package mobi.chouette.dao;

import lombok.extern.log4j.Log4j;
import mobi.chouette.model.VehicleJourneyAtStop;
import org.apache.commons.collections.CollectionUtils;
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
import java.util.List;

@Log4j
@Stateless
public class VehicleJourneyAtStopDAOImpl extends GenericDAOImpl<VehicleJourneyAtStop> implements VehicleJourneyAtStopDAO {

    // Max nb of lines that will be sent to copyIn command at once
    private final int MAX_NB_OF_LINES = 1000;
    private String[] linesToCopy;

    public VehicleJourneyAtStopDAOImpl() {
        super(VehicleJourneyAtStop.class);
    }

    @PersistenceContext(unitName = "referential")
    public void setEntityManager(EntityManager em) {
        this.em = em;
    }

    @Override
    public VehicleJourneyAtStop findByObjectId(String objectId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int deleteByVehicleJourneyObjectIds(List<String> vehicleJourneyObjectIds) {
         if (CollectionUtils.isEmpty(vehicleJourneyObjectIds)) {
            log.info("No vjas to delete");
            return 0;
        }
        log.info("Delete vjas started");
        int nbVjasDeleted = em.createNativeQuery(
                "delete from vehicle_journey_at_stops where vehicle_journey_id in (select id from " +
                        "vehicle_journeys where objectid in (:objectIds))")
                .setParameter("objectIds", vehicleJourneyObjectIds)
                .executeUpdate();
        log.info(String.format("Deleted %d vjas", nbVjasDeleted));
        log.info("Delete vjas finished");
        return nbVjasDeleted;
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

            PGConnection pgConnection = (PGConnection) ((WrappedConnection) connection).getUnderlyingConnection();
            org.postgresql.copy.CopyManager manager = pgConnection.getCopyAPI();
            String copyStatement = "COPY vehicle_journey_at_stops(" + "objectid, object_version, creation_time, creator_id, " + "vehicle_journey_id, stop_point_id, " + "arrival_time, departure_time, " + "arrival_day_offset, departure_day_offset, boardingalightingpossibility)" + " FROM STDIN WITH DELIMITER '|'";

            manager.copyIn(copyStatement, from);


        } catch (IOException e) {
            log.error("Error while vjas copy");
            log.error(e);
        }
    }

}
