package mobi.chouette.exchange.importer;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Color;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.ProviderDAO;
import mobi.chouette.dao.RouteDAO;
import mobi.chouette.dao.RoutePointDAO;
import mobi.chouette.model.JourneyPattern;
import mobi.chouette.model.Provider;
import mobi.chouette.model.Route;
import mobi.chouette.model.RoutePoint;
import mobi.chouette.model.StopPoint;
import mobi.chouette.model.VehicleJourney;
import mobi.chouette.model.VehicleJourneyAtStop;
import mobi.chouette.model.type.PTDirectionEnum;
import mobi.chouette.persistence.hibernate.ContextHolder;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Log4j
@Stateless(name = RouteMergerCommand.COMMAND)
public class RouteMergerCommand implements Command {

	public static final String COMMAND = "RouteMergerCommand";


	@EJB
	private RouteDAO routeDAO;

	@EJB
	private RoutePointDAO routePointDAO;

	@EJB
	private ProviderDAO providerDAO;

	private Map<Long, Set<PTDirectionEnum>> lineDirections = new HashMap<>();

	public static final Comparator<StopPoint> STOP_POINT_POSITION_COMPARATOR = new Comparator<StopPoint>() {
		@Override
		public int compare(StopPoint sp1, StopPoint sp2) {
			return Integer.compare(sp1.getPosition(), sp2.getPosition());
		}
	};


	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public boolean execute(Context context) throws Exception {


		Monitor monitor = MonitorFactory.start(COMMAND);

		Optional<Provider> provider = providerDAO.findBySchema(ContextHolder.getContext());

		if (provider.isPresent() && provider.get().isIdfm()){
			buildLineDirectionsMap();

			for (Map.Entry<Long, Set<PTDirectionEnum>> lineDirectionEntry : lineDirections.entrySet()) {
				for (PTDirectionEnum ptDirectionEnum : lineDirectionEntry.getValue()) {
					launchMergeForLineAndDirection(lineDirectionEntry.getKey(), ptDirectionEnum);
				}
			}

			log.info(Color.MAGENTA + monitor.stop() + Color.NORMAL);
		}

		else{
			provider.ifPresent(value -> log.info("Filiale non IDFM : " + value.getSchemaName() + " route merge non effectué"));
		}

		return SUCCESS;
	}

	/**
	 * Execute a loop on all routes of a specific line/direction to check and merge route that can be merged
	 * The loop is executed as long as merge happens and stops when no merge could be done.
	 * @param lineId
	 * 		Id of the line on which merges must be done
	 * @param ptDirectionEnum
	 * 		Direction on which merges must be done
	 */
	private void launchMergeForLineAndDirection(Long lineId, PTDirectionEnum ptDirectionEnum) {

		//1st pass : trying to merge routes that have at least one StopPoint in common
		boolean hasMergeHappened = true;
		while(hasMergeHappened){
			hasMergeHappened = mergeLineAndDirection(lineId,ptDirectionEnum, false);
		}

		//second pass allow merge for route that have no point in common
		hasMergeHappened = true;
		while(hasMergeHappened){
			hasMergeHappened = mergeLineAndDirection(lineId,ptDirectionEnum, true);
		}
	}


	/**
	 * Compares each route to other routes with same line/direction and merge them if possible.
	 * The function exits after the first merge is done
	 * @param lineId
	 * 	id of the line on which merge must be done
	 * @param ptDirectionEnum
	 * 	direction on which merge must be done
	 * @param allowTotallyDistinctRoute
	 * 	false : only route that have at least one point in common are merged
	 * 	true: routes that have no point in common can be merged
	 * @return
	 */
	private boolean mergeLineAndDirection(Long lineId, PTDirectionEnum ptDirectionEnum, boolean allowTotallyDistinctRoute) {

		log.info(" mergeLineAndDirection - " + lineId + " - " + ptDirectionEnum.name());

		List<Route> routes = routeDAO.findByLineIdAndDirection(lineId, ptDirectionEnum);
		for (Route currentRouteTryingToMerge : routes) {


			//collecting all routes different than the current one
			List<Route> otherRoutes = routes.stream()
					.filter(route -> route != currentRouteTryingToMerge)
					.collect(Collectors.toList());

			for (Route otherRoute : otherRoutes) {
				// looping on each other route and trying to merge the currentRoute with one of any other route


				if (!allowTotallyDistinctRoute && !havePointInCommon(currentRouteTryingToMerge, otherRoute)){
					// routes have absolutely no point in common and we don't want them to merge. Going to the next route
					continue;
				}

				if (checkAndMergeIfPossible(currentRouteTryingToMerge, otherRoute)){
					// a merge has been done for the current line/direction. Exiting the process on this loop.
					// (to avoid working on old data that have already been merged)
					return true;
				}
			}
		}

		// no merge has been done for this line/direction
		return false;

	}

	/**
	 * Check if 2 routes have at least one point in common
	 * @param route1
	 * @param route2
	 * @return
	 * 	true : at least one point is used by route1 and route2
	 * 	false : the routes have no points in common
	 */
	private boolean havePointInCommon(Route route1, Route route2) {

		for (StopPoint stopPointRoute1 : route1.getStopPoints()) {
			for (StopPoint stopPointRoute2 : route2.getStopPoints()) {
				String stopAreaRoute1 = stopPointRoute1.getScheduledStopPoint().getContainedInStopAreaRef().getObjectId();
				String stopAreaRoute2 = stopPointRoute2.getScheduledStopPoint().getContainedInStopAreaRef().getObjectId();

				if (stopAreaRoute1.equals(stopAreaRoute2)){
					return true;
				}
			}
		}
		return false;
	}


	/**
	 * Check if a route can be merged into another route. If
	 * @param currentRouteTryingToMerge
	 * @param otherRoute
	 * @return
	 */
	private boolean checkAndMergeIfPossible(Route currentRouteTryingToMerge, Route otherRoute) {

		if (areRoutesCompatibles(otherRoute, currentRouteTryingToMerge)){
			mergeRoutes(otherRoute, currentRouteTryingToMerge);
			completeRouteInformations(currentRouteTryingToMerge);
			log.info(" checkAndMergeIfPossible - merge done from: " + otherRoute.getObjectId() + " - " + currentRouteTryingToMerge.getObjectId());
			// a merge has been done. Loop can be stopped
			return true;
		}

		if (areRoutesCompatibles(currentRouteTryingToMerge, otherRoute)){
			mergeRoutes(currentRouteTryingToMerge, otherRoute);
			completeRouteInformations(otherRoute);
			log.info(" checkAndMergeIfPossible - merge done from : " + currentRouteTryingToMerge.getObjectId() + " - " + otherRoute.getObjectId());
			// a merge has been done. Loop can be stopped
			return true;
		}

		log.info(" checkAndMergeIfPossible - no merge done" + currentRouteTryingToMerge.getObjectId() + " - " + otherRoute.getObjectId());
		//no merge has been done
		return false;

	}


	/**
	 * Merge points from fromRoute into destinationRoute
	 * Update points in journeyPatterns
	 * @param fromRoute
	 * 		the route from which points must be moved to destinationRoute, and then be deleted
	 * @param destinationRoute
	 * 		the route that will hold all data from the 2 routes
	 */
	private void mergeRoutes(Route fromRoute, Route destinationRoute) {
		log.info("lineId :"+fromRoute.getLine().getId() + " direction:" + fromRoute.getDirection().toString());
		log.info("Merging route : " + generateRoutePathLog(fromRoute));
		log.info("into route : " + generateRoutePathLog(destinationRoute));

		// creating a new list to avoid dirty data issues when modifing the route we are looping on
		List<StopPoint> pointListFromRoute = new ArrayList<>(fromRoute.getStopPoints());

		Integer previousPosition = null;


		for (StopPoint currentFromStopPoint : pointListFromRoute) {
			List<String> fromSuccessors = getSuccessors(fromRoute, currentFromStopPoint.getPosition());
			Optional<StopPoint> toStopPointOpt = findStopPointInRoute(destinationRoute, currentFromStopPoint.getScheduledStopPoint().getContainedInStopAreaRef().getObjectId(), fromSuccessors, previousPosition);
			StopPoint toStopPoint;


			if (toStopPointOpt.isPresent()){
				toStopPoint = toStopPointOpt.get();
				replaceStopPoint(fromRoute, currentFromStopPoint, toStopPoint);
				previousPosition = toStopPoint.getPosition();
			}else{
				int targetPosition = previousPosition == null ? 0 : previousPosition + 1;
				shiftSuccessors(destinationRoute, targetPosition);

				insertPointInRoute(destinationRoute, currentFromStopPoint, targetPosition);
				previousPosition = targetPosition;
			}
		}


		// After all stop points have been replaced in journey patterns of fromRoute, all journey patterns can be moved to destinationRoute

		List<JourneyPattern> journeyPatternsToMove = new ArrayList<>(fromRoute.getJourneyPatterns());

		journeyPatternsToMove.forEach(journeyPattern -> moveJourneyPatternToRoute(journeyPattern, destinationRoute));

		destinationRoute.getStopPoints().sort(STOP_POINT_POSITION_COMPARATOR);
		log.info("new route : " + generateRoutePathLog(destinationRoute));

		fromRoute.getStopPoints().clear();
		fromRoute.getRoutePoints().clear();
		fromRoute.getJourneyPatterns().clear();
		routeDAO.delete(fromRoute);
	}


	/**
	 * Move a journeyPattern to a new route
	 * also update route association in vehicleJourneys
	 * @param journeyPattern
	 * 	the journey pattern to update
	 * @param newRoute
	 * 	the new route on which the journey pattern must be associated
	 */
	private void moveJourneyPatternToRoute(JourneyPattern journeyPattern, Route newRoute){

		journeyPattern.setRoute(newRoute);
		for (VehicleJourney vehicleJourney : journeyPattern.getVehicleJourneys()) {
			vehicleJourney.setRoute(newRoute);
		}
	}


	/**
	 * Generates a string that represent the path
	 * @param route
	 * 	the route for which the
	 * @return
	 * 	a string with all stops (A-B-C)
	 */
	private String generateRoutePathLog(Route route){
		StringBuilder tmpPath = new StringBuilder();
		for (StopPoint stopPoint : route.getStopPoints()) {
			tmpPath.append(stopPoint.getScheduledStopPoint().getContainedInStopAreaRef().getObject().getName() + ",");
		}
		String completePath = tmpPath.toString();
		if (completePath.endsWith(",")){
			completePath = completePath.substring(0,completePath.length() -1);
		}

		return completePath;
	}

	/**
	 * Execute a shift for all stopPoints located ON OR AFTER newPointPosition
	 * e.g. : Route : A(0)-B(1)-C(2)
	 * 			After shifting successors with newPointPosition=1 => A(0)-B(2)-C(3)
	 * @param route
	 * 		route on which the shift must be made
	 * @param newPointPosition
	 * 		the starting position from which the shift must be made
	 */
	private void shiftSuccessors(Route route, int newPointPosition){
		route.getStopPoints().stream()
				.filter(stopPoint -> stopPoint.getPosition() >= newPointPosition)
				.forEach(stopPoint -> stopPoint.setPosition(stopPoint.getPosition() + 1));

	}

	/**
	 * Insert a point in a route, on a given position
	 * @param destinationRoute
	 * 	the route on which the insertion must be done
	 * @param stopPointToMove
	 * 	the stop point to insert
	 * @param targetPositionInDestinationRoute
	 * 	the position on which the stopPoint must be inserted
	 */
	private void insertPointInRoute(Route destinationRoute, StopPoint stopPointToMove, Integer targetPositionInDestinationRoute) {
		stopPointToMove.setPosition(targetPositionInDestinationRoute);
		stopPointToMove.setRoute(destinationRoute);
	}

	/**
	 * Replace a stopPoint by another, on each journey patterns of a route
	 * @param route
	 * 	the route on which the replacement  must be done
	 * @param oldStopPoint
	 * 	the stopPoint to replace
	 * @param newStopPoint
	 * 	the new stopPoint
	 */
	private void replaceStopPoint(Route route, StopPoint oldStopPoint, StopPoint newStopPoint) {
		route.getJourneyPatterns().forEach(journeyPattern -> replaceStopPoint(journeyPattern, oldStopPoint, newStopPoint));
	}

	/**
	 * Replace a stopPoint by another, in a journey pattern
	 * @param journeyPattern
	 * 	the journeyPattern on which the replacement must be done
	 * @param oldStopPoint
	 * 	the stopPoint to replace
	 * @param newStopPoint
	 * 	the new stopPoint
	 */
	private void replaceStopPoint(JourneyPattern journeyPattern, StopPoint oldStopPoint, StopPoint newStopPoint) {

		List<StopPoint> resultList = new ArrayList<>();

		for (StopPoint stopPoint : journeyPattern.getStopPoints()) {
			if (stopPoint.equals(oldStopPoint)){
				resultList.add(newStopPoint);
			}else{
				resultList.add(stopPoint);
			}
		}
		journeyPattern.getStopPoints().clear();
		journeyPattern.getStopPoints().addAll(resultList);

		if (journeyPattern.getDepartureStopPoint().equals(oldStopPoint)){
			journeyPattern.setDepartureStopPoint(newStopPoint);
		}

		if (journeyPattern.getArrivalStopPoint().equals(oldStopPoint)){
			journeyPattern.setArrivalStopPoint(newStopPoint);
		}

		replaceStopPointInVehicleJourneyAtStops(journeyPattern, oldStopPoint, newStopPoint);
	}

	/***
	 * Replace old stop point by new stop point, in vehicleJourneyAt stop
	 * @param journeyPattern
	 * @param oldStopPoint
	 * @param newStopPoint
	 */
	private void replaceStopPointInVehicleJourneyAtStops(JourneyPattern journeyPattern, StopPoint oldStopPoint, StopPoint newStopPoint){

		for (VehicleJourney vehicleJourney : journeyPattern.getVehicleJourneys()) {
			for (VehicleJourneyAtStop vehicleJourneyAtStop : vehicleJourney.getVehicleJourneyAtStops()) {
				if (vehicleJourneyAtStop.getStopPoint().equals(oldStopPoint)){
					vehicleJourneyAtStop.setStopPoint(newStopPoint);
				}
			}
		}
	}


	/**
	 * Search a point in route
	 * recovered point must match the following conditions:
	 * - stopAreaId of the stopPoint must match stopAreaId given as parameter
	 * - all successors of the point must be either not existing or after the stopPoint
	 * @param route
	 * 	the route to read
	 * @param stopAreaId
	 * 	the stopArea id that must match with stopPoint stop area id
	 * @param successors
	 * 	the successors that must be located after the stopPoint (or not existing)
	 * @param previousPosition
	 * 	 position of the previous point
	 * @return
	 */
	private Optional<StopPoint> findStopPointInRoute(Route route, String stopAreaId, List<String> successors, Integer previousPosition){

		for (StopPoint destinationStopPoint : route.getStopPoints()) {
			String destinationStopArea = destinationStopPoint.getScheduledStopPoint().getContainedInStopAreaRef().getObjectId();
			if (!destinationStopArea.equals(stopAreaId) || (previousPosition != null && previousPosition > destinationStopPoint.getPosition())){
				// the current stop point in destination route, does not match the searched stopAreaId. Moving to next one
				// OR
				// the current stop point is before the previously inserted stopPoint. we need to find another one
				continue;
			}


			if (checkSuccessors(route,destinationStopPoint.getPosition(), successors)){
				// the current stop point in destination route matches with stopAreaId, and all successors from fromRoute are located after. It is the good one
				return Optional.of(destinationStopPoint);
			}
		}

		// no stop point has been found matching the constraints
		return Optional.empty();
	}


	/**
	 * Check if route1 can be merged into destinationRoute
	 * @param fromRoute
	 * 		the first route
	 * @param destinationRoute
	 * 		the second route
	 * @return
	 * 		true : fromRoute can be merged into destinationRoute
	 * 	    false: fromRoute cannot be merged into destinationRoute
	 */
	private boolean areRoutesCompatibles(Route fromRoute, Route destinationRoute){


		for (StopPoint stopPoint : fromRoute.getStopPoints()) {

			List<String> successors = getSuccessors(fromRoute, stopPoint.getPosition());

			if (!checkSuccessors(destinationRoute, stopPoint.getScheduledStopPoint().getContainedInStopAreaRef().getObjectId(),successors)){
				log.info(" route not compatible because check failed for successors of stopArea :" + stopPoint.getScheduledStopPoint().getContainedInStopAreaRef().getObjectId());
				log.info(" position :" + stopPoint.getPosition());
				log.info(" successors :" + successors.stream().collect(Collectors.joining(",")));
				return false;
			}
		}
		return true;
	}


	/**
	 * Checks, in the route,  for each item in the list of successors is either:
	 *  - not existing in the refRoute  (it means the point is a new point and is not a problem)
	 *  - located after the startStopAreaId
	 *
	 * @param route
	 * 		the route to read
	 * @param startStopAreaId
	 * 		stopArea from which the search begins
	 * @param successors
	 * 		list of stopAreas to check
	 * @return
	 * 		true : all incoming successors are corrects
	 * 	    false : problem : one of the successor is existing in ref route but not after stopAreaId
	 *
	 */
	private boolean checkSuccessors(Route route, String startStopAreaId, List<String> successors){

		for (StopPoint stopPoint : route.getStopPoints()) {
			String currentStopAreaId = stopPoint.getScheduledStopPoint().getContainedInStopAreaRef().getObjectId();
			if (!currentStopAreaId.equals(startStopAreaId)){
				continue;
			}

			return checkSuccessors(route, stopPoint.getPosition(), successors);
		}
		return true;
	}


	/**
	 * Checks, in the route,  for each item in the list of successors is either:
	 *  - not existing in the refRoute  (it means the point is a new point and is not a problem)
	 *  - located after the startPosition
	 *
	 *  e.g. : Route : A-B-C/  startPosition : 0/  successors : C-B-E  => true (because B and C are after position 0 and E is not existing
	 *  e.g. : Route : A-B-C/  startPosition : 2/  successors : B-E  => false (because B exists and the route but is not after position 2)
	 *
	 * @param route
	 * 		the route to read
	 * @param startPosition
	 * 		start position after which all successors must be existing
	 * @param incomingSuccessorsToCheck
	 * 		list of stopAreas to check
	 * @return
	 * 		true : all incoming successors are corrects
	 * 	    false : problem : one of the successor is existing in ref route but not after startPosition
	 *
	 */
	private boolean checkSuccessors(Route route, Integer startPosition, List<String> incomingSuccessorsToCheck){


		List<String> routeRefSuccessors = route.getStopPoints().stream()
				.filter(stopPoint -> stopPoint.getPosition() > startPosition)
				.map(stopPoint -> stopPoint.getScheduledStopPoint().getContainedInStopAreaRef().getObjectId())
				.collect(Collectors.toList());


		for (String incomingStopArea : incomingSuccessorsToCheck) {
			if (isStopAreaUsedInRoute(route,incomingStopArea) && !routeRefSuccessors.contains(incomingStopArea)){
				// incoming stopArea is existing in routeRef but is not after startPosition. It means there is an inversion between the 2 routes. They are incompatible
				return false;
			}
		}
		return true;
	}


	/**
	 * Checks if a stopArea is used or not in a route
	 * @param route
	 * 		the route to read
	 * @param stopAreaId
	 * 		the stopAreaId to search
	 * @return
	 * 		true : the stopAreaId is used in the route
	 * 		false : the stopAreaId is not used in the route
	 */
	private boolean isStopAreaUsedInRoute(Route route, String stopAreaId){
		return route.getStopPoints().stream()
				.anyMatch(stopPoint -> stopPoint.getScheduledStopPoint().getContainedInStopAreaRef().getObjectId().equals(stopAreaId));
	}


	/**
	 * Read a route and build a list of successors, after the position given as parameter
	 * @param route
	 * 		the route to read
	 * @param currentPosition
	 * 		the current position after which successors must be returned
	 * @return
	 * 		a list of successors
	 */
	private List<String> getSuccessors(Route route, Integer currentPosition ){
		String currentStopAreaId = route.getStopPoints().stream()
				.filter(stopPoint -> stopPoint.getPosition().equals(currentPosition))
				.findFirst()
				.get()
				.getScheduledStopPoint()
				.getContainedInStopAreaRef()
				.getObjectId();


		return route.getStopPoints().stream()
				.filter(stopPoint -> stopPoint.getPosition() > currentPosition && !stopPoint.getScheduledStopPoint().getContainedInStopAreaRef().getObjectId().equals(currentStopAreaId))
				.map(stopPoint -> stopPoint.getScheduledStopPoint().getContainedInStopAreaRef().getObjectId())
				.collect(Collectors.toList());
	}


	/**
	 * Builds a map that represents the route path.
	 *  key : the objectId of the stopArea
	 *  value : a list of positions where this stopArea is positionned
	 * e.g :
	 * 	Route : A-B-C-B
	 * 	Result :
	 * 		A : 0
	 * 		B : 1-3
	 * 		C : 2
	 *
	 * @param route
	 * @return
	 */
	private Map<String,List<Integer>> buildRoutePath(Route route){
		Map<String,List<Integer>> resultMap = new HashMap<>();


		for (StopPoint stopPoint : route.getStopPoints()) {
			List<Integer> positionsForCurrentPoint;
			String currentStopAreaId = stopPoint.getScheduledStopPoint().getContainedInStopAreaRef().getObjectId();
			if (resultMap.containsKey(currentStopAreaId)){
				positionsForCurrentPoint = resultMap.get(currentStopAreaId);
			}else{
				positionsForCurrentPoint = new ArrayList<>();
				resultMap.put(currentStopAreaId, positionsForCurrentPoint);
			}

			positionsForCurrentPoint.add(stopPoint.getPosition());
		}
		return resultMap;
	}


	/**
	 * Complete routePoints in a route
	 * @param route
	 *  the route that must be filled
	 */
	private void completeRouteInformations(Route route) {

		route.getStopPoints().sort(STOP_POINT_POSITION_COMPARATOR);


		if (!route.getStopPoints().isEmpty()) {
			StopPoint firstStopPoint = route.getStopPoints().get(0);
			StopPoint lastStopPoint = route.getStopPoints().get(route.getStopPoints().size() - 1);

			if (firstStopPoint.getScheduledStopPoint().getContainedInStopAreaRef().getObject() != null && lastStopPoint.getScheduledStopPoint().getContainedInStopAreaRef().getObject() != null) {
				String first = firstStopPoint.getScheduledStopPoint().getContainedInStopAreaRef().getObject().getName();
				String last = lastStopPoint.getScheduledStopPoint().getContainedInStopAreaRef().getObject().getName();
				route.setName(first + " -> " + last);
			}

		}


		// Create route point from first an last stop point on route
		StopPoint firstStopPoint = route.getStopPoints().get(0);
		StopPoint lastStopPoint = route.getStopPoints().get(route.getStopPoints().size() - 1);

		if (route.getRoutePoints().isEmpty() || !route.getRoutePoints().get(0).getScheduledStopPoint().equals(firstStopPoint.getScheduledStopPoint())){
			route.getRoutePoints().add(createRoutePointFromStopPoint(firstStopPoint));
		}

		if (route.getRoutePoints().isEmpty() || route.getRoutePoints().size() < 2 ||  !route.getRoutePoints().get(1).getScheduledStopPoint().equals(lastStopPoint.getScheduledStopPoint())){
			route.getRoutePoints().add(createRoutePointFromStopPoint(lastStopPoint));
		}


		route.setFilled(true);

	}

	/**
	 * Creates a routePoint from a stopPoint
	 *
	 * @param stopPoint the stop point
	 * @return a new routePoint
	 */
	private RoutePoint createRoutePointFromStopPoint(StopPoint stopPoint) {

		String objectId =  stopPoint.objectIdPrefix() + ":RoutePoint:" + stopPoint.objectIdSuffix();
		RoutePoint routePoint = routePointDAO.findByObjectId(objectId);
		if (routePoint != null){
			return routePoint;
		}

		RoutePoint firstRoutePoint = new RoutePoint();
		firstRoutePoint.setObjectId(objectId);
		firstRoutePoint.setScheduledStopPoint(stopPoint.getScheduledStopPoint());
		firstRoutePoint.setFilled(true);
		return firstRoutePoint;
	}


	/**
	 * Read all routes existing in DB and build a map to associate a line id with all directions availables
	 * e.g :
	 * 		1 -> A, R
	 * 		2 -> A
	 * 	    3 -> ClockWise
	 */
	private void buildLineDirectionsMap(){
		List<Route> routeList = routeDAO.findAll();

		for (Route route : routeList) {

			Long lineId = route.getLine().getId();
			PTDirectionEnum direction = route.getDirection();

			Set<PTDirectionEnum> directionSet;
			if (lineDirections.containsKey(lineId)){
				directionSet = lineDirections.get(lineId);
			}else{
				directionSet = new HashSet<>();
				lineDirections.put(lineId,directionSet);
			}
			directionSet.add(direction);
		}
	}


	public static class DefaultCommandFactory extends CommandFactory {

		@Override
		protected Command create(InitialContext context) throws IOException {
			Command result = null;
			try {
				String name = "java:app/mobi.chouette.exchange/" + COMMAND;
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
		CommandFactory.factories.put(RouteMergerCommand.class.getName(), new DefaultCommandFactory());
	}
}
