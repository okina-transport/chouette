package mobi.chouette.dao;

import lombok.extern.slf4j.Slf4j;
import mobi.chouette.model.VehicleJourneyAtStopTranslation;
import org.apache.commons.collections4.CollectionUtils;
import org.hibernate.Session;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Stateless(name = "VehicleJourneyAtStopTranslationDAO")
@Slf4j
public class VehicleJourneyAtStopTranslationDAOImpl extends GenericDAOImpl<VehicleJourneyAtStopTranslation> implements VehicleJourneyAtStopTranslationDAO {

    private static final String SELECT_OBJECTID_ID_FROM_VJAS = "select objectid, id from vehicle_journey_at_stops where objectid in (:objectIds)";
    private static final String INSERT_VJAS_TRANSLATION_PREPARED_STATEMENT = """
            insert into vehicle_journey_at_stop_translations
            (id, objectid, object_version, creation_time, creator_id, vehicle_journey_at_stop_id, field_name, language, translation, field_value)
            values (nextval('vehicle_journey_at_stop_translations_id_seq'), ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
    private static final int MAX_NB_OF_LINES = 1000;

    public VehicleJourneyAtStopTranslationDAOImpl() {
        super(VehicleJourneyAtStopTranslation.class);
    }

    @PersistenceContext(unitName = "referential")
    public void setEntityManager(EntityManager em) {
        this.em = em;
    }

    @Override
    public void persistTranslations(List<VehicleJourneyAtStopTranslation> translations) {
        if (CollectionUtils.isEmpty(translations)) {
            return;
        }
        log.info("Vjas translations persist started");

        Set<String> parentObjectIds = translations.stream()
                .map(translation -> translation.getVehicleJourneyAtStop().getObjectId())
                .collect(Collectors.toSet());

        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
                        SELECT_OBJECTID_ID_FROM_VJAS)
                .setParameter("objectIds", parentObjectIds)
                .getResultList();
        Map<String, Long> vjasIdsByObjectId = rows.stream()
                .collect(Collectors.toMap(row -> (String) row[0], row -> ((Number) row[1]).longValue()));

        Session session = em.unwrap(Session.class);
        session.doWork(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(INSERT_VJAS_TRANSLATION_PREPARED_STATEMENT)) {
                int batched = 0;
                for (VehicleJourneyAtStopTranslation translation : translations) {
                    Long vjasId = vjasIdsByObjectId.get(translation.getVehicleJourneyAtStop().getObjectId());
                    if (vjasId == null) {
                        log.warn("No persisted VehicleJourneyAtStop found for translation " + translation.getObjectId() + ", skipping");
                        continue;
                    }
                    statement.setString(1, translation.getObjectId());
                    statement.setInt(2, translation.getObjectVersion());
                    statement.setTimestamp(3, translation.getCreationTime() != null ? Timestamp.valueOf(translation.getCreationTime()) : null);
                    statement.setString(4, translation.getCreatorId());
                    statement.setLong(5, vjasId);
                    statement.setString(6, translation.getFieldName());
                    statement.setString(7, translation.getLanguage());
                    statement.setString(8, translation.getTranslation());
                    statement.setString(9, translation.getFieldValue());
                    statement.addBatch();
                    batched++;
                    if (batched % MAX_NB_OF_LINES == 0) {
                        statement.executeBatch();
                    }
                }
                statement.executeBatch();
            }
        });
        log.info("Vjas translations persist finished");
    }
}
