package mobi.chouette.dao;


import mobi.chouette.model.Attribution;


public interface AttributionDAO extends GenericDAO<Attribution> {

    void insertAttribution(Attribution attribution);

}
