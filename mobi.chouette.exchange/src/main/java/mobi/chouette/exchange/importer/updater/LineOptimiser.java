package mobi.chouette.exchange.importer.updater;

import mobi.chouette.dao.*;
import mobi.chouette.model.*;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.Referential;
import org.apache.commons.lang.StringUtils;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.util.Collection;
import java.util.List;

@Stateless
public class LineOptimiser {

	@EJB
	private ConnectionLinkDAO connectionLinkDAO;

	@EJB
	private AccessLinkDAO accessLinkDAO;

	@EJB
	private AccessPointDAO accessPointDAO;

	@EJB
	private StopAreaDAO stopAreaDAO;

	@EJB
	private TimetableDAO timetableDAO;

	@EJB
	private NetworkDAO ptNetworkDAO;

	@EJB
	private CompanyDAO companyDAO;

	@EJB
	private GroupOfLineDAO groupOfLineDAO;

	@EJB
	private LineDAO lineDAO;

	@EJB
	private RouteDAO routeDAO;

	@EJB
	private JourneyPatternDAO journeyPatternDAO;

	@EJB
	private VehicleJourneyDAO vehicleJourneyDAO;

	@EJB
	private StopPointDAO stopPointDAO;

	@EJB
	private TimebandDAO timebandDAO;

	@EJB
	private AccessibilityAssessmentDAO accessibilityAssessmentDAO;

	@EJB
	private AccessibilityLimitationDAO accessibilityLimitationDAO;

	@EJB
	private TrainDAO trainDAO;

	public void initialize(Referential cache, Referential referential) {

//		Monitor monitor = MonitorFactory.start("LineOptimiser");
		initializeStopArea(cache, referential.getStopAreas().values());

		initializeConnectionLink(cache, referential.getConnectionLinks().values());
		initializeAccessLink(cache, referential.getAccessLinks().values());
		initializeAccessPoint(cache, referential.getAccessPoints().values());

		initializeTimetable(cache, referential.getTimetables().values());
		initializePTNetwork(cache, referential.getPtNetworks().values());
		initializeCompany(cache, referential.getCompanies().values());
		initializeGroupOfLine(cache, referential.getGroupOfLines().values());

		initializeLine(cache, referential.getLines().values());
		initializeRoute(cache, referential.getRoutes().values());
		initializeStopPoint(cache, referential.getStopPoints().values());
		initializeJourneyPattern(cache, referential.getJourneyPatterns().values());
		initializeVehicleJourney(cache, referential.getVehicleJourneys().values());
		initializeAccessibilityAssessment(cache, referential.getAccessibilityAssessments().values());
		initializeAccessibilityLimitation(cache, referential.getAccessibilityLimitations().values());

		initializeTimeband(cache, referential.getTimebands().values());
		initializeTrains(cache, referential.getTrains().values());
//		monitor.stop();
	}

	private void initializeStopArea(Referential cache, Collection<StopArea> list) {
		if (list != null && !list.isEmpty()) {
			Collection<String> objectIds = UpdaterUtils.getObjectIds(list);
			List<StopArea> objects = stopAreaDAO.findByObjectId(objectIds);
			for (StopArea object : objects) {
				cache.getStopAreas().put(object.getObjectId(), object);
			}

			// TODO check if stoparea really exists
			for (StopArea item : list) {
				StopArea object = cache.getStopAreas().get(item.getObjectId());
				if (object == null) {
					object = ObjectFactory.getStopArea(cache, item.getObjectId());
				}

				if (StringUtils.isNotEmpty(item.getOriginalStopId()))
					object.setOriginalStopId(item.getOriginalStopId());
			}
		}
	}

	private void initializeConnectionLink(Referential cache, Collection<ConnectionLink> list) {
		if (list != null && !list.isEmpty()) {
			Collection<String> objectIds = UpdaterUtils.getObjectIds(list);
			List<ConnectionLink> objects = connectionLinkDAO.findByObjectId(objectIds);
			for (ConnectionLink object : objects) {
				cache.getConnectionLinks().put(object.getObjectId(), object);
			}

			for (ConnectionLink item : list) {
				ConnectionLink object = cache.getConnectionLinks().get(item.getObjectId());
				if (object == null) {
					object = ObjectFactory.getConnectionLink(cache, item.getObjectId());
				}
			}
		}
	}

	private void initializeAccessLink(Referential cache, Collection<AccessLink> list) {
		if (list != null && !list.isEmpty()) {
			Collection<String> objectIds = UpdaterUtils.getObjectIds(list);
			List<AccessLink> objects = accessLinkDAO.findByObjectId(objectIds);
			for (AccessLink object : objects) {
				cache.getAccessLinks().put(object.getObjectId(), object);
			}

			for (AccessLink item : list) {
				AccessLink object = cache.getAccessLinks().get(item.getObjectId());
				if (object == null) {
					object = ObjectFactory.getAccessLink(cache, item.getObjectId());
				}
			}
		}
	}

	private void initializeAccessPoint(Referential cache, Collection<AccessPoint> list) {
		if (list != null && !list.isEmpty()) {
			Collection<String> objectIds = UpdaterUtils.getObjectIds(list);
			List<AccessPoint> objects = accessPointDAO.findByObjectId(objectIds);
			for (AccessPoint object : objects) {
				cache.getAccessPoints().put(object.getObjectId(), object);
			}

			for (AccessPoint item : list) {
				AccessPoint object = cache.getAccessPoints().get(item.getObjectId());
				if (object == null) {
					object = ObjectFactory.getAccessPoint(cache, item.getObjectId());
				}
			}
		}
	}

	private void initializeTimetable(Referential cache, Collection<Timetable> list) {
		if (list != null && !list.isEmpty()) {
			Collection<String> objectIds = UpdaterUtils.getObjectIds(list);
			List<Timetable> objects = timetableDAO.findByObjectId(objectIds);
			for (Timetable object : objects) {
				cache.getTimetables().put(object.getObjectId(), object);
			}

			for (Timetable item : list) {
				Timetable object = cache.getTimetables().get(item.getObjectId());
				if (object == null) {
					object = ObjectFactory.getTimetable(cache, item.getObjectId());
				}
			}
		}
	}

	private void initializePTNetwork(Referential cache, Collection<Network> list) {
		if (list != null && !list.isEmpty()) {
			Collection<String> objectIds = UpdaterUtils.getObjectIds(list);
			List<Network> objects = ptNetworkDAO.findByObjectId(objectIds);
			for (Network object : objects) {
				cache.getPtNetworks().put(object.getObjectId(), object);
			}

			for (Network item : list) {
				Network object = cache.getPtNetworks().get(item.getObjectId());
				if (object == null) {
					object = ObjectFactory.getPTNetwork(cache, item.getObjectId());
				}
			}
		}
	}

	private void initializeCompany(Referential cache, Collection<Company> list) {
		if (list != null && !list.isEmpty()) {
			Collection<String> objectIds = UpdaterUtils.getObjectIds(list);
			List<Company> objects = companyDAO.findByObjectId(objectIds);
			for (Company object : objects) {
				cache.getCompanies().put(object.getObjectId(), object);
			}

			for (Company item : list) {
				Company object = cache.getCompanies().get(item.getObjectId());
				if (object == null) {
					object = ObjectFactory.getCompany(cache, item.getObjectId());
				}
			}
		}
	}

	private void initializeGroupOfLine(Referential cache, Collection<GroupOfLine> list) {
		if (list != null && !list.isEmpty()) {
			Collection<String> objectIds = UpdaterUtils.getObjectIds(list);
			List<GroupOfLine> objects = groupOfLineDAO.findByObjectId(objectIds);
			for (GroupOfLine object : objects) {
				cache.getGroupOfLines().put(object.getObjectId(), object);
			}

			for (GroupOfLine item : list) {
				GroupOfLine object = cache.getGroupOfLines().get(item.getObjectId());
				if (object == null) {
					object = ObjectFactory.getGroupOfLine(cache, item.getObjectId());
				}
			}
		}
	}

	private void initializeLine(Referential cache, Collection<Line> list) {
		if (list != null && !list.isEmpty()) {
			Collection<String> objectIds = UpdaterUtils.getObjectIds(list);
			List<Line> objects = lineDAO.findByObjectId(objectIds);
			for (Line object : objects) {
				cache.getLines().put(object.getObjectId(), object);
			}

			for (Line item : list) {
				Line object = cache.getLines().get(item.getObjectId());
				if (object == null) {
					object = ObjectFactory.getLine(cache, item.getObjectId());
				}
			}
		}
	}

	private void initializeRoute(Referential cache, Collection<Route> list) {
		if (list != null && !list.isEmpty()) {
			Collection<String> objectIds = UpdaterUtils.getObjectIds(list);
			List<Route> objects = routeDAO.findByObjectId(objectIds);
			for (Route object : objects) {
				cache.getRoutes().put(object.getObjectId(), object);
			}

			for (Route item : list) {
				Route object = cache.getRoutes().get(item.getObjectId());
				if (object == null) {
					object = ObjectFactory.getRoute(cache, item.getObjectId());
				}
			}
		}
	}

	private void initializeStopPoint(Referential cache, Collection<StopPoint> list) {
		if (list != null && !list.isEmpty()) {
			Collection<String> objectIds = UpdaterUtils.getObjectIds(list);
			List<StopPoint> objects = stopPointDAO.findByObjectId(objectIds);
			for (StopPoint object : objects) {
				cache.getStopPoints().put(object.getObjectId(), object);
			}

			for (StopPoint item : list) {
				StopPoint object = cache.getStopPoints().get(item.getObjectId());
				if (object == null) {
					object = ObjectFactory.getStopPoint(cache, item.getObjectId());
				}
			}
		}
	}

	private void initializeJourneyPattern(Referential cache, Collection<JourneyPattern> list) {
		if (list != null && !list.isEmpty()) {
			Collection<String> objectIds = UpdaterUtils.getObjectIds(list);
			List<JourneyPattern> objects = journeyPatternDAO.findByObjectId(objectIds);
			for (JourneyPattern object : objects) {
				cache.getJourneyPatterns().put(object.getObjectId(), object);
			}

			for (JourneyPattern item : list) {
				JourneyPattern object = cache.getJourneyPatterns().get(item.getObjectId());
				if (object == null) {
					object = ObjectFactory.getJourneyPattern(cache, item.getObjectId());
				}
			}
		}
	}

	private void initializeVehicleJourney(Referential cache, Collection<VehicleJourney> list) {
		if (list != null && !list.isEmpty()) {
			Collection<String> objectIds = UpdaterUtils.getObjectIds(list);
			List<VehicleJourney> objects = vehicleJourneyDAO.findByObjectId(objectIds);
			for (VehicleJourney object : objects) {
				cache.getVehicleJourneys().put(object.getObjectId(), object);
			}

			for (VehicleJourney item : list) {
				VehicleJourney object = cache.getVehicleJourneys().get(item.getObjectId());
				if (object == null) {
					object = ObjectFactory.getVehicleJourney(cache, item.getObjectId());
				}
			}
		}	
	}

	private void initializeTimeband(Referential cache, Collection<Timeband> list) {
		if (list != null && !list.isEmpty()) {
			Collection<String> objectIds = UpdaterUtils.getObjectIds(list);
			List<Timeband> objects = timebandDAO.findByObjectId(objectIds);
			for (Timeband object : objects) {
				cache.getTimebands().put(object.getObjectId(), object);
			}

			for (Timeband item : list) {
				Timeband object = cache.getTimebands().get(item.getObjectId());
				if (object == null) {
					object = ObjectFactory.getTimeband(cache, item.getObjectId());
				}
			}
		}
	}

	private void initializeAccessibilityAssessment(Referential cache, Collection<AccessibilityAssessment> list) {
		if (list != null && !list.isEmpty()) {
			Collection<String> objectIds = UpdaterUtils.getObjectIds(list);
			List<AccessibilityAssessment> objects = accessibilityAssessmentDAO.findByObjectId(objectIds);
			for (AccessibilityAssessment object : objects) {
				cache.getAccessibilityAssessments().put(object.getObjectId(), object);
			}

			for (AccessibilityAssessment item : list) {
				AccessibilityAssessment object = cache.getAccessibilityAssessments().get(item.getObjectId());
				if (object == null) {
					object = ObjectFactory.getAccessibilityAssessment(cache, item.getObjectId());
				}
			}
		}
	}

	private void initializeAccessibilityLimitation(Referential cache, Collection<AccessibilityLimitation> list) {
		if (list != null && !list.isEmpty()) {
			Collection<String> objectIds = UpdaterUtils.getObjectIds(list);
			List<AccessibilityLimitation> objects = accessibilityLimitationDAO.findByObjectId(objectIds);
			for (AccessibilityLimitation object : objects) {
				cache.getAccessibilityLimitations().put(object.getObjectId(), object);
			}

			for (AccessibilityLimitation item : list) {
				AccessibilityLimitation object = cache.getAccessibilityLimitations().get(item.getObjectId());
				if (object == null) {
					object = ObjectFactory.getAccessibilityLimitation(cache, item.getObjectId());
				}
			}
		}
	}

	private void initializeTrains(Referential cache, Collection<Train> list) {
		if (list != null && !list.isEmpty()) {
			Collection<String> objectIds = UpdaterUtils.getObjectIds(list);
			List<Train> objects = trainDAO.findByObjectId(objectIds);
			for (Train object : objects) {
				cache.getTrains().put(object.getObjectId(), object);
			}

			for (Train item : list) {
				Train object = cache.getTrains().get(item.getObjectId());
				if (object == null) {
					object = ObjectFactory.getTrain(cache, item.getObjectId());
				}
			}
		}
	}

}
