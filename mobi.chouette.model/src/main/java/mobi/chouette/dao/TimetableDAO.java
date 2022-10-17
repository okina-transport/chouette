package mobi.chouette.dao;

import mobi.chouette.dao.exception.ChouetteStatisticsTimeoutException;
import mobi.chouette.model.Timetable;
import mobi.chouette.model.statistics.LineAndTimetable;

import java.util.Collection;

public interface TimetableDAO extends GenericDAO<Timetable> {

	/**
	 *
	 * @return an aggregated view for all timetables in all lines for the current referential.
	 * @throws ChouetteStatisticsTimeoutException if the statistics query times out, most likely due to an import process locking the database tables (TRUNCATE operations block SELECT queries)
	 */
	Collection<LineAndTimetable> getAllTimetableForAllLines() throws ChouetteStatisticsTimeoutException;

	Collection<Timetable>getByCompanyRegistrationNumber(String companyRegistrationNumber);
}
