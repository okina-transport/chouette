package mobi.chouette.exchange.importer;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Color;
import mobi.chouette.common.ContenerChecker;
import mobi.chouette.common.Context;
import mobi.chouette.common.PropertyNames;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.AccessPointDAO;
import mobi.chouette.dao.AccessibilityAssessmentDAO;
import mobi.chouette.dao.AccessibilityLimitationDAO;
import mobi.chouette.dao.CategoriesForLinesDAO;
import mobi.chouette.dao.LineDAO;
import mobi.chouette.dao.VehicleJourneyDAO;
import mobi.chouette.exchange.importer.updater.LineOptimiser;
import mobi.chouette.exchange.importer.updater.LineUpdater;
import mobi.chouette.exchange.importer.updater.NeTExStopPlaceRegisterUpdater;
import mobi.chouette.exchange.importer.updater.StopAreaIdMapper;
import mobi.chouette.exchange.importer.updater.Updater;
import mobi.chouette.exchange.parameters.AbstractImportParameter;
import mobi.chouette.exchange.report.ActionReporter;
import mobi.chouette.exchange.report.ActionReporter.ERROR_CODE;
import mobi.chouette.exchange.report.ActionReporter.OBJECT_STATE;
import mobi.chouette.exchange.report.ActionReporter.OBJECT_TYPE;
import mobi.chouette.exchange.report.IO_TYPE;
import mobi.chouette.model.AccessLink;
import mobi.chouette.model.AccessPoint;
import mobi.chouette.model.AccessibilityAssessment;
import mobi.chouette.model.AccessibilityLimitation;
import mobi.chouette.model.JourneyPattern;
import mobi.chouette.model.Line;
import mobi.chouette.model.Route;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.StopPoint;
import mobi.chouette.model.Timetable;
import mobi.chouette.model.VehicleJourney;
import mobi.chouette.model.VehicleJourneyAtStop;
import mobi.chouette.model.type.BoardingAlightingPossibilityEnum;
import mobi.chouette.model.util.NamingUtil;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.Referential;
import mobi.chouette.persistence.hibernate.ContextHolder;
import org.apache.commons.lang.StringUtils;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.*;

@Log4j
@Stateless(name = LineRegisterCommand.COMMAND)
public class LineRegisterCommand implements Command {

	public static final String COMMAND = "LineRegisterCommand";

	@EJB
	private LineOptimiser optimiser;

	@EJB
	private LineDAO lineDAO;

	@EJB
	private AccessPointDAO accessPointDAO;

	@EJB
	private ContenerChecker checker;

	@EJB
	private VehicleJourneyDAO vehicleJourneyDAO;

	@EJB
	private AccessibilityAssessmentDAO accessibilityAssessmentDAO;

	@EJB
	private AccessibilityLimitationDAO accessibilityLimitationDAO;

	@EJB
	private CategoriesForLinesDAO categoriesForLinesDAO;

	@EJB(beanName = LineUpdater.BEAN_NAME)
	private Updater<Line> lineUpdater;

	@EJB(beanName = StopAreaIdMapper.BEAN_NAME)
	private StopAreaIdMapper stopAreaIdMapper;

    @EJB(beanName = NeTExStopPlaceRegisterUpdater.BEAN_NAME)
    private NeTExStopPlaceRegisterUpdater stopPlaceRegisterUpdater;

    @Override
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public boolean execute(Context context) throws Exception {

		boolean result = ERROR;
		Monitor monitor = MonitorFactory.start(COMMAND);

		if (!context.containsKey(OPTIMIZED)) {
			context.put(OPTIMIZED, Boolean.TRUE);
		}
		Boolean optimized = (Boolean) context.get(OPTIMIZED);
		Referential cache = new Referential();
		context.put(CACHE, cache);

		String ref = ContextHolder.getContext();
		context.put("ref", ref);

		Referential referential = (Referential) context.get(REFERENTIAL);


		// Use property based enabling of stop place updater, but allow disabling if property exist in context
		Line newValue  = referential.getLines().values().iterator().next();
		context.put(CURRENT_LINE_ID,newValue.getObjectId());

		// Read Line color, usefull for Neptune Import using purge to keep color line
		if (context.get(LINE_COLOR) != null){
			HashMap<String, String> lineColorMap = context.get(LINE_COLOR) instanceof HashMap<?,?> ? (HashMap<String, String>) context.get(LINE_COLOR) : new HashMap<>();
			lineColorMap.entrySet().stream()
					.filter(entry -> entry.getKey().equals(newValue.getObjectId()))
					.forEach(entry -> newValue.setColor(entry.getValue()));
		}

		if (newValue.getNetwork() == null){
			mobi.chouette.model.Network defaultNetwork = ObjectFactory.getPTNetwork(referential, NETEX_VALID_PREFIX + ":Network:DefaultNetwork");
			newValue.setNetwork(defaultNetwork);
		}

		removeEmptyRoutes(newValue);

		AbstractImportParameter importParameter = (AbstractImportParameter) context.get(CONFIGURATION);
		int currentLineNb = context.get(CURRENT_LINE_NB) == null ? 1 : (int) context.get(CURRENT_LINE_NB) + 1;
		context.put(CURRENT_LINE_NB,currentLineNb);

		log.info("Importing line: " + newValue.getObjectId() + " with stop area import mode: " + importParameter.getStopAreaImportMode() + "[" +  currentLineNb + "/" + context.get(TOTAL_NB_OF_LINES) + "]");

		if (importParameter.isKeepObsoleteLines() || isLineValidInFuture(newValue)) {

			boolean shouldMapIds =
					Boolean.parseBoolean(System.getProperty(checker.getContext() + PropertyNames.STOP_PLACE_ID_MAPPING)) && importParameter.isStopAreaRemoteIdMapping();
			if(shouldMapIds) {
				stopAreaIdMapper.mapStopAreaIds(referential);
			} else {
				log.info("Will not map ids against external stop place registry as import parameter stop_registry_map_id != true");
			}


            boolean shouldUpdateStopPlaceRegistry =
                    Boolean.parseBoolean(System.getProperty(checker.getContext() + PropertyNames.STOP_PLACE_REGISTER_UPDATE));
            if(shouldUpdateStopPlaceRegistry) {
                stopPlaceRegisterUpdater.update(context, referential);
            } else {
                log.warn("Stop place register will not be updated. Neither is property " + PropertyNames.STOP_PLACE_REGISTER_UPDATE + " set nor has import parameter update_stop_registry = true.");
            }


			log.info("register line : " + newValue.getObjectId() + " " + newValue.getName() + " vehicleJourney count = "
					+ referential.getVehicleJourneys().size());
			try {
	
				optimiser.initialize(cache, referential);

				Line oldValue = cache.getLines().get(newValue.getObjectId());
				lineUpdater.update(context, oldValue, newValue);
				if(oldValue.getCategoriesForLine() == null){
					oldValue.setCategoriesForLine(categoriesForLinesDAO.find(0L));
				}
				if(oldValue.getPosition() == null) {
					oldValue.setPosition(newValue.getPosition());
				}
				searchEmptyOriginalStopIds(referential,oldValue);
				lineDAO.create(oldValue);

				findRefToLoc(oldValue);
				persistAccessPoints(oldValue);
				lineDAO.flush(); // to prevent SQL error outside method

				if (optimized) {
					Monitor wMonitor = MonitorFactory.start("prepareCopy");
					StringWriter bufferVjas = new StringWriter(1024);
					StringWriter bufferAa = new StringWriter(1024);
					StringWriter bufferAl = new StringWriter(1024);
					final List<String> vehicleJourneysToDelete = new ArrayList<>(referential.getVehicleJourneys().keySet());
					HashMap<String, String> mapIdsVjAa = new HashMap<>();
					HashMap<String, String> mapIdsAaAl = new HashMap<>();

					for (VehicleJourney vj : referential.getVehicleJourneys().values()) {
						VehicleJourney vehicleJourney = cache.getVehicleJourneys().get(vj.getObjectId());

						List<VehicleJourneyAtStop> vehicleJourneyAtStops = vj.getVehicleJourneyAtStops();
						for (VehicleJourneyAtStop vehicleJourneyAtStop : vehicleJourneyAtStops) {

							StopPoint stopPoint = cache.getStopPoints().get(
									vehicleJourneyAtStop.getStopPoint().getObjectId());

							writeVjas(bufferVjas, vehicleJourney, stopPoint, vehicleJourneyAtStop, importParameter.isKeepBoardingAlighting());
						}
						writeAccessibilityAssessmentVj(bufferAa, bufferAl, vj.getAccessibilityAssessment());
						mapIdsVjAa.put(vj.getObjectId(), vj.getAccessibilityAssessment().getObjectId());
						mapIdsAaAl.put(vj.getAccessibilityAssessment().getObjectId(),  vj.getAccessibilityAssessment().getAccessibilityLimitation().getObjectId());
					}

					accessibilityLimitationDAO.copy(bufferAl.toString());
					accessibilityAssessmentDAO.copy(bufferAa.toString());


					vehicleJourneyDAO.deleteChildren(vehicleJourneysToDelete);
					context.put(BUFFER_VJAS, bufferVjas.toString());
					context.put(MAP_IDS_VJ_AA, mapIdsVjAa);
					context.put(MAP_IDS_AA_AL, mapIdsAaAl);
					wMonitor.stop();
				}
				result = SUCCESS;
			} catch (Exception ex) {
				log.error(ex.getMessage());
				ActionReporter reporter = ActionReporter.Factory.getInstance();
				reporter.addObjectReport(context, newValue.getObjectId(), 
						OBJECT_TYPE.LINE, NamingUtil.getName(newValue), OBJECT_STATE.ERROR, IO_TYPE.INPUT);
				if (ex.getCause() != null) {
					Throwable e = ex.getCause();
					while (e.getCause() != null) {
						log.error(e.getMessage());
						e = e.getCause();
					}
					if (e instanceof SQLException) {
						e = ((SQLException) e).getNextException();
						reporter.addErrorToObjectReport(context, newValue.getObjectId(), OBJECT_TYPE.LINE, ERROR_CODE.WRITE_ERROR,  e.getMessage());
						
					} else {
						reporter.addErrorToObjectReport(context, newValue.getObjectId(), OBJECT_TYPE.LINE, ERROR_CODE.INTERNAL_ERROR,  e.getMessage());
					}
				} else {
					reporter.addErrorToObjectReport(context, newValue.getObjectId(), OBJECT_TYPE.LINE, ERROR_CODE.INTERNAL_ERROR,  ex.getMessage());
				}
				throw ex;
			} finally {
				log.info(Color.MAGENTA + monitor.stop() + Color.NORMAL);
			}
		} else {
			log.info("Skipping obsolete line : " + newValue.getObjectId());
			log.info(Color.MAGENTA + monitor.stop() + Color.NORMAL);
		}
		return result;
	}

	/**
	 * Read a line and remove routes that have no journey patterns associated
	 *  (to avoid error on validation)
	 * @param line
	 * 	the line for which we need to remove empty routes
	 */
	private void removeEmptyRoutes(Line line) {

		List<Route> allRoutes = line.getRoutes();
		List<Route> cleanedRoutes = new ArrayList<>();

		for (Route route : allRoutes) {
			if (route.getJourneyPatterns() == null || route.getJourneyPatterns().isEmpty()) {
				log.warn("Route has been removed because no journey patterns associated to it :" + route.getObjectId());
			} else {
				cleanedRoutes.add(route);
			}
		}
		line.setRoutes(cleanedRoutes);

	}


	private void persistAccessPoints(Line line){

		for (Route route : line.getRoutes()) {
			for (JourneyPattern journeyPattern : route.getJourneyPatterns()) {
				for (StopPoint stopPoint : journeyPattern.getStopPoints()) {
					StopArea stopArea = stopPoint.getScheduledStopPoint().getContainedInStopAreaRef().getObject();
					persistAccessPointsForStopArea(stopArea);
				}
			}

		}
	}

	private void persistAccessPointsForStopArea(StopArea stopArea){
		for (AccessLink accessLink : stopArea.getAccessLinks()) {
			AccessPoint accessPoint = accessLink.getAccessPoint();
			AccessPoint recoveredAccessPoint = accessPointDAO.findByObjectId(accessPoint.getObjectId());
			if (recoveredAccessPoint == null){
				accessPointDAO.create(accessPoint);
			}
		}
	}


	private void findRefToLoc(Line line){
		for (Route route : line.getRoutes()) {
			checkStopPointList(route.getStopPoints());



			for (JourneyPattern journeyPattern : route.getJourneyPatterns()) {
				checkStopPointList(journeyPattern.getStopPoints());

				for (VehicleJourney vehicleJourney : journeyPattern.getVehicleJourneys()) {

					for (VehicleJourneyAtStop vehicleJourneyAtStop : vehicleJourney.getVehicleJourneyAtStops()) {
						checkStopPoint(vehicleJourneyAtStop.getStopPoint());
					}
				}
			}
		}
	}

	private void checkStopPointList(List<StopPoint> spList){
		for (StopPoint stopPoint : spList) {
			checkStopPoint(stopPoint);
		}
	}

	private void checkStopPoint(StopPoint stopPoint) {
		if (stopPoint.getScheduledStopPoint().getContainedInStopAreaRef().getObjectId().contains("LOC")){
			log.error("prob ici");
		}
	}

	private void searchEmptyOriginalStopIds(Referential referential, Line line){
		for (Route route : line.getRoutes()) {
			for (JourneyPattern journeyPattern : route.getJourneyPatterns()) {
				for (StopPoint stopPoint : journeyPattern.getStopPoints()) {
					StopArea stopArea = stopPoint.getScheduledStopPoint().getContainedInStopAreaRef().getObject();
					if (stopArea != null && StringUtils.isEmpty(stopArea.getOriginalStopId())){
						StopArea sharedStopArea = referential.getSharedStopAreas().get(stopArea.getObjectId());
						stopArea.setOriginalStopId(sharedStopArea != null ? sharedStopArea.getOriginalStopId() : null);
					}
				}
			}
		}
	}


	private boolean isLineValidInFuture(Line line) {

		LocalDate today = LocalDate.now();
		
		for(Route r : (line.getRoutes() == null ? new ArrayList<Route>() : line.getRoutes())) {
			for(JourneyPattern jp : (r.getJourneyPatterns() == null? new ArrayList<JourneyPattern>() : r.getJourneyPatterns())) {
				for(VehicleJourney vj : (jp.getVehicleJourneys() == null? new ArrayList<VehicleJourney>() : jp.getVehicleJourneys())) {
					for(Timetable t : (vj.getTimetables() == null ? new ArrayList<Timetable>() : vj.getTimetables())) {
						if(t.getEndOfPeriod() != null && !t.getEndOfPeriod().isBefore(today)) {
							return true;
						}
					}
				}
			}
		}
		
		
		return false;
	}
	
	
	protected void writeVjas(StringWriter buffer, VehicleJourney vehicleJourney, StopPoint stopPoint,
			VehicleJourneyAtStop vehicleJourneyAtStop, boolean keepBoardingAlighting) {
		DateTimeFormatter timeFormat = DateTimeFormat.forPattern("HH:mm:ss");
		DateTimeFormatter dateTimeFormat = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss");

		if (keepBoardingAlighting){
			Optional<BoardingAlightingPossibilityEnum> currentBoardingAlightingPossibilityOpt = getActualBoardingAlightingPossibility(vehicleJourney, vehicleJourneyAtStop);
			currentBoardingAlightingPossibilityOpt.ifPresent(vehicleJourneyAtStop::setBoardingAlightingPossibility);
		}
		
		buffer.write(vehicleJourneyAtStop.getObjectId().replace('|', '_'));
		buffer.append(SEP);
		buffer.write(vehicleJourneyAtStop.getObjectVersion().toString());
		buffer.append(SEP);
		if(vehicleJourneyAtStop.getCreationTime() != null) {
			buffer.write(dateTimeFormat.print(vehicleJourneyAtStop.getCreationTime()));
		} else {
			buffer.write(NULL);
		}
		buffer.append(SEP);
		if(vehicleJourneyAtStop.getCreatorId() != null) {
			buffer.write(vehicleJourneyAtStop.getCreatorId().replace('|', '_'));
		} else {
			buffer.write(NULL);
		}
		buffer.append(SEP);
		buffer.write(vehicleJourney.getId().toString());
		buffer.append(SEP);
		buffer.write(stopPoint.getId().toString());
		buffer.append(SEP);
		if (vehicleJourneyAtStop.getArrivalTime() != null)
			buffer.write(timeFormat.print(vehicleJourneyAtStop.getArrivalTime()));
		else
			buffer.write(NULL);
		buffer.append(SEP);
		if (vehicleJourneyAtStop.getDepartureTime() != null)
			buffer.write(timeFormat.print(vehicleJourneyAtStop.getDepartureTime()));
		else
			buffer.write(NULL);
		buffer.append(SEP);
		buffer.write(Integer.toString(vehicleJourneyAtStop.getArrivalDayOffset()));
		buffer.append(SEP);
		buffer.write(Integer.toString(vehicleJourneyAtStop.getDepartureDayOffset()));
		buffer.append(SEP);
		if (vehicleJourneyAtStop.getBoardingAlightingPossibility() != null) {
			buffer.write(vehicleJourneyAtStop.getBoardingAlightingPossibility().name());
		} else {
			buffer.write(NULL);
		}

		buffer.append('\n');

	}

	protected void writeAccessibilityAssessmentVj(StringWriter bufferAa, StringWriter bufferAl, AccessibilityAssessment accessibilityAssessment) {
		DateTimeFormatter dateTimeFormat = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss");


		bufferAa.write(accessibilityAssessment.getObjectId().replace('|', '_'));
		bufferAa.append(SEP);
		bufferAa.write(accessibilityAssessment.getObjectVersion().toString());
		bufferAa.append(SEP);
		if(accessibilityAssessment.getCreationTime() != null) {
			bufferAa.write(dateTimeFormat.print(accessibilityAssessment.getCreationTime()));
		} else {
			bufferAa.write(NULL);
		}
		bufferAa.append(SEP);
		if(accessibilityAssessment.getCreatorId() != null) {
			bufferAa.write(accessibilityAssessment.getCreatorId().replace('|', '_'));
		} else {
			bufferAa.write(NULL);
		}

		bufferAa.append(SEP);
		if(accessibilityAssessment.getMobilityImpairedAccess() != null) {
			bufferAa.write(accessibilityAssessment.getMobilityImpairedAccess().toString());
		} else {
			bufferAa.write(NULL);
		}

		bufferAa.append('\n');

		writeAccessibilityLimitationVj(bufferAl, accessibilityAssessment.getAccessibilityLimitation());

	}

	protected void writeAccessibilityLimitationVj(StringWriter bufferAl, AccessibilityLimitation accessibilityLimitation) {
		DateTimeFormatter dateTimeFormat = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss");

		bufferAl.write(accessibilityLimitation.getObjectId().replace('|', '_'));
		bufferAl.append(SEP);
		bufferAl.write(accessibilityLimitation.getObjectVersion().toString());
		bufferAl.append(SEP);
		if(accessibilityLimitation.getCreationTime() != null) {
			bufferAl.write(dateTimeFormat.print(accessibilityLimitation.getCreationTime()));
		} else {
			bufferAl.write(NULL);
		}
		bufferAl.append(SEP);
		if(accessibilityLimitation.getCreatorId() != null) {
			bufferAl.write(accessibilityLimitation.getCreatorId().replace('|', '_'));
		} else {
			bufferAl.write(NULL);
		}
		bufferAl.append(SEP);
		if(accessibilityLimitation.getWheelchairAccess() != null) {
			bufferAl.write(accessibilityLimitation.getWheelchairAccess().toString());
		} else {
			bufferAl.write(NULL);
		}
		bufferAl.append(SEP);
		if(accessibilityLimitation.getVisualSignsAvailable() != null) {
			bufferAl.write(accessibilityLimitation.getVisualSignsAvailable().toString());
		} else {
			bufferAl.write(NULL);
		}
		bufferAl.append(SEP);
		if(accessibilityLimitation.getStepFreeAccess() != null) {
			bufferAl.write(accessibilityLimitation.getStepFreeAccess().toString());
		} else {
			bufferAl.write(NULL);
		}
		bufferAl.append(SEP);
		if(accessibilityLimitation.getLiftFreeAccess() != null) {
			bufferAl.write(accessibilityLimitation.getLiftFreeAccess().toString());
		} else {
			bufferAl.write(NULL);
		}
		bufferAl.append(SEP);
		if(accessibilityLimitation.getEscalatorFreeAccess() != null) {
			bufferAl.write(accessibilityLimitation.getEscalatorFreeAccess().toString());
		} else {
			bufferAl.write(NULL);
		}
		bufferAl.append(SEP);
		if(accessibilityLimitation.getAudibleSignalsAvailable() != null) {
			bufferAl.write(accessibilityLimitation.getAudibleSignalsAvailable().toString());
		} else {
			bufferAl.write(NULL);
		}

		bufferAl.append('\n');

	}

	/**
	 * Read the old VehicleJourney to recover boardingAlightingPossibility, for the vehicleJourneyAtStop passed as parameter
	 * (Needed if user wants to keep boardingAlighting between 2 imports)
	 *
	 * @param vehicleJourney
	 * 		VehicleJourney that contains old data from DB
	 * @param newVehicleJourneyAtStop
	 * 		newVehicleJourney
	 * @return
	 */
	private Optional<BoardingAlightingPossibilityEnum> getActualBoardingAlightingPossibility(VehicleJourney vehicleJourney, VehicleJourneyAtStop newVehicleJourneyAtStop){
		return vehicleJourney.getVehicleJourneyAtStops().stream()
												.filter(currentVehicleJourneyAtStop -> currentVehicleJourneyAtStop.getStopPoint().equals(newVehicleJourneyAtStop.getStopPoint()) &&
																						currentVehicleJourneyAtStop.getVehicleJourney().equals(newVehicleJourneyAtStop.getVehicleJourney()))
												.map(VehicleJourneyAtStop::getBoardingAlightingPossibility)
												.filter(Objects::nonNull)
												.findFirst();
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
		CommandFactory.factories.put(LineRegisterCommand.class.getName(), new DefaultCommandFactory());
	}
}
