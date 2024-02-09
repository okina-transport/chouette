package mobi.chouette.exchange.netexprofile.exporter.producer;

import mobi.chouette.common.Context;
import mobi.chouette.common.TimeUtil;
import mobi.chouette.exchange.netexprofile.Constant;
import mobi.chouette.exchange.netexprofile.ConversionUtil;
import mobi.chouette.exchange.netexprofile.exporter.ExportableData;
import mobi.chouette.exchange.netexprofile.exporter.ExportableNetexData;
import mobi.chouette.exchange.netexprofile.importer.util.NetexTimeConversionUtil;
import mobi.chouette.model.JourneyFrequency;
import mobi.chouette.model.JourneyPattern;
import mobi.chouette.model.Timetable;
import mobi.chouette.model.VehicleJourney;
import mobi.chouette.model.VehicleJourneyAtStop;
import mobi.chouette.model.type.LimitationStatusEnum;
import org.apache.commons.collections.CollectionUtils;
import org.joda.time.LocalTime;
import org.rutebanken.netex.model.AccessibilityAssessment;
import org.rutebanken.netex.model.AccessibilityLimitation;
import org.rutebanken.netex.model.AccessibilityLimitations_RelStructure;
import org.rutebanken.netex.model.DayTypeRefStructure;
import org.rutebanken.netex.model.DayTypeRefs_RelStructure;
import org.rutebanken.netex.model.FrequencyGroups_RelStructure;
import org.rutebanken.netex.model.HeadwayJourneyGroup;
import org.rutebanken.netex.model.HeadwayJourneyGroupRefStructure;
import org.rutebanken.netex.model.JourneyPatternRefStructure;
import org.rutebanken.netex.model.LimitationStatusEnumeration;
import org.rutebanken.netex.model.LuggageCarriageEnumeration;
import org.rutebanken.netex.model.ServiceFacilitySet;
import org.rutebanken.netex.model.ServiceFacilitySets_RelStructure;
import org.rutebanken.netex.model.ServiceJourney_VersionStructure;
import org.rutebanken.netex.model.TemplateServiceJourney;
import org.rutebanken.netex.model.TemplateVehicleJourneyTypeEnumeration;
import org.rutebanken.netex.model.TimetabledPassingTime;
import org.rutebanken.netex.model.TimetabledPassingTimes_RelStructure;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.List;

import static mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducer.netexFactory;

public class ServiceJourneyFranceProducer {

    private static KeyListStructureProducer keyListStructureProducer = new KeyListStructureProducer();

    public ServiceJourney_VersionStructure produce(Context context, VehicleJourney vehicleJourney) {
        ExportableData exportableData = (ExportableData) context.get(Constant.EXPORTABLE_DATA);
        ExportableNetexData exportableNetexData = (ExportableNetexData) context.get(Constant.EXPORTABLE_NETEX_DATA);

        ServiceJourney_VersionStructure serviceJourney;

        if (!vehicleJourney.getJourneyFrequencies().isEmpty()) {
            serviceJourney = netexFactory.createTemplateServiceJourney();
            TemplateServiceJourney templateServiceJourney = (TemplateServiceJourney) serviceJourney;
            templateServiceJourney.setTemplateVehicleJourneyType(TemplateVehicleJourneyTypeEnumeration.HEADWAY);
            buildHeadWayJourneys(exportableNetexData, templateServiceJourney, vehicleJourney);
        } else {
            serviceJourney = netexFactory.createServiceJourney();
        }

        NetexProducerUtils.populateIdAndVersion(vehicleJourney, serviceJourney);

        serviceJourney.setName(ConversionUtil.getMultiLingualString(vehicleJourney.getPublishedJourneyName()));

        JourneyPattern journeyPattern = vehicleJourney.getJourneyPattern();
        JourneyPatternRefStructure journeyPatternRefStruct = netexFactory.createJourneyPatternRefStructure();
        NetexProducerUtils.populateReferenceIDFM(journeyPattern, journeyPatternRefStruct);
        serviceJourney.setJourneyPatternRef(netexFactory.createJourneyPatternRef(journeyPatternRefStruct));

        NoticeFranceProducer.addNoticeAndNoticeAssignments(context, exportableNetexData, serviceJourney, vehicleJourney.getFootnotes());


        if (!vehicleJourney.getTimetables().isEmpty()) {
            DayTypeRefs_RelStructure dayTypeStruct = netexFactory.createDayTypeRefs_RelStructure();
            serviceJourney.setDayTypes(dayTypeStruct);

            for (Timetable t : vehicleJourney.getTimetables()) {
                for (Timetable timetable : exportableData.getTimetables()) {
                    if (timetable.getObjectId().equals(t.getObjectId())) {
                        DayTypeRefStructure dayTypeRefStruct = netexFactory.createDayTypeRefStructure();
                        NetexProducerUtils.populateReferenceIDFM(t, dayTypeRefStruct);
                        dayTypeRefStruct.withVersionRef("any");
                        dayTypeRefStruct.withVersion(null);
                        dayTypeStruct.getDayTypeRef().add(netexFactory.createDayTypeRef(dayTypeRefStruct));
                    }
                }
            }
        }

        if (CollectionUtils.isNotEmpty(vehicleJourney.getVehicleJourneyAtStops())) {
            List<VehicleJourneyAtStop> vehicleJourneyAtStops = vehicleJourney.getVehicleJourneyAtStops();
            vehicleJourneyAtStops.sort(Comparator.comparingInt(o -> o.getStopPoint().getPosition()));

            TimetabledPassingTimes_RelStructure passingTimesStruct = netexFactory.createTimetabledPassingTimes_RelStructure();

            for (VehicleJourneyAtStop vehicleJourneyAtStop : vehicleJourneyAtStops) {
                TimetabledPassingTime timetabledPassingTime = netexFactory.createTimetabledPassingTime();
                timetabledPassingTime.setVersion("any");

                LocalTime departureTime = vehicleJourneyAtStop.getDepartureTime();
                LocalTime arrivalTime = vehicleJourneyAtStop.getArrivalTime();

                if (arrivalTime != null) {
                    if (arrivalTime.equals(departureTime)) {
                        NetexTimeConversionUtil.populatePassingTimeUtc(timetabledPassingTime, true, vehicleJourneyAtStop);
                    } else {
                        NetexTimeConversionUtil.populatePassingTimeUtc(timetabledPassingTime, true, vehicleJourneyAtStop);
                    }
                }
                if (departureTime != null) {
                    NetexTimeConversionUtil.populatePassingTimeUtc(timetabledPassingTime, false, vehicleJourneyAtStop);
                    timetabledPassingTime.setDepartureTime(TimeUtil.toLocalTimeFromJoda(departureTime));
                    if (vehicleJourneyAtStop.getDepartureDayOffset() > 0) {
                        timetabledPassingTime.setDepartureDayOffset(BigInteger.valueOf(vehicleJourneyAtStop.getDepartureDayOffset()));
                    }
                }

                passingTimesStruct.getTimetabledPassingTime().add(timetabledPassingTime);
            }

            serviceJourney.setPassingTimes(passingTimesStruct);
        }

        ServiceFacilitySet serviceFacilitySet = new ServiceFacilitySet();
        NetexProducerUtils.populateIdAndVersion(vehicleJourney, serviceFacilitySet);
        serviceFacilitySet.setId(serviceFacilitySet.getId().replace("ServiceJourney", "ServiceFacilitySet"));
        serviceFacilitySet.setVersion("any");
        if (vehicleJourney.getBikesAllowed() != null && vehicleJourney.getBikesAllowed().equals(true)) {
            serviceFacilitySet.withLuggageCarriageFacilityList(LuggageCarriageEnumeration.CYCLES_ALLOWED);
        } else if (vehicleJourney.getBikesAllowed() != null && vehicleJourney.getBikesAllowed().equals(false)) {
            serviceFacilitySet.withLuggageCarriageFacilityList(LuggageCarriageEnumeration.NO_CYCLES);
        } else {
            serviceFacilitySet.withLuggageCarriageFacilityList(LuggageCarriageEnumeration.UNKNOWN);
        }

        ServiceFacilitySets_RelStructure serviceFacilitySets_relStructure = new ServiceFacilitySets_RelStructure();
        serviceFacilitySets_relStructure.withServiceFacilitySetRefOrServiceFacilitySet(serviceFacilitySet);
        serviceJourney.setFacilities(serviceFacilitySets_relStructure);

        serviceJourney.setKeyList(keyListStructureProducer.produce(vehicleJourney.getKeyValues()));
        serviceJourney.setServiceAlteration(ConversionUtil.toServiceAlterationEnumeration(vehicleJourney.getServiceAlteration()));

        getAccessibility(vehicleJourney, serviceJourney);

        return serviceJourney;
    }

    private static void getAccessibility(VehicleJourney vehicleJourney, ServiceJourney_VersionStructure serviceJourney) {
        if(vehicleJourney.getAccessibilityAssessment() != null && vehicleJourney.getAccessibilityAssessment().getAccessibilityLimitation() != null) {
            AccessibilityLimitation netexAccessibilityLimitation = netexFactory.createAccessibilityLimitation();
            mobi.chouette.model.AccessibilityLimitation accessibilityLimitation = vehicleJourney.getAccessibilityAssessment().getAccessibilityLimitation();
            NetexProducerUtils.populateIdAndVersion(accessibilityLimitation, netexAccessibilityLimitation);

            AccessibilityAssessment netexAccessibilityAssessment = netexFactory.createAccessibilityAssessment();
            mobi.chouette.model.AccessibilityAssessment accessibilityAssessment = vehicleJourney.getAccessibilityAssessment();
            NetexProducerUtils.populateIdAndVersion(accessibilityAssessment, netexAccessibilityAssessment);

            if(vehicleJourney.getAccessibilityAssessment().getMobilityImpairedAccess() != null){
                if(vehicleJourney.getAccessibilityAssessment().getMobilityImpairedAccess().equals(LimitationStatusEnum.TRUE)){
                    netexAccessibilityAssessment.setMobilityImpairedAccess(LimitationStatusEnumeration.TRUE);
                }
                else if(vehicleJourney.getAccessibilityAssessment().getMobilityImpairedAccess().equals(LimitationStatusEnum.FALSE)){
                    netexAccessibilityAssessment.setMobilityImpairedAccess(LimitationStatusEnumeration.FALSE);
                }
                else if(vehicleJourney.getAccessibilityAssessment().getMobilityImpairedAccess().equals(LimitationStatusEnum.PARTIAL)){
                    netexAccessibilityAssessment.setMobilityImpairedAccess(LimitationStatusEnumeration.PARTIAL);
                }
                else {
                    netexAccessibilityAssessment.setMobilityImpairedAccess(LimitationStatusEnumeration.UNKNOWN);
                }
            }

            if(vehicleJourney.getAccessibilityAssessment().getAccessibilityLimitation().getWheelchairAccess() != null){
                if(vehicleJourney.getAccessibilityAssessment().getAccessibilityLimitation().getWheelchairAccess().equals(LimitationStatusEnumeration.TRUE)){
                    netexAccessibilityLimitation.setWheelchairAccess(LimitationStatusEnumeration.TRUE);
                }
                else if(vehicleJourney.getAccessibilityAssessment().getAccessibilityLimitation().getWheelchairAccess().equals(LimitationStatusEnumeration.FALSE)){
                    netexAccessibilityLimitation.setWheelchairAccess(LimitationStatusEnumeration.FALSE);
                }
                else if(vehicleJourney.getAccessibilityAssessment().getAccessibilityLimitation().getWheelchairAccess().equals(LimitationStatusEnumeration.PARTIAL)){
                    netexAccessibilityLimitation.setWheelchairAccess(LimitationStatusEnumeration.PARTIAL);
                }
                else {
                    netexAccessibilityLimitation.setWheelchairAccess(LimitationStatusEnumeration.UNKNOWN);
                }
            }

            AccessibilityLimitations_RelStructure accessibilityLimitationsRelStructure = new AccessibilityLimitations_RelStructure();
            accessibilityLimitationsRelStructure.setAccessibilityLimitation(netexAccessibilityLimitation);
            netexAccessibilityAssessment.setLimitations(accessibilityLimitationsRelStructure);

            serviceJourney.setAccessibilityAssessment(netexAccessibilityAssessment);
        }
    }


    private void buildHeadWayJourneys(ExportableNetexData exportableNetexData, TemplateServiceJourney templateServiceJourney, VehicleJourney vehicleJourney) {
        FrequencyGroups_RelStructure freqGroup = netexFactory.createFrequencyGroups_RelStructure();

        for (JourneyFrequency journeyFrequency : vehicleJourney.getJourneyFrequencies()) {

            HeadwayJourneyGroup headwayJourneyGroup = netexFactory.createHeadwayJourneyGroup();
            headwayJourneyGroup.setScheduledHeadwayInterval(TimeUtil.toDurationFromJodaDuration(journeyFrequency.getScheduledHeadwayInterval()));
            headwayJourneyGroup.setFirstDepartureTime(TimeUtil.toLocalTimeFromJoda(journeyFrequency.getFirstDepartureTime()));
            headwayJourneyGroup.setLastDepartureTime(TimeUtil.toLocalTimeFromJoda(journeyFrequency.getLastDepartureTime()));

            headwayJourneyGroup.setId(journeyFrequency.getObjectId());
            headwayJourneyGroup.setVersion("any");
            exportableNetexData.getHeadwayJourneys().add(headwayJourneyGroup);

            HeadwayJourneyGroupRefStructure struct = netexFactory.createHeadwayJourneyGroupRefStructure();
            struct.setRef(journeyFrequency.getObjectId());
            struct.setVersion("any");
            freqGroup.getHeadwayJourneyGroupRefOrHeadwayJourneyGroupOrRhythmicalJourneyGroupRef().add(struct);

        }

        templateServiceJourney.withFrequencyGroups(freqGroup);

    }
}
