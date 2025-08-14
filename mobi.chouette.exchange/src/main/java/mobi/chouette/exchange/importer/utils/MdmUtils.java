package mobi.chouette.exchange.importer.utils;

import mobi.chouette.model.*;


/**
 * Utilitary class to read Line data and write ids into ChouetteData object that will later sent to mdm
 */
public class MdmUtils {


    public static void fillMdmDataWithVjas(ChouetteData chouetteData, String referential, VehicleJourneyAtStop vjas) {
        if (vjas == null){
            return;
        }

        ChouetteIdentifier vehicleJourneyAtStopId = new ChouetteIdentifier();
        vehicleJourneyAtStopId.setDataset(referential);
        vehicleJourneyAtStopId.setId(vjas.getObjectId());
        chouetteData.getVehicleJourneyAtStops().add(vehicleJourneyAtStopId);

    }

    public static void fillMdmDataWithTimetable(ChouetteData chouetteData, String referential, Timetable timetable) {
        ChouetteIdentifier timetableId = new ChouetteIdentifier();
        timetableId.setId(timetable.getObjectId());
        timetableId.setDataset(referential);
        chouetteData.getTimetables().add(timetableId);
    }

    public static void fillMdmDataWithAttribution(ChouetteData chouetteData, String referential, Attribution attribution) {
        ChouetteIdentifier attributionId = new ChouetteIdentifier();
        attributionId.setId(attribution.getObjectId());
        attributionId.setDataset(referential);
        chouetteData.getAttributions().add(attributionId);
    }

    public static void fillMdmDataWithAccessibilityAssessment(ChouetteData chouetteData, String referential, AccessibilityAssessment accessibilityAssessment) {
        ChouetteIdentifier accessibilityAssessmentId = new ChouetteIdentifier();
        accessibilityAssessmentId.setId(accessibilityAssessment.getObjectId());
        accessibilityAssessmentId.setDataset(referential);
        chouetteData.getAccessibilityAssessments().add(accessibilityAssessmentId);
    }

    public static void fillMdmDataWithVehicleJourneyFacility(ChouetteData chouetteData, String referential, VehicleJourneyFacility vjFacility) {
        ChouetteIdentifier facilityId = new ChouetteIdentifier();
        facilityId.setDataset(referential);
        facilityId.setId(vjFacility.getObjectId());
        chouetteData.getVehicleJourneyFacilities().add(facilityId);
    }

    public static void fillMdmDataWithJourneyPattern(ChouetteData chouetteData, String referential, JourneyPattern journeyPattern) {

        for (VehicleJourney vehicleJourney : journeyPattern.getVehicleJourneys()) {
            fillMdmDataWithVehicleJourney(chouetteData, referential, vehicleJourney);
        }

        for (RouteSection routeSection : journeyPattern.getRouteSections()) {
            fillMdmDataWithRouteSection(chouetteData, referential, routeSection);
        }

        for (StopPoint stopPoint : journeyPattern.getStopPoints()) {
            fillMdmDataWithStopPoint(chouetteData, referential, stopPoint);
        }

        ChouetteIdentifier patternId = new ChouetteIdentifier();
        patternId.setId(journeyPattern.getObjectId());
        patternId.setDataset(referential);
        chouetteData.getJourneyPatterns().add(patternId);

    }

    public static void fillMdmDataWithRouteSection(ChouetteData chouetteData, String referential, RouteSection routeSection) {
        ChouetteIdentifier routeSectionId = new ChouetteIdentifier();
        routeSectionId.setId(routeSection.getObjectId());
        routeSectionId.setDataset(referential);
        chouetteData.getRouteSections().add(routeSectionId);
    }

    public static void fillMdmDataWithStopPoint(ChouetteData chouetteData, String referential, StopPoint stopPoint) {
        ChouetteIdentifier stopPointId = new ChouetteIdentifier();
        stopPointId.setId(stopPoint.getObjectId());
        stopPointId.setDataset(referential);
        chouetteData.getStopPoints().add(stopPointId);

        if (stopPoint.getScheduledStopPoint() != null){
            fillMdmDataWithScheduledStopPoint(chouetteData, referential, stopPoint.getScheduledStopPoint());
        }
    }

    public static void fillMdmDataWithScheduledStopPoint(ChouetteData chouetteData, String referential, ScheduledStopPoint ssp) {
        ChouetteIdentifier scheduledStopPointId = new ChouetteIdentifier();
        scheduledStopPointId.setId(ssp.getObjectId());
        scheduledStopPointId.setDataset(referential);
        chouetteData.getScheduledStopPoints().add(scheduledStopPointId);
    }

    public static void fillMdmDataWithRoute(ChouetteData chouetteData, String referential, Route route) {
        ChouetteIdentifier routeId = new ChouetteIdentifier();
        routeId.setId(route.getObjectId());
        routeId.setDataset(referential);
        chouetteData.getRoutes().add(routeId);

        for (JourneyPattern journeyPattern : route.getJourneyPatterns()) {
            fillMdmDataWithJourneyPattern(chouetteData, referential, journeyPattern);
        }

        for (RoutePoint routePoint : route.getRoutePoints()) {
            fillMdmDataWithRoutePoint(chouetteData, referential, routePoint);
        }

        for (StopPoint stopPoint : route.getStopPoints()) {
            fillMdmDataWithStopPoint(chouetteData, referential, stopPoint);
        }

    }

    public static void fillMdmDataWithRoutePoint(ChouetteData chouetteData, String referential, RoutePoint routePoint) {
        ChouetteIdentifier routePointId = new ChouetteIdentifier();
        routePointId.setId(routePoint.getObjectId());
        routePointId.setDataset(referential);
        chouetteData.getRoutePoints().add(routePointId);
    }


    public static void fillMdmDataWithVehicleJourney(ChouetteData chouetteData, String referential, VehicleJourney vehicleJourney) {
        ChouetteIdentifier vjId = new ChouetteIdentifier();
        vjId.setId(vehicleJourney.getObjectId());
        vjId.setDataset(referential);
        chouetteData.getVehicleJourneys().add(vjId);

        for (Timetable timetable : vehicleJourney.getTimetables()) {
            fillMdmDataWithTimetable(chouetteData, referential, timetable);
        }

        if (vehicleJourney.getAccessibilityAssessment() != null){
            fillMdmDataWithAccessibilityAssessment(chouetteData, referential, vehicleJourney.getAccessibilityAssessment());
        }

        for (VehicleJourneyAtStop vehicleJourneyAtStop : vehicleJourney.getVehicleJourneyAtStops()) {
            fillMdmDataWithVjas(chouetteData, referential, vehicleJourneyAtStop);
        }

        for (VehicleJourneyFacility vehicleJourneyFacility : vehicleJourney.getVehicleJourneyFacilities()) {
            fillMdmDataWithVehicleJourneyFacility(chouetteData, referential, vehicleJourneyFacility);
        }
    }


    public static void fillMdmData(ChouetteData chouetteData, String referential, Line line) {
        chouetteData.setLineId(line.getObjectId());
        chouetteData.setDatasetId(referential);

        for (Route route : line.getRoutes()) {
            fillMdmDataWithRoute(chouetteData, referential, route);
        }

        if (line.getCompany() != null){
            chouetteData.setCompanyId(line.getCompany().getObjectId());
        }

        if (line.getNetwork() != null){
            chouetteData.setNetworkId(line.getNetwork().getObjectId());
        }

        if (line.getAccessibilityAssessment() != null) {
            fillMdmDataWithAccessibilityAssessment(chouetteData, referential, line.getAccessibilityAssessment());
        }

    }
}
