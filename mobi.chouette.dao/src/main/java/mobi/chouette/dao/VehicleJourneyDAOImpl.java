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
public class VehicleJourneyDAOImpl extends GenericDAOImpl<VehicleJourney> implements VehicleJourneyDAO{

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
				String sql = String.format(SQL, buffer.toString());
				int count = statement.executeUpdate(sql);
				log.info("[DSU] delete " + count + " objects.");
			}
		});
	}

	@Override
	public void copy(final String data) {

		Session session = em.unwrap(Session.class);

		session.doWork(new Work() {

			@Override
			public void execute(Connection connection) throws SQLException {
				// Monitor monitor = MonitorFactory.start("COPY");

					linesToCopy = data.split("\n");
					log.info("nb of lines to copy:" + linesToCopy.length);
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
							launchCopy(connection,currentBatchOfLines);
							log.info("copy batch terminated. Current index:" + i);
							currentBatchOfLines = null;
							nbOfLinesInBatch=0;
						}
					}

					if (currentBatchOfLines != null){
						launchCopy(connection,currentBatchOfLines);
						log.info("last batch has been copied. Length:" + nbOfLinesInBatch);
					}

					log.info("copie VJAS terminée");
				// log.info(Color.MAGENTA + monitor.stop() + Color.NORMAL);
			}
		});
	}


	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public void launchCopy(Connection connection, String currentBatchOfLines) throws SQLException{

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
					// + "arrival_time, departure_time, "
					// + "elapse_duration, headway_frequency)"
					+ " FROM STDIN WITH DELIMITER '|'";

			manager.copyIn(copyStatement, from);


		} catch (IOException e) {
			log.error("Erreur pendant la copie des VJAS");
			log.error(e);
		}
	}

}
