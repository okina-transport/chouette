package mobi.chouette.dao;

import lombok.extern.log4j.Log4j;
import mobi.chouette.model.IneoToVjRef;
import mobi.chouette.model.VehicleJourney;
import mobi.chouette.model.type.PTDirectionEnum;
import org.apache.commons.collections.CollectionUtils;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Date;
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
    public long updateAccessibilityId(Long accessibilityId, List<String> objectIds){
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
    public long updateDefaultAccessibility(Long defaultAccessibilityId){
        return em.createNativeQuery("UPDATE vehicle_journeys SET accessibility_assessment_id = :accessId WHERE accessibility_assessment_id IS NULL")
                .setParameter("accessId", defaultAccessibilityId)
                .executeUpdate();
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public List<IneoToVjRef> getIneoToVjRefs() {
        List<Object[]> res = (List<Object[]>) em.createNativeQuery("select " +
                        "	ttd.\"date\" as timetableDate, " +
                        "	vjas.departure_time as departureTime, " +
                        "	sa.objectid as stopAreaObjectId, " +
                        "	l.number as lineNumber, " +
                        "	r.direction as routeDirection, " +
                        "	vj.objectid as vehicleJourneyObjectId " +
                        "from vehicle_journey_at_stops vjas " +
                        "inner join vehicle_journeys vj on vjas.vehicle_journey_id = vj.id " +
                        "inner join time_tables_vehicle_journeys ttvj on	vj.id = ttvj.vehicle_journey_id " +
                        "inner join time_tables tt on ttvj.time_table_id = tt.id " +
                        "inner join time_table_dates ttd on tt.id = ttd.time_table_id " +
                        "inner join stop_points sp on vjas.stop_point_id = sp.id " +
                        "inner join stop_areas sa on sp.stop_area_id = sa.id " +
                        "inner join routes r on vj.route_id = r.id " +
                        "inner join lines l on r.line_id = l.id " +
                        "order by ttd.\"date\", vjas.departure_time, l.number, r.direction"
                )
                .getResultList();
        if (CollectionUtils.isEmpty(res)) {
            return new ArrayList<>();
        }
        return res.stream().map(
                        e -> new IneoToVjRef((Date)e[0], ((Time)e[1]).toLocalTime(), (String)e[2], (String)e[3],
                                PTDirectionEnum.valueOf((String)e[4]),	(String)e[5]))
                .collect(Collectors.toList());
    }

}
