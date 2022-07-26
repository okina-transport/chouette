package mobi.chouette.dao;


import mobi.chouette.model.Route;
import mobi.chouette.model.type.PTDirectionEnum;

import java.util.List;

public interface RouteDAO extends GenericDAO<Route> {

    List<Route> findByLineIdAndDirection(Long lineId, PTDirectionEnum direction);

}
