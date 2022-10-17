package mobi.chouette.dao;

import mobi.chouette.core.CoreException;

import java.time.LocalDate;

public interface CleanUpDAO {

    String lauchCleanUpFunction(String functionName) throws CoreException;

    String removeExpiredTimetableDates(LocalDate startDate);

    String removeUnusedPeriods(LocalDate validationStartDate, LocalDate validationEndDate);

}
