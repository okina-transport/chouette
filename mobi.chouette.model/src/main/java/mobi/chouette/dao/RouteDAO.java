package mobi.chouette.dao;

import mobi.chouette.core.CoreException;
import mobi.chouette.dao.GenericDAO;
import mobi.chouette.model.Route;

public interface RouteDAO extends GenericDAO<Route> {

    String removeDeletedRoutes() throws CoreException;

}
