package mobi.chouette.exchange.netexprofile.exporter.producer;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.exchange.netexprofile.Constant;
import mobi.chouette.exchange.netexprofile.exporter.NetexprofileExportParameters;
import mobi.chouette.model.AccessibilityAssessment;
import mobi.chouette.model.AccessibilityLimitation;
import mobi.chouette.model.JourneyPattern;
import mobi.chouette.model.Line;
import mobi.chouette.model.Network;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.VehicleJourney;
import mobi.chouette.model.*;
import mobi.chouette.model.type.ChouetteAreaEnum;
import mobi.chouette.model.type.DayTypeEnum;
import mobi.chouette.model.type.LimitationStatusEnum;
import mobi.chouette.model.type.OrganisationTypeEnum;
import org.rutebanken.netex.model.*;

import javax.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducer.NETEX_DEFAULT_OBJECT_VERSION;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.*;

@Log4j
public class NetexProducerUtils {

    public static boolean isSet(Object... objects) {
        for (Object val : objects) {
            if (val != null) {
                if (val instanceof String) {
                    if (!((String) val).isEmpty())
                        return true;
                } else {
                    return true;
                }
            }
        }
        return false;
    }

    public static OrganisationTypeEnumeration getOrganisationTypeEnumeration(OrganisationTypeEnum organisationTypeEnum) {
        if (organisationTypeEnum == null)
            return null;
        switch (organisationTypeEnum) {
            case Authority:
                return OrganisationTypeEnumeration.AUTHORITY;
            case Operator:
                return OrganisationTypeEnumeration.OPERATOR;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static List<DayOfWeekEnumeration> toDayOfWeekEnumeration(List<DayTypeEnum> dayTypeEnums) {
        EnumSet<DayTypeEnum> actualDaysOfWeek = EnumSet.noneOf(DayTypeEnum.class);
        actualDaysOfWeek.addAll(dayTypeEnums);

        if (actualDaysOfWeek.isEmpty()) {
            return Collections.EMPTY_LIST;
        } else if (actualDaysOfWeek
                .equals(EnumSet.of(DayTypeEnum.Monday, DayTypeEnum.Tuesday, DayTypeEnum.Wednesday, DayTypeEnum.Thursday, DayTypeEnum.Friday))) {
            return Collections.singletonList(DayOfWeekEnumeration.WEEKDAYS);
        } else if (actualDaysOfWeek.equals(EnumSet.of(DayTypeEnum.Saturday, DayTypeEnum.Sunday))) {
            return Collections.singletonList(DayOfWeekEnumeration.WEEKEND);
        } else if (actualDaysOfWeek.equals(EnumSet.of(DayTypeEnum.Monday, DayTypeEnum.Tuesday, DayTypeEnum.Wednesday, DayTypeEnum.Thursday, DayTypeEnum.Friday,
                DayTypeEnum.Saturday, DayTypeEnum.Sunday))) {
            return Collections.singletonList(DayOfWeekEnumeration.EVERYDAY);
        }

        List<DayOfWeekEnumeration> dayOfWeekEnumerations = new ArrayList<>();

        for (DayTypeEnum dayTypeEnum : dayTypeEnums) {
            switch (dayTypeEnum) {
                case Monday:
                    dayOfWeekEnumerations.add(DayOfWeekEnumeration.MONDAY);
                    break;
                case Tuesday:
                    dayOfWeekEnumerations.add(DayOfWeekEnumeration.TUESDAY);
                    break;
                case Wednesday:
                    dayOfWeekEnumerations.add(DayOfWeekEnumeration.WEDNESDAY);
                    break;
                case Thursday:
                    dayOfWeekEnumerations.add(DayOfWeekEnumeration.THURSDAY);
                    break;
                case Friday:
                    dayOfWeekEnumerations.add(DayOfWeekEnumeration.FRIDAY);
                    break;
                case Saturday:
                    dayOfWeekEnumerations.add(DayOfWeekEnumeration.SATURDAY);
                    break;
                case Sunday:
                    dayOfWeekEnumerations.add(DayOfWeekEnumeration.SUNDAY);
                    break;
                default:
                    // None
            }
        }

        return dayOfWeekEnumerations;
    }

    public static List<DayOfWeekEnumeration> toDayOfWeekEnumerationIDFM(List<DayTypeEnum> dayTypeEnums) {
        List<DayOfWeekEnumeration> dayOfWeekEnumerations = new ArrayList<>();

        for (DayTypeEnum dayTypeEnum : dayTypeEnums) {
            switch (dayTypeEnum) {
                case Monday:
                    dayOfWeekEnumerations.add(DayOfWeekEnumeration.MONDAY);
                    break;
                case Tuesday:
                    dayOfWeekEnumerations.add(DayOfWeekEnumeration.TUESDAY);
                    break;
                case Wednesday:
                    dayOfWeekEnumerations.add(DayOfWeekEnumeration.WEDNESDAY);
                    break;
                case Thursday:
                    dayOfWeekEnumerations.add(DayOfWeekEnumeration.THURSDAY);
                    break;
                case Friday:
                    dayOfWeekEnumerations.add(DayOfWeekEnumeration.FRIDAY);
                    break;
                case Saturday:
                    dayOfWeekEnumerations.add(DayOfWeekEnumeration.SATURDAY);
                    break;
                case Sunday:
                    dayOfWeekEnumerations.add(DayOfWeekEnumeration.SUNDAY);
                    break;
                default:
                    // None
            }
        }

        return dayOfWeekEnumerations;
    }

    private static final AtomicInteger idCounter = new AtomicInteger(0);

    public static String netexId(String objectIdPrefix, String elementName, String objectIdSuffix) {
        return objectIdPrefix + OBJECT_ID_SPLIT_CHAR + elementName + OBJECT_ID_SPLIT_CHAR + objectIdSuffix;
    }

    public static String translateObjectId(String original, String newType) {
        String[] splittedParts = original.split(":");
        if (splittedParts.length == 3) {
            return original.replaceAll(splittedParts[1], newType);
        } else {
            log.warn("Could not transform identifier " + original + " to type " + newType + " as it does not conform to id standard (XXX:Type:YYY)");
            return original;
        }
    }

    public static String replaceObjectIdPart(String original, String newValue, int indexPart, Object clazz) {
        if (clazz instanceof Line) {
            String[] splittedParts = original.split(":");
            if (splittedParts.length > indexPart) {
                return original.replaceAll(splittedParts[indexPart], newValue);
            } else {
                log.warn("Could not transform identifier " + original + " to value " + newValue + " as it does not conform to id standard (XXX:Type:YYY) or (XXX:Type:YYY:LOC)");
                return original;
            }
        }
        return original;
    }

    public static String createUniqueId(Context context, String type) {
        NetexprofileExportParameters configuration = (NetexprofileExportParameters) context.get(Constant.CONFIGURATION);
        return configuration.getDefaultCodespacePrefix() + OBJECT_ID_SPLIT_CHAR + type + OBJECT_ID_SPLIT_CHAR + idCounter.incrementAndGet();
    }

    public static String createUniqueIDFMId(Context context, String type) {
        NetexprofileExportParameters configuration = (NetexprofileExportParameters) context.get(Constant.CONFIGURATION);
        return configuration.getDefaultCodespacePrefix() + OBJECT_ID_SPLIT_CHAR + type + OBJECT_ID_SPLIT_CHAR + idCounter.incrementAndGet() + OBJECT_ID_SPLIT_CHAR + LOC;
    }

    public static String createUniqueGeneralFrameId(Context context, String type, String typeFile, String time) {
        NetexprofileExportParameters configuration = (NetexprofileExportParameters) context.get(Constant.CONFIGURATION);
        time = time.replaceAll("-", "");
        time = time.replaceAll("T", "");
        time = time.replaceAll(":", "");
        return configuration.getDefaultCodespacePrefix() + OBJECT_ID_SPLIT_CHAR + type + OBJECT_ID_SPLIT_CHAR + typeFile + OBJECT_ID_SPLIT_DASH + time + OBJECT_ID_SPLIT_CHAR + LOC;
    }

    public static String createUniqueGeneralFrameInLineId(Context context, String typeFile, String time) {
        NetexprofileExportParameters configuration = (NetexprofileExportParameters) context.get(Constant.CONFIGURATION);
        time = time.replaceAll("-", "");
        time = time.replaceAll("T", "");
        time = time.replaceAll(":", "");
        return configuration.getDefaultCodespacePrefix() + OBJECT_ID_SPLIT_CHAR + typeFile + OBJECT_ID_SPLIT_DASH + time + OBJECT_ID_SPLIT_CHAR + LOC;
    }

    public static String createUniqueCompositeFrameInLineId(Context context, String type, String typeFile) {
        NetexprofileExportParameters configuration = (NetexprofileExportParameters) context.get(Constant.CONFIGURATION);
        return configuration.getDefaultCodespacePrefix() + OBJECT_ID_SPLIT_CHAR + type + OBJECT_ID_SPLIT_CHAR + typeFile + OBJECT_ID_SPLIT_DASH + LOC;
    }


    public static String translateType(NeptuneObject v) {
        if (v instanceof Timetable) {
            return "DayType";
        } else if (v instanceof Company) {
            Company c = (Company) v;
            if (OrganisationTypeEnum.Authority.equals(c.getOrganisationType())) {
                return "Authority";
            } else if (OrganisationTypeEnum.Operator.equals(c.getOrganisationType())) {
                return "Operator";
            } else {
                return "GeneralOrganisation";
            }
        } else if (v instanceof VehicleJourney) {
            return "ServiceJourney";
        } else if (v instanceof JourneyPattern) {
            return "JourneyPattern";
        } else if (v instanceof StopArea) {
            StopArea sa = (StopArea) v;
            if (ChouetteAreaEnum.BoardingPosition.equals(sa.getAreaType())) {
                return "Quay";
            } else if (ChouetteAreaEnum.CommercialStopPoint.equals(sa.getAreaType())) {
                return "StopPlace";
            }
        } else if (v instanceof Footnote) {
            return "Notice";
        } else if (v instanceof StopPoint) {
            return "StopPointInJourneyPattern";
        } else if (v instanceof VehicleJourneyAtStop) {
            return "TimetabledPassingTime";
        } else if (v instanceof Network) {
            return "Network";
        } else if (v instanceof RouteSection) {
            return "ServiceLink";
        }

        return null;


    }


    public static String generateNetexId(NeptuneIdentifiedObject source) {
        if (source == null) {
            log.error("Cannot generate netexid as source is null");
            return null;
        }
        String newType = translateType(source);
        if (newType != null) {
            return translateObjectId(source.getObjectId(), newType);
        } else {
            return source.getObjectId();
        }
    }


    public static void populateId(NeptuneIdentifiedObject source, EntityInVersionStructure destination) {
        if (source == null || destination == null) {
            log.error("Cannot set id since either source or destination is null");
            return;
        }
        String newType = translateType(source);
        if (newType != null) {
            destination.setId(translateObjectId(source.getObjectId(), newType));
        } else {
            destination.setId(source.getObjectId());
        }
        destination.setVersion(source.getObjectVersion() == null ? "1" : source.getObjectVersion().toString());
    }

    public static void populateReference(NeptuneIdentifiedObject source, VersionOfObjectRefStructure destination, boolean withVersion) {
        if (source == null || destination == null) {
            log.error("Cannot set reference since either source or destination is null");
            return;
        }
        String newType = translateType(source);
        if (newType != null) {
            destination.setRef(translateObjectId(source.getObjectId(), newType));
        } else {
            destination.setRef(source.getObjectId());
        }
        if (withVersion) {
            destination.setVersion(source.getObjectVersion() == null ? "1" : source.getObjectVersion().toString());
        }

    }

    public static void populateReference(EntityInVersionStructure source, VersionOfObjectRefStructure destination, boolean withVersion) {
        if (source == null || destination == null) {
            log.error("Cannot set reference since either source or destination is null");
            return;
        }
        destination.setRef(source.getId());
        if (withVersion) {
            destination.setVersion(source.getVersion());
        }

    }

    public static JAXBElement<? extends LineRefStructure> createLineRef(Line neptuneLine, ObjectFactory netexFactory) {
        if (Boolean.TRUE.equals(neptuneLine.getFlexibleService())) {
            FlexibleLineRefStructure lineRefStruct = netexFactory.createFlexibleLineRefStructure();
            NetexProducerUtils.populateReference(neptuneLine, lineRefStruct, true);
            return netexFactory.createFlexibleLineRef(lineRefStruct);
        }
        LineRefStructure lineRefStruct = netexFactory.createLineRefStructure();
        NetexProducerUtils.populateReference(neptuneLine, lineRefStruct, true);
        return netexFactory.createLineRef(lineRefStruct);
    }

    public static JAXBElement<? extends LineRefStructure> createLineIDFMRef(Line neptuneLine, ObjectFactory netexFactory) {
        boolean isFlexibleService = Boolean.TRUE.equals(neptuneLine.getFlexibleService());
        LineRefStructure lrs;
        if (isFlexibleService) {
            lrs = netexFactory.createFlexibleLineRefStructure();
        } else {
            lrs = netexFactory.createLineRefStructure();
        }
        if (!(neptuneLine.getObjectId().endsWith(OBJECT_ID_SPLIT_CHAR + LOC))){
            lrs.setRef(neptuneLine.getObjectId() + OBJECT_ID_SPLIT_CHAR + LOC);
        } else {
            lrs.setRef(neptuneLine.getObjectId());
        }
        if (isFlexibleService) {
            return netexFactory.createFlexibleLineRef((FlexibleLineRefStructure) lrs);
        }
        return netexFactory.createLineRef(lrs);
    }

    public static void populateIdAndVersion(NeptuneIdentifiedObject source, EntityInVersionStructure destination) {
        if (source == null || destination == null) {
            log.error("Cannot set id since either source or destination is null");
            return;
        }
        String newType = translateTypeFrance(source);
        if (newType != null) {
            destination.setId(translateObjectId(source.getObjectId(), newType));
        } else {
            destination.setId(source.getObjectId());
        }

        if (!(destination.getId().endsWith(OBJECT_ID_SPLIT_CHAR + LOC) )) {
            destination.setId(destination.getId() + OBJECT_ID_SPLIT_CHAR + LOC);
        }
        destination.setVersion(NETEX_DEFAULT_OBJECT_VERSION);
    }

    public static void populateLineAccessibilityAssessment(Line source, org.rutebanken.netex.model.Line_VersionStructure destination) {
        if (source == null || destination == null) {
            log.error("Cannot set id since either source or destination is null");
            return;
        }

        if (source.getAccessibilityAssessment() == null)
            return;

        AccessibilityAssessment sourceAssessment = source.getAccessibilityAssessment();
        org.rutebanken.netex.model.AccessibilityAssessment accessibilityAssessment = new org.rutebanken.netex.model.AccessibilityAssessment();
        if (sourceAssessment.getMobilityImpairedAccess() != null) {
            if(sourceAssessment.getMobilityImpairedAccess().equals(LimitationStatusEnum.TRUE)){
                accessibilityAssessment.setMobilityImpairedAccess(LimitationStatusEnumeration.TRUE);
            }
            else if(sourceAssessment.getMobilityImpairedAccess().equals(LimitationStatusEnum.FALSE)){
                accessibilityAssessment.setMobilityImpairedAccess(LimitationStatusEnumeration.FALSE);
            }
            else if(sourceAssessment.getMobilityImpairedAccess().equals(LimitationStatusEnum.PARTIAL)){
                accessibilityAssessment.setMobilityImpairedAccess(LimitationStatusEnumeration.PARTIAL);
            }
            else{
                accessibilityAssessment.setMobilityImpairedAccess(LimitationStatusEnumeration.UNKNOWN);
            }
        }

        NetexProducerUtils.populateIdAndVersion(sourceAssessment, accessibilityAssessment);

        AccessibilityLimitation sourceLimitation = sourceAssessment.getAccessibilityLimitation();

        if (sourceLimitation != null) {
            org.rutebanken.netex.model.AccessibilityLimitation accessibilityLimitation = new org.rutebanken.netex.model.AccessibilityLimitation();
            NetexProducerUtils.populateIdAndVersion(sourceLimitation, accessibilityLimitation);

            AccessibilityLimitations_RelStructure accessibilityLimitations_relStructure = new AccessibilityLimitations_RelStructure();

            if (sourceLimitation.getAudibleSignalsAvailable() != null) {
                accessibilityLimitation.setAudibleSignalsAvailable(sourceLimitation.getAudibleSignalsAvailable());
            }

            if (sourceLimitation.getEscalatorFreeAccess() != null) {
                accessibilityLimitation.setEscalatorFreeAccess(sourceLimitation.getEscalatorFreeAccess());
            }

            if (sourceLimitation.getLiftFreeAccess() != null) {
                accessibilityLimitation.setLiftFreeAccess(sourceLimitation.getLiftFreeAccess());
            }

            if (sourceLimitation.getStepFreeAccess() != null) {
                accessibilityLimitation.setStepFreeAccess(sourceLimitation.getStepFreeAccess());
            }

            if (sourceLimitation.getVisualSignsAvailable() != null) {
                accessibilityLimitation.setVisualSignsAvailable(sourceLimitation.getVisualSignsAvailable());
            }

            if (sourceLimitation.getWheelchairAccess() != null) {
                accessibilityLimitation.setWheelchairAccess(sourceLimitation.getWheelchairAccess());
            }

            accessibilityLimitations_relStructure.setAccessibilityLimitation(accessibilityLimitation);
            accessibilityAssessment.setLimitations(accessibilityLimitations_relStructure);

        }

        destination.setAccessibilityAssessment(accessibilityAssessment);
    }

    public static void populateReferenceIDFM(NeptuneIdentifiedObject source, VersionOfObjectRefStructure destination) {
        if (source == null || destination == null) {
            log.error("Cannot set reference since either source or destination is null");
            return;
        }
        String newType = translateTypeFrance(source);
        if (newType != null) {
            destination.setRef(replaceObjectIdPart(translateObjectId(source.getObjectId(), newType), "FR1", 0, source));
        } else {
            destination.setRef(replaceObjectIdPart(source.getObjectId(), "FR1", 0, source));
        }
        if (!destination.getRef().endsWith(OBJECT_ID_SPLIT_CHAR + LOC)) {
            destination.setRef(replaceObjectIdPart(destination.getRef() + OBJECT_ID_SPLIT_CHAR + LOC, "FR1", 0, source));
        }
        destination.setVersion(NETEX_DEFAULT_OBJECT_VERSION);

    }

    public static String translateTypeFrance(NeptuneObject v) {
        if (v instanceof Timetable) {
            return "DayType";
        } else if (v instanceof Company) {
            Company c = (Company) v;
            if (OrganisationTypeEnum.Authority.equals(c.getOrganisationType())) {
                return "Authority";
            } else if (OrganisationTypeEnum.Operator.equals(c.getOrganisationType())) {
                return "Operator";
            } else {
                return "GeneralOrganisation";
            }
        } else if (v instanceof VehicleJourney) {
            return "ServiceJourney";
        } else if (v instanceof JourneyPattern) {
            return "ServiceJourneyPattern";
        } else if (v instanceof StopArea) {
            StopArea sa = (StopArea) v;
            if (ChouetteAreaEnum.BoardingPosition.equals(sa.getAreaType())) {
                return "Quay";
            } else if (ChouetteAreaEnum.CommercialStopPoint.equals(sa.getAreaType())) {
                return "StopPlace";
            }
        } else if (v instanceof Footnote) {
            return "Notice";
        } else if (v instanceof StopPoint) {
            return "StopPointInJourneyPattern";
        } else if (v instanceof VehicleJourneyAtStop) {
            return "TimetabledPassingTime";
        } else if (v instanceof Network) {
            return "Network";
        } else if (v instanceof RouteSection) {
            return "RouteLink";
        }

        return null;


    }

}
