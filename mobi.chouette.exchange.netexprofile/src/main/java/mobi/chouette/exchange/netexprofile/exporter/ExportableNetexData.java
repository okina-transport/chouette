package mobi.chouette.exchange.netexprofile.exporter;

import lombok.Getter;
import lombok.Setter;
import mobi.chouette.model.ConnectionLink;
import org.rutebanken.netex.model.*;

import java.util.*;

@Setter
@Getter
public class ExportableNetexData {

    private AvailabilityCondition commonCondition;

    private AvailabilityCondition lineCondition;

    private Map<String, Codespace> sharedCodespaces = new HashMap<>();

    private Map<String, Network> sharedNetworks = new HashMap<>();

    private Map<String, Line_VersionStructure> sharedLines = new HashMap<>();

    private Map<String, GroupOfLines> sharedGroupsOfLines = new HashMap<>();

    private Line_VersionStructure line;

    private Map<String, Organisation_VersionStructure> sharedOrganisations = new HashMap<>();

    private Map<String, StopPlace> sharedStopPlaces = new HashMap<>();

    private Map<String, ScheduledStopPoint> sharedScheduledStopPoints = new HashMap<>();

    private Map<String, DestinationDisplay> sharedDestinationDisplays = new HashMap<>();

    private Map<String, PassengerStopAssignment> sharedStopAssignments = new HashMap<>();

    private Map<String, RoutePoint> sharedRoutePoints = new HashMap<>();

    private Map<String, ServiceLink> sharedServiceLinks = new HashMap<>();

    private List<Route> routes = new ArrayList<>();

    private List<RouteLink> routeLinks = new ArrayList<>();

    private List<JourneyPattern> journeyPatterns = new ArrayList<>();

    private List<ServiceJourney_VersionStructure> serviceJourneys = new ArrayList<>();

    private List<HeadwayJourneyGroup> headwayJourneys = new ArrayList<>();

    private Map<String, Notice> sharedNotices = new HashMap<>();

    private Map<String, Branding> sharedBrandings = new HashMap<>();

    private Set<NoticeAssignment> noticeAssignmentsTimetableFrame = new HashSet<>();

    private Set<NoticeAssignment> noticeAssignmentsServiceFrame = new HashSet<>();

    private Map<String, DayType> sharedDayTypes = new HashMap<>();

    private Set<DayTypeAssignment> sharedDayTypeAssignments = new HashSet<>();

    private Map<String, OperatingPeriod> sharedOperatingPeriods = new HashMap<>();

    private List<ServiceJourneyInterchange> serviceJourneyInterchanges = new ArrayList<>();

    private List<Direction> directions = new ArrayList<>();

    private List<ServiceJourneyPattern> serviceJourneyPatterns = new ArrayList<>();

    private Map<String, ScheduledStopPoint> scheduledStopPoints = new HashMap<>();

    private Map<String, DestinationDisplay> destinationDisplays = new HashMap<>();

    private Map<String, PassengerStopAssignment> stopAssignments = new HashMap<>();

    private List<ConnectionLink> connectionLinks = new ArrayList<>();

    private List<TrainNumber> trainNumbers = new ArrayList<>();

    public void clear() {
        lineCondition = null;
        line = null;
        routes.clear();
        routeLinks.clear();
        journeyPatterns.clear();
        serviceJourneys.clear();
        headwayJourneys.clear();
        noticeAssignmentsServiceFrame.clear();
        noticeAssignmentsTimetableFrame.clear();
        serviceJourneyInterchanges.clear();
        directions.clear();
        serviceJourneyPatterns.clear();
        stopAssignments.clear();
        scheduledStopPoints.clear();
        destinationDisplays.clear();
        connectionLinks.clear();
        trainNumbers.clear();
    }

    public void dispose() {
        clear();
        sharedDayTypes.clear();
        sharedDayTypeAssignments.clear();
        sharedOperatingPeriods.clear();
        commonCondition = null;
        sharedCodespaces.clear();
        sharedNetworks.clear();
        sharedGroupsOfLines.clear();
        sharedOrganisations.clear();
        sharedStopPlaces.clear();
        sharedNotices.clear();
        sharedStopAssignments.clear();
        sharedScheduledStopPoints.clear();
        sharedBrandings.clear();
        sharedRoutePoints.clear();
        sharedLines.clear();
    }

}
