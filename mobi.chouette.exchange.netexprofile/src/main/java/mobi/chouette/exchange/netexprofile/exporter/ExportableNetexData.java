package mobi.chouette.exchange.netexprofile.exporter;

import lombok.Getter;
import lombok.Setter;
import mobi.chouette.model.ConnectionLink;
import org.rutebanken.netex.model.*;

import java.util.*;

public class ExportableNetexData {

    @Getter
    @Setter
    private AvailabilityCondition commonCondition;

    @Getter
    @Setter
    private AvailabilityCondition lineCondition;

    @Getter
    @Setter
    private Map<String, Codespace> sharedCodespaces = new HashMap<>();

    @Getter
    @Setter
    private Map<String, Network> sharedNetworks = new HashMap<>();

    @Getter
    @Setter
    private Map<String, Line_VersionStructure> sharedLines = new HashMap<>();

    @Getter
    @Setter
    private Map<String, GroupOfLines> sharedGroupsOfLines = new HashMap<>();

    @Getter
    @Setter
    private Line_VersionStructure line;

    @Getter
    @Setter
    private Map<String, Organisation_VersionStructure> sharedOrganisations = new HashMap<>();

    @Getter
    @Setter
    private Map<String, StopPlace> sharedStopPlaces = new HashMap<>();

    @Getter
    @Setter
    private Map<String, ScheduledStopPoint> sharedScheduledStopPoints = new HashMap<>();

    @Getter
    @Setter
    private Map<String, DestinationDisplay> sharedDestinationDisplays = new HashMap<>();

    @Getter
    @Setter
    private Map<String, PassengerStopAssignment> sharedStopAssignments = new HashMap<>();

    @Getter
    @Setter
    private Map<String, RoutePoint> sharedRoutePoints = new HashMap<>();

    @Getter
    @Setter
    private Map<String, ServiceLink> sharedServiceLinks = new HashMap<>();

    @Getter
    @Setter
    private List<Route> routes = new ArrayList<>();

    @Getter
    @Setter
    private List<RouteLink> routeLinks = new ArrayList<>();

    @Getter
    @Setter
    private List<JourneyPattern> journeyPatterns = new ArrayList<>();

    @Getter
    @Setter
    private List<ServiceJourney_VersionStructure> serviceJourneys = new ArrayList<>();

    @Getter
    @Setter
    private List<HeadwayJourneyGroup> headwayJourneys = new ArrayList<>();

    @Getter
    @Setter
    private List<DeadRun> deadRuns = new ArrayList<>();

    @Getter
    @Setter
    private List<DatedServiceJourney> datedServiceJourneys = new ArrayList<>();

    @Getter
    @Setter
    private List<Block> blocks = new ArrayList<>();

    @Getter
    @Setter
    private Map<String,Notice> sharedNotices = new HashMap<>();

    @Getter
    @Setter
    private Map<String,Branding> sharedBrandings = new HashMap<>();

    @Getter
    @Setter
    private Set<NoticeAssignment> noticeAssignmentsTimetableFrame = new HashSet<>();

    @Getter
    @Setter
    private Set<NoticeAssignment> noticeAssignmentsServiceFrame = new HashSet<>();

    @Getter
    @Setter
    private Map<String,DayType> sharedDayTypes = new HashMap<>();

    @Getter
    @Setter
    private Set<DayTypeAssignment> sharedDayTypeAssignments = new HashSet<>();

    @Getter
    @Setter
    private Map<String,OperatingPeriod> sharedOperatingPeriods = new HashMap<>();

    @Getter
    @Setter
    private Map<String, OperatingDay> sharedOperatingDays = new HashMap<>();

    @Getter
    @Setter
    private List<ServiceJourneyInterchange> serviceJourneyInterchanges = new ArrayList<>();

    @Getter
    @Setter
    private List<Direction> directions = new ArrayList<>();

    @Getter
    @Setter
    private List<ServiceJourneyPattern> serviceJourneyPatterns = new ArrayList<>();

    @Getter
    @Setter
    private Map<String, ScheduledStopPoint> scheduledStopPoints = new HashMap<>();

    @Getter
    @Setter
    private Map<String, DestinationDisplay> destinationDisplays = new HashMap<>();

    @Getter
    @Setter
    private Map<String, PassengerStopAssignment> stopAssignments = new HashMap<>();

    @Getter
    @Setter
    private List<ConnectionLink> connectionLinks = new ArrayList<>();


    public void clear() {
        lineCondition = null;
        line = null;
        routes.clear();
        routeLinks.clear();
        journeyPatterns.clear();
        serviceJourneys.clear();
        headwayJourneys.clear();
        datedServiceJourneys.clear();
        noticeAssignmentsServiceFrame.clear();
        noticeAssignmentsTimetableFrame.clear();
        serviceJourneyInterchanges.clear();
        directions.clear();
        serviceJourneyPatterns.clear();
        stopAssignments.clear();
        scheduledStopPoints.clear();
        destinationDisplays.clear();
        connectionLinks.clear();
    }

    public void dispose() {
        clear();
        sharedDayTypes.clear();
        sharedDayTypeAssignments.clear();
        sharedOperatingPeriods.clear();
        sharedOperatingDays.clear();
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
