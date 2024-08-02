package mobi.chouette.dao;

import mobi.chouette.model.Network;

import java.util.List;

public interface NetworkDAO extends GenericDAO<Network> {

    List<Network> findByName(String name);

}
