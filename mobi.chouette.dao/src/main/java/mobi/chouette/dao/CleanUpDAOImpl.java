package mobi.chouette.dao;

import mobi.chouette.core.CoreException;
import mobi.chouette.core.CoreExceptionCode;
import org.joda.time.LocalDate;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.Date;
import java.util.List;

@Stateless
public class CleanUpDAOImpl implements  CleanUpDAO{


    @PersistenceContext(unitName = "referential")
    EntityManager em;


    @Override
    public String lauchCleanUpFunction(String functionName) throws CoreException {

        if (functionName == null || functionName == ""){
            throw new IllegalArgumentException("Function name must be provided");
        }

        String fctResult = "";
        try {
            Object postgresResult = em.createNativeQuery(
                    "SELECT " + functionName + "()")
                    .getSingleResult();

            if (postgresResult instanceof String){
                fctResult = (String) postgresResult;
            }

            return fctResult;
        } catch (Exception e) {
            throw new CoreException(CoreExceptionCode.DELETE_IMPOSSIBLE,"Error while trying to launch function:" + functionName);
        }
    }

    @Override
    public String removeExpiredTimetableDates(LocalDate startDate) {
        String fctResult = "";

        Query query = em.createNativeQuery("SELECT remove_expired_time_table_dates(cast (:dateParam as date))");
        query.setParameter("dateParam", startDate.toDate());

        Object postgresResult =  query.getSingleResult();

        if (postgresResult instanceof String){
            fctResult = (String) postgresResult;
        }
        return fctResult;

    }


    @Override
    public String removeUnusedPeriods(LocalDate validationStartDate, LocalDate validationEndDate) {
        String fctResult = "";

        Query query = em.createNativeQuery("SELECT remove_unused_periods(cast (:validationStartPeriod as date), cast (:validationEndPeriod as date))");
        query.setParameter("validationStartPeriod", validationStartDate.toDate());
        query.setParameter("validationEndPeriod", validationEndDate.toDate());

        Object postgresResult =  query.getSingleResult();

        if (postgresResult instanceof String){
            fctResult = (String) postgresResult;
        }
        return fctResult;

    }

    @Override
    public String removeExpiredAttributions() {
        String removeLineAttributionsStringQuery = "DELETE FROM attributions a WHERE (" +
                "SELECT MAX(ttp.period_end) FROM time_table_periods ttp WHERE EXISTS (" +
                "SELECT ttvj.time_table_id FROM time_tables_vehicle_journeys ttvj WHERE ttp.time_table_id = ttvj.time_table_id AND EXISTS (" +
                "SELECT vj.id FROM vehicle_journeys vj WHERE ttvj.vehicle_journey_id = vj.id AND EXISTS (" +
                "SELECT r.id FROM routes r WHERE vj.route_id = r.id AND r.line_id = a.line_id)))) < NOW();";

        String removeVehicleJourneyAttributionsStringQuery = "DELETE FROM attributions a WHERE (" +
                "SELECT ttp.period_end FROM time_table_periods ttp WHERE EXISTS (" +
                "SELECT ttvj.time_table_id FROM time_tables_vehicle_journeys ttvj WHERE ttvj.time_table_id = ttp .time_table_id AND ttvj.vehicle_journey_id = a.vehicle_journey_id)) < NOW();";

        Query removeLineAttributionsQuery = em.createNativeQuery(removeLineAttributionsStringQuery);
        Query removeVehicleJourneyAttributionsQuery = em.createNativeQuery(removeVehicleJourneyAttributionsStringQuery);

        Object removeLineAttributionResult = removeLineAttributionsQuery.executeUpdate();
        Object removeVehicleJourneyAttributionResult = removeVehicleJourneyAttributionsQuery.executeUpdate();

        String deletionsResult = "";

        if (removeLineAttributionResult instanceof String){
            deletionsResult += (String) removeLineAttributionResult;
        }

        if (removeVehicleJourneyAttributionResult instanceof String){
            if (!deletionsResult.isEmpty()) {
                deletionsResult += " ";
            }
            deletionsResult += (String) removeVehicleJourneyAttributionResult;
        }

        return deletionsResult;
    }
}
