package mobi.chouette.exchange.netexprofile.exporter.producer;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import mobi.chouette.exchange.netexprofile.exporter.NetexprofileExportParameters;
import mobi.chouette.model.KeyValue;
import org.rutebanken.netex.model.DayOfWeekEnumeration;
import org.rutebanken.netex.model.DayType;
import org.rutebanken.netex.model.DayTypeAssignment;
import org.rutebanken.netex.model.DayTypeRefStructure;
import org.rutebanken.netex.model.OperatingPeriod;
import org.rutebanken.netex.model.OperatingPeriodRefStructure;
import org.rutebanken.netex.model.PropertiesOfDay_RelStructure;
import org.rutebanken.netex.model.PropertyOfDay;

import mobi.chouette.common.Context;
import mobi.chouette.common.TimeUtil;
import mobi.chouette.exchange.netexprofile.exporter.ExportableData;
import mobi.chouette.exchange.netexprofile.exporter.ExportableNetexData;
import mobi.chouette.model.CalendarDay;
import mobi.chouette.model.Period;
import mobi.chouette.model.Timetable;
import static mobi.chouette.common.Constant.CONFIGURATION;
import static mobi.chouette.common.Constant.EXTERNAL_REF;

public class CalendarProducer extends NetexProducer {

	private static KeyListStructureProducer keyListStructureProducer = new KeyListStructureProducer();

	public void produce(Context context, ExportableData exportableData, ExportableNetexData exportableNetexData) {
		NetexprofileExportParameters configuration = (NetexprofileExportParameters) context.get(CONFIGURATION);

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
							.withFromDate(TimeUtil.toLocalDateFromJoda(p.getStartDate()).atStartOfDay()).withToDate(TimeUtil.toLocalDateFromJoda(p.getEndDate()).atStartOfDay());
					if (!exportableNetexData.getSharedOperatingPeriods().containsKey(operatingPeriodId)) {
						exportableNetexData.getSharedOperatingPeriods().put(operatingPeriodId, operatingPeriod);
					}

					OperatingPeriodRefStructure operatingPeriodRef = netexFactory.createOperatingPeriodRefStructure();
					NetexProducerUtils.populateReference(operatingPeriod, operatingPeriodRef, true);

					// Assign operatingperiod to daytype
					DayTypeAssignment dayTypeAssignment = netexFactory.createDayTypeAssignment()
							.withId(NetexProducerUtils.translateObjectId(netexDaytypeId, "DayTypeAssignment") + "-" + counter).withVersion(NETEX_DEFAULT_OBJECT_VERSION)
							.withOrder(BigInteger.ONE).withDayTypeRef(netexFactory.createDayTypeRef(dayTypeRef)).withOperatingPeriodRef(netexFactory.createOperatingPeriodRef(operatingPeriodRef));

					List<KeyValue> keyValues = new ArrayList<>();
					KeyValue  keyValue = new KeyValue();
					keyValue.setKey(EXTERNAL_REF);
					keyValue.setValue(timetable.getObjectId().split(":")[2]);
					keyValue.setTypeOfKey("ALTERNATIVE_IDENTIFIER");
					keyValues.add(keyValue);
					dayTypeAssignment.setKeyList(keyListStructureProducer.produce(keyValues, configuration.isExportExternalIds()));
					exportableNetexData.getSharedDayTypeAssignments().add(dayTypeAssignment);

				}

				for (CalendarDay day : timetable.getCalendarDays()) {
					counter++;

					DayTypeAssignment dayTypeAssignment = netexFactory.createDayTypeAssignment()
							.withId(NetexProducerUtils.translateObjectId(netexDaytypeId, "DayTypeAssignment") + "-" + counter).withVersion(NETEX_DEFAULT_OBJECT_VERSION)
							.withOrder(BigInteger.ONE).withDayTypeRef(netexFactory.createDayTypeRef(dayTypeRef))
							.withDate(TimeUtil.toLocalDateFromJoda(day.getDate()).atStartOfDay());

					if (day.getIncluded() != null && !day.getIncluded()) {
						dayTypeAssignment.setIsAvailable(day.getIncluded());
					}

					List<KeyValue> keyValues = new ArrayList<>();
					KeyValue  keyValue = new KeyValue();
					keyValue.setKey(EXTERNAL_REF);
					keyValue.setValue(timetable.getObjectId().contains(":") && timetable.getObjectId().split(":")[2] !=null ? timetable.getObjectId().split(":")[2] : timetable.getObjectId());
					keyValue.setTypeOfKey("ALTERNATIVE_IDENTIFIER");
					keyValues.add(keyValue);
					dayTypeAssignment.setKeyList(keyListStructureProducer.produce(keyValues, configuration.isExportExternalIds()));
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
