package mobi.chouette.exchange.netexprofile.exporter.producer;

import mobi.chouette.common.Context;
import mobi.chouette.exchange.netexprofile.exporter.ExportableData;
import mobi.chouette.exchange.netexprofile.exporter.ExportableNetexData;
import mobi.chouette.model.CalendarDay;
import mobi.chouette.model.Period;
import mobi.chouette.model.Timetable;
import org.rutebanken.netex.model.*;

import java.math.BigInteger;
import java.util.List;

public class CalendarProducer extends NetexProducer {

	public void produce(Context context, ExportableData exportableData, ExportableNetexData exportableNetexData) {

		for (Timetable timetable : exportableData.getTimetables()) {

			String netexDaytypeId = NetexProducerUtils.generateNetexId(timetable);
			if (!exportableNetexData.getSharedDayTypes().containsKey(netexDaytypeId)) {
				DayType dayType = netexFactory.createDayType();
				NetexProducerUtils.populateId(timetable, dayType);

				List<DayOfWeekEnumeration> dayOfWeekEnumerations = NetexProducerUtils.toDayOfWeekEnumeration(timetable.getDayTypes());
				if (!dayOfWeekEnumerations.isEmpty()) {
					dayType.setProperties(createPropertiesOfDay_RelStructure(dayOfWeekEnumerations));
				}

				exportableNetexData.getSharedDayTypes().put(netexDaytypeId, dayType);

				DayTypeRefStructure dayTypeRef = netexFactory.createDayTypeRefStructure();
				NetexProducerUtils.populateReference(timetable, dayTypeRef, true);

				int counter = 0; // Used for creating unique dayTypeAssignments
				// Operating periods
				for (int i = 0; i < timetable.getPeriods().size(); i++) {
					counter++;

					Period p = timetable.getPeriods().get(i);
					// Create Operating period
					String operatingPeriodId=NetexProducerUtils.translateObjectId(netexDaytypeId, "OperatingPeriod")+ "-" + counter;
					OperatingPeriod operatingPeriod = new OperatingPeriod().withVersion(dayType.getVersion())
							.withId(operatingPeriodId)
							.withFromDate(p.getStartDate().atStartOfDay()).withToDate(p.getEndDate().atStartOfDay());
					if (!exportableNetexData.getSharedOperatingPeriods().containsKey(operatingPeriodId)) {
						exportableNetexData.getSharedOperatingPeriods().put(operatingPeriodId, operatingPeriod);
					}

					OperatingPeriodRefStructure operatingPeriodRef = netexFactory.createOperatingPeriodRefStructure();
					NetexProducerUtils.populateReference(operatingPeriod, operatingPeriodRef, true);

					// Assign operatingperiod to daytype
					DayTypeAssignment dayTypeAssignment = netexFactory.createDayTypeAssignment()
							.withId(NetexProducerUtils.translateObjectId(netexDaytypeId, "DayTypeAssignment") + "-" + counter).withVersion(NETEX_DEFAULT_OBJECT_VERSION)
							.withOrder(BigInteger.ONE).withDayTypeRef(netexFactory.createDayTypeRef(dayTypeRef)).withOperatingPeriodRef(netexFactory.createOperatingPeriodRef(operatingPeriodRef).getValue());
					exportableNetexData.getSharedDayTypeAssignments().add(dayTypeAssignment);

				}

				for (CalendarDay day : timetable.getCalendarDays()) {
					counter++;

					DayTypeAssignment dayTypeAssignment = netexFactory.createDayTypeAssignment()
							.withId(NetexProducerUtils.translateObjectId(netexDaytypeId, "DayTypeAssignment") + "-" + counter).withVersion(NETEX_DEFAULT_OBJECT_VERSION)
							.withOrder(BigInteger.ONE).withDayTypeRef(netexFactory.createDayTypeRef(dayTypeRef))
							.withDate(day.getDate().atStartOfDay());

					if (day.getIncluded() != null && !day.getIncluded()) {
						dayTypeAssignment.setIsAvailable(day.getIncluded());
					}
					exportableNetexData.getSharedDayTypeAssignments().add(dayTypeAssignment);
				}

			}
		}

	}

	private PropertiesOfDay_RelStructure createPropertiesOfDay_RelStructure(List<DayOfWeekEnumeration> dayOfWeekEnumerations) {
		PropertyOfDay propertyOfDay = netexFactory.createPropertyOfDay();
		for (DayOfWeekEnumeration dayOfWeekEnumeration : dayOfWeekEnumerations) {
			propertyOfDay.getDaysOfWeek().add(dayOfWeekEnumeration);
		}

		PropertiesOfDay_RelStructure propertiesOfDay = netexFactory.createPropertiesOfDay_RelStructure();
		propertiesOfDay.getPropertyOfDay().add(propertyOfDay);
		return propertiesOfDay;
	}

}
