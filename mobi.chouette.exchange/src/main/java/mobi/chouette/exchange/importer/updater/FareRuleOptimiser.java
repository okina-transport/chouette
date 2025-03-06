package mobi.chouette.exchange.importer.updater;

import mobi.chouette.dao.FareRuleDAO;
import mobi.chouette.dao.RouteDAO;
import mobi.chouette.model.FareRule;
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
    private FareRuleDAO fareRuleDAO;

    public void initialize(Referential cache, Referential referential) {
        initializeRoute(cache, referential.getRoutes().values());
        initializeFareRule(cache, referential.getFareRules().values());
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
