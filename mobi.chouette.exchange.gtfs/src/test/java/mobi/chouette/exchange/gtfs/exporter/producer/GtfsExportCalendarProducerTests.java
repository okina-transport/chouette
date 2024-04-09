package mobi.chouette.exchange.gtfs.exporter.producer;

import java.util.*;
import java.util.stream.Collectors;

import mobi.chouette.exchange.gtfs.exporter.producer.mock.GtfsExporterMock;
import mobi.chouette.exchange.gtfs.model.GtfsCalendar;
import mobi.chouette.exchange.gtfs.model.GtfsCalendarDate;
import mobi.chouette.exchange.gtfs.model.GtfsCalendarDate.ExceptionType;
import mobi.chouette.exchange.gtfs.model.exporter.CalendarDateExporter;
import mobi.chouette.exchange.gtfs.model.exporter.CalendarExporter;
import mobi.chouette.exchange.gtfs.model.importer.Context;
import mobi.chouette.model.CalendarDay;
import mobi.chouette.model.Period;
import mobi.chouette.model.Timetable;
import mobi.chouette.model.type.DayTypeEnum;

import org.joda.time.LocalDate;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.Test;

public class GtfsExportCalendarProducerTests 
{

   private final GtfsExporterMock mock = new GtfsExporterMock();
   private final GtfsServiceProducer producer = new GtfsServiceProducer(mock);
   private final Context context = new Context();

   @Test(groups = { "Producers" }, description = "test timetable with period")
   public void verifyCalendarProducer1()
   {
      mock.reset();
      Calendar c = Calendar.getInstance();
      c.set(Calendar.YEAR, 2013);
      c.set(Calendar.MONTH, Calendar.JULY);
      c.set(Calendar.DAY_OF_MONTH, 1);
      c.set(Calendar.HOUR_OF_DAY, 12);

      Timetable neptuneObject = new Timetable();
      neptuneObject.setObjectId("GTFS:Timetable:1234");
      neptuneObject.setComment("name");
      neptuneObject.addDayType(DayTypeEnum.Monday);
      neptuneObject.addDayType(DayTypeEnum.Saturday);
      LocalDate startDate = LocalDate.fromCalendarFields(c);
      c.add(Calendar.DATE, 15);
      LocalDate endDate = LocalDate.fromCalendarFields(c);
      Period period = new Period(startDate, endDate);
      neptuneObject.addPeriod(period);

      List<Timetable> tms = new ArrayList<>();
      tms.add(neptuneObject);
      producer.save(tms,  "GTFS",false, null, null);
      Reporter.log("verifyCalendarProducer1");
      Assert.assertEquals(mock.getExportedCalendars().size(),1,"Calendar must be returned");
      GtfsCalendar gtfsObject = mock.getExportedCalendars().get(0);
      Reporter.log(CalendarExporter.CONVERTER.to(context, gtfsObject));

      Assert.assertEquals(gtfsObject.getServiceId(), toGtfsId(neptuneObject.getObjectId()), "timetable id must be correcty set");
      Assert.assertEquals(gtfsObject.getStartDate(), startDate, "start date must be correcty set");
      Assert.assertEquals(gtfsObject.getEndDate(), endDate, "end date must be correcty set");
      Assert.assertTrue(gtfsObject.getMonday(), "monday must be true");
      Assert.assertFalse(gtfsObject.getTuesday(), "tuesday must be false");
      Assert.assertFalse(gtfsObject.getWednesday(), "wednesday must be false");
      Assert.assertFalse(gtfsObject.getThursday(), "thursday must be false");
      Assert.assertFalse(gtfsObject.getFriday(), "friday must be false");
      Assert.assertTrue(gtfsObject.getSaturday(), "saturday must be true");
      Assert.assertFalse(gtfsObject.getSunday(), "sunday must be false");

   }

   @Test(groups = { "Producers" }, description = "test timetable with dates")
   public void verifyCalendarProducer2()
   {
      mock.reset();
      Calendar c = Calendar.getInstance();
      c.set(Calendar.YEAR, 2013);
      c.set(Calendar.MONTH, Calendar.JULY);
      c.set(Calendar.DAY_OF_MONTH, 1);
      c.set(Calendar.HOUR_OF_DAY, 12);

      Timetable neptuneObject = new Timetable();
      neptuneObject.setObjectId("GTFS:Timetable:1234");
      neptuneObject.setComment("name");
      for (int i = 0; i < 5; i++) {
         neptuneObject.addCalendarDay(new CalendarDay(LocalDate.fromCalendarFields(c), true));
         c.add(Calendar.DATE, 3);
      }
      Reporter.log(neptuneObject.toString());

      List<Timetable> tms = new ArrayList<>();
      tms.add(neptuneObject);
      producer.save(tms,  "GTFS",false, null, null);
      Reporter.log("verifyCalendarProducer2");

      Assert.assertEquals(mock.getExportedCalendars().size(), 0, "no calendar produced");

      Assert.assertEquals(mock.getExportedCalendarDates().size(), 5, "calendar must have 5 dates");
      c.set(Calendar.YEAR, 2013);
      c.set(Calendar.MONTH, Calendar.JULY);
      c.set(Calendar.DAY_OF_MONTH, 1);

      for (GtfsCalendarDate gtfsCalendarDate : mock.getExportedCalendarDates())
      {
         Reporter.log(CalendarDateExporter.CONVERTER.to(context,gtfsCalendarDate));
         LocalDate date = LocalDate.fromCalendarFields(c);
         c.add(Calendar.DATE, 3);
         Assert.assertEquals(gtfsCalendarDate.getServiceId(), toGtfsId(neptuneObject.getObjectId()), "service id must be correcty set");
         Assert.assertEquals(gtfsCalendarDate.getDate(), date, "calendar date must be correctly");
         Assert.assertEquals(gtfsCalendarDate.getExceptionType(), ExceptionType.Added, "calendar date must be inclusive");
      }

   }

   @Test(groups = { "Producers" }, description = "test timetable with period and dates")
   public void verifyCalendarProducer3()
   {
      mock.reset();
      Calendar c = Calendar.getInstance();
      c.set(Calendar.YEAR, 2013);
      c.set(Calendar.MONTH, Calendar.JULY);
      c.set(Calendar.DAY_OF_MONTH, 1);
      c.set(Calendar.HOUR_OF_DAY, 12);

      Timetable neptuneObject = new Timetable();
      neptuneObject.setObjectId("GTFS:Timetable:1234");
      neptuneObject.setComment("name");
      neptuneObject.addDayType(DayTypeEnum.Tuesday);
      neptuneObject.addDayType(DayTypeEnum.Wednesday);
      neptuneObject.addDayType(DayTypeEnum.Thursday);
      neptuneObject.addDayType(DayTypeEnum.Friday);
      neptuneObject.addDayType(DayTypeEnum.Sunday);
      LocalDate startDate = LocalDate.fromCalendarFields(c);
      c.add(Calendar.DATE, 15);
      LocalDate endDate =  LocalDate.fromCalendarFields(c);
      Period period = new Period(startDate, endDate);
      neptuneObject.addPeriod(period);
      c.add(Calendar.DATE, 15);
      for (int i = 0; i < 5; i++) {
         LocalDate date = LocalDate.fromCalendarFields(c);
         neptuneObject.addCalendarDay(new CalendarDay(date, true));
         c.add(Calendar.DATE, 3);
      }

      List<Timetable> tms = new ArrayList<>();
      tms.add(neptuneObject);
      producer.save(tms,  "GTFS",false, null, null);
      Reporter.log("verifyCalendarProducer3");
      Assert.assertEquals(mock.getExportedCalendars().size(),1,"Calendar must be returned");
      GtfsCalendar gtfsObject = mock.getExportedCalendars().get(0);
      Reporter.log(CalendarExporter.CONVERTER.to(context, gtfsObject));

      Assert.assertEquals(gtfsObject.getServiceId(), toGtfsId(neptuneObject.getObjectId()), "service id must be correcty set");
      Assert.assertEquals(gtfsObject.getStartDate(), startDate, "start date must be correcty set");
      Assert.assertEquals(gtfsObject.getEndDate(), endDate, "end date must be correcty set");
      Assert.assertFalse(gtfsObject.getMonday(), "monday must be false");
      Assert.assertTrue(gtfsObject.getTuesday(), "tuesday must be true");
      Assert.assertTrue(gtfsObject.getWednesday(), "wednesday must be true");
      Assert.assertTrue(gtfsObject.getThursday(), "thursday must be true");
      Assert.assertTrue(gtfsObject.getFriday(), "friday must be true");
      Assert.assertFalse(gtfsObject.getSaturday(), "saturday must be false");
      Assert.assertTrue(gtfsObject.getSunday(), "sunday must be true");
      Assert.assertEquals(mock.getExportedCalendarDates().size(), 5, "calendar must have 5 dates");
      c.set(Calendar.YEAR, 2013);
      c.set(Calendar.MONTH, Calendar.JULY);
      c.set(Calendar.DAY_OF_MONTH, 1);
      c.add(Calendar.DATE, 30);
      for (GtfsCalendarDate gtfsCalendarDate : mock.getExportedCalendarDates())
      {
         Reporter.log(CalendarDateExporter.CONVERTER.to(context,gtfsCalendarDate));
         LocalDate date = LocalDate.fromCalendarFields(c);
         c.add(Calendar.DATE, 3);
         Assert.assertEquals(gtfsCalendarDate.getServiceId(), toGtfsId(neptuneObject.getObjectId()), "service id must be correcty set");
         Assert.assertEquals(gtfsCalendarDate.getDate(), date, "calendar date must be correctly");
         Assert.assertEquals(gtfsCalendarDate.getExceptionType(), ExceptionType.Added, "calendar date must be inclusive");
      }

   }

   @Test(groups = { "Producers" }, description = "test timetable with 2 periods")
   public void verifyCalendarProducer4()
   {
      mock.reset();
      Calendar c = Calendar.getInstance();
      c.set(Calendar.YEAR, 2013);
      c.set(Calendar.MONTH, Calendar.JULY);
      c.set(Calendar.DAY_OF_MONTH, 1);
      c.set(Calendar.HOUR_OF_DAY, 12);

      Timetable neptuneObject = new Timetable();
      neptuneObject.setObjectId("GTFS:Timetable:1234");
      neptuneObject.setComment("name");
      neptuneObject.addDayType(DayTypeEnum.Monday);
      neptuneObject.addDayType(DayTypeEnum.Tuesday);
      neptuneObject.addDayType(DayTypeEnum.Thursday);
      neptuneObject.addDayType(DayTypeEnum.Friday);
      neptuneObject.addDayType(DayTypeEnum.Saturday);
      neptuneObject.addDayType(DayTypeEnum.Sunday);
      LocalDate startDate1 = LocalDate.fromCalendarFields(c);
      c.add(Calendar.DATE, 15);
      LocalDate endDate1 = LocalDate.fromCalendarFields(c);
      Period period1 = new Period(startDate1, endDate1);
      neptuneObject.addPeriod(period1);
      c.add(Calendar.DATE, 60);
      LocalDate startDate2 = LocalDate.fromCalendarFields(c);
      c.add(Calendar.DATE, 15);
      LocalDate endDate2 = LocalDate.fromCalendarFields(c);
      Period period2 = new Period(startDate2, endDate2);
      neptuneObject.addPeriod(period2);

      List<Timetable> tms = new ArrayList<>();
      tms.add(neptuneObject);
      producer.save(tms,  "GTFS",false, null, null);
      Reporter.log("verifyCalendarProducer4");

      Assert.assertEquals(mock.getExportedCalendars().size(), 0, "no calendar produced");

      Assert.assertEquals(mock.getExportedCalendarDates().size(), 28, "calendar must have 28 dates");
      c.set(Calendar.YEAR, 2013);
      c.set(Calendar.MONTH, Calendar.JULY);
      c.set(Calendar.DAY_OF_MONTH, 1);
      if (c.get(Calendar.DAY_OF_WEEK) == Calendar.WEDNESDAY)
         c.add(Calendar.DATE, 1);
      int cpt = 0;
      for (GtfsCalendarDate gtfsCalendarDate : mock.getExportedCalendarDates())
      {
         Reporter.log(CalendarDateExporter.CONVERTER.to(context,gtfsCalendarDate));
         LocalDate date = LocalDate.fromCalendarFields(c);
         cpt++;
         if (cpt == 14)
         {
            c.add(Calendar.DATE, 59);
         }
         c.add(Calendar.DATE, 1);
         if (c.get(Calendar.DAY_OF_WEEK) == Calendar.WEDNESDAY)
            c.add(Calendar.DATE, 1);
         Assert.assertEquals(gtfsCalendarDate.getServiceId(), toGtfsId(neptuneObject.getObjectId()), "service id must be correcty set");
         Assert.assertEquals(gtfsCalendarDate.getDate(), date, "calendar date must be correctly");
         Assert.assertEquals(gtfsCalendarDate.getExceptionType(), ExceptionType.Added, "calendar date must be inclusive");
      }

   }

   @Test(groups = { "Producers" }, description = "when export start date is after timetable start date should use export start date")
  public void verifyExportStartDate()
   {
      mock.reset();

      Timetable tt = new Timetable();
      tt.setObjectId("GTFS:Timetable:1234");
      tt.setComment("name");
      tt.addDayType(DayTypeEnum.WeekDay);
      LocalDate ttStartDate = new LocalDate(2024, 2, 1);
      LocalDate ttEndDate = new LocalDate(2024, 5, 30);
      tt.setStartOfPeriod(ttStartDate);
      tt.setEndOfPeriod(ttEndDate);
      tt.getPeriods().add(new Period(ttStartDate, ttEndDate));

      LocalDate exportStartDate = new LocalDate(2024, 4, 10);

      Assert.assertTrue(exportStartDate.isAfter(tt.getPeriods().get(0).getStartDate()),
              "export start date should be after timetable start date");

      producer.save(Arrays.asList(tt),  "GTFS",false, exportStartDate, null);

      Reporter.log("verifyExportStartDate");

      Assert.assertEquals(mock.getExportedCalendars().size(), 1, "one calendar should be produced");
      Assert.assertEquals(mock.getExportedCalendars().get(0).getStartDate(), exportStartDate, "calendar should use export start date");
      Assert.assertEquals(mock.getExportedCalendars().get(0).getEndDate(), ttEndDate, "calendar should use timetable end date");
   }

   @Test(groups = { "Producers" }, description = "when export end date is before timetable end date should use export end date")
   public void verifyExportEndDate()
   {
      mock.reset();

      Timetable tt = new Timetable();
      tt.setObjectId("GTFS:Timetable:1234");
      tt.setComment("name");
      tt.addDayType(DayTypeEnum.WeekDay);
      LocalDate ttStartDate = new LocalDate(2024, 2, 1);
      LocalDate ttEndDate = new LocalDate(2024, 5, 30);
      tt.setStartOfPeriod(ttStartDate);
      tt.setEndOfPeriod(ttEndDate);
      tt.getPeriods().add(new Period(ttStartDate, ttEndDate));

      LocalDate exportEndDate = new LocalDate(2024, 4, 12);

      Assert.assertTrue(exportEndDate.isBefore(tt.getPeriods().get(0).getEndDate()),
              "export end date should be before timetable end date");

      producer.save(Arrays.asList(tt),  "GTFS",false, null, exportEndDate);

      Reporter.log("verifyExportEndDate");

      Assert.assertEquals(mock.getExportedCalendars().size(), 1, "one calendar produced");
      Assert.assertEquals(mock.getExportedCalendars().get(0).getStartDate(), ttStartDate, "should use timetable start date");
      Assert.assertEquals(mock.getExportedCalendars().get(0).getEndDate(), exportEndDate, "should use export end date");
   }

   @Test(groups = { "Producers" }, description = "when export start/end date is after/before timetable start/end date should use export start/end date")
   public void verifyExportWithStartDateAndEndDate()
   {
      mock.reset();

      Timetable tt = new Timetable();
      tt.setObjectId("GTFS:Timetable:1234");
      tt.setComment("name");
      tt.addDayType(DayTypeEnum.WeekDay);
      LocalDate ttStartDate = new LocalDate(2024, 2, 1);
      LocalDate ttEndDate = new LocalDate(2024, 5, 30);
      tt.setStartOfPeriod(ttStartDate);
      tt.setEndOfPeriod(ttEndDate);
      tt.getPeriods().add(new Period(ttStartDate, ttEndDate));

      LocalDate exportStartDate = new LocalDate(2024, 4, 10);
      LocalDate exportEndDate = new LocalDate(2024, 4, 12);

      Assert.assertTrue(exportStartDate.isAfter(tt.getPeriods().get(0).getStartDate()),
              "export start date should be after timetable start date");
      Assert.assertTrue(exportEndDate.isBefore(tt.getPeriods().get(0).getEndDate()),
              "export end date should be before timetable end date");

      producer.save(Arrays.asList(tt),  "GTFS",false, exportStartDate, exportEndDate);

      Reporter.log("verifyExportWithStartDateAndEndDate");

      Assert.assertEquals(mock.getExportedCalendars().size(), 1, "one calendar produced");
      Assert.assertEquals(mock.getExportedCalendars().get(0).getStartDate(), exportStartDate, "should use export start date");
      Assert.assertEquals(mock.getExportedCalendars().get(0).getEndDate(), exportEndDate, "should use export end date");
   }

   @Test(groups = { "Producers" }, description = "export with start/end dates and timetable with 2 periods")
   public void verifyExportWithStartDateEndDateAndTimetableWith2Periods()
   {
      mock.reset();

      Timetable tt = new Timetable();
      tt.setObjectId("GTFS:Timetable:1234");
      tt.setComment("name");
      for (DayTypeEnum dayType : Arrays.asList(
              DayTypeEnum.Monday,
              DayTypeEnum.Tuesday,
              DayTypeEnum.Wednesday,
              DayTypeEnum.Thursday,
              DayTypeEnum.Friday,
              DayTypeEnum.Saturday,
              DayTypeEnum.Sunday
      )) {
         tt.addDayType(dayType);
      }
      LocalDate ttStartDateFirstPeriod = new LocalDate(2024, 2, 1);
      LocalDate ttEndDateFirstPeriod = new LocalDate(2024, 5, 31);
      LocalDate ttStartDateSecondPeriod = new LocalDate(2024, 7, 1);
      LocalDate ttEndDateSecondPeriod = new LocalDate(2024, 9, 30);
      tt.setStartOfPeriod(ttStartDateFirstPeriod);
      tt.setEndOfPeriod(ttEndDateSecondPeriod);
      // timetable first period is 01/02/24 -> 31/05/24
      // timetable second period is 01/07/24 -> 30/09/24
      tt.getPeriods().add(new Period(ttStartDateFirstPeriod, ttEndDateFirstPeriod));
      tt.getPeriods().add(new Period(ttStartDateSecondPeriod, ttEndDateSecondPeriod));

      // export period is 28/05/24 -> 06/07/24
      LocalDate exportStartDate = new LocalDate(2024, 5, 28);
      LocalDate exportEndDate = new LocalDate(2024, 7, 6);

      producer.save(Arrays.asList(tt),"GTFS",false, exportStartDate, exportEndDate);

      Reporter.log("verifyExportWithStartDateEndDateAndTimetableWith2Periods");

      // the exported dates must belong to the export period and the timetable period(s)
      // => 28/05/24 -> 31/05/24 (included)
      // => 01/07/24 -> 06/07/24 (included)
      List<LocalDate> expectedDates = Arrays.asList(
              new LocalDate(2024, 5, 28),
              new LocalDate(2024, 5, 29),
              new LocalDate(2024, 5, 30),
              new LocalDate(2024, 5, 31),
              new LocalDate(2024, 7, 1),
              new LocalDate(2024, 7, 2),
              new LocalDate(2024, 7, 3),
              new LocalDate(2024, 7, 4),
              new LocalDate(2024, 7, 5),
              new LocalDate(2024, 7, 6)
      );
      List<LocalDate> output = mock.getExportedCalendarDates().stream().map(GtfsCalendarDate::getDate).collect(Collectors.toList());

      Assert.assertEquals(mock.getExportedCalendars().size(), 0, "no calendar should be produced");
      Assert.assertEquals(output, expectedDates, "export should contain only these dates");
   }

   protected String toGtfsId(String neptuneId)
   {
      String[] tokens = neptuneId.split(":");
      return tokens[2];
   }

}
