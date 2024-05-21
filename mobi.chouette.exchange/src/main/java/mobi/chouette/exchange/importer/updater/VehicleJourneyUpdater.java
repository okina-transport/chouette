package mobi.chouette.exchange.importer.updater;

import mobi.chouette.common.CollectionUtil;
import mobi.chouette.common.Context;
import mobi.chouette.common.Pair;
import mobi.chouette.dao.*;
import mobi.chouette.exchange.validation.ValidationData;
import mobi.chouette.exchange.validation.report.ValidationReporter;
import mobi.chouette.model.*;
import mobi.chouette.model.util.NeptuneUtil;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.Referential;
import org.apache.commons.beanutils.BeanUtils;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

@Stateless(name = VehicleJourneyUpdater.BEAN_NAME)
public class VehicleJourneyUpdater implements Updater<VehicleJourney> {

	public static final String BEAN_NAME = "VehicleJourneyUpdater";

	private boolean dataTripIdfm;

	private static final Comparator<VehicleJourneyAtStop> VEHICLE_JOURNEY_AT_STOP_COMPARATOR = new Comparator<VehicleJourneyAtStop>() {
		@Override
		public int compare(VehicleJourneyAtStop o1, VehicleJourneyAtStop o2) {
			int result = -1;
			if (o1.getStopPoint() != null && o2.getStopPoint() != null) {
				result = (o1.getStopPoint().equals(o2.getStopPoint())) ? 0 : -1;
			}
			return result;
		}
	};

	private static final Comparator<JourneyFrequency> JOURNEY_FREQUENCY_COMPARATOR = new Comparator<JourneyFrequency>() {
		@Override
		public int compare(JourneyFrequency o1, JourneyFrequency o2) {
			int result = 1;
			if(o2.getObjectId() != null && (o1.getObjectId() == null || o2.getObjectId().equals(o1.getObjectId()))) {
				result = 0;
			}
			return result;
		}
	};

	@EJB(beanName = CompanyUpdater.BEAN_NAME)
	private Updater<Company> companyUpdater;

	@EJB
	private CompanyDAO companyDAO;

	@EJB
	private RouteDAO routeDAO;

	@EJB
	private StopPointDAO stopPointDAO;

	@EJB
	private VehicleJourneyAtStopDAO vehicleJourneyAtStopDAO;

	@EJB
	private TimetableDAO timetableDAO;

	@EJB
	private TimebandDAO timebandDAO;

	@EJB
	private JourneyFrequencyDAO journeyFrequencyDAO;

	@EJB
	private LineDAO lineDAO;

	@EJB
	private InterchangeDAO interchangeDAO;

	@EJB(beanName = TimetableUpdater.BEAN_NAME)
	private Updater<Timetable> timetableUpdater;

	@EJB(beanName = VehicleJourneyAtStopUpdater.BEAN_NAME)
	private Updater<VehicleJourneyAtStop> vehicleJourneyAtStopUpdater;

	@EJB(beanName = JourneyFrequencyUpdater.BEAN_NAME)
	private Updater<JourneyFrequency> journeyFrequencyUpdater;

	@EJB
	private FootnoteDAO footnoteDAO;

	@EJB(beanName = FootnoteUpdater.BEAN_NAME)
	private Updater<Footnote> footnoteUpdater;

	@EJB(beanName = InterchangeUpdater.BEAN_NAME)
	private Updater<Interchange> interchangeUpdater;

	@EJB(beanName = AccessibilityAssessmentUpdater.BEAN_NAME)
	private Updater<AccessibilityAssessment> accessibilityAssessmentUpdater;


	@EJB(beanName = TrainUpdater.BEAN_NAME)
	private Updater<Train> trainUpdater;

	@EJB
	private AccessibilityAssessmentDAO accessibilityAssessmentDAO;


	@Override
	public void update(Context context, VehicleJourney oldValue, VehicleJourney newValue) throws Exception {

		String dataTripIdfmProperty = "iev.data.trip.idfm";
		dataTripIdfm = Boolean.parseBoolean(System.getProperty(dataTripIdfmProperty));

		if (newValue.isSaved()) {
			return;
		}
		newValue.setSaved(true);

//		Monitor monitor = MonitorFactory.start(BEAN_NAME);
		Referential cache = (Referential) context.get(CACHE);
		cache.getVehicleJourneys().put(oldValue.getObjectId(), oldValue);

		boolean optimized = (Boolean) context.get(OPTIMIZED);

		// Database test init
		ValidationReporter validationReporter = ValidationReporter.Factory.getInstance();
		validationReporter.addItemToValidationReport(context, DATABASE_VEHICLE_JOURNEY_2, "W");
		ValidationData data = (ValidationData) context.get(VALIDATION_DATA);


		if (oldValue.isDetached()) {
			// object does not exist in database
			oldValue.setObjectId(newValue.getObjectId());
			oldValue.setObjectVersion(newValue.getObjectVersion());
			oldValue.setCreationTime(newValue.getCreationTime());
			oldValue.setCreatorId(newValue.getCreatorId());
			oldValue.setComment(newValue.getComment());
			oldValue.setTransportMode(newValue.getTransportMode());
			oldValue.setTransportSubMode(newValue.getTransportSubMode());
			oldValue.setPrivateCode(newValue.getPrivateCode());
			oldValue.setPublishedJourneyName(newValue.getPublishedJourneyName());
			oldValue.setPublishedJourneyIdentifier(newValue.getPublishedJourneyIdentifier());
			oldValue.setFacility(newValue.getFacility());
			oldValue.setVehicleTypeIdentifier(newValue.getVehicleTypeIdentifier());
			oldValue.setNumber(newValue.getNumber());
			oldValue.setBikesAllowed(newValue.getBikesAllowed());
			oldValue.setFlexibleService(newValue.getFlexibleService());
			oldValue.setJourneyCategory(newValue.getJourneyCategory());
			oldValue.setKeyValues(newValue.getKeyValues());
			oldValue.setServiceAlteration(newValue.getServiceAlteration());
			oldValue.setFlexibleServiceProperties(newValue.getFlexibleServiceProperties());
			oldValue.setDetached(false);
		} else {
			twoDatabaseVehicleJourneyTwoTest(validationReporter, context, oldValue.getCompany(), newValue.getCompany(), data);
			if (newValue.getObjectId() != null && !newValue.getObjectId().equals(oldValue.getObjectId())) {
				oldValue.setObjectId(newValue.getObjectId());
			}
			if (newValue.getObjectVersion() != null && !newValue.getObjectVersion().equals(oldValue.getObjectVersion())) {
				oldValue.setObjectVersion(newValue.getObjectVersion());
			}
			if (newValue.getCreationTime() != null && !newValue.getCreationTime().equals(oldValue.getCreationTime())) {
				oldValue.setCreationTime(newValue.getCreationTime());
			}
			if (newValue.getCreatorId() != null && !newValue.getCreatorId().equals(oldValue.getCreatorId())) {
				oldValue.setCreatorId(newValue.getCreatorId());
			}
			if (newValue.getComment() != null && !newValue.getComment().equals(oldValue.getComment())) {
				oldValue.setComment(newValue.getComment());
			}
			if (newValue.getTransportMode() != null && !newValue.getTransportMode().equals(oldValue.getTransportMode())) {
				oldValue.setTransportMode(newValue.getTransportMode());
			}
			if (newValue.getTransportSubMode() != null && !newValue.getTransportSubMode().equals(oldValue.getTransportMode())) {
				oldValue.setTransportSubMode(newValue.getTransportSubMode());
			}
			if (newValue.getPrivateCode() != null && !newValue.getPrivateCode().equals(oldValue.getPrivateCode())) {
				oldValue.setPrivateCode(newValue.getPrivateCode());
			}
			if (newValue.getPublishedJourneyName() != null
					&& !newValue.getPublishedJourneyName().equals(oldValue.getPublishedJourneyName()) && !dataTripIdfm) {
				oldValue.setPublishedJourneyName(newValue.getPublishedJourneyName());
			}
			if (newValue.getPublishedJourneyIdentifier() != null
					&& !newValue.getPublishedJourneyIdentifier().equals(oldValue.getPublishedJourneyIdentifier())) {
				oldValue.setPublishedJourneyIdentifier(newValue.getPublishedJourneyIdentifier());
			}
			if (newValue.getFacility() != null && !newValue.getFacility().equals(oldValue.getFacility())) {
				oldValue.setFacility(newValue.getFacility());
			}
			if (newValue.getVehicleTypeIdentifier() != null
					&& !newValue.getVehicleTypeIdentifier().equals(oldValue.getVehicleTypeIdentifier())) {
				oldValue.setVehicleTypeIdentifier(newValue.getVehicleTypeIdentifier());
			}
			if (newValue.getNumber() != null && !newValue.getNumber().equals(oldValue.getNumber())) {
				oldValue.setNumber(newValue.getNumber());
			}
			if (newValue.getBikesAllowed() != null
					&& !newValue.getBikesAllowed().equals(oldValue.getBikesAllowed()) && !dataTripIdfm) {
				oldValue.setBikesAllowed(newValue.getBikesAllowed());
			}
			if (newValue.getFlexibleService() != null
					&& !newValue.getFlexibleService().equals(oldValue.getFlexibleService())) {
				oldValue.setFlexibleService(newValue.getFlexibleService());
			}
			if (newValue.getJourneyCategory() != null
					&& !newValue.getJourneyCategory().equals(oldValue.getJourneyCategory())) {
				oldValue.setJourneyCategory(newValue.getJourneyCategory());
			}
			if (newValue.getKeyValues() != null && !newValue.getKeyValues().equals(oldValue.getKeyValues())) {
				oldValue.setKeyValues(newValue.getKeyValues());
			}
			if (newValue.getServiceAlteration() != null && !newValue.getServiceAlteration().equals(oldValue.getServiceAlteration())) {
				oldValue.setServiceAlteration(newValue.getServiceAlteration());
			}
			if (newValue.getFlexibleServiceProperties() != null && !newValue.getFlexibleServiceProperties().equals(oldValue.getFlexibleServiceProperties())) {
				oldValue.setFlexibleServiceProperties(newValue.getFlexibleServiceProperties());
			}
		}

		// Company
		if (newValue.getCompany() == null) {
			oldValue.setCompany(null);
		} else {
			String objectId = newValue.getCompany().getObjectId();
			Company company = cache.getCompanies().get(objectId);
			if (company == null) {
				company = companyDAO.findByObjectId(objectId);
				if (company != null) {
					cache.getCompanies().put(objectId, company);
				}
			}

			if (company == null) {
				company = ObjectFactory.getCompany(cache, objectId);
			}
			oldValue.setCompany(company);
			companyUpdater.update(context, oldValue.getCompany(), newValue.getCompany());
		}

		// Route
		if (oldValue.getRoute() == null || !oldValue.getRoute().equals(newValue.getRoute())) {

			String objectId = newValue.getRoute().getObjectId();
			Route route = cache.getRoutes().get(objectId);
			if (route == null) {
				route = routeDAO.findByObjectId(objectId);
				if (route != null) {
					cache.getRoutes().put(objectId, route);
				}
			}

			if (route != null) {
				oldValue.setRoute(route);
			}
		}

		// VehicleJourneyAtStop
		if (!optimized) {

			Collection<VehicleJourneyAtStop> addedVehicleJourneyAtStop = CollectionUtil.substract(
					newValue.getVehicleJourneyAtStops(), oldValue.getVehicleJourneyAtStops(),
					VEHICLE_JOURNEY_AT_STOP_COMPARATOR);

			final Collection<String> objectIds = new ArrayList<String>();
			for (VehicleJourneyAtStop vehicleJourneyAtStop : addedVehicleJourneyAtStop) {
				objectIds.add(vehicleJourneyAtStop.getStopPoint().getObjectId());
			}
			List<StopPoint> stopPoints = null;
			for (VehicleJourneyAtStop item : addedVehicleJourneyAtStop) {
				VehicleJourneyAtStop vehicleJourneyAtStop = ObjectFactory.getVehicleJourneyAtStop(cache,item.getObjectId());

				StopPoint stopPoint = cache.getStopPoints().get(item.getStopPoint().getObjectId());
				if (stopPoint == null) {
					if (stopPoints == null) {
						stopPoints = stopPointDAO.findByObjectId(objectIds);
						for (StopPoint object : stopPoints) {
							cache.getStopPoints().put(object.getObjectId(), object);
						}
					}
					stopPoint = cache.getStopPoints().get(item.getStopPoint().getObjectId());
				}

				if (stopPoint != null) {
					vehicleJourneyAtStop.setStopPoint(stopPoint);
				}
				vehicleJourneyAtStop.setVehicleJourney(oldValue);
			}

			Collection<Pair<VehicleJourneyAtStop, VehicleJourneyAtStop>> modifiedVehicleJourneyAtStop = CollectionUtil
					.intersection(oldValue.getVehicleJourneyAtStops(), newValue.getVehicleJourneyAtStops(),
							VEHICLE_JOURNEY_AT_STOP_COMPARATOR);
			for (Pair<VehicleJourneyAtStop, VehicleJourneyAtStop> pair : modifiedVehicleJourneyAtStop) {
				vehicleJourneyAtStopUpdater.update(context, pair.getLeft(), pair.getRight());
			}

			Collection<VehicleJourneyAtStop> removedVehicleJourneyAtStop = CollectionUtil.substract(
					oldValue.getVehicleJourneyAtStops(), newValue.getVehicleJourneyAtStops(),
					VEHICLE_JOURNEY_AT_STOP_COMPARATOR);
			for (VehicleJourneyAtStop vehicleJourneyAtStop : removedVehicleJourneyAtStop) {
				vehicleJourneyAtStop.setVehicleJourney(null);
				vehicleJourneyAtStopDAO.delete(vehicleJourneyAtStop);
			}
		}

		// Timetable
		Collection<Timetable> addedTimetable = CollectionUtil.substract(newValue.getTimetables(),
				oldValue.getTimetables(), NeptuneIdentifiedObjectComparator.INSTANCE);

		List<Timetable> timetables = null;
		for (Timetable item : addedTimetable) {

			Timetable timetable = cache.getTimetables().get(item.getObjectId());
			if (timetable == null) {
				if (timetables == null) {
					timetables = timetableDAO.findByObjectId(UpdaterUtils.getObjectIds(addedTimetable));
					for (Timetable object : timetables) {
						cache.getTimetables().put(object.getObjectId(), object);
					}
				}
				timetable = cache.getTimetables().get(item.getObjectId());
			}

			if (timetable == null) {
				timetable = ObjectFactory.getTimetable(cache, item.getObjectId());
			}
			timetable.addVehicleJourney(oldValue);
		}

		Collection<Pair<Timetable, Timetable>> modifiedTimetable = CollectionUtil.intersection(
				oldValue.getTimetables(), newValue.getTimetables(), NeptuneIdentifiedObjectComparator.INSTANCE);
		for (Pair<Timetable, Timetable> pair : modifiedTimetable) {
			timetableUpdater.update(context, pair.getLeft(), pair.getRight());
		}


		// journey frequency
		/* if (!optimized) */{
			Collection<JourneyFrequency> addedJourneyFrequency = CollectionUtil.substract(
					newValue.getJourneyFrequencies(), oldValue.getJourneyFrequencies(), JOURNEY_FREQUENCY_COMPARATOR);
			final Collection<String> objectIds = new ArrayList<String>();
			for (JourneyFrequency journeyFrequency : addedJourneyFrequency) {
				if(journeyFrequency.getTimeband() != null)
					objectIds.add(journeyFrequency.getTimeband().getObjectId());

			}
			List<Timeband> timebands = null;
			for (JourneyFrequency item : addedJourneyFrequency) {
				JourneyFrequency journeyFrequency = new JourneyFrequency();
				if(cache.getTimebands() != null && !cache.getTimebands().isEmpty()
						&& item.getTimeband() != null){
					Timeband timeband = cache.getTimebands().get(item.getTimeband().getObjectId());
					if (timeband == null) {
						if (timebands == null) {
							timebands = timebandDAO.findByObjectId(objectIds);
							for (Timeband object : timebands) {
								cache.getTimebands().put(object.getObjectId(), object);
							}
						}
						timeband = cache.getTimebands().get(item.getTimeband().getObjectId());
					}
					if (timeband != null) {
						journeyFrequency.setTimeband(timeband);
					}
				}
				journeyFrequency.setObjectId(item.getObjectId() != null ? item.getObjectId() : createJFObjectId(oldValue));
				journeyFrequency.setVehicleJourney(oldValue);
			}

			Collection<Pair<JourneyFrequency, JourneyFrequency>> modifiedJourneyFrequency = CollectionUtil
					.intersection(oldValue.getJourneyFrequencies(), newValue.getJourneyFrequencies(),
							JOURNEY_FREQUENCY_COMPARATOR);
			for (Pair<JourneyFrequency, JourneyFrequency> pair : modifiedJourneyFrequency) {
				journeyFrequencyUpdater.update(context, pair.getLeft(), pair.getRight());
			}

			Collection<JourneyFrequency> removedJourneyFrequency = CollectionUtil.substract(
					oldValue.getJourneyFrequencies(), newValue.getJourneyFrequencies(), JOURNEY_FREQUENCY_COMPARATOR);
			for (JourneyFrequency journeyFrequency : removedJourneyFrequency) {
				journeyFrequency.setVehicleJourney(null);
				journeyFrequencyDAO.delete(journeyFrequency);
			}
		}

		updateFootnotes(context, oldValue, newValue, cache);
		updateInterchanges(context, oldValue, newValue);
		updateAccessibilityAssessment(context, cache, oldValue, newValue);
		updateTrains(context, oldValue, newValue);
//		monitor.stop();
	}

	private void updateAccessibilityAssessment(Context context, Referential cache, VehicleJourney oldValue, VehicleJourney newValue) throws Exception {
		// Accessibility assessment
		if (newValue.getAccessibilityAssessment() == null) {
			oldValue.setAccessibilityAssessment(null);
		} else {
			String objectId = newValue.getAccessibilityAssessment().getObjectId();
			AccessibilityAssessment accessibilityAssessment = cache.getAccessibilityAssessments().get(objectId);
			if (accessibilityAssessment == null) {
				accessibilityAssessment = accessibilityAssessmentDAO.findByObjectId(objectId);
				if (accessibilityAssessment != null) {
					cache.getAccessibilityAssessments().put(objectId, accessibilityAssessment);
				}
			}
			if (accessibilityAssessment == null) {
				accessibilityAssessment = ObjectFactory.getAccessibilityAssessment(cache, objectId);
			}
			accessibilityAssessmentUpdater.update(context, accessibilityAssessment, newValue.getAccessibilityAssessment());
		}
	}

	private String createJFObjectId(VehicleJourney oldValue) {
		return oldValue.getObjectId().trim().split(":")[0] + ":JourneyFrequency:" + oldValue.getObjectId().trim().split(":")[2];
	}

	private void updateFootnotes(Context context, VehicleJourney oldValue, VehicleJourney newValue, Referential cache) throws Exception {
		Collection<Footnote> addedFootnote = CollectionUtil.substract(newValue.getFootnotes(),
				oldValue.getFootnotes(), NeptuneIdentifiedObjectComparator.INSTANCE);
		List<Footnote> footnotes = null;
		for (Footnote item : addedFootnote) {
			Footnote footnote = cache.getFootnotes().get(item.getObjectId());
			if (footnote == null) {
				if (footnotes == null) {
					footnotes = footnoteDAO.findByObjectId(UpdaterUtils.getObjectIds(addedFootnote));
					for (Footnote object : footnotes) {
						cache.getFootnotes().put(object.getObjectId(), object);
					}
				}
				footnote = cache.getFootnotes().get(item.getObjectId());
			}
			if (footnote == null) {
				footnote = ObjectFactory.getFootnote(cache, item.getObjectId());
			}
			oldValue.getFootnotes().add(footnote);
		}

		Collection<Pair<Footnote, Footnote>> modifiedFootnote = CollectionUtil.intersection(
				oldValue.getFootnotes(), newValue.getFootnotes(),
				NeptuneIdentifiedObjectComparator.INSTANCE);
		for (Pair<Footnote, Footnote> pair : modifiedFootnote) {
			footnoteUpdater.update(context, pair.getLeft(), pair.getRight());
		}

		Collection<Footnote> removedFootnote = CollectionUtil.substract(oldValue.getFootnotes(),
				newValue.getFootnotes(), NeptuneIdentifiedObjectComparator.INSTANCE);
		for (Footnote Footnote : removedFootnote) {
			oldValue.getFootnotes().remove(Footnote);
		}
	}


	public void updateInterchanges(Context context, VehicleJourney oldValue, VehicleJourney newValue) throws Exception {
		updateInterchanges(context, oldValue, newValue, oldValue.getConsumerInterchanges(), newValue.getConsumerInterchanges(), "consumerVehicleJourney");
		updateInterchanges(context, oldValue, newValue, oldValue.getFeederInterchanges(), newValue.getFeederInterchanges(), "feederVehicleJourney");
	}

	private void updateInterchanges(Context context, VehicleJourney oldValue, VehicleJourney newValue, List<Interchange> oldValueInterchanges, List<Interchange> newValueInterchanges, String method) throws Exception {
		Referential cache = (Referential) context.get(CACHE);

		Collection<Interchange> addedInterchange = CollectionUtil.substract(newValueInterchanges,
				oldValueInterchanges, NeptuneIdentifiedObjectComparator.INSTANCE);

		List<Interchange> interchanges = null;
		for (Interchange item : addedInterchange) {

			Interchange interchange = cache.getInterchanges().get(item.getObjectId());
			if (interchange == null) {
				if (interchanges == null) {
					interchanges = interchangeDAO.findByObjectId(UpdaterUtils.getObjectIds(addedInterchange));
					for (Interchange object : interchanges) {
						cache.getInterchanges().put(object.getObjectId(), object);
					}
				}
				interchange = cache.getInterchanges().get(item.getObjectId());
			}

			if (interchange == null) {
				interchange = ObjectFactory.getInterchange(cache, item.getObjectId());
			}
			BeanUtils.setProperty(interchange, method, oldValue);
			oldValueInterchanges.add(interchange);
		}

		Collection<Pair<Interchange, Interchange>> modifiedInterchange = CollectionUtil.intersection(
				oldValueInterchanges, newValueInterchanges, NeptuneIdentifiedObjectComparator.INSTANCE);
		for (Pair<Interchange, Interchange> pair : modifiedInterchange) {
			interchangeUpdater.update(context, pair.getLeft(), pair.getRight());
		}

		Collection<Interchange> removedInterchange = CollectionUtil.substract(oldValueInterchanges,
				newValueInterchanges, NeptuneIdentifiedObjectComparator.INSTANCE);
		for (Interchange interchange : removedInterchange) {
			BeanUtils.setProperty(interchange, method, oldValue);
			oldValueInterchanges.remove(interchange);
		}

	}

	private void updateTrains(Context context, VehicleJourney oldValue, VehicleJourney newValue) throws Exception {
		Collection<Train> addedTrains = CollectionUtil.substract(newValue.getTrains(),
				oldValue.getTrains(), NeptuneIdentifiedObjectComparator.INSTANCE);

		for (Train train : addedTrains) {
			oldValue.getTrains().add(train);
		}

		Collection<Pair<Train, Train>> modifiedTrains = CollectionUtil.intersection(
				oldValue.getTrains(), newValue.getTrains(),
				NeptuneIdentifiedObjectComparator.INSTANCE);

		for (Pair<Train, Train> pair : modifiedTrains) {
			trainUpdater.update(context, pair.getLeft(), pair.getRight());
		}

		Collection<Train> removedTrains = CollectionUtil.substract(oldValue.getTrains(), newValue.getTrains(), NeptuneIdentifiedObjectComparator.INSTANCE);
		for (Train train : removedTrains) {
			oldValue.getTrains().remove(train);
		}

	}


	/**
	 * Test 2-DATABASE-VehicleJourney-2
	 * @param validationReporter
	 * @param context
	 * @param oldCompany
	 * @param newCompany
	 */
	private void twoDatabaseVehicleJourneyTwoTest(ValidationReporter validationReporter, Context context, Company oldCompany,  Company newCompany, ValidationData data) {
		if(!NeptuneUtil.sameValue(oldCompany, newCompany))
			validationReporter.addCheckPointReportError(context, DATABASE_VEHICLE_JOURNEY_2, data.getDataLocations().get(newCompany.getObjectId()));
		else
			validationReporter.reportSuccess(context, DATABASE_VEHICLE_JOURNEY_2);
	}
}
