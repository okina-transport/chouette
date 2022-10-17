package mobi.chouette.service;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Color;
import mobi.chouette.common.TimeUtil;
import mobi.chouette.dao.LineDAO;
import mobi.chouette.dao.TimetableDAO;
import mobi.chouette.dao.exception.ChouetteStatisticsTimeoutException;
import mobi.chouette.model.CalendarDay;
import mobi.chouette.model.statistics.*;
import mobi.chouette.persistence.hibernate.ContextHolder;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.rutebanken.helper.calendar.CalendarPattern;
import org.rutebanken.helper.calendar.CalendarPatternAnalyzer;

import javax.ejb.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static javax.ejb.ConcurrencyManagementType.BEAN;
import static mobi.chouette.common.TimeUtil.toDate;
import static mobi.chouette.common.TimeUtil.toLocalDate;

@ConcurrencyManagement(BEAN)
@Singleton(name = TransitDataStatisticsService.BEAN_NAME)
@Log4j
public class TransitDataStatisticsService {

    public static final String BEAN_NAME = "TransitDataStatisticsService";

    @EJB
    LineDAO lineDAO;

    @EJB
    TimetableDAO timetableDAO;

    /**
     * Returns a list of Lines grouped by Line "number". Create merged timetable
     * periods. Not supporting frequency based yet.
     * The transaction attribute is set to NEVER so that temporary modifications in Line objects
	 * (creation of a line number if missing, ... ) are not persisted to the database
	 *
     * @param referential
     * @param startDate                 the first date to return data from (that is, filter away old
     *                                  and obsolete periods
     * @param minDaysValidityCategories organize lineNumbers into validity categories. First category
     *                                  is from 0 to lowest key value. Corresponding value in map is
     *                                  used as categegory name,
     * @return the line statistics
     * @throws RequestServiceException if the statistics cannot be calculated
     */
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public LineStatistics getLineStatisticsByLineNumber(String referential, Date startDate, int days,
                                                        Map<Integer, String> minDaysValidityCategories) throws ServiceException {

        ContextHolder.setContext(referential);

        log.info(Color.CYAN + "Gettings statistics for "
                + referential
                + " using startDate="
                + startDate
                + " and minDaysValidityCategories="
                + minDaysValidityCategories);

        // Defaulting to today if not given
        if (startDate == null) {
            startDate = TimeUtil.toDate(LocalDate.now().atStartOfDay());
        }

        Map<String, PublicLine> publicLines = new HashMap<String, PublicLine>();

        // Convert Chouette internal model to the statistics model used
        try {
			convertChouetteModelToStatisticsModel(startDate, publicLines);
		} catch (ChouetteStatisticsTimeoutException e) {
			throw new RequestServiceException(RequestExceptionCode.REFERENTIAL_BUSY, "Query timeout while calculating statistics for referential " + referential, e );
		}

        // If Line->Timetable->Period is empty, remove Line but keep publicLine
        filterLinesWithEmptyTimetablePeriods(publicLines);

        // Merge overlapping periods in PublicLine for readability
        mergePeriods(publicLines);

        List<PublicLine> pL = new ArrayList<>(publicLines.values());
        Collections.sort(pL);

        LineStatistics lineStats = new LineStatistics(startDate, days, pL, true, true);
        // Put lineNumbers into buckets depending on validity in the future
        categorizeValidity(lineStats, startDate, minDaysValidityCategories);

        // Merge identical names to display in PublicLines
        mergeNames(lineStats);

        // If all lines are expiring or expired, send a mail to report
        alertExpired(lineStats, startDate, minDaysValidityCategories);

        return lineStats;
    }

    private void alertExpired(LineStatistics lineStats, Date startDate, Map<Integer, String> minDaysValidityCategories) {
        Calendar cd = Calendar.getInstance();
        cd.setTime(startDate);
        List<Integer> categories = new ArrayList<>(minDaysValidityCategories.keySet());
        Collections.sort(categories);
        cd.add(Calendar.DATE, categories.get(0));
        Date dateExpiring = cd.getTime();
        if (lineStats.getPublicLines().size() == 0) {
            lineStats.setInvalid(false);
            lineStats.setExpiring(false);
        } else {
            for (PublicLine pl : lineStats.getPublicLines()) {
                if (pl.getEffectivePeriods().size() != 0) {
                    int s = pl.getEffectivePeriods().size();
                    Period p = pl.getEffectivePeriods().get(s - 1);
                    if (p.getTo().compareTo(startDate) > 0) {
                        lineStats.setInvalid(false);
                    }
                    if (p.getTo().compareTo(dateExpiring) > 0) {
                        lineStats.setExpiring(false);
                    }
                }
            }
        }

        if (lineStats.getPublicLines().size() != 0 && lineStats.isInvalid()) {
            lineStats.setExpiring(false);
        }
    }

    private void mergeNames(LineStatistics lineStats) {
        for (PublicLine pl : lineStats.getPublicLines()) {
            Set<String> names = new TreeSet<String>();
            for (Line l : pl.getLines()) {
                if (l.getName() != null) {
                    names.add(l.getName());
                } else {
                    log.warn("Ignored line with null name: " + l.getObjectId());
                }
            }

            pl.getLineNames().addAll(names);
        }
    }

    void categorizeValidity(LineStatistics lineStats, Date startDate, Map<Integer, String> minDaysValidityCategories) {

        ValidityCategory defaultCategory = new ValidityCategory(getCategoryName(minDaysValidityCategories, 0, "EXPIRING"), 0, new ArrayList<>());
        ValidityCategory invalidCategory = new ValidityCategory(getCategoryName(minDaysValidityCategories, -1, "INVALID"), -1, new ArrayList<>());
        List<ValidityCategory> validityCategories = new ArrayList<>();

        List<Integer> categories = new ArrayList<>(minDaysValidityCategories.keySet());
        Collections.sort(categories);
        Collections.reverse(categories);

        for (Integer numDays : categories) {
            if (numDays > 0) {
                ValidityCategory category = new ValidityCategory(getCategoryName(minDaysValidityCategories, numDays, numDays.toString()), numDays,
                        new ArrayList<>());
                validityCategories.add(category);
            }
        }

        LocalDate startDateLocal = toLocalDate(startDate);

        for (PublicLine pl : lineStats.getPublicLines()) {
            boolean foundCategory = false;
            for (int i = 0; i < validityCategories.size(); i++) {
                ValidityCategory vc = validityCategories.get(i);

                foundCategory = isValidAtLeastNumberOfDays(pl, startDateLocal, vc.getNumDaysAtLeastValid());
                if (foundCategory) {
                    vc.getLineNumbers().add(pl.getLineNumber());
                    break;
                }

            }

            if (!foundCategory) {
                if (isValidAtLeastNumberOfDays(pl, startDateLocal, 0)) {
                    defaultCategory.getLineNumbers().add(pl.getLineNumber());
                } else {
                    invalidCategory.getLineNumbers().add(pl.getLineNumber());
                }
            }
        }

        lineStats.getValidityCategories().add(invalidCategory);
        lineStats.getValidityCategories().add(defaultCategory);
        lineStats.getValidityCategories().addAll(validityCategories);
    }

    private String getCategoryName(Map<Integer, String> minDaysValidityCategories, Integer limit, String defaultName) {
        String name = minDaysValidityCategories.get(limit);
        if (name != null) {
            return name;
        }
        return defaultName;
    }

    private boolean isValidAtLeastNumberOfDays(PublicLine pl, LocalDate startDateLocal, Integer numDays) {
        if (pl.getEffectivePeriods().size() > 0) {
            Date limitDate = TimeUtil.toDate(startDateLocal.plusDays(numDays));
            List<Period> allPeriods = new ArrayList<>();
            allPeriods.addAll(pl.getEffectivePeriods());
            allPeriods.sort(Comparator.comparing(o -> o.getTo().getTime()));
            Date startDateGlobalPeriods = allPeriods.get(0).getFrom();
            allPeriods.sort(Collections.reverseOrder());
            Date endDateGlobalPeriods = allPeriods.get(0).getTo();
            return !startDateGlobalPeriods.after(TimeUtil.toDate(startDateLocal)) && !endDateGlobalPeriods.before(limitDate);
        }

        return false;
    }

    protected void mergePeriods(Map<String, PublicLine> publicLines) {
        for (PublicLine pl : publicLines.values()) {
            Set<Period> uniquePeriods = new TreeSet<Period>();
            for (Line l : pl.getLines()) {
                for (Timetable t : l.getTimetables()) {
                    t.setPeriods(mergeOverlappingPeriods(t.getPeriods()));
                    uniquePeriods.addAll(t.getPeriods());
                }
            }

            pl.getEffectivePeriods().addAll(uniquePeriods);
            pl.setEffectivePeriods(mergeOverlappingPeriods(pl.getEffectivePeriods()));
        }
    }

    protected void convertChouetteModelToStatisticsModel(Date startDate, Map<String, PublicLine> publicLines) throws ChouetteStatisticsTimeoutException {
        // Load list of lineIds with corresponding Timetables
        long now = System.currentTimeMillis();
        Collection<LineAndTimetable> allTimetableForAllLines = timetableDAO.getAllTimetableForAllLines();
        log.debug("Timetables took " + (System.currentTimeMillis() - now) + "ms");

        // Find all ids and load all Chouette Lines
        Set<Long> lineIds = new HashSet<>();
        for (LineAndTimetable lat : allTimetableForAllLines) {
            lineIds.add(lat.getLineId());
        }
        now = System.currentTimeMillis();
        List<mobi.chouette.model.Line> lines = lineDAO.findAll(lineIds);
        log.debug("Lines took " + (System.currentTimeMillis() - now) + "ms");

        Map<Long, mobi.chouette.model.Line> lineIdToLine = new HashMap<>();
        for (mobi.chouette.model.Line l : lines) {
            lineIdToLine.put(l.getId(), l);
        }

        Map<String, String> lineNameToFakeLineNumber = new HashMap<>();

        int fakeLineNumberCounter = 0;

        for (LineAndTimetable lat : allTimetableForAllLines) {
            mobi.chouette.model.Line l = lineIdToLine.get(lat.getLineId());

            String number = StringUtils.trimToNull(l.getNumber());
            if (number == null) {
                String lineNameKey = l.getName() + "-" + l.getCompany().getName();
                number = lineNameToFakeLineNumber.get(lineNameKey);
                if (number == null) {
                    number = "<" + (++fakeLineNumberCounter) + ">";
                    lineNameToFakeLineNumber.put(lineNameKey, number);
                }
            }
            l.setNumber(number);

            PublicLine publicLine = publicLines.get(l.getNumber());
            if (publicLine == null) {
                publicLine = new PublicLine(l.getNumber());
                publicLines.put(l.getNumber(), publicLine);
            }

            Line line = new Line(l.getId(), l.getObjectId(), l.getName());
            publicLine.getLines().add(line);

            Set<CalendarDay> calendarDaysForLine = new HashSet<>();

            Timetable timetableForCalendarDays = null;

            boolean foundStartEndDateOfTimetable = false;


            for (mobi.chouette.model.Timetable t : lat.getTimetables()) {
                Timetable timetable = new Timetable(t.getId(), t.getObjectId());

                line.getTimetables().add(timetable);

                if (t.getStartOfPeriod() != null && t.getEndOfPeriod() != null) {
                    timetable.getPeriods().add(new Period(t.getStartOfPeriod(), t.getEndOfPeriod()));
                    foundStartEndDateOfTimetable = true;
                } else {

                    if (t.getPeriods() != null && t.getPeriods().size() > 0) {
                        // Use periods
                        for (mobi.chouette.model.Period p : t.getPeriods()) {
                            Period period = new Period(p.getStartDate(), p.getEndDate());
                            if (!period.isEmpty() && !period.getTo().before(startDate)) {
                                // log.info("Adding normal period " + p);
                                timetable.getPeriods().add(period);
                            }
                        }
                    }

                    if (t.getCalendarDays() != null) {
                        for (CalendarDay day : t.getCalendarDays()) {
                            if (day.getIncluded() && !startDate.after(TimeUtil.toDate(day.getDate()))) {
                                timetable.getPeriods().add(new Period(day.getDate(), day.getDate()));
                                calendarDaysForLine.add(day);
                                timetableForCalendarDays = timetable;
                            }
                        }
                    }

                    if (timetable.getPeriods().isEmpty()) {
                        // Use timetable from/to as period
                        t.computeLimitOfPeriods();
                        Period period = new Period(t.getStartOfPeriod(), t.getEndOfPeriod());

                        // TODO could be separate days here as well that should
                        // be
                        // included
                        if (!period.isEmpty() && !period.getTo().before(startDate)) {
                            // log.info("Adding timetable period " + period);
                            timetable.getPeriods().add(period);
                        } else {
                            if(log.isTraceEnabled()) {
								log.trace("No from/to in timetable objectId=" + t.getObjectId() + " id=" + t.getId());
                        }}

                    }
                }

                // Inclusion of excluded dates.

                List<Period> periodsToDelete = new ArrayList<>();
                List<CalendarDay> daystoDelete = new ArrayList<>();
                List<CalendarDay> daysToDeleteReverse = new ArrayList<>();

                if (t.getPeriods() != null && t.getPeriods().size() > 0) {
                    for (mobi.chouette.model.Period p : t.getPeriods()) {
                        if (t.getCalendarDays() != null) {

                            // All excluded dates are retrieved for sorting in chronological and inverse order.

                            for (CalendarDay dayExcluded : t.getCalendarDays()) {
                                if (!dayExcluded.getIncluded()) {
                                    daystoDelete.add(dayExcluded);
                                }
                            }
                            daystoDelete.sort(Comparator.comparing(o -> o.getDate()));
                            daysToDeleteReverse.addAll(daystoDelete);
                            daysToDeleteReverse.sort(Collections.reverseOrder());

                            Calendar newDateStart = Calendar.getInstance();
                            Calendar newDateEnd = Calendar.getInstance();
                            boolean newDateStartValued = false;
                            boolean newDateEndValued = false;

                            // We manage the beginning and the end of the calendars.

                            for (CalendarDay dayDelete : daystoDelete) {
                                if (p.getStartDate().equals(dayDelete.getDate())) {
                                    newDateStart.setTime(toDate(p.getStartDate()));
                                    newDateStart.add(Calendar.DATE, 1);
                                    newDateStartValued = true;
                                }

                                if (newDateStartValued && newDateStart.getTime().equals(toDate(dayDelete.getDate()))) {
                                    newDateStart.add(Calendar.DATE, 1);
                                }
                            }

                            for (CalendarDay dayDeleteReverse : daysToDeleteReverse) {
                                if (p.getEndDate().equals(dayDeleteReverse.getDate())) {
                                    newDateEnd.setTime(toDate(p.getEndDate()));
                                    newDateEnd.add(Calendar.DATE, -1);
                                    newDateEndValued = true;
                                }

                                if (newDateEndValued && newDateEnd.getTime().equals(toDate(dayDeleteReverse.getDate()))) {
                                    newDateEnd.add(Calendar.DATE, -1);
                                }
                            }

                            // Exclusions between the beginning and the end of the calendar are taken into account.

                            for (CalendarDay dayDelete : daystoDelete) {
                                if (!newDateStartValued) {
                                    newDateStart.setTime(toDate(p.getStartDate()));
                                    newDateStartValued = true;
                                }
                                if (!newDateEndValued) {
                                    newDateEnd.setTime(toDate(p.getEndDate()));
                                    newDateEndValued = true;
                                }

                                // Management of excluded dates between the beginning and the end of the period.

                                if (newDateStart.getTime().compareTo(toDate(dayDelete.getDate())) <= 0 && newDateEnd.getTime().compareTo(toDate(dayDelete.getDate())) > 0) {
                                    periodsToDelete.add(new Period(newDateStart.getTime(), newDateEnd.getTime()));
                                    Calendar dateEnd = Calendar.getInstance();
                                    Calendar dateStart = Calendar.getInstance();

                                    if (newDateStart.getTime().compareTo(toDate(dayDelete.getDate())) < 0) {
                                        dateEnd.setTime(toDate(dayDelete.getDate()));
                                        dateEnd.add(Calendar.DATE, -1);

                                        timetable.getPeriods().add(new Period(newDateStart.getTime(), dateEnd.getTime()));
                                    }

                                    dateStart.setTime(toDate(dayDelete.getDate()));
                                    dateStart.add(Calendar.DATE, 1);

                                    timetable.getPeriods().add(new Period(dateStart.getTime(), newDateEnd.getTime()));
                                    newDateStart = dateStart;
                                    newDateStartValued = true;
                                } else if (!newDateStart.getTime().equals(toDate(p.getStartDate())) || !newDateEnd.getTime().equals(toDate(p.getEndDate()))) {
                                    periodsToDelete.add(new Period(toDate(p.getStartDate()), toDate(p.getEndDate())));
                                    timetable.getPeriods().add(new Period(newDateStart.getTime(), newDateEnd.getTime()));
                                }
                            }
                        }
                    }
                }


                if (periodsToDelete != null && periodsToDelete.size() > 0) {
                    timetable.getPeriods().removeAll(periodsToDelete);
                }

            }

            if (!foundStartEndDateOfTimetable) {
                // Legacy after reimport?
                Period fromCalendarDaysPattern = calculatePeriodFromCalendarDaysPattern(calendarDaysForLine);

                if (fromCalendarDaysPattern != null) {
                    log.debug("Successfully created validity interval from included days for line: " + line.getId());
                    timetableForCalendarDays.getPeriods().add(fromCalendarDaysPattern);
                }
            }

        }
    }

    private Period calculatePeriodFromCalendarDaysPattern(Collection<CalendarDay> calendarDays) {

        Set<java.time.LocalDate> includedDays = calendarDays.stream().filter(CalendarDay::getIncluded)
                .map(c -> java.time.LocalDate.of(c.getDate().getYear(), c.getDate().getMonthValue(), c.getDate().getDayOfMonth())).collect(Collectors.toSet());

        CalendarPattern pattern = new CalendarPatternAnalyzer().computeCalendarPattern(includedDays);

        if (pattern != null) {

            return new Period(pattern.from, pattern.to);
        }
        return null;
    }

    protected void filterLinesWithEmptyTimetablePeriods(Map<String, PublicLine> publicLines) {

        List<PublicLine> filteredPublicLines = new ArrayList<>();

        for (PublicLine pl : publicLines.values()) {
            List<Line> filteredLines = new ArrayList<>();
            for (Line l : pl.getLines()) {
                List<Timetable> filteredTimetables = new ArrayList<>();

                for (Timetable t : l.getTimetables()) {
                    if (t.getPeriods().size() > 0) {
                        filteredTimetables.add(t);
                    }
                }

                l.getTimetables().clear();
                l.getTimetables().addAll(filteredTimetables);

                if (l.getTimetables().size() > 0) {
                    filteredLines.add(l);
                }
            }

            pl.getLines().clear();
            pl.getLines().addAll(filteredLines);

            if (pl.getLines().size() > 0) {
                filteredPublicLines.add(pl);
            }

        }

        // List<String> lineNumbersToRemove = new ArrayList<>();
        //
        // for(String lineNumber : publicLines.keySet()) {
        // if(publicLines.get(lineNumber).getLines().size() == 0) {
        // lineNumbersToRemove.add(lineNumber);
        // }
        // }
        //
        // for(String lineNumber : lineNumbersToRemove) {
        // publicLines.remove(lineNumber);
        // }

    }

    public List<Period> mergeOverlappingPeriods(List<Period> intervals) {

        if (intervals.size() == 0 || intervals.size() == 1)
            return intervals;

        Collections.sort(intervals, new Comparator<Period>() {

            @Override
            public int compare(Period o1, Period o2) {
                return o1.getFrom().compareTo(o2.getFrom());
            }
        });

        Period first = intervals.get(0);
        Date start = first.getFrom();
        Date end = first.getTo();

        List<Period> result = new ArrayList<Period>();

        for (int i = 1; i < intervals.size(); i++) {
            Period current = intervals.get(i);
            if (!current.getFrom().after(DateUtils.addDays(end, 1))) {
                end = current.getTo().before(end) ? end : current.getTo();
            } else {
                result.add(new Period(start, end));
                start = current.getFrom();
                end = current.getTo();
            }
        }

        result.add(new Period(start, end));
        return result;
    }

}
