package mobi.chouette.dao;

import mobi.chouette.core.CoreException;
import mobi.chouette.core.CoreExceptionCode;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.time.LocalDate;

import static mobi.chouette.common.TimeUtil.toDate;

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
        query.setParameter("dateParam", toDate(startDate));

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
        query.setParameter("validationStartPeriod", toDate(validationStartDate));
        query.setParameter("validationEndPeriod", toDate(validationEndDate));

        Object postgresResult =  query.getSingleResult();

        if (postgresResult instanceof String){
            fctResult = (String) postgresResult;
        }
        return fctResult;

    }






}
