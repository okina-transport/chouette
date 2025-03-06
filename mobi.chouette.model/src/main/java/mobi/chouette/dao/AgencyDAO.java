package mobi.chouette.dao;

import mobi.chouette.model.Agency;

import java.util.List;


public interface AgencyDAO extends GenericDAO<Agency> {

    List<Agency> findByAgencyIds(List<String> agencyIds);

    Agency findByAgencyId(String agencyIds);
}
