package mobi.chouette.exchange.netexprofile.exporter.producer;

import mobi.chouette.common.Context;
import mobi.chouette.exchange.netexprofile.exporter.ExportableData;
import mobi.chouette.exchange.netexprofile.exporter.ExportableNetexData;
import mobi.chouette.exchange.netexprofile.exporter.NetexprofileExportParameters;
import mobi.chouette.model.CalendarDay;
import mobi.chouette.model.KeyValue;
import mobi.chouette.model.Period;
import mobi.chouette.model.Timetable;
import org.rutebanken.netex.model.*;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static mobi.chouette.common.Constant.CONFIGURATION;
import static mobi.chouette.common.Constant.EXTERNAL_REF;

public class CalendarFranceProducer extends NetexProducer {

	private static final KeyListStructureProducer keyListStructureProducer = new KeyListStructureProducer();

    public void produce(Context context, ExportableData exportableData, ExportableNetexData exportableNetexData) {
        int uniqueID = 0;
		NetexprofileExportParameters configuration = (NetexprofileExportParameters) context.get(CONFIGURATION);

        for (Timetable timetable : exportableData.getTimetables()) {

            String netexDaytypeId = NetexProducerUtils.generateNetexId(timetable);
            netexDaytypeId += ":LOC";
            if (!exportableNetexData.getSharedDayTypes().containsKey(netexDaytypeId)) {
                DayType dayType = netexFactory.createDayType();
                NetexProducerUtils.populateIdAndVersion(timetable, dayType);

                List<DayOfWeekEnumeration> dayOfWeekEnumerations = NetexProducerUtils.toDayOfWeekEnumerationIDFM(timetable.getDayTypes());
                if (!dayOfWeekEnumerations.isEmpty()) {
                    if (timetable.getPeriods().stream().allMatch(period -> period.getStartDate().isBefore(period.getEndDate()))) {
                        dayType.setProperties(createPropertiesOfDay_RelStructure(dayOfWeekEnumerations));
                        NetexProducerUtils.addAlternateIdentifier(dayType, timetable.getObjectId());
                    }
                }

                exportableNetexData.getSharedDayTypes().put(netexDaytypeId, dayType);

                DayTypeRefStructure dayTypeRef = netexFactory.createDayTypeRefStructure();
                NetexProducerUtils.populateReferenceIDFM(timetable, dayTypeRef);

                // Operating periods
                for (int i = 0; i < timetable.getPeriods().size(); i++) {
                    // Assign operatingperiod or date to daytype
                    String dayTypeAssignmentId = netexDaytypeId.replace("DayType", "DayTypeAssignment");
                    dayTypeAssignmentId = dayTypeAssignmentId.substring(0, dayTypeAssignmentId.indexOf(":LOC")) + "-" + uniqueID + ":LOC";
                    uniqueID++;
                    DayTypeAssignment dayTypeAssignment;

                    Period p = timetable.getPeriods().get(i);
                    if (p.getStartDate().isBefore(p.getEndDate())) {
                        OperatingPeriodRefStructure operatingPeriodRef = netexFactory.createOperatingPeriodRefStructure();
                        // Create Operating period
                        String operatingPeriodId = netexDaytypeId.replace("DayType", "OperatingPeriod").replace(":LOC", "") + "-" + i + ":LOC";
                        LocalDateTime toDate =p.getEndDate().atTime(23, 59, 59);
                        OperatingPeriod operatingPeriod = new OperatingPeriod().withVersion(dayType.getVersion())
                                .withId(operatingPeriodId)
                                .withFromDate(p.getStartDate().atStartOfDay())
                                .withToDate(toDate);

                        NetexProducerUtils.addAlternateIdentifier(operatingPeriod, operatingPeriodId);
                        if (!exportableNetexData.getSharedOperatingPeriods().containsKey(operatingPeriodId)) {
                            exportableNetexData.getSharedOperatingPeriods().put(operatingPeriodId, operatingPeriod);
                        }

                        NetexProducerUtils.populateReference(operatingPeriod, operatingPeriodRef, true);
                        operatingPeriodRef.setVersion("any");
                        dayTypeAssignment = netexFactory.createDayTypeAssignment()
                                .withId(dayTypeAssignmentId)
                                .withVersion(NETEX_DEFAULT_OBJECT_VERSION)
                                .withOrder(BigInteger.valueOf(i+1))
                                .withDayTypeRef(netexFactory.createDayTypeRef(dayTypeRef))
                                .withOperatingPeriodRef(netexFactory.createOperatingPeriodRef(operatingPeriodRef));
                    } else {
                        dayTypeAssignment = netexFactory.createDayTypeAssignment()
                                .withId(dayTypeAssignmentId)
                                .withVersion(NETEX_DEFAULT_OBJECT_VERSION)
                                .withOrder(BigInteger.valueOf(i+1))
                                .withDayTypeRef(netexFactory.createDayTypeRef(dayTypeRef))
                                .withDate(p.getStartDate().atStartOfDay());
                    }
                    NetexProducerUtils.addAlternateIdentifier(dayTypeAssignment, dayTypeAssignmentId);

					List<KeyValue> keyValues = new ArrayList<>();
					KeyValue  keyValue = new KeyValue();
					keyValue.setKey(EXTERNAL_REF);
					keyValue.setValue(timetable.getObjectId().contains(":") && timetable.getObjectId().split(":")[2] !=null ? timetable.getObjectId().split(":")[2] : timetable.getObjectId());
					keyValue.setTypeOfKey("ALTERNATIVE_IDENTIFIER");
					keyValues.add(keyValue);
					dayTypeAssignment.setKeyList(keyListStructureProducer.produce(keyValues, configuration.isExportExternalIds()));

                    exportableNetexData.getSharedDayTypeAssignments().add(dayTypeAssignment);
                }

                int calendarIndex = 0;
                for (CalendarDay day : timetable.getCalendarDays()) {

                    String dayTypeAssignmentId = netexDaytypeId.replace("DayType", "DayTypeAssignment");
                    dayTypeAssignmentId = dayTypeAssignmentId.substring(0, dayTypeAssignmentId.indexOf(":LOC")) + "-" + uniqueID + ":LOC";
                    uniqueID++;
                    DayTypeAssignment dayTypeAssignment = netexFactory.createDayTypeAssignment()
                            .withId(dayTypeAssignmentId)
                            .withVersion(NETEX_DEFAULT_OBJECT_VERSION)
                            .withOrder(BigInteger.valueOf(calendarIndex + 1))
                            .withDayTypeRef(netexFactory.createDayTypeRef(dayTypeRef))
                            .withDate(day.getDate().atStartOfDay());

                    NetexProducerUtils.addAlternateIdentifier(dayTypeAssignment, dayTypeAssignmentId);
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
                    calendarIndex ++;
                }

            }
        }

    }

    private PropertiesOfDay_RelStructure createPropertiesOfDay_RelStructure(List<DayOfWeekEnumeration> dayOfWeekEnumerations) {
        PropertiesOfDay_RelStructure propertiesOfDay = netexFactory.createPropertiesOfDay_RelStructure();
        for (DayOfWeekEnumeration dayOfWeekEnumeration : dayOfWeekEnumerations) {
            PropertyOfDay propertyOfDay = netexFactory.createPropertyOfDay();
            propertyOfDay.getDaysOfWeek().add(dayOfWeekEnumeration);
            propertiesOfDay.getPropertyOfDay().add(propertyOfDay);
        }
        return propertiesOfDay;
    }
}
