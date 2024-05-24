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

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.util.Collection;
import java.util.List;

@Stateless(name = LineUpdater.BEAN_NAME)
public class LineUpdater implements Updater<Line> {

	public static final String BEAN_NAME = "LineUpdater";

	private boolean dataRouteIdfm;

	@EJB
	private NetworkDAO ptNetworkDAO;

	@EJB(beanName = PTNetworkUpdater.BEAN_NAME)
	private Updater<Network> ptNetworkUpdater;

	@EJB
	private CompanyDAO companyDAO;

	@EJB(beanName = CompanyUpdater.BEAN_NAME)
	private Updater<Company> companyUpdater;

	@EJB
	private GroupOfLineDAO groupOfLineDAO;

	@EJB(beanName = GroupOfLineUpdater.BEAN_NAME)
	private Updater<GroupOfLine> groupOfLineUpdater;

	@EJB
	private RouteDAO routeDAO;

	@EJB(beanName = RouteUpdater.BEAN_NAME)
	private Updater<Route> routeUpdater;

	@EJB
	private StopAreaDAO stopAreaDAO;

	@EJB(beanName = StopAreaUpdater.BEAN_NAME)
	private Updater<StopArea> stopAreaUpdater;

	@EJB
	private FootnoteDAO footnoteDAO;

	@EJB(beanName = FootnoteUpdater.BEAN_NAME)
	private Updater<Footnote> footnoteUpdater;

	@EJB(beanName = AccessibilityAssessmentUpdater.BEAN_NAME)
	private Updater<AccessibilityAssessment> accessibilityAssessmentUpdater;

	@EJB
	private AccessibilityAssessmentDAO accessibilityAssessmentDAO;

	@Override
	public void update(Context context, Line oldValue, Line newValue) throws Exception {

		String dataRouteIdfmProperty = "iev.data.route.idfm";
		dataRouteIdfm = Boolean.parseBoolean(System.getProperty(dataRouteIdfmProperty));

		if (newValue.isSaved()) {
			return;
		}
		newValue.setSaved(true);
//		Monitor monitor = MonitorFactory.start(BEAN_NAME);
		Referential cache = (Referential) context.get(CACHE);
		
		// Database test init
		ValidationReporter validationReporter = ValidationReporter.Factory.getInstance();
		validationReporter.addItemToValidationReport(context, "2-DATABASE-", "Line", 2, "W", "W");
		validationReporter.addItemToValidationReport(context, DATABASE_ROUTE_1, "E");
		ValidationData data = (ValidationData) context.get(VALIDATION_DATA);
		
		if (oldValue.isDetached()) {
			// object does not exist in database
			oldValue.setObjectId(newValue.getObjectId());
			oldValue.setObjectVersion(newValue.getObjectVersion());
			oldValue.setCreationTime(newValue.getCreationTime());
			oldValue.setCreatorId(newValue.getCreatorId());
			oldValue.setName(newValue.getName());
			oldValue.setComment(newValue.getComment());
			oldValue.setNumber(newValue.getNumber());
			oldValue.setPublishedName(newValue.getPublishedName());
			oldValue.setRegistrationNumber(newValue.getRegistrationNumber());
			oldValue.setTransportModeName(newValue.getTransportModeName());
			oldValue.setTransportSubModeName(newValue.getTransportSubModeName());
			oldValue.setIntUserNeeds(newValue.getIntUserNeeds());
			oldValue.setUrl(newValue.getUrl());
			oldValue.setColor(newValue.getColor());
			oldValue.setTextColor(newValue.getTextColor());
			oldValue.setKeyValues(newValue.getKeyValues());
			oldValue.setFlexibleService(newValue.getFlexibleService());
			oldValue.setFlexibleLineProperties(newValue.getFlexibleLineProperties());
			oldValue.setPosition(newValue.getPosition());
			oldValue.setDetached(false);
		} else {
			twoDatabaseLineOneTest(validationReporter, context, oldValue, newValue, data);
			twoDatabaseLineTwoTest(validationReporter, context, oldValue, newValue, data);

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
			if (newValue.getName() != null && !newValue.getName().equals(oldValue.getName()) && !dataRouteIdfm) {
				oldValue.setName(newValue.getName());
			}
			if (newValue.getComment() != null && !newValue.getComment().equals(oldValue.getComment()) && !dataRouteIdfm) {
				oldValue.setComment(newValue.getComment());
			}
			if (newValue.getNumber() != null && !newValue.getNumber().equals(oldValue.getNumber()) && !dataRouteIdfm) {
				oldValue.setNumber(newValue.getNumber());
			}
			if (newValue.getPublishedName() != null && !newValue.getPublishedName().equals(oldValue.getPublishedName()) && !dataRouteIdfm) {
				oldValue.setPublishedName(newValue.getPublishedName());
			}
			if (newValue.getRegistrationNumber() != null
					&& !newValue.getRegistrationNumber().equals(oldValue.getRegistrationNumber())) {
				oldValue.setRegistrationNumber(newValue.getRegistrationNumber());
			}
			if (newValue.getTransportSubModeName() != null
					&& !newValue.getTransportSubModeName().equals(oldValue.getTransportSubModeName())) {
				oldValue.setTransportSubModeName(newValue.getTransportSubModeName());
			}
			if (newValue.getIntUserNeeds() != null && !newValue.getIntUserNeeds().equals(oldValue.getIntUserNeeds())) {
				oldValue.setIntUserNeeds(newValue.getIntUserNeeds());
			}
			if (newValue.getUrl() != null && !newValue.getUrl().equals(oldValue.getUrl()) && !dataRouteIdfm) {
				oldValue.setUrl(newValue.getUrl());
			}
			if (newValue.getColor() != null && !newValue.getColor().equals(oldValue.getColor()) && !dataRouteIdfm) {
				oldValue.setColor(newValue.getColor());
			}
			if (newValue.getTextColor() != null && !newValue.getTextColor().equals(oldValue.getTextColor()) && !dataRouteIdfm) {
				oldValue.setTextColor(newValue.getTextColor());
			}
			if (newValue.getKeyValues() != null && !newValue.getKeyValues().equals(oldValue.getKeyValues())) {
				oldValue.setKeyValues(newValue.getKeyValues());
			}
			if (newValue.getFlexibleService() != null && !newValue.getFlexibleService().equals(oldValue.getFlexibleService())) {
				oldValue.setFlexibleService(newValue.getFlexibleService());
			}
			if (newValue.getFlexibleLineProperties() != null && !newValue.getFlexibleLineProperties().equals(oldValue.getFlexibleLineProperties())) {
				oldValue.setFlexibleLineProperties(newValue.getFlexibleLineProperties());
			}
			if (newValue.getPosition() != null && !newValue.getPosition().equals(oldValue.getPosition())) {
				oldValue.setPosition(newValue.getPosition());
			}
		}

		// PTNetwork

		if (newValue.getNetwork() == null) {
			oldValue.setNetwork(null);
		} else {
			String objectId = newValue.getNetwork().getObjectId();
			Network ptNetwork = cache.getPtNetworks().get(objectId);
			if (ptNetwork == null) {
				ptNetwork = ptNetworkDAO.findByObjectId(objectId);
				if (ptNetwork != null) {
					cache.getPtNetworks().put(objectId, ptNetwork);
				}
			}
			if (ptNetwork == null) {
				ptNetwork = ObjectFactory.getPTNetwork(cache, objectId);
			}
			oldValue.setNetwork(ptNetwork);
			ptNetworkUpdater.update(context, oldValue.getNetwork(), newValue.getNetwork());
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

		// GroupOfLine
		Collection<GroupOfLine> addedGroupOfLine = CollectionUtil.substract(newValue.getGroupOfLines(),
				oldValue.getGroupOfLines(), NeptuneIdentifiedObjectComparator.INSTANCE);
		List<GroupOfLine> groupOfLines = null;
		for (GroupOfLine item : addedGroupOfLine) {
			GroupOfLine groupOfLine = cache.getGroupOfLines().get(item.getObjectId());
			if (groupOfLine == null) {
				if (groupOfLines == null) {
					groupOfLines = groupOfLineDAO.findByObjectId(UpdaterUtils.getObjectIds(addedGroupOfLine));
					for (GroupOfLine object : groupOfLines) {
						cache.getGroupOfLines().put(object.getObjectId(), object);
					}
				}
				groupOfLine = cache.getGroupOfLines().get(item.getObjectId());
			}
			if (groupOfLine == null) {
				groupOfLine = ObjectFactory.getGroupOfLine(cache, item.getObjectId());
			}
			groupOfLine.addLine(oldValue);
		}

		Collection<Pair<GroupOfLine, GroupOfLine>> modifiedGroupOfLine = CollectionUtil.intersection(
				oldValue.getGroupOfLines(), newValue.getGroupOfLines(), NeptuneIdentifiedObjectComparator.INSTANCE);
		for (Pair<GroupOfLine, GroupOfLine> pair : modifiedGroupOfLine) {
			groupOfLineUpdater.update(context, pair.getLeft(), pair.getRight());
		}

		Collection<GroupOfLine> removedGroupOfLine = CollectionUtil.substract(oldValue.getGroupOfLines(),
				newValue.getGroupOfLines(), NeptuneIdentifiedObjectComparator.INSTANCE);
		for (GroupOfLine groupOfLine : removedGroupOfLine) {
			groupOfLine.removeLine(oldValue);
		}

		// Route
		Collection<Route> addedRoute = CollectionUtil.substract(newValue.getRoutes(), oldValue.getRoutes(),
				NeptuneIdentifiedObjectComparator.INSTANCE);
		List<Route> routes = null;
		for (Route item : addedRoute) {
			Route route = cache.getRoutes().get(item.getObjectId());
			if (route == null) {
				if (routes == null) {
					routes = routeDAO.findByObjectId(UpdaterUtils.getObjectIds(addedRoute));
					for (Route object : routes) {
						cache.getRoutes().put(object.getObjectId(), object);
					}
				}
				route = cache.getRoutes().get(item.getObjectId());
			}
			if (route == null) {
				route = ObjectFactory.getRoute(cache, item.getObjectId());
			}
			// If new route doesn't belong to line, we add temporarly it to the line and check if old route has same line as new route
			if(route.getLine() != null) {
				twoDatabaseRouteOneTest(validationReporter, context, route, item, data);
			} else {
				route.setLine(oldValue);
			}

		}

		Collection<Pair<Route, Route>> modifiedRoute = CollectionUtil.intersection(oldValue.getRoutes(),
				newValue.getRoutes(), NeptuneIdentifiedObjectComparator.INSTANCE);
		for (Pair<Route, Route> pair : modifiedRoute) {
			routeUpdater.update(context, pair.getLeft(), pair.getRight());
		}

		// TODO stop area list (routingConstraintLines)
		Collection<StopArea> addedRoutingConstraint = CollectionUtil.substract(newValue.getRoutingConstraints(),
				oldValue.getRoutingConstraints(), NeptuneIdentifiedObjectComparator.INSTANCE);
		List<StopArea> routingConstraints = null;
		for (StopArea item : addedRoutingConstraint) {
			StopArea routingConstraint = cache.getStopAreas().get(item.getObjectId());
			if (routingConstraint == null) {
				if (routingConstraints == null) {
					routingConstraints = stopAreaDAO.findByObjectId(UpdaterUtils.getObjectIds(addedRoutingConstraint));
					for (StopArea object : routingConstraints) {
						cache.getStopAreas().put(object.getObjectId(), object);
					}
				}
				routingConstraint = cache.getStopAreas().get(item.getObjectId());
			}
			if (routingConstraint == null) {
				routingConstraint = ObjectFactory.getStopArea(cache, item.getObjectId());
			}
			oldValue.addRoutingConstraint(routingConstraint);
		}

		Collection<Pair<StopArea, StopArea>> modifiedRoutingConstraint = CollectionUtil.intersection(
				oldValue.getRoutingConstraints(), newValue.getRoutingConstraints(),
				NeptuneIdentifiedObjectComparator.INSTANCE);
		for (Pair<StopArea, StopArea> pair : modifiedRoutingConstraint) {
			stopAreaUpdater.update(context, pair.getLeft(), pair.getRight());
		}

		Collection<StopArea> removedRoutingConstraint = CollectionUtil.substract(oldValue.getRoutingConstraints(),
				newValue.getRoutingConstraints(), NeptuneIdentifiedObjectComparator.INSTANCE);
		for (StopArea stopArea : removedRoutingConstraint) {
			oldValue.removeRoutingConstraint(stopArea);
		}

		updateFootnotes(context, oldValue,newValue,cache);
		updateAccessibilityAssessment(context, cache, oldValue, newValue);

//		monitor.stop();
	}

	private void updateAccessibilityAssessment(Context context, Referential cache, Line oldValue, Line newValue) throws Exception {
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
			oldValue.setAccessibilityAssessment(accessibilityAssessment);
			accessibilityAssessmentUpdater.update(context, oldValue.getAccessibilityAssessment(), newValue.getAccessibilityAssessment());
		}
	}

	private void updateFootnotes(Context context, Line oldValue, Line newValue, Referential cache) throws Exception {
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

	/**
	 * Test 2-Line-1
	 * @param validationReporter
	 * @param context
	 * @param oldLine
	 * @param newLine
	 */
	private void twoDatabaseLineOneTest(ValidationReporter validationReporter, Context context, Line oldLine, Line newLine, ValidationData data) {
		if(!NeptuneUtil.sameValue(oldLine.getNetwork(), newLine.getNetwork()))
			validationReporter.addCheckPointReportError(context, DATABASE_LINE_1, data.getDataLocations().get(newLine.getObjectId()));
		else
			validationReporter.reportSuccess(context, DATABASE_LINE_1);
	}

	/**
	 * Test 2-Line-2
	 * @param validationReporter
	 * @param context
	 * @param oldLine
	 * @param newLine
	 */
	private void twoDatabaseLineTwoTest(ValidationReporter validationReporter, Context context, Line oldLine, Line newLine, ValidationData data) {
		if(!NeptuneUtil.sameValue(oldLine.getCompany(), newLine.getCompany()))
			validationReporter.addCheckPointReportError(context, DATABASE_LINE_2, data.getDataLocations().get(newLine.getObjectId()));
		else
			validationReporter.reportSuccess(context, DATABASE_LINE_2);
	}

	/**
	 * Test 2-Route-1
	 * @param validationReporter
	 * @param context
	 * @param oldRoute
	 * @param newRoute
	 */
	private void twoDatabaseRouteOneTest(ValidationReporter validationReporter, Context context, Route oldRoute, Route newRoute, ValidationData data) {
		if(!NeptuneUtil.sameValue(oldRoute.getLine(), newRoute.getLine()))
			validationReporter.addCheckPointReportError(context, DATABASE_ROUTE_1, data.getDataLocations().get(newRoute.getObjectId()));
		else
			validationReporter.reportSuccess(context, DATABASE_ROUTE_1);
	}
}
