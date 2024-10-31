package mobi.chouette.dao;

import mobi.chouette.model.Timetable;
import mobi.chouette.model.statistics.LineAndTimetable;
import org.joda.time.LocalDate;

import java.util.Collection;

public interface TimetableDAO extends GenericDAO<Timetable> {

	Collection<LineAndTimetable> getAllTimetableForAllLines();

	Collection<Timetable> getByCompanyRegistrationNumber(String companyRegistrationNumber);

	Collection<? extends Number> getActiveTimetableIdsByDay(LocalDate day);
}
