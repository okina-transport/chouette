package mobi.chouette.exchange.importer.updater;

import mobi.chouette.dao.RouteDAO;
import mobi.chouette.dao.StopAreaDAO;
import mobi.chouette.dao.TransfersDAO;
import mobi.chouette.model.Route;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.Transfers;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.Referential;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Stateless
public class TransfersOptimiser {

    @EJB
    private RouteDAO routeDAO;

    @EJB
    private StopAreaDAO stopAreaDAO;

    @EJB
    private TransfersDAO transfersDAO;

    public void initialize(Referential cache, Referential referential) {
        initializeRoute(cache, referential.getRoutes().values());
        initializeStopAreas(cache, referential.getStopAreas().values());
        initializeTransfers(cache, referential.getTransfers().values());
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

    private void initializeStopAreas(Referential cache, Collection<StopArea> list) {
        if (list != null && !list.isEmpty()) {
            List<String> ids = list.stream()
                    .filter(stop -> stop.getObjectId() != null)
                    .map(stop -> stop.getObjectId())
                    .collect(Collectors.toList());

            List<StopArea> objects = stopAreaDAO.findByObjectId(ids);
            for (StopArea object : objects) {
                cache.getStopAreas().put(object.getObjectId(), object);
            }

            for (StopArea item : list) {
                StopArea object = cache.getStopAreas().get(item.getObjectId());
                if (object == null) {
                    object = ObjectFactory.getStopArea(cache, item.getObjectId());
                }
            }
        }
    }

    private void initializeTransfers(Referential cache, Collection<Transfers> list) {
        if (list != null && !list.isEmpty()) {
            List<String> ids = list.stream()
                    .filter(agency -> agency.getObjectId() != null)
                    .map(agency -> agency.getObjectId())
                    .collect(Collectors.toList());

            List<Transfers> objects = transfersDAO.findByObjectId(ids);
            for (Transfers object : objects) {
                cache.getTransfers().put(object.getObjectId(), object);
            }

            for (Transfers item : list) {
                Transfers object = cache.getTransfers().get(item.getObjectId());
                if (object == null) {
                    object = ObjectFactory.getTransfers(cache, item.getObjectId());
                }
            }
        }
    }
}
