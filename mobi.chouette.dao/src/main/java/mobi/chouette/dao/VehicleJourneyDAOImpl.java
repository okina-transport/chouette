package mobi.chouette.dao;

import lombok.extern.log4j.Log4j;
import mobi.chouette.model.IneoVJMapping;
import mobi.chouette.model.VehicleJourney;
import mobi.chouette.model.type.DayTypeEnum;
import mobi.chouette.model.type.PTDirectionEnum;
import org.apache.commons.collections.CollectionUtils;
import org.joda.time.LocalDate;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
        Integer dayBitMask = DayTypeEnum.from(date.getDayOfWeek()).buildBitMask();
        // Get effective timetable ids for current date
        List effectiveTimetableIds = em.createNativeQuery(
                        "select distinct tt.id from time_tables tt " +
                                "inner join time_table_dates ttd on tt.id = ttd.time_table_id " +
                                "inner join time_table_periods ttp on ttp.time_table_id = tt.id " +
                                "where " +
                                "   (" +
                                "       ttp.period_start <= cast(:date as date) " +
                                "       and ttp.period_end >= cast(:date as date)" +
                                        // make sure day of week is in period
                                "       and tt.int_day_types & :dayBitMask = :dayBitMask " +
                                        // make sure day is not excluded from period
                                "       and not (ttd.date = cast(:date as date) and ttd.in_out = false)" +
                                "   ) " +
                                        // check if day is not in period but is added to it
                                "   or (ttd.date = cast(:date as date) and ttd.in_out = true)")
                .setParameter("date", date.toDate())
                .setParameter("dayBitMask", dayBitMask)
                .getResultList();

        if (CollectionUtils.isEmpty(effectiveTimetableIds)) {
            return new ArrayList<>();
        }

        List<Object[]> res = (List<Object[]>) em.createNativeQuery(
                "select " +
                        "	vjas.departure_time as departureTime, " +
                        "	sa.objectid as stopAreaObjectId, " +
                        "	l.number as lineNumber, " +
                        "	r.direction as routeDirection, " +
                        "	vj.objectid as vehicleJourneyObjectId " +
                        "from time_tables_vehicle_journeys ttvj " +
                        "inner join vehicle_journeys vj on ttvj.time_table_id in :effectiveTimetableIds and vj.id = ttvj.vehicle_journey_id " +
                        "inner join vehicle_journey_at_stops vjas on vjas.vehicle_journey_id = vj.id " +
                        "inner join stop_points sp on vjas.stop_point_id = sp.id " +
                        "inner join stop_areas sa on sp.stop_area_id = sa.id " +
                        "inner join routes r on vj.route_id = r.id " +
                        "inner join lines l on r.line_id = l.id " +
                        "order by vjas.departure_time, l.number, r.direction"
                )
                .setParameter("effectiveTimetableIds", effectiveTimetableIds)
                .getResultList();
        if (CollectionUtils.isEmpty(res)) {
            return new ArrayList<>();
        }
        return res.stream().map(
                        e -> new IneoVJMapping(date.toDate(), ((Time) e[0]).toLocalTime(), (String) e[1], (String) e[2],
                                PTDirectionEnum.valueOf((String) e[3]), (String) e[4]))
                .collect(Collectors.toList());
    }

}
