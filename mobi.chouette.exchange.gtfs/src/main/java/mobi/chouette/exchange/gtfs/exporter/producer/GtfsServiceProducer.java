/**
 * Projet CHOUETTE
 *
 * ce projet est sous license libre
 * voir LICENSE.txt pour plus de details
 *
 */

package mobi.chouette.exchange.gtfs.exporter.producer;

import lombok.extern.log4j.Log4j;
import mobi.chouette.exchange.gtfs.model.GtfsCalendar;
import mobi.chouette.exchange.gtfs.model.GtfsCalendarDate;
import mobi.chouette.exchange.gtfs.model.exporter.GtfsExporterInterface;
import mobi.chouette.model.CalendarDay;
import mobi.chouette.model.Period;
import mobi.chouette.model.Timetable;
import mobi.chouette.model.type.DayTypeEnum;
import org.joda.time.LocalDate;

import java.util.List;

/**
 * convert Timetable to Gtfs Calendar and CalendarDate
 * <p>
 * optimise multiple period timetable with calendarDate inclusion or exclusion
 */
@Log4j
public class GtfsServiceProducer extends
AbstractProducer
{
   public GtfsServiceProducer(GtfsExporterInterface exporter)
   {
      super(exporter);
   }

   GtfsCalendar calendar = new GtfsCalendar();
   GtfsCalendarDate calendarDate = new GtfsCalendarDate();

   public boolean save(Timetable timetable, String prefix, boolean keepOriginalId, LocalDate startDate, LocalDate endDate)
   {

      if (timetable == null) return false;

      String serviceId = toGtfsId(timetable.getObjectId(), prefix, keepOriginalId);
      calendar.setServiceId(serviceId);
      clear(calendar);

      if (!isEmpty(timetable.getPeriods()))
      {

         for (DayTypeEnum dayType : timetable.getDayTypes())
         {
            switch (dayType)
            {
            case Monday:
               calendar.setMonday(true);
               break;
            case Tuesday:
               calendar.setTuesday(true);
               break;
            case Wednesday:
               calendar.setWednesday(true);
               break;
            case Thursday:
               calendar.setThursday(true);
               break;
            case Friday:
               calendar.setFriday(true);
               break;
            case Saturday:
               calendar.setSaturday(true);
               break;
            case Sunday:
               calendar.setSunday(true);
               break;
            case WeekDay:
               calendar.setMonday(true);
               calendar.setTuesday(true);
               calendar.setWednesday(true);
               calendar.setThursday(true);
               calendar.setFriday(true);
               break;
            case WeekEnd:
               calendar.setSaturday(true);
               calendar.setSunday(true);
               break;
            default:
               // nothing to do
            }
         }


         Period period = timetable.getPeriods().get(0);

         if(startDate != null && startDate.isAfter(period.getStartDate())){
            calendar.setStartDate(startDate);
         }
         else{
            calendar.setStartDate(period.getStartDate());

         }

         if(endDate != null && endDate.isBefore(period.getEndDate())){
            calendar.setEndDate(endDate);
         }
         else{
            calendar.setEndDate(period.getEndDate());
         }


      }
      if (!isEmpty(timetable.getCalendarDays()))
      {
         List<CalendarDay> calendarDays = timetable.getCalendarDays();

         LocalDate currentMin = null;
         LocalDate currentMax = null;

         for (CalendarDay day : calendarDays)
         {
            if(startDate == null && endDate == null ||
                    startDate != null && endDate != null && !day.getDate().isBefore(startDate) && !day.getDate().isAfter(endDate)) {
               saveDay(serviceId,day);
            }

            if (currentMin == null || currentMin.isAfter(day.getDate())){
               currentMin = day.getDate();
            }

            if (currentMax == null || currentMax.isBefore(day.getDate())){
               currentMax = day.getDate();
            }

         }


         if (calendar.getStartDate() == null || calendar.getStartDate().isAfter(currentMin)){
            calendar.setStartDate(currentMin);
         }

         if (calendar.getEndDate() == null || calendar.getEndDate().isBefore(currentMax)){
            calendar.setEndDate(currentMax);
         }
      }

      try
      {
         getExporter().getCalendarExporter().export(calendar);
      }
      catch (Exception e)
      {
         log.error(e.getMessage(),e);
         return false;
      }

      return true;
   }


   private void clear(GtfsCalendar c)
   {
      c.setMonday(false);
      c.setTuesday(false);
      c.setWednesday(false);
      c.setThursday(false);
      c.setFriday(false);
      c.setSaturday(false);
      c.setSunday(false);
   }


   private boolean saveDay(String serviceId,CalendarDay day)
   {

      calendarDate.setDate(day.getDate());
      calendarDate.setServiceId(serviceId);
      calendarDate.setExceptionType(day.getIncluded() ? GtfsCalendarDate.ExceptionType.Added: GtfsCalendarDate.ExceptionType.Removed);
      try
      {
         getExporter().getCalendarDateExporter().export(calendarDate);
      }
      catch (Exception e)
      {
         log.error(e.getMessage(),e);
         return false;
      }
      return true;

   }

}
