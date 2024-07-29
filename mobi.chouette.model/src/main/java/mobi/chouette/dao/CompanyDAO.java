package mobi.chouette.dao;

import mobi.chouette.model.Company;

import java.util.List;

public interface CompanyDAO extends GenericDAO<Company> {

        List<Company> findByNameAndActive(String name);
}
