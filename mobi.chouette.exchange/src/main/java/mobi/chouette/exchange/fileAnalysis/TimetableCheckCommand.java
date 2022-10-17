package mobi.chouette.exchange.fileAnalysis;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Constant;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.LineDAO;
import mobi.chouette.dao.TimetableDAO;
import mobi.chouette.dao.exception.ChouetteStatisticsTimeoutException;
import mobi.chouette.exchange.importer.AbstractImporterCommand;
import mobi.chouette.exchange.report.AnalyzeReport;
import mobi.chouette.model.CalendarDay;
import mobi.chouette.model.Line;
import mobi.chouette.model.Period;
import mobi.chouette.model.Timetable;
import mobi.chouette.model.statistics.LineAndTimetable;
import mobi.chouette.model.util.Referential;
import org.apache.commons.lang3.tuple.Pair;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.util.*;

/**
 *  Class used to check if some calendars in database are used by 2 or more agencies
 *  (This is a bad practice. A calendar should be used by only one agency)
 */
@Log4j
@Stateless(name = TimetableCheckCommand.COMMAND)
public class TimetableCheckCommand extends AbstractImporterCommand implements Command, Constant {

    public static final String COMMAND = "TimetableCheckCommand";

    private static final double MAX_ALLOWED_DISTANCE = 200d;

    @EJB
    private
    TimetableDAO timetableDAO;

    @EJB
    private
    LineDAO lineDAO;

    private Map<String, Set<String>> networksBytimetables = new HashMap<>();

    private AnalyzeReport analyzeReport;
    private List<Pair<Timetable, Timetable>> modifiedTimetables;


    @Override
    public boolean execute(Context context) throws Exception {
        analyzeReport = (AnalyzeReport)context.get(ANALYSIS_REPORT);
        modifiedTimetables = analyzeReport.getModifiedTimetables();
        checkMultipleUsedTimetables();
        checkModificationsOnExistingTimetables(context);

        return SUCCESS;
    }


    /**
     * Read all timetables incoming from input file and compares them to existing timetables
     * @param context
     */
    private void checkModificationsOnExistingTimetables(Context context) {
        Referential referential = (Referential) context.get(REFERENTIAL);

        referential.getSharedTimetables().values()
                                         .forEach(this::checkModificationsForTimetable);


    }


    /**
     * Checks if a modification is existing between incoming timetable and existing timetable
     * If so, writes it into the report
     * @param incomingTimeTable
     *     timetable coming from the input file
     */
    private void checkModificationsForTimetable(Timetable incomingTimeTable){

        Timetable existingTimetable = timetableDAO.findByObjectId(incomingTimeTable.getObjectId());
        if (existingTimetable == null){
            //incoming timetable does not exist in DB. No check is performed
            return;
        }

        if (isThereDifferenceBetweenTimetables(existingTimetable, incomingTimeTable)){
            //difference has been found between incoming timetables and existing. Write 2 objects into report
            modifiedTimetables.add(Pair.of(existingTimetable, incomingTimeTable));
        }

    }


    /**
     * Detect if there is a difference between 2 timetables
     *
     * @param tt1
     *    first timetable to compare
     * @param tt2
     *    second timetable to compare
     * @return
     *    true : there is a difference between the 2 timetables
     *    false : timetables are identical
     */
    private boolean isThereDifferenceBetweenTimetables(Timetable tt1, Timetable tt2){


        // 1st check : chack on periods
        List<Period> tt1Periods = tt1.getPeriods();
        List<Period> tt2Periods = tt2.getPeriods();

        if (tt1Periods.size() != tt2Periods.size()){
            return true;
        }

        Collections.sort(tt1Periods);
        Collections.sort(tt2Periods);

        for (int i = 0; i< tt1Periods.size(); i++) {
            Period tt1Period = tt1Periods.get(i);
            Period tt2Period = tt2Periods.get(i);

            if (!tt1Period.getStartDate().equals(tt2Period.getStartDate()) || !tt1Period.getEndDate().equals(tt2Period.getEndDate())){
                return true;
            }
        }


        // 2nd check : chack on exception dates
        List<CalendarDay> tt1CalendarDays = tt1.getCalendarDays();
        List<CalendarDay> tt2CalendarDays = tt2.getCalendarDays();

        if (tt1CalendarDays.size() != tt2CalendarDays.size()){
            return true;
        }

        Collections.sort(tt1CalendarDays);
        Collections.sort(tt2CalendarDays);

        for (int i = 0; i< tt1CalendarDays.size(); i++) {
            CalendarDay tt1Calday = tt1CalendarDays.get(i);
            CalendarDay tt2Calday = tt2CalendarDays.get(i);

            if (!tt1Calday.getIncluded().equals(tt2Calday.getIncluded()) || !tt1Calday.getDate().equals(tt2Calday.getDate())){
                return true;
            }
        }
        return false;
    }


    /**
     * Checks if a timetable is used for 2 or more networks (in database)     *
     *
     */
    private void checkMultipleUsedTimetables() throws ChouetteStatisticsTimeoutException {
        Map<String, Set<String>> multipleUsedTimetables = analyzeReport.getMultipleUsedTimetables();

        Collection<LineAndTimetable> timetableAndLines = timetableDAO.getAllTimetableForAllLines();
        for (LineAndTimetable timetableAndLine : timetableAndLines) {

            Long lineId = timetableAndLine.getLineId();
            Line line = lineDAO.find(lineId);
            String networkName = line.getNetwork().getName();

            for (Timetable timetable : timetableAndLine.getTimetables()) {
                String timetableName = timetable.getComment();

                if (!networksBytimetables.containsKey(timetableName)){
                    networksBytimetables.put(timetableName, new HashSet<>());
                }

                Set<String> networkList = networksBytimetables.get(timetableName);
                networkList.add(networkName);
            }
        }

        networksBytimetables.entrySet().stream()
                                        .filter(entry -> entry.getValue() != null &&  entry.getValue().size() > 1)
                                        .forEach(entry -> multipleUsedTimetables.put(entry.getKey(), entry.getValue()));
    }

    public static class DefaultCommandFactory extends CommandFactory {

        @Override
        protected Command create(InitialContext context) throws IOException {
            Command result = null;
            try {
                String name = "java:app/mobi.chouette.exchange/" + COMMAND;
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
        CommandFactory.factories.put(TimetableCheckCommand.class.getName(), new TimetableCheckCommand.DefaultCommandFactory());
    }
}
