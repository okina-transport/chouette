package mobi.chouette.exchange.importer.updater;

import mobi.chouette.dao.CompanyDAO;
import mobi.chouette.dao.FareRuleDAO;
import mobi.chouette.dao.LineDAO;
import mobi.chouette.dao.RouteDAO;
import mobi.chouette.model.Company;
import mobi.chouette.model.FareRule;
import mobi.chouette.model.Line;
import mobi.chouette.model.Route;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.Referential;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Stateless
public class FareRuleOptimiser {

	@EJB
	private RouteDAO routeDAO;

	@EJB
	private LineDAO lineDAO;

	@EJB
	private CompanyDAO companyDAO;

	@EJB
	private FareRuleDAO fareRuleDAO;

	public void initialize(Referential cache, Referential referential) {
//		initializeRoute(cache, referential.getRoutes().values());
		initializeCompany(cache, referential.getCompanies().values());
		initializeLine(cache, referential.getLines().values());
		initializeFareRule(cache, referential.getFareRules().values());
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

	private void initializeRoute(Referential cache, Collection<Route> list) {
		if (list != null && !list.isEmpty()) {
			List<String> ids = list.stream()
					.filter(route -> route.getObjectId() != null)
					.map(route -> route.getObjectId())
					.collect(Collectors.toList());

			List<Route> objects = routeDAO.findByObjectId(ids);
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

	private void initializeLine(Referential cache, Collection<Line> list) {
		if (list != null && !list.isEmpty()) {
			List<String> ids = list.stream()
					.filter(line -> line.getObjectId() != null)
					.map(line -> line.getObjectId())
					.collect(Collectors.toList());

			List<Line> objects = lineDAO.findByObjectId(ids);
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

	private void initializeFareRule(Referential cache, Collection<FareRule> list) {
		if (list != null && !list.isEmpty()) {
			List<String> ids = list.stream()
					.filter(agency -> agency.getObjectId() != null)
					.map(agency -> agency.getObjectId())
					.collect(Collectors.toList());

			List<FareRule> objects = fareRuleDAO.findByObjectId(ids);
			for (FareRule object : objects) {
				cache.getFareRules().put(object.getObjectId(), object);
			}

			for (FareRule item : list) {
				FareRule object = cache.getFareRules().get(item.getObjectId());
				if (object == null) {
					object = ObjectFactory.getFareRule(cache, item.getObjectId());
				}
			}
		}
	}
}
