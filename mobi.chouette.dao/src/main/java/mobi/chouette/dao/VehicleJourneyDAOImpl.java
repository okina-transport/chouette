package mobi.chouette.dao;

import lombok.extern.log4j.Log4j;
import mobi.chouette.model.IneoVJMapping;
import mobi.chouette.model.VehicleJourney;
import mobi.chouette.model.type.PTDirectionEnum;
import org.apache.commons.collections.CollectionUtils;
import org.joda.time.LocalDate;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import java.math.BigInteger;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Stateless
@Log4j
public class VehicleJourneyDAOImpl extends GenericDAOImpl<VehicleJourney> implements VehicleJourneyDAO {

    @EJB
    TimetableDAO timetableDAO;

    public VehicleJourneyDAOImpl() {
        super(VehicleJourney.class);
    }

    @PersistenceContext(unitName = "referential")
    public void setEntityManager(EntityManager em) {
        this.em = em;
    }

    @Override
    public long updateAccessibilityId(Long accessibilityId, List<String> objectIds) {
        return em.createNativeQuery("UPDATE vehicle_journeys SET accessibility_assessment_id = :accessId WHERE objectid IN (:objectIdList)")
                .setParameter("accessId", accessibilityId)
                .setParameter("objectIdList", objectIds)
                .executeUpdate();
    }

    @Override
    public long updateBrandingId(Long brandingId, List<String> objectIds) {
        return em.createNativeQuery("UPDATE vehicle_journeys SET branding_id = :brandId WHERE objectid IN (:objectIdList)")
                .setParameter("brandId", brandingId)
                .setParameter("objectIdList", objectIds)
                .executeUpdate();
    }

    @Override
    public long updateDefaultAccessibility(Long defaultAccessibilityId) {
        return em.createNativeQuery("UPDATE vehicle_journeys SET accessibility_assessment_id = :accessId WHERE accessibility_assessment_id IS NULL")
                .setParameter("accessId", defaultAccessibilityId)
                .executeUpdate();
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public List<IneoVJMapping> getIneoVJMappingData(LocalDate date) {
        Collection<? extends Number>  activeTimetableIds = timetableDAO.getActiveTimetableIdsByDay(date);

        if (CollectionUtils.isEmpty(activeTimetableIds)) {
            log.warn(String.format("No active timetables found for %s", date));
            return new ArrayList<>();
        }

        List<Object[]> res = (List<Object[]>) em.createNativeQuery(
                "select " +
                        "	vjas.departure_time as departureTime, " +
                        "	sa.original_stop_id as originalStopId, " +
                        "   coalesce(psa.original_stop_id, '') as originalParentStopId, " +
                        "	l.number as lineNumber, " +
                        "	r.direction as routeDirection, " +
                        "	vj.objectid as vehicleJourneyObjectId, " +
                        "   sp.position +1 as position " +
                        "from time_tables_vehicle_journeys ttvj " +
                        "inner join vehicle_journeys vj on ttvj.time_table_id in :activeTimetableIds and vj.id = ttvj.vehicle_journey_id " +
                        "inner join vehicle_journey_at_stops vjas on vjas.vehicle_journey_id = vj.id " +
                        "inner join stop_points sp on vjas.stop_point_id = sp.id " +
                        "inner join stop_areas sa on sp.stop_area_id = sa.id " +
                        "left join stop_areas psa on sa.parent_id = psa.id " +
                        "inner join routes r on vj.route_id = r.id " +
                        "inner join lines l on r.line_id = l.id " +
                        "order by vjas.departure_time, l.number, r.direction"
                )
                .setParameter("activeTimetableIds", activeTimetableIds)
                .getResultList();

        if (CollectionUtils.isEmpty(res)) {
            log.warn(String.format("No VJ mapping data found for %s", date));
            return new ArrayList<>();
        }

        return res.stream().map(
                        e -> new IneoVJMapping(date.toDate(), ((Time) e[0]).toLocalTime(), (String) e[1], (String) e[2],
                                (String) e[3],  e[4] != null ? PTDirectionEnum.valueOf((String) e[4]) : PTDirectionEnum.A, (String) e[5],(Integer) e[6]))
                .collect(Collectors.toList());
    }

}
