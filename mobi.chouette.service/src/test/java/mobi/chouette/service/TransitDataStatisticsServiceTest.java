package mobi.chouette.service;

import mobi.chouette.common.TimeUtil;
import mobi.chouette.model.statistics.LineStatistics;
import mobi.chouette.model.statistics.Period;
import mobi.chouette.model.statistics.PublicLine;
import mobi.chouette.model.statistics.ValidityCategory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransitDataStatisticsServiceTest {

	@Test
	public void categorizeValidityWithInvalidLines() {
		LocalDate startDate = LocalDate.now();
		LineStatistics lineStats = new LineStatistics();
		lineStats.getPublicLines().add(createPublicLine("invalidNoDate", null,null));
		lineStats.getPublicLines().add(createPublicLine("invalidHistoricToDate", startDate.plusDays(-5), startDate.plusDays(-2)));
		lineStats.getPublicLines().add(createPublicLine("invalidFutureFromDate", startDate.plusDays(5), startDate.plusDays(20)));
		lineStats.getPublicLines().add(createPublicLine("validOutsideCategory", startDate.plusDays(-5),startDate.plusDays(2)));
		lineStats.getPublicLines().add(createPublicLine("validCat1", startDate.plusDays(-5), startDate.plusDays(7)));
		lineStats.getPublicLines().add(createPublicLine("validCat2", startDate.plusDays(-5), startDate.plusDays(15)));


		Map<Integer, String> minDaysValidityCategories=new HashMap<>();
		minDaysValidityCategories.put(5, "OK");
		minDaysValidityCategories.put(10,"GOOD");
		new TransitDataStatisticsService().categorizeValidity(lineStats, TimeUtil.toDate(startDate), minDaysValidityCategories);
		Assert.assertEquals(lineStats.getValidityCategories().size(), 4);
		Assert.assertEquals("INVALID",getCategory(lineStats, -1).getName());
		Assert.assertEquals("EXPIRING",getCategory(lineStats, 0).getName());
		Assert.assertEquals("OK",getCategory(lineStats, 5).getName());
		Assert.assertEquals("GOOD",getCategory(lineStats, 10).getName());
		Assert.assertTrue(getCategory(lineStats, -1).getLineNumbers().contains("invalidNoDate"));
		Assert.assertTrue(getCategory(lineStats, -1).getLineNumbers().contains("invalidHistoricToDate"));
		Assert.assertTrue(getCategory(lineStats, -1).getLineNumbers().contains("invalidFutureFromDate"));
		Assert.assertTrue(getCategory(lineStats, 0).getLineNumbers().contains("validOutsideCategory"));
		Assert.assertTrue(getCategory(lineStats, 5).getLineNumbers().contains("validCat1"));
		Assert.assertTrue(getCategory(lineStats, 10).getLineNumbers().contains("validCat2"));
	}

	@Test
	public void categorizeValidityWithHistoricPeriods() {
		LocalDate startDate = LocalDate.now();
		LineStatistics lineStats = new LineStatistics();
		PublicLine validNowAndInTheFuture= createPublicLine("validNowAndInTheFuture", startDate.minusDays(5),startDate.minusDays(3));
		validNowAndInTheFuture.getEffectivePeriods().add(new Period(startDate.minusDays(1),startDate.plusDays(15)));
		lineStats.getPublicLines().add(validNowAndInTheFuture);

		PublicLine validNowAndForSomeDays= createPublicLine("validNowAndForSomeDays", startDate.minusDays(5),startDate.minusDays(3));
		validNowAndForSomeDays.getEffectivePeriods().add(new Period(startDate.minusDays(1),startDate.plusDays(3)));
		lineStats.getPublicLines().add(validNowAndForSomeDays);

		PublicLine invalidNowWithHistoricPeriod= createPublicLine("invalidNowWithHistoricPeriod", startDate.minusDays(5),startDate.minusDays(3));
		invalidNowWithHistoricPeriod.getEffectivePeriods().add(new Period(startDate.plusDays(5),startDate.plusDays(15)));
		lineStats.getPublicLines().add(invalidNowWithHistoricPeriod);

		Map<Integer, String> minDaysValidityCategories=new HashMap<>();
		minDaysValidityCategories.put(5, "OK");
		minDaysValidityCategories.put(10,"GOOD");
		new TransitDataStatisticsService().categorizeValidity(lineStats, TimeUtil.toDate(startDate), minDaysValidityCategories);
		Assert.assertEquals(lineStats.getValidityCategories().size(), 4);
		Assert.assertEquals("INVALID",getCategory(lineStats, -1).getName());
		Assert.assertEquals("EXPIRING",getCategory(lineStats, 0).getName());
		Assert.assertEquals("OK",getCategory(lineStats, 5).getName());
		Assert.assertEquals("GOOD",getCategory(lineStats, 10).getName());
		//Assert.assertTrue(getCategory(lineStats, -1).getLineNumbers().contains("invalidNowWithHistoricPeriod"));
		Assert.assertTrue(getCategory(lineStats, 0).getLineNumbers().contains("validNowAndForSomeDays"));
		Assert.assertTrue(getCategory(lineStats, 10).getLineNumbers().contains("validNowAndInTheFuture"));
		Assert.assertTrue(getCategory(lineStats, 10).getLineNumbers().contains("invalidNowWithHistoricPeriod"));
	}


	@Test
	public void testMergeOverlappingPeriods() {
		List<Period> periods = new ArrayList<Period>();

		LocalDate today = LocalDate.now();
		periods.add(createPeriod(today, 2));
		periods.add(createPeriod(today.plusDays(1), 2));

		List<Period> splitPeriods = new TransitDataStatisticsService().mergeOverlappingPeriods(periods);

		Assert.assertEquals(splitPeriods.size(), 1);
	}



	@Test
	public void testMergeMultipleAdjacentPeriods() {
		List<Period> periods = new ArrayList<Period>();

		LocalDate today = LocalDate.now();
		periods.add(createPeriod(today, 0));
		periods.add(createPeriod(today.plusDays(1), 0));
		periods.add(createPeriod(today.plusDays(2), 0));
		List<Period> splitPeriods = new TransitDataStatisticsService().mergeOverlappingPeriods(periods);

		Assert.assertEquals(splitPeriods.size(), 1);
	}



	private Period createPeriod(LocalDate startDate, int days) {
		LocalDate endDate = startDate.plusDays(days);
		Period p1 = new Period(startDate, endDate);

		return p1;

	}

	private ValidityCategory getCategory(LineStatistics lineStatistics, int numDaysAtLeastValid) {
		ValidityCategory matchingCategory = null;
		for (ValidityCategory validityCategory : lineStatistics.getValidityCategories()) {
			if (validityCategory.getNumDaysAtLeastValid() == numDaysAtLeastValid) {
				matchingCategory = validityCategory;
				break;
			}
		}
		return matchingCategory;
	}

	private PublicLine createPublicLine(String no, LocalDate startDate, LocalDate endDate) {
		PublicLine publicLine = new PublicLine(no);
		if (endDate != null) {
			publicLine.getEffectivePeriods().add(new Period(startDate, endDate));
		}

		return publicLine;
	}
}
