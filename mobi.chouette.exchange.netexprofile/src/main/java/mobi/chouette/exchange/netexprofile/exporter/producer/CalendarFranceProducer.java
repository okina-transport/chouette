package mobi.chouette.exchange.netexprofile.exporter.producer;

import mobi.chouette.exchange.netexprofile.exporter.ExportableData;
import mobi.chouette.exchange.netexprofile.exporter.ExportableNetexData;
import mobi.chouette.model.CalendarDay;
import mobi.chouette.model.Period;
import mobi.chouette.model.Timetable;
import org.rutebanken.netex.model.*;

import java.math.BigInteger;
import java.util.List;

public class CalendarFranceProducer extends NetexProducer {

    public void produce(ExportableData exportableData, ExportableNetexData exportableNetexData) {
        int uniqueID = 0;
        for (Timetable timetable : exportableData.getTimetables()) {

            String netexDaytypeId = NetexProducerUtils.generateNetexId(timetable);
            netexDaytypeId += ":LOC";
            if (!exportableNetexData.getSharedDayTypes().containsKey(netexDaytypeId)) {
                DayType dayType = netexFactory.createDayType();
                NetexProducerUtils.populateIdAndVersionIDFM(timetable, dayType);

                List<DayOfWeekEnumeration> dayOfWeekEnumerations = NetexProducerUtils.toDayOfWeekEnumerationIDFM(timetable.getDayTypes());
                if (!dayOfWeekEnumerations.isEmpty()) {
                    if (timetable.getPeriods().stream().allMatch(period -> period.getStartDate().isBefore(period.getEndDate()))) {
                        dayType.setProperties(createPropertiesOfDay_RelStructure(dayOfWeekEnumerations));
                    }
                }

                exportableNetexData.getSharedDayTypes().put(netexDaytypeId, dayType);

                DayTypeRefStructure dayTypeRef = netexFactory.createDayTypeRefStructure();
                NetexProducerUtils.populateReferenceIDFM(timetable, dayTypeRef);

                // Operating periods
                for (int i = 0; i < timetable.getPeriods().size(); i++) {
                    // Assign operatingperiod or date to daytype
                    String dayTypeAssignmentId = netexDaytypeId.replace("DayType", "DayTypeAssignment");
                    dayTypeAssignmentId = dayTypeAssignmentId.substring(0, dayTypeAssignmentId.indexOf(":LOC")) + uniqueID + ":LOC";
                    uniqueID++;
                    DayTypeAssignment dayTypeAssignment;

                    Period p = timetable.getPeriods().get(i);
                    if (p.getStartDate().isBefore(p.getEndDate())) {
                        OperatingPeriodRefStructure operatingPeriodRef = netexFactory.createOperatingPeriodRefStructure();
                        // Create Operating period
                        String operatingPeriodId = netexDaytypeId.replace("DayType", "OperatingPeriod");
                        OperatingPeriod operatingPeriod = new OperatingPeriod().withVersion(dayType.getVersion())
                                .withId(operatingPeriodId)
                                .withFromDate(p.getStartDate().atStartOfDay()).withToDate(p.getEndDate().atStartOfDay());
                        if (!exportableNetexData.getSharedOperatingPeriods().containsKey(operatingPeriodId)) {
                            exportableNetexData.getSharedOperatingPeriods().put(operatingPeriodId, operatingPeriod);
                        }

                        NetexProducerUtils.populateReference(operatingPeriod, operatingPeriodRef, true);
                        operatingPeriodRef.setVersion("any");
                        dayTypeAssignment = netexFactory.createDayTypeAssignment()
                                .withId(dayTypeAssignmentId)
                                .withVersion(NETEX_DEFAULT_OBJECT_VERSION)
                                .withOrder(BigInteger.valueOf(0))
                                .withDayTypeRef(netexFactory.createDayTypeRef(dayTypeRef))
                                .withOperatingPeriodRef(netexFactory.createOperatingPeriodRef(operatingPeriodRef));
                    }
                    else{
                        dayTypeAssignment = netexFactory.createDayTypeAssignment()
                                .withId(dayTypeAssignmentId)
                                .withVersion(NETEX_DEFAULT_OBJECT_VERSION)
                                .withOrder(BigInteger.valueOf(0))
                                .withDayTypeRef(netexFactory.createDayTypeRef(dayTypeRef))
                                .withDate(p.getStartDate().atStartOfDay());
                    }
                    exportableNetexData.getSharedDayTypeAssignments().add(dayTypeAssignment);
                }

                for (CalendarDay day : timetable.getCalendarDays()) {
                    String dayTypeAssignmentId = netexDaytypeId.replace("DayType", "DayTypeAssignment");
                    dayTypeAssignmentId = dayTypeAssignmentId.substring(0, dayTypeAssignmentId.indexOf(":LOC")) + uniqueID + ":LOC";
                    uniqueID++;
                    DayTypeAssignment dayTypeAssignment = netexFactory.createDayTypeAssignment()
                            .withId(dayTypeAssignmentId)
                            .withVersion(NETEX_DEFAULT_OBJECT_VERSION)
                            .withOrder(BigInteger.valueOf(0))
                            .withDayTypeRef(netexFactory.createDayTypeRef(dayTypeRef))
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
        PropertiesOfDay_RelStructure propertiesOfDay = netexFactory.createPropertiesOfDay_RelStructure();
        for (DayOfWeekEnumeration dayOfWeekEnumeration : dayOfWeekEnumerations) {
            PropertyOfDay propertyOfDay = netexFactory.createPropertyOfDay();
            propertyOfDay.getDaysOfWeek().add(dayOfWeekEnumeration);
            propertiesOfDay.getPropertyOfDay().add(propertyOfDay);
        }
        return propertiesOfDay;
    }
}
