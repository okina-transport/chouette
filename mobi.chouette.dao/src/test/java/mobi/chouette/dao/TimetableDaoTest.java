package mobi.chouette.dao;

import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import javax.ejb.EJB;

import javax.transaction.Transactional;

import mobi.chouette.model.*;
import mobi.chouette.model.type.DayTypeEnum;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import java.time.LocalDate;
import org.testng.Assert;
import org.testng.annotations.Test;

import mobi.chouette.model.statistics.LineAndTimetable;
import mobi.chouette.persistence.hibernate.ContextHolder;

public class TimetableDaoTest extends Arquillian {
	@EJB
	TimetableDAO timetableDao;

	@EJB
	LineDAO lineDao;

	@EJB
	RouteDAO routeDao;
	
	@EJB
	VehicleJourneyDAO vjDao;

	@Deployment
	public static WebArchive createDeployment() {

		try {
			WebArchive result;
			File[] files = Maven.resolver().loadPomFromFile("pom.xml").resolve("mobi.chouette:mobi.chouette.dao")
					.withTransitivity().asFile();

			result = ShrinkWrap.create(WebArchive.class, "test.war").addAsWebInfResource("postgres-ds.xml")
					.addAsLibraries(files).addAsResource(EmptyAsset.INSTANCE, "beans.xml");
			return result;
		} catch (RuntimeException e) {
			System.out.println(e.getClass().getName());
			throw e;
		}
	}


	@Test
	@Transactional
	public void testExclusionDates() {
		ContextHolder.setContext("chouette_gui"); // set tenant schema
		timetableDao.truncate();



		// Creating a first TT with a very large period, available on all days with 1 exclusion day (20/08/2025)
		LocalDate startLargePeriod = LocalDate.of(2020, 1, 1);
		LocalDate endLargePeriod = LocalDate.of(2099,1,1);
		LocalDate specificDate = LocalDate.of(2025, 8, 20);

		Timetable tt1_large_period = new Timetable();
		tt1_large_period.setObjectId("TT1");
		tt1_large_period.setStartOfPeriod(startLargePeriod);
		tt1_large_period.setStartOfPeriod(endLargePeriod);
		List<DayTypeEnum> days = new ArrayList<>();
		days.add(DayTypeEnum.Monday);
		days.add(DayTypeEnum.Tuesday);
		days.add(DayTypeEnum.Wednesday);
		days.add(DayTypeEnum.Thursday);
		days.add(DayTypeEnum.Friday);
		days.add(DayTypeEnum.Saturday);
		days.add(DayTypeEnum.Sunday);

		tt1_large_period.setDayTypes(days);

		Period largePeriod = new Period();
		largePeriod.setStartDate(startLargePeriod);
		largePeriod.setEndDate(endLargePeriod);

		tt1_large_period.addPeriod(largePeriod);
		CalendarDay exclusionForTT1 = new CalendarDay();
		exclusionForTT1.setDate(specificDate);
		exclusionForTT1.setIncluded(false);
		tt1_large_period.addCalendarDay(exclusionForTT1);

		CalendarDay exclusion2ForTT1 = new CalendarDay();
		exclusion2ForTT1.setDate(LocalDate.of(2022,5,5));
		exclusion2ForTT1.setIncluded(false);
		tt1_large_period.addCalendarDay(exclusion2ForTT1);
		timetableDao.create(tt1_large_period);


		// Creating a second TT, only available on exclusion day (20/08/2025)
		Timetable tt2_inclusion_day = new Timetable();
		tt2_inclusion_day.setObjectId("TT2");
		CalendarDay inclusionForTT2 = new CalendarDay();
		inclusionForTT2.setDate(specificDate);
		inclusionForTT2.setIncluded(true);
		tt2_inclusion_day.addCalendarDay(inclusionForTT2);
		timetableDao.create(tt2_inclusion_day);


		// recovering available TT for 20/08/2025
		Collection<? extends Number> recoveredTT = timetableDao.getActiveTimetableIdsByDay(specificDate);

		// only TT2 should be recovered because TT1 is excluded on 20/08/2025
		Assert.assertEquals(recoveredTT.size(), 1);
		Assert.assertEquals(BigInteger.valueOf(tt2_inclusion_day.getId()), recoveredTT.iterator().next());

	}



	@Test
	@Transactional
	public void getAllTimetableForAllLines() {
		ContextHolder.setContext("chouette_gui"); // set tenant schema

		// Cleanup
		lineDao.truncate();
		vjDao.truncate();
		timetableDao.truncate();
		routeDao.truncate();
		
		String uuid = UUID.randomUUID().toString();
		
		Line l = new Line();
		l.setObjectId("TST:Line:"+uuid);

		Route r = new Route();
		r.setObjectId("TST:Route:"+uuid);

		VehicleJourney vj = new VehicleJourney();
		vj.setObjectId("TST:VehicleJourney:"+uuid);

		Timetable t = new Timetable();
		t.setObjectId("TST:Timetable:"+uuid);
		

		// Wire together

		r.setLine(l);
		vj.setRoute(r);
		t.addVehicleJourney(vj);
		
		lineDao.create(l);
		vjDao.create(vj);
		
		
		Collection<LineAndTimetable> allTimetableForAllLines = timetableDao.getAllTimetableForAllLines();

		Assert.assertNotNull(allTimetableForAllLines);
		Assert.assertEquals(1, allTimetableForAllLines.size());
		LineAndTimetable lat = allTimetableForAllLines.iterator().next();

		Assert.assertNotNull(lat.getLineId());
		Assert.assertEquals(1, lat.getTimetables().size());

	}

}
