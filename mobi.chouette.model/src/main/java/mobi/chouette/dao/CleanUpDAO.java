package mobi.chouette.dao;

import mobi.chouette.core.CoreException;
import mobi.chouette.model.Referential;
import org.joda.time.LocalDate;

import java.util.List;

public interface CleanUpDAO  {

    String lauchCleanUpFunction(String functionName) throws CoreException;
    String removeExpiredTimetableDates(LocalDate startDate);
    String removeUnusedPeriods(LocalDate validationStartDate, LocalDate validationEndDate);
    String removeExpiredAttributions();

    }
