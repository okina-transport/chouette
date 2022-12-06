package mobi.chouette.exchange.transfer.exporter;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.CleanUpDAO;
import mobi.chouette.exchange.transfer.Constant;
import org.jboss.ejb3.annotation.TransactionTimeout;
import org.joda.time.LocalDate;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.time.ZoneId;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Log4j
@Stateless(name = PostTransferCleanup.COMMAND)
public class PostTransferCleanup implements Command, Constant {


    public static final String COMMAND = "PostTransferCleanup";

    // SQL functions that will be called to clean the new schema
    private static String REMOVE_DELETED_LINES_FUNCTION_NAME = "remove_deleted_lines";
    private static String REMOVE_DELETED_ROUTES_FUNCTION_NAME = "remove_deleted_routes";
    private static String REMOVE_DELETED_JOURNEY_PATTERNS_FUNCTION_NAME = "remove_deleted_journey_patterns";
    private static String REMOVE_EMPTY_JOURNEY_PATTERNS_FUNCTION_NAME = "remove_empty_journey_patterns";
    private static String REMOVE_EMPTY_ROUTES_FUNCTION_NAME = "remove_empty_routes";
    private static String REMOVE_INCORRECT_JOURNEY_PATTERNS_FUNCTION_NAME = "remove_incorrect_journey_patterns";
    private static String REMOVE_INCORRECT_ROUTES_FUNCTION_NAME = "remove_incorrect_routes";
    private static String REFRESH_DEPARTURE_ARRIVALS_FUNCTION_NAME = "refresh_departure_arrivals";
    private static String REMOVE_INCORRECT_VEHICLE_JOURNEYS_FUNCTION_NAME = "remove_incorrect_vehicle_journeys";
    private static String REMOVE_EMPTY_TIME_TABLES_FUNCTION_NAME = "remove_empty_time_tables";
    private static String REMOVE_VJ_WITH_NO_TT_FUNCTION_NAME = "remove_vehicle_journey_with_no_time_tables";
    private static String REMOVE_EMPTY_LINES_FUNCTION_NAME = "remove_empty_lines";
    private static String REORG_PERIODS_FUNCTION_NAME = "reorg_time_table_periods";





    @EJB
    CleanUpDAO cleanUpDAO;


    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @TransactionTimeout(value = 8, unit = TimeUnit.HOURS)
    public boolean execute(Context context) throws Exception {

        TransferExportParameters configuration = (TransferExportParameters) context.get(CONFIGURATION);

        LocalDate startDate = configuration.getStartDate() == null ? LocalDate.now() : new LocalDate(configuration.getStartDate());
        LocalDate endDate = configuration.getEndDate() == null ? LocalDate.now() : new LocalDate(configuration.getEndDate());


        log.info("Starting post-transfer cleanup");
        log.info("Start date: " + startDate.toString() + ", end date :" + endDate.toString());

        //1st step : remove all objects flagged as "deleted" in database

        String removedLines =  cleanUpDAO.lauchCleanUpFunction(REMOVE_DELETED_LINES_FUNCTION_NAME);
        log.info("removed deleted lines :" + removedLines);

        String removedRoutes = cleanUpDAO.lauchCleanUpFunction(REMOVE_DELETED_ROUTES_FUNCTION_NAME);
        log.info("removed deleted routes :" + removedRoutes);

        String removedJourneyPatterns = cleanUpDAO.lauchCleanUpFunction(REMOVE_DELETED_JOURNEY_PATTERNS_FUNCTION_NAME);
        log.info("removed deleted journey patterns :" + removedJourneyPatterns);

        //2nd step : remove expired data

        String expiredTimetableDates = cleanUpDAO.removeExpiredTimetableDates(startDate);
        log.info("removed expired time table dates :" + expiredTimetableDates);

        String unusedTimePeriods = cleanUpDAO.removeUnusedPeriods(startDate, endDate);
        log.info("removed unused Time periods :" + unusedTimePeriods);

        String emptyTimeTables = cleanUpDAO.lauchCleanUpFunction(REMOVE_EMPTY_TIME_TABLES_FUNCTION_NAME);
        log.info("removed empty time tables :" + emptyTimeTables);

        //3rd step : when all timetables are cleaned, removal of empty objects (vehicle journeys, then journeypatterns, route)

        String expiredAttributions = cleanUpDAO.removeExpiredAttributions();
        log.info("removed expired attributions" + expiredAttributions);

        String vjWithNoTimetables = cleanUpDAO.lauchCleanUpFunction(REMOVE_VJ_WITH_NO_TT_FUNCTION_NAME);
        log.info("removed vehicle journeys with no time tables :" + vjWithNoTimetables);

        String incorrectVehicleJourneys = cleanUpDAO.lauchCleanUpFunction(REMOVE_INCORRECT_VEHICLE_JOURNEYS_FUNCTION_NAME);
        log.info("removed incorrect vehicle journeys :" + incorrectVehicleJourneys);

        String emptyJourneyPatterns = cleanUpDAO.lauchCleanUpFunction(REMOVE_EMPTY_JOURNEY_PATTERNS_FUNCTION_NAME);
        log.info("removed empty journey patterns :" + emptyJourneyPatterns);

        String incorrectJourneyPatterns = cleanUpDAO.lauchCleanUpFunction(REMOVE_INCORRECT_JOURNEY_PATTERNS_FUNCTION_NAME);
        log.info("removed incorrect journey patterns :" + incorrectJourneyPatterns);

        String refreshedJourneyPatterns = cleanUpDAO.lauchCleanUpFunction(REFRESH_DEPARTURE_ARRIVALS_FUNCTION_NAME);
        log.info("refreshed patterns :" + refreshedJourneyPatterns);

        String incorrectRoutes = cleanUpDAO.lauchCleanUpFunction(REMOVE_INCORRECT_ROUTES_FUNCTION_NAME);
        log.info("removed incorrect routes :" + incorrectRoutes);

        String emptyRoutes = cleanUpDAO.lauchCleanUpFunction(REMOVE_EMPTY_ROUTES_FUNCTION_NAME);
        log.info("removed empty routes :" + emptyRoutes);

        String emptyLines = cleanUpDAO.lauchCleanUpFunction(REMOVE_EMPTY_LINES_FUNCTION_NAME);
        log.info("removed empty lines :" + emptyLines);

        cleanUpDAO.lauchCleanUpFunction(REORG_PERIODS_FUNCTION_NAME);
        log.info("period reorganization completed successfully" );

        return true;

    }



    public static class DefaultCommandFactory extends CommandFactory {

        @Override
        protected Command create(InitialContext context) throws IOException {
            Command result = null;
            try {
                String name = "java:app/mobi.chouette.exchange.transfer/" + COMMAND;
                result = (Command) context.lookup(name);
            } catch (NamingException e) {
                // try another way on test context
                String name = "java:module/" + COMMAND;
                try {
                    result = (Command) context.lookup(name);
                } catch (NamingException e1) {
                    log.error(e);
                }
            }
            return result;
        }
    }

    static {
        CommandFactory.factories.put(PostTransferCleanup.class.getName(), new DefaultCommandFactory());
    }
}
