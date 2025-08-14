package mobi.chouette.model;

import lombok.*;

import java.util.HashSet;

import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ChouetteData {
    Set<ChouetteIdentifier> vehicleJourneys;
    Set<ChouetteIdentifier> journeyPatterns;
    Set<ChouetteIdentifier> routes;
    Set<ChouetteIdentifier> timetables;
    Set<ChouetteIdentifier> accessibilityAssessments;
    Set<ChouetteIdentifier> accessibilityLimitations;
    Set<ChouetteIdentifier> routePoints;
    Set<ChouetteIdentifier> routeSections;
    Set<ChouetteIdentifier> scheduledStopPoints;
    Set<ChouetteIdentifier> stopPoints;
    Set<ChouetteIdentifier> vehicleJourneyAtStops;
    Set<ChouetteIdentifier> vehicleJourneyFacilities;
    Set<ChouetteIdentifier> attributions;

    String lineId;
    String datasetId;
    String companyId;
    String networkId;


    public Set<ChouetteIdentifier> getVehicleJourneys() {
        if (vehicleJourneys == null) {
            vehicleJourneys = new HashSet<>();
        }
        return vehicleJourneys;
    }

    public Set<ChouetteIdentifier> getJourneyPatterns() {
        if (journeyPatterns == null) {
            journeyPatterns = new HashSet<>();
        }
        return journeyPatterns;
    }

    public Set<ChouetteIdentifier> getRoutes() {
        if (routes == null) {
            routes = new HashSet<>();
        }
        return routes;
    }

    public Set<ChouetteIdentifier> getTimetables() {
        if (timetables == null) {
            timetables = new HashSet<>();
        }
        return timetables;
    }

    public Set<ChouetteIdentifier> getAccessibilityAssessments() {
        if (accessibilityAssessments == null) {
            accessibilityAssessments = new HashSet<>();
        }
        return accessibilityAssessments;
    }

    public Set<ChouetteIdentifier> getAccessibilityLimitations() {
        if (accessibilityLimitations == null) {
            accessibilityLimitations = new HashSet<>();
        }
        return accessibilityLimitations;
    }

    public Set<ChouetteIdentifier> getRoutePoints() {
        if (routePoints == null) {
            routePoints = new HashSet<>();
        }
        return routePoints;
    }

    public Set<ChouetteIdentifier> getRouteSections() {
        if (routeSections == null) {
            routeSections = new HashSet<>();
        }
        return routeSections;
    }

    public Set<ChouetteIdentifier> getScheduledStopPoints() {
        if (scheduledStopPoints == null) {
            scheduledStopPoints = new HashSet<>();
        }
        return scheduledStopPoints;
    }

    public Set<ChouetteIdentifier> getStopPoints() {
        if (stopPoints == null) {
            stopPoints = new HashSet<>();
        }
        return stopPoints;
    }

    public Set<ChouetteIdentifier> getVehicleJourneyAtStops() {
        if (vehicleJourneyAtStops == null) {
            vehicleJourneyAtStops = new HashSet<>();
        }
        return vehicleJourneyAtStops;
    }

    public Set<ChouetteIdentifier> getVehicleJourneyFacilities() {
        if (vehicleJourneyFacilities == null) {
            vehicleJourneyFacilities = new HashSet<>();
        }
        return vehicleJourneyFacilities;
    }

    public Set<ChouetteIdentifier> getAttributions() {
        if (attributions == null) {
            attributions = new HashSet<>();
        }
        return attributions;
    }
}
