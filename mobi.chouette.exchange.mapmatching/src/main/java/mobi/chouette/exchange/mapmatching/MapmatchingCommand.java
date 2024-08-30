package mobi.chouette.exchange.mapmatching;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.operation.linemerge.LineMerger;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Color;
import mobi.chouette.common.Constant;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.JourneyPatternDAO;
import mobi.chouette.dao.LineDAO;
import mobi.chouette.dao.ProfileOSRMJourneyPatternDAO;
import mobi.chouette.dao.RouteSectionDAO;
import mobi.chouette.dao.StopPointDAO;
import mobi.chouette.exchange.CommandCancelledException;
import mobi.chouette.exchange.ProgressionCommand;
import mobi.chouette.exchange.report.ActionReport;
import mobi.chouette.exchange.report.ActionReporter;
import mobi.chouette.exchange.report.ActionReporter.ERROR_CODE;
import mobi.chouette.exchange.report.MapMatchingReport;
import mobi.chouette.model.JourneyPattern;
import mobi.chouette.model.LatLngMapMatching;
import mobi.chouette.model.Line;
import mobi.chouette.model.OSRMProfile;
import mobi.chouette.model.ProfileOSRMInterStopJourneyPattern;
import mobi.chouette.model.ProfileOSRMJourneyPattern;
import mobi.chouette.model.RouteSection;
import mobi.chouette.model.ScheduledStopPoint;
import mobi.chouette.model.StopPoint;
import mobi.chouette.persistence.hibernate.ContextHolder;
import mobi.chouette.service.OSRMService;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.wololo.geojson.GeoJSON;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static mobi.chouette.service.OSRMService.GEOMETRY;
import static mobi.chouette.service.OSRMService.ROUTES;

@Log4j
@Stateless(name = MapmatchingCommand.COMMAND)
public class MapmatchingCommand implements Command, Constant {

    public static final String COMMAND = "MapmatchingCommand";

    @EJB
    LineDAO lineDAO;

    @EJB
    JourneyPatternDAO journeyPatternDAO;

    @EJB
    StopPointDAO stopPointDAO;

    @EJB
    RouteSectionDAO routeSectionDAO;

    @EJB
    OSRMService osrmService;

    @EJB
    ProfileOSRMJourneyPatternDAO profileOSRMJourneyPatternDAO;


    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean execute(Context context) throws Exception {
        boolean result = ERROR;
        Monitor monitor = MonitorFactory.start(COMMAND);

        InitialContext initialContext = (InitialContext) context.get(INITIAL_CONTEXT);
        ActionReport report = (ActionReport) context.get(REPORT);
        ProgressionCommand progression = (ProgressionCommand) CommandFactory.create(initialContext, ProgressionCommand.class.getName());
        ActionReporter reporter = ActionReporter.Factory.getInstance();
        MapMatchingReport mapMatchingReport = new MapMatchingReport();
        progression.initialize(context, 1);
        progression.execute(context);

        try {
            progression.start(context, 1);
            progression.execute(context);
            generateGeoJsonAllJourneyPatterns(mapMatchingReport);
            context.put(MAP_MATCHING_REPORT_FILE, mapMatchingReport);
            report.setResult("OK");
            progression.saveMapMatchingReport(context, true);
            log.info("Map matching completed");

            result = SUCCESS;
            progression.terminate(context, 1);
            progression.execute(context);
        } catch (CommandCancelledException e) {
            reporter.setActionError(context, ERROR_CODE.INTERNAL_ERROR, "Command cancelled");
            log.error(e.getMessage());
        } catch (Exception e) {
            reporter.setActionError(context, ERROR_CODE.INTERNAL_ERROR, "Fatal :" + e);
            log.error(e.getMessage(), e);
        } finally {
            progression.dispose(context);
            log.info(Color.YELLOW + monitor.stop() + Color.NORMAL);
        }

        return result;
    }

    public boolean generateGeoJsonAllJourneyPatterns(MapMatchingReport mapMatchingReport) {
        List<Line> lineList = lineDAO.findAll();
        for(Line line : lineList){
            log.info("------------- Start mapmatching line - " + line.getPublishedName() + " -------------- in schema : " + ContextHolder.getContext());
            List<JourneyPattern> journeyPatterns = journeyPatternDAO.getEnabledJourneyOfLine(line);
            for (JourneyPattern ojp : journeyPatterns) {
                generateAllGeoJsonForJourneyPatternsByLine(ojp, mapMatchingReport);
                mapMatchingReport.getRoutesOk().put(line.getObjectId(), ojp.getObjectId());
            }
            log.info("** Line done " + line.getPublishedName() + " **");
        }
        return true;
    }

    /**
     * Generate all GeoJSON by journey pattern
     */
    public void generateAllGeoJsonForJourneyPatternsByLine(JourneyPattern ojp, MapMatchingReport mapMatchingReport) {
        ojp = journeyPatternDAO.findByIdMapMatchingLazyDeps(ojp.getId());
        if (ojp.getGeojson() == null || ojp.getRouteSections().isEmpty()) {
            try {
                addNewMapMatching(ojp);
                journeyPatternDAO.flush();
                journeyPatternDAO.clear();
                log.info("** OJP done " + ojp.getName() + " **");
            } catch (Exception e) {
                mapMatchingReport.getRoutesError().put(ojp.getRoute().getLine().getObjectId(), ojp.getObjectId());
                log.error("Failed to get the trace of journey pattern : " + ojp.getName() + ", : " + ojp.getObjectId());
            }
        }
    }

    /**
     * Get the points in the journey pattern and create a default route.
     * @param journeyPattern
     * @return
     * @throws Exception
     */
    public JourneyPattern addNewMapMatching(JourneyPattern journeyPattern) throws Exception {
        if (journeyPattern.getGeojson() == null && !journeyPattern.getRouteSections().isEmpty()) {
            journeyPattern.setGeojson(journeyPattern.getGeoJSONFromRouteSections());
            return journeyPatternDAO.update(journeyPattern);
        } else {
            return addNewMapMatchingByPoints(journeyPattern, getPoints(journeyPattern));
        }
    }

    /**
     * Create a geoJson with points
     *
     * @param journeyPattern
     * @param points
     * @return
     * @throws Exception
     */
    public JourneyPattern addNewMapMatchingByPoints(JourneyPattern journeyPattern, List<LatLngMapMatching> points) throws Exception {
        GeoJSON geoJSON = loadSectionRoute(journeyPattern, points);

        journeyPattern = journeyPatternDAO.updateGeoJson(journeyPattern, geoJSON);

        return journeyPattern;
    }

    /**
     * Load for a journey pattern with a set of points composed of the stop points of the route,
     * no longer potentially manually configured deflection points,
     * the set of route sections specific to it for exporting GTFS shapeFiles.
     *
     * @param journeyPattern
     * @param listStopPoints
     * @return
     */
    public GeoJSON loadSectionRoute(JourneyPattern journeyPattern, List<LatLngMapMatching> listStopPoints) throws Exception {
        List<StopPoint> stopPoints = stopPointDAO.getStopPointsofJourneyPattern(journeyPattern);

        routeSectionDAO.deleteSectionUsedByJourneyPattern(journeyPattern);

        OSRMProfile osrmProfile = getOSRMProfile(journeyPattern);

        // full journey pattern osrm line: we use this as a reference for section lines
        List<JSONObject> osrmResponseMultiLines = fetchOsrmLinesWithTurnBack(listStopPoints, osrmProfile);

        // On utilise le LineMerger pour transformer l'ensemble des sous lignes en une seule ligne
        LineMerger lineMerger = new LineMerger();
        for (JSONObject osrmResponse : osrmResponseMultiLines) {
            lineMerger.add(parseOsrmJsonResponseToGeometry(osrmResponse));
        }

        // On récupère donc le tracé complet fusionné
        com.vividsolutions.jts.geom.LineString osrmJourneyPatternLine = (com.vividsolutions.jts.geom.LineString) lineMerger.getMergedLineStrings().iterator().next();

        // Enregistrer les informations sur le trajet (distance, temps, ...) et points intermédiaires
        ProfileOSRMJourneyPattern profile = addProfileJourneyPattern(
                journeyPattern,
                osrmProfile,
                osrmResponseMultiLines
        );


        List<List<LatLngMapMatching>> listOfSectionStopPoints = new ArrayList<>();
        for (int i = 0; i < listStopPoints.size(); i++) {
            if (i < listStopPoints.size() - 1) {
                List<LatLngMapMatching> list = new ArrayList<>();
                LatLngMapMatching departureStopPoint = listStopPoints.get(i);
                list.add(departureStopPoint);

                // On cherche le prochain latlngMapMatching avec un stopPoint
                int j = i + 1;
                while (j < listStopPoints.size() && listStopPoints.get(j).getStopPoint() == null) {
                    list.add(listStopPoints.get(j));
                    j++;
                }

                LatLngMapMatching arrivalStopPoint = listStopPoints.get(j);
                list.add(arrivalStopPoint);

                listOfSectionStopPoints.add(list);

                // On place i au dernier StopPoint rencontré -1 pour le prendre comme départ a la prochaine itération.
                i = j - 1;
            }
        }

        for (List<LatLngMapMatching> sectionStopPoint : listOfSectionStopPoints) {
            JSONObject osrmResponseSection = osrmService.getRoute(osrmProfile, sectionStopPoint);

            com.vividsolutions.jts.geom.LineString osrmSectionLine = parseOsrmJsonResponseToGeometry(osrmResponseSection);

            // force the section line to be "consistent" with the JP line
            try {
                osrmSectionLine = correctSectionLineWithJourneyPatternLine(osrmSectionLine, osrmJourneyPatternLine);
            } catch (Exception e) {
                log.error(e.getMessage());
            }

            LatLngMapMatching departureStopPoint = sectionStopPoint.get(0);
            LatLngMapMatching arrivalStopPoint = sectionStopPoint.get(sectionStopPoint.size() - 1);

            // Récupération des StopPoint correspondants au points choisi plus haut
            StopPoint stopPointDeparture = stopPoints.stream()
                    .filter(sp -> sp.getId().equals(departureStopPoint.getStopPoint().getId()))
                    .findAny()
                    .orElseThrow(() -> new Exception("StopPoint not found in the list of points provided."));

            StopPoint stopPointArrival = stopPoints.stream()
                    .filter(sp -> sp.getId().equals(arrivalStopPoint.getStopPoint().getId()))
                    .findAny()
                    .orElseThrow(() -> new Exception("StopPoint not found in the list of points provided."));

            int indexStart = listStopPoints.indexOf(departureStopPoint);
            int indexNext = listStopPoints.indexOf(arrivalStopPoint);

            RouteSection routeSection = new RouteSection();
            if (OSRMProfile.AIR.equals(osrmProfile) || OSRMProfile.FERRY.equals(osrmProfile)){
                try {
                    routeSection.setDistance(BigDecimal.valueOf(osrmResponseSection.getJSONArray(ROUTES).getJSONObject(0).getDouble("distance")));
                } catch (JSONException e) {
                    log.error("Error while setting distance for air section",e);
                }

            }else{
                // Inscription en base des interStop
                ProfileOSRMInterStopJourneyPattern profileInterStopJP = getInterStopInformations(
                        osrmResponseMultiLines,
                        // On refait la requête OSRM entre les deux arrêts choisis pour déterminer le geoJson correspondant
                        stopPointDeparture,
                        stopPointArrival,
                        indexStart,
                        indexNext,
                        profile.getProfileOSRMInterStopJourneyPatternList()
                );
                profile.addProfileInterStop(profileInterStopJP);
                routeSection.setDistance(BigDecimal.valueOf(profileInterStopJP.getDistance()));
            }

            profileOSRMJourneyPatternDAO.update(profile);

            routeSection.setDeparture(stopPointDeparture.getScheduledStopPoint().getContainedInStopAreaRef().getObject());
            routeSection.setArrival(stopPointArrival.getScheduledStopPoint().getContainedInStopAreaRef().getObject());
            routeSection.setInputGeometry(osrmSectionLine);
            routeSection.setProcessedGeometry(osrmSectionLine);
            // "Fais moi confiance Hibernate, cet objet avec cet id existe". Ainsi Hibernate chargea l'objet de manière ultralight, juste avec son id
            ScheduledStopPoint scheduledStopPoint1 = routeSectionDAO.getEm().getReference(ScheduledStopPoint.class, stopPointDeparture.getScheduledStopPoint().getId());
            ScheduledStopPoint scheduledStopPoint2 = routeSectionDAO.getEm().getReference(ScheduledStopPoint.class, stopPointArrival.getScheduledStopPoint().getId());
            routeSection.setFromScheduledStopPoint(scheduledStopPoint1);
            routeSection.setToScheduledStopPoint(scheduledStopPoint2);

            routeSection.setNoProcessing(true);
            routeSection.setObjectId(String.valueOf(System.currentTimeMillis()));

            routeSectionDAO.create(routeSection);

            routeSection.setObjectId(journeyPattern.getRoute().getLine().getNetwork().getName() + ":RouteSection:" + routeSection.getId());

            routeSectionDAO.update(routeSection);
            journeyPattern.getRouteSections().add(routeSection);
        }

        journeyPatternDAO.update(journeyPattern);
        return journeyPattern.getGeoJSONFromRouteSections();
    }

    /**
     * Get the location of journey pattern points or return the list of intermediate points.
     *
     * @param journeyPattern
     * @return
     */
    protected List<LatLngMapMatching> getPoints(JourneyPattern journeyPattern) {
        if (journeyPattern.getLatLngMapMatching().isEmpty()) {
            return stopPointDAO.getStopPointsofJourneyPattern(journeyPattern)
                    .stream()
                    .map(LatLngMapMatching::new)
                    .collect(toList());
        } else {
            return journeyPattern.getLatLngMapMatching();
        }
    }

    /**
     * Add journey information to the OSRM profile journey pattern.
     *
     * @param journeyPattern
     * @param profile
     * @param json
     *
     * @return
     */
    private ProfileOSRMJourneyPattern addProfileJourneyPattern(JourneyPattern journeyPattern, OSRMProfile profile, List<JSONObject> json) throws Exception {
        // S'il existe un élément, on le supprime
        ProfileOSRMJourneyPattern profileJP = journeyPattern.getProfileOSRMJourneyPatterns()
                .stream()
                .findFirst()
                // On créé un nouvel élément s'il n'existe pas
                .orElseGet(() -> {
                    ProfileOSRMJourneyPattern pojp = new ProfileOSRMJourneyPattern();
                    profileOSRMJourneyPatternDAO.create(pojp);
                    pojp.setjourneyPattern(journeyPattern);
                    return pojp;
                });

        profileJP.setProfile(profile);
        try {
            double distance = 0;
            double duration = 0;
            JSONArray array = new JSONArray();
            for (JSONObject obj : json) {
                distance += obj.getJSONArray(ROUTES).getJSONObject(0).getDouble("distance");
                duration += obj.getJSONArray(ROUTES).getJSONObject(0).getDouble("duration");
                array.put(obj);
            }

            profileJP.setDistance(distance);
            profileJP.setDuration(duration);
        } catch (JSONException e) {
            String motif = "Unable to get the distance or time contained in the OSRM response.";
            log.error(motif, e);
            throw new Exception(motif);
        }
        profileJP = profileOSRMJourneyPatternDAO.update(profileJP);

        return profileJP;
    }

    private OSRMProfile getOSRMProfile(JourneyPattern journeyPattern) {
        switch (journeyPattern.getRoute().getLine().getTransportModeName()) {
            case Tram:
                return OSRMProfile.TRAM;
            case Rail:
                return OSRMProfile.RAIL;
            case Bus:
            case Coach:
                return OSRMProfile.BUS;
            case Air:
            case Funicular:
                return OSRMProfile.AIR;
            case Ferry:
            case Water:
                return OSRMProfile.FERRY;
            default:
                return OSRMProfile.DRIVING;
        }
    }

    /**
     * Returns the list of GeoJson corresponding to the points provided.
     * If points are marked in turnBack = true, a second calculation is made from this point to force a U-turn on the spot.
     *
     * @param stopPoints
     * @param osrmProfile
     *
     * @return
     *
     * @throws Exception
     */
    private List<JSONObject> fetchOsrmLinesWithTurnBack(List<LatLngMapMatching> stopPoints, OSRMProfile osrmProfile) throws Exception {
        List<List<LatLngMapMatching>> listToFetch = new ArrayList<>();
        List<LatLngMapMatching> currentList = new ArrayList<>();
        for (LatLngMapMatching point : stopPoints) {
            currentList.add(point);
            // Si demi-tour a ce point
            if (point.isTurnBack()) {
                listToFetch.add(currentList);
                // On sauvegarde la précédente liste et on en démarre une nouvelle
                currentList = new ArrayList<>();
                currentList.add(point);
            }
        }
        // Ajout de la dernière liste de point construite
        listToFetch.add(currentList);

        List<JSONObject> result = new ArrayList<>();
        for (List<LatLngMapMatching> list : listToFetch) {
            result.add(osrmService.getRoute(osrmProfile, list));
        }

        return result;
    }

    private com.vividsolutions.jts.geom.LineString parseOsrmJsonResponseToGeometry(JSONObject json) throws Exception {
        // Trouver l'élément Geometry
        try {
            return osrmService.getLineStringFromOSRM(json.getJSONArray(ROUTES).getJSONObject(0).getJSONObject(GEOMETRY));
        } catch (JSONException e) {
            String motif = "Failed to get the GeoJson in the OSRM response.";
            log.error(motif,e);
            throw new Exception(motif);
        }
    }

    /**
     * Corrects section line coords to match corresponding journey pattern (JP) line coords
     * In certain cases, the section last stop point is not reached through the same path on the section line and on the JP line,
     * depending on the JP stop points following the section
     * We need the section line coords to be strictly consistent with the full JP line coords, so no path incoherence can exist between the MapMatching path(built using a single JP osrm line) and the definitive JP path
     * built out of the section lines merge
     * cf redmine: #6329
     * @param sectionLine OSRM section line
     * @param jpLine OSRM JourneyPattern line
     * @return
     */
    private com.vividsolutions.jts.geom.LineString correctSectionLineWithJourneyPatternLine(com.vividsolutions.jts.geom.LineString sectionLine, com.vividsolutions.jts.geom.LineString jpLine) throws Exception {
        Coordinate[] sectionCoords = sectionLine.getCoordinates();
        Coordinate[] jpCoords = jpLine.getCoordinates();

        int jpIndexMatchingSectionStart = findClosestJPCoordinateIndex(jpCoords, sectionCoords, 0, 0, 10)
                .orElseThrow(() ->
                        new Exception("Parsing OSRM results: Section and Journey Pattern path are incompatible")
                );

        int jpIndexMatchingSectionEnd = findClosestJPCoordinateIndex(jpCoords, sectionCoords, sectionCoords.length - 1, jpIndexMatchingSectionStart + 1 , 10)
                .orElseThrow(() ->
                        new Exception("Parsing OSRM results: Section and Journey Pattern path are incompatible")
                );

        Coordinate[] correctedCoords = IntStream.rangeClosed(jpIndexMatchingSectionStart, jpIndexMatchingSectionEnd).mapToObj(i -> jpCoords[i]).toArray(Coordinate[]::new);
        CoordinateSequence correctedCoordsSeq = sectionLine.getFactory().getCoordinateSequenceFactory().create(correctedCoords);
        return new com.vividsolutions.jts.geom.LineString(correctedCoordsSeq, sectionLine.getFactory());
    }

    /**
     * Finds the coordinate of the JP which is equal or closest to the coordinate of the section specified by the index sectionCoordToMatchIndex.
     *
     * @param jpCoords                 Coordinates of JP
     * @param sectionCoords            Coordinates of section
     * @param sectionCoordToMatchIndex Index of the coordinate of the section to match
     * @param jpSearchStartIndex       Starting index for searching in JP coordinates
     * @param maxTrials                Max number of attempts on close coordinates
     *
     * @return
     */
    private OptionalInt findClosestJPCoordinateIndex(Coordinate[] jpCoords, Coordinate[] sectionCoords, int sectionCoordToMatchIndex, int jpSearchStartIndex, int maxTrials) {
        Coordinate coordToMatch = sectionCoords[sectionCoordToMatchIndex];
        OptionalInt index = IntStream.range(jpSearchStartIndex, jpCoords.length).filter(i -> jpCoords[i].equals(coordToMatch)).findFirst();

        if (!index.isPresent()) {
            double currentBestDistance = -1;
            for (int i = jpSearchStartIndex; i < jpCoords.length; i++) {
                if (currentBestDistance < 0 || currentBestDistance > coordToMatch.distance(jpCoords[i])) {
                    currentBestDistance = coordToMatch.distance(jpCoords[i]);
                    index = OptionalInt.of(i);
                }
            }
        }
        return index;
    }

    /**
     * Build a InterStopPointProfile
     *
     * @param mainGeoJsonList           Complete response of route OSRM
     * @param departureStop             Indicates the starting StopPoint of the profile
     * @param arrivalStop               Indicates the end StopPoint of the profile
     * @param sectionIndexStart         Indicates the start identifier of the section in the legs of the full route OSRM response
     * @param sectionIndexEnd           Indicates the end identifier of the section in the legs of the full route OSRM response
     *
     * @throws Exception
     */
    private ProfileOSRMInterStopJourneyPattern getInterStopInformations(List<JSONObject> mainGeoJsonList,
                                                                        StopPoint departureStop,
                                                                        StopPoint arrivalStop,
                                                                        int sectionIndexStart,
                                                                        int sectionIndexEnd,
                                                                        List<ProfileOSRMInterStopJourneyPattern> existingListOnProfile) throws Exception {

        // S'il existe un élément, on le supprime
        ProfileOSRMInterStopJourneyPattern profileInterStopJP = existingListOnProfile
                .stream()
                .filter(interStop -> Objects.equals(interStop.getDepartureStopPoint(), departureStop) && Objects.equals(interStop.getArrivalStopPoint(), arrivalStop))
                .findFirst()
                // On créé un nouvel élément s'il n'existe pas
                .orElseGet(() -> {
                    ProfileOSRMInterStopJourneyPattern profile = new ProfileOSRMInterStopJourneyPattern();
                    profile.setDepartureStopPoint(departureStop);
                    profile.setArrivalStopPoint(arrivalStop);
                    return profile;
                });

        try {
            JSONArray legs = new JSONArray();

            // Récupération de l'objet JSON correspondant à la réponse complète sur l'ensemble de l'itinéraire
            for (JSONObject partOfGeojson : mainGeoJsonList) {
                // Récupération du tableau des "legs" correspondant a la description du tracé entre chaque point
                JSONArray currentLegs = partOfGeojson.getJSONArray(ROUTES).getJSONObject(0).getJSONArray("legs");
                for (int i = 0; i < currentLegs.length(); i++) {
                    legs.put(currentLegs.getJSONObject(i));
                }
            }

            // On recherche la sous liste correspondant au sectionIndexStart et sectionIndexEnd
            List<JSONObject> listLegs = new ArrayList<>();
            if (sectionIndexStart == sectionIndexEnd) {
                listLegs.add(legs.getJSONObject(sectionIndexStart));
            } else {
                for (int i = sectionIndexStart; i < sectionIndexEnd; i++) {
                    listLegs.add(legs.getJSONObject(i));
                }
            }

            double distance = 0;
            double duration = 0;

            // On somme la distance et la durée de la sous liste
            for (JSONObject leg : listLegs) {
                distance += leg.getDouble("distance");
                duration += leg.getDouble("duration");
            }

            profileInterStopJP.setDistance(distance);
            profileInterStopJP.setDuration(duration);

        } catch (JSONException e) {
            String motif = "Unable to get the distance or time contained in the OSRM response.";
            log.error(motif, e);
            throw new Exception(motif);
        }

        return profileInterStopJP;
    }


    public static class DefaultCommandFactory extends CommandFactory {

        @Override
        protected Command create(InitialContext context) {
            Command result = null;
            try {
                String name = "java:app/mobi.chouette.exchange.mapmatching/" + COMMAND;
                result = (Command) context.lookup(name);
            } catch (NamingException e) {
                // try another way on test context
                String name = "java:module/" + COMMAND;
                try {
                    result = (Command) context.lookup(name);
                } catch (NamingException e1) {
                    log.error(e);
                }
            }
            return result;
        }
    }

    static {
        CommandFactory.factories.put(MapmatchingCommand.class.getName(), new DefaultCommandFactory());
    }
}
