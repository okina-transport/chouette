package mobi.chouette.exchange.fileAnalysis;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Constant;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.exchange.importer.AbstractImporterCommand;
import mobi.chouette.exchange.importer.updater.LineOptimiser;
import mobi.chouette.exchange.importer.updater.LineUpdater;
import mobi.chouette.exchange.importer.updater.Updater;
import mobi.chouette.exchange.report.AnalyzeReport;
import mobi.chouette.model.CalendarDay;
import mobi.chouette.model.JourneyPattern;
import mobi.chouette.model.Line;
import mobi.chouette.model.ObjectReference;
import mobi.chouette.model.Period;
import mobi.chouette.model.Route;
import mobi.chouette.model.ScheduledStopPoint;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.StopPoint;
import mobi.chouette.model.Timetable;
import mobi.chouette.model.VehicleJourney;
import mobi.chouette.model.type.ChouetteAreaEnum;
import mobi.chouette.model.type.Utils;
import mobi.chouette.model.util.Referential;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.LocalDate;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Log4j
@Stateless(name = ProcessAnalyzeCommand.COMMAND)
public class ProcessAnalyzeCommand extends AbstractImporterCommand implements Command, Constant {

    public static final String COMMAND = "ProcessAnalyzeCommand";


    @Override
    public boolean execute(Context context) throws Exception {
        boolean result = ERROR;

        DateTime startingTime = new DateTime();
        int currentLineNb = context.get(CURRENT_LINE_NB) == null ? 1 : (int) context.get(CURRENT_LINE_NB) + 1;
        context.put(CURRENT_LINE_NB,currentLineNb);

        log.info("Starting analysis " + currentLineNb + "/" + context.get(TOTAL_NB_OF_LINES));
        Referential cache = new Referential();
        context.put(CACHE, cache);
        context.put(OPTIMIZED, Boolean.FALSE);

        Referential referential = (Referential) context.get(REFERENTIAL);

        Line newValue  = referential.getLines().values().iterator().next();

        feedAnalysisWithLineData(context, newValue);
        feedAnalysisWithStopAreaData(context, newValue);

        DateTime endingTime = new DateTime();

        Duration duration = new Duration(endingTime, startingTime);
        log.info("analysis completed in:" + duration.toString());
        result = SUCCESS;


        return result;
    }



    /**
     * Read the context to recover all data of stopAreas and write analysis results into analyzeReport
     * @param context
     * @param line
     */
    private void feedAnalysisWithStopAreaData(Context context, Line line){
        AnalyzeReport analyzeReport = (AnalyzeReport)context.get(ANALYSIS_REPORT);
        Referential referential = (Referential) context.get(REFERENTIAL);



        List<StopArea> stopAreaList = new ArrayList<>();


        for (Route route : line.getRoutes()) {
            for (JourneyPattern journeyPattern : route.getJourneyPatterns()) {
                for (StopPoint stopPoint : journeyPattern.getStopPoints()) {
                    Optional<StopArea> stopAreaOpt = Utils.getStopAreaFromScheduledStopPoint(stopPoint);
                    stopAreaOpt.ifPresent(stopAreaList::add);
                }

            }
        }


        stopAreaList.forEach(stopArea -> {
            if (!analyzeReport.getStops().contains(stopArea)){
                analyzeReport.getStops().add(stopArea);
            }
        });
    }


    /**
     * Read the context to recover all data of the files and write analysis results into analyzeReport
     * @param context
     */
    private void feedAnalysisWithLineData(Context context, Line line){

        AnalyzeReport analyzeReport = (AnalyzeReport)context.get(ANALYSIS_REPORT);
        List incomingLineList = (List) context.get(INCOMING_LINE_LIST);

        List<String> vehicleJourneys = new ArrayList<>();



        String lineName = line.getName();

        //If line is not part of the incoming file or if line has already been analyzed, we skip it
        if (!incomingLineList.contains(line.getObjectId()) || analyzeReport.getLines().contains(lineName))
            return;


        analyzeReport.getLines().add(lineName);
        analyzeReport.addLineTextColor(lineName,line.getTextColor());
        analyzeReport.addLineBackgroundColor(lineName,line.getColor());
        analyzeReport.addLineShortName(lineName,line.getNumber());

        for (Route route : line.getRoutes()) {

            for (JourneyPattern journeyPattern : route.getJourneyPatterns()) {

                for (VehicleJourney vehicleJourney : journeyPattern.getVehicleJourneys()) {
                    vehicleJourneys.add(vehicleJourney.getObjectId());

                    for (Timetable timetable : vehicleJourney.getTimetables()) {

                        Optional<LocalDate> startOfPeriod = getMinDateOfTimeTable(timetable);
                        Optional<LocalDate> endOfPeriod = getMaxDateOfTimeTable(timetable);

                        if (startOfPeriod.isPresent() && (analyzeReport.getOldestPeriodOfCalendars() == null || (analyzeReport.getOldestPeriodOfCalendars().isAfter(startOfPeriod.get())))){
                            analyzeReport.setOldestPeriodOfCalendars(startOfPeriod.get());
                        }

                        if (endOfPeriod.isPresent() && (analyzeReport.getNewestPeriodOfCalendars() == null || (analyzeReport.getNewestPeriodOfCalendars().isBefore(endOfPeriod.get())))){
                            analyzeReport.setNewestPeriodOfCalendars(endOfPeriod.get());
                        }
                    }
                }
            }
        }


        vehicleJourneys.forEach(vehicleJourney->{
            if(!analyzeReport.getJourneys().contains(vehicleJourney)){
                analyzeReport.getJourneys().add(vehicleJourney);
            }
        });
    }

    private Optional<LocalDate> getMinDateOfTimeTable(Timetable timetable ){

        List<LocalDate> startPeriodList = timetable.getPeriods().stream()
                .map(Period::getStartDate)
                .collect(Collectors.toList());


        List<LocalDate> calendarDates = timetable.getCalendarDays().stream()
                .filter(CalendarDay::getIncluded)
                .map(CalendarDay::getDate)
                .collect(Collectors.toList());

        startPeriodList.addAll(calendarDates);

        return startPeriodList.isEmpty() ? Optional.empty() : startPeriodList.stream().min(LocalDate::compareTo);
    }

    private Optional<LocalDate> getMaxDateOfTimeTable(Timetable timetable ){

        List<LocalDate> endPeriodList = timetable.getPeriods().stream()
                .map(Period::getEndDate)
                .collect(Collectors.toList());


        List<LocalDate> calendarDates = timetable.getCalendarDays().stream()
                .filter(CalendarDay::getIncluded)
                .map(CalendarDay::getDate)
                .collect(Collectors.toList());

        endPeriodList.addAll(calendarDates);
        return endPeriodList.isEmpty() ? Optional.empty() : endPeriodList.stream().max(LocalDate::compareTo);
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
        CommandFactory.factories.put(ProcessAnalyzeCommand.class.getName(), new ProcessAnalyzeCommand.DefaultCommandFactory());
    }
}
